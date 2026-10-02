package com.d4viddf.hyperbridge.service.translators

import android.content.Context
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.data.widget.CustomWidgetRepository
import com.d4viddf.hyperbridge.data.widget.DeviceVariables
import com.d4viddf.hyperbridge.data.widget.SourceRepository
import com.d4viddf.hyperbridge.data.widget.VariableContext
import com.d4viddf.hyperbridge.data.widget.WidgetVariableEngine
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.service.smartactions.SmartActionIntents
import com.d4viddf.hyperbridge.service.smartactions.SmartActionNotificationText
import com.d4viddf.hyperbridge.service.smartactions.SmartActionsExtractor
import com.d4viddf.hyperbridge.service.widget.CustomWidgetRenderer
import com.d4viddf.hyperbridge.service.widget.WidgetActionIntents
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import kotlinx.coroutines.runBlocking

/**
 * Renders a custom design (Phase 5, #273): a translator whose presentation mode is WIDGET carries
 * a `widgetId`, and the island body becomes that Studio document rendered as a RemoteViews tree.
 *
 * Which notifications reach it is decided by Phase 3's `TranslatorRegistry` like every other
 * translator, so there is no package-keyed binding lookup on the hot path any more.
 */
class CustomWidgetTranslator(
    context: Context,
    repository: ThemeRepository? = null
) : BaseTranslator(context, repository) {

    private val widgetRepo = CustomWidgetRepository(context)
    private val sourceRepo = SourceRepository(context)
    private val renderer = CustomWidgetRenderer(context, WidgetVariableEngine(), widgetRepo)

    suspend fun translate(
        sbn: StatusBarNotification,
        picKey: String,
        effectiveTitle: String,
        effectiveText: String,
        config: IslandConfig,
        theme: HyperTheme?,
        widgetId: String
    ): HyperIslandData {
        val doc = requireNotNull(widgetRepo.getWidget(widgetId)) {
            "Custom design $widgetId is missing from the Studio store"
        }

        val progress = sbn.notification.extras.getInt("android.progress", -1).takeIf { it >= 0 }

        // What the notification itself offers: its buttons, whether one of them replies inline,
        // and any Smart Action (#270) found in its text. Conditional nodes are evaluated against
        // these facts, and a node can fire the matching intent (#328).
        val actions = sbn.notification.actions?.toList().orEmpty()
        val replyAction = actions.firstOrNull { !it.remoteInputs.isNullOrEmpty() }
        val smartActions = extractSmartActions(sbn, config)

        val ctx = VariableContext(
            notifTitle = effectiveTitle,
            notifText = effectiveText,
            notifProgress = progress,
            notifPackage = sbn.packageName,
            deviceBatteryPercent = DeviceVariables.batteryPercent(context),
            timeNowFormatted = DeviceVariables.timeNow(),
            notificationActionTitles = actions.map { it.title?.toString().orEmpty() },
            hasInlineReply = replyAction != null,
            smartActionTypes = smartActions.keys,
            sourceLookup = { id, field -> runBlocking { sourceRepo.lookup(id, field) } }
        )

        val intents = WidgetActionIntents(
            notificationActions = actions.map { it.actionIntent },
            inlineReply = replyAction?.actionIntent,
            smartActions = smartActions
        )

        val rv = renderer.render(doc, ctx, intents = intents)

        val builder = HyperIslandNotification.Builder(context, "custom_widget_channel", effectiveTitle)
        // Collapsed pill: the source app's icon (same recipe as WidgetTranslator). Without a big
        // island info block HyperOS shows an anonymous pill that never expands.
        val iconBitmap = try {
            context.packageManager.getApplicationIcon(sbn.packageName).toBitmap()
        } catch (_: Exception) {
            null
        }
        if (iconBitmap != null) {
            builder.addPicture(HyperPicture(picKey, Icon.createWithBitmap(iconBitmap)))
            builder.setBigIslandInfo(left = ImageTextInfoLeft(picInfo = PicInfo(pic = picKey)))
            builder.setSmallIsland(picKey)
        } else {
            builder.setBigIslandInfo(left = ImageTextInfoLeft(picInfo = PicInfo(pic = "default_icon")))
            builder.setSmallIsland("default_icon")
            builder.addPicture(getTransparentPicture("default_icon"))
        }
        // Expanded island: the rendered micro-widget. HyperOS reads the expanded content from the
        // island-expand slot, not from the notification's custom view, so both must be set
        // (verified on device: with setCustomRemoteView alone the pill shows but never expands).
        builder.setCustomRemoteView(rv)
        builder.setCustomIslandExpandRemoteView(rv)
        builder.setIslandConfig(timeout = config.timeout, dismissible = true)
        builder.setEnableFloat(config.isFloat == true)
        builder.setShowNotification(config.isShowShade == true)
        builder.setReopen(true)

        return HyperIslandData(builder.buildCustomExtras(), builder.buildJsonParam())
    }

    /**
     * Smart Actions detected on this notification, keyed by type name. Returns nothing -- and
     * reads no notification text -- when the user has the feature off, matching BaseTranslator.
     */
    private fun extractSmartActions(
        sbn: StatusBarNotification,
        config: IslandConfig
    ): Map<String, android.app.PendingIntent> {
        val smartConfig = config.smartActions ?: return emptyMap()
        if (!smartConfig.isActiveFor(sbn.packageName)) return emptyMap()

        val text = SmartActionNotificationText.collect(sbn.notification)
        return SmartActionsExtractor.extract(text, smartConfig).associate { smart ->
            val key = SmartActionIntents.actionKey(sbn.key, smart)
            smart.type.name to SmartActionIntents.pendingIntent(context, smart, key)
        }
    }
}
