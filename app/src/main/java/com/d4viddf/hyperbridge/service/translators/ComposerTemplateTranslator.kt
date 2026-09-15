package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.composer.ComposerTemplate
import com.d4viddf.hyperbridge.models.composer.FieldBinding
import com.d4viddf.hyperbridge.models.composer.GraphicSource
import com.d4viddf.hyperbridge.models.composer.NotificationField
import com.d4viddf.hyperbridge.models.composer.ProgressKind
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

/**
 * Renders a user-authored Phase 4 [ComposerTemplate] (issue #272) into a [HyperIslandData], the
 * "smallest clean hook" the ground rules ask for in lieu of the not-yet-built Phase 3
 * TranslatorRegistry. Mirrors the Builder usage of [StandardTranslator]/[ProgressTranslator],
 * parameterized by the stored template's slots instead of hardcoded per notification type.
 */
class ComposerTemplateTranslator(
    context: Context,
    repo: ThemeRepository
) : BaseTranslator(context, repo) {

    private companion object {
        const val MAX_TEMPLATE_BUTTONS = 3
    }

    fun translate(
        sbn: StatusBarNotification,
        title: String,
        text: String,
        picKey: String,
        config: IslandConfig,
        theme: HyperTheme?,
        template: ComposerTemplate
    ): HyperIslandData {
        val definition = template.definition
        val hiddenKey = "hidden_pixel"

        val highlightColor = resolveColor(theme, sbn.packageName, definition.highlightColor ?: "#FFFFFF")

        val builder = HyperIslandNotification.Builder(context, "bridge_${sbn.packageName}", title)
        builder.setEnableFloat(config.isFloat ?: false)
        builder.setIslandConfig(timeout = config.timeout, dismissible = true, highlightColor = highlightColor, expandedTimeMs = config.floatTimeout)
        builder.setShowNotification(config.isShowShade ?: false)
        builder.setReopen(true)
        builder.setIslandFirstFloat(config.isFloat ?: false)

        // --- Left graphic slot ---
        val leftPic = resolveLeftGraphic(sbn, picKey, definition.leftGraphic.source)
        builder.addPicture(leftPic)
        builder.addPicture(getTransparentPicture(hiddenKey))

        // --- Center text slot ---
        val resolvedTitle = resolveField(definition.text.title, sbn, title, text)
        val resolvedContent = resolveField(definition.text.content, sbn, title, text)

        builder.setBaseInfo(type = 2, title = resolvedTitle, content = resolvedContent)
        builder.setIconTextInfo(picKey = picKey, title = resolvedTitle, content = resolvedContent)

        val percent = resolveProgressPercent(definition.progress.valueBinding, sbn, title, text)
        val activeColor = resolveColor(theme, sbn.packageName, definition.progress.activeColor ?: "#007AFF")

        when (definition.progress.kind) {
            ProgressKind.LINEAR -> builder.setProgressBar(percent, activeColor)
            ProgressKind.CIRCULAR -> {
                builder.setBigIslandProgressCircle(picKey, "", percent, activeColor, true)
                builder.setSmallIslandCircularProgress(picKey, percent, activeColor, isCCW = true)
            }
            ProgressKind.MULTI_STEP -> builder.setMultiProgress(resolvedTitle, percent, activeColor, 4)
            ProgressKind.NONE -> { /* handled by setBigIslandInfo below */ }
        }

        if (definition.progress.kind != ProgressKind.CIRCULAR) {
            builder.setBigIslandInfo(
                left = ImageTextInfoLeft(1, PicInfo(1, picKey), TextInfo("", "")),
                right = ImageTextInfoRight(1, PicInfo(1, hiddenKey), TextInfo(resolvedTitle, resolvedContent))
            )
        }

        builder.setSmallIsland(picKey)

        // --- Bottom action buttons ---
        val nativeActions = extractBridgeActions(sbn = sbn, config = config, theme = theme, includeSmartActions = true)
        val resolvedButtons = definition.buttons.take(MAX_TEMPLATE_BUTTONS).mapIndexedNotNull { index, buttonDef ->
            resolveActionButton(buttonDef, index, nativeActions, sbn)
        }
        if (resolvedButtons.isNotEmpty()) {
            builder.setTextButtons(*resolvedButtons.toTypedArray())
            resolvedButtons.forEach { builder.addHiddenAction(it) }
        }
        nativeActions.forEach { it.actionImage?.let { pic -> builder.addPicture(pic) } }

        return HyperIslandData(builder.buildResourceBundle(), builder.buildJsonParam())
    }

    private fun resolveLeftGraphic(sbn: StatusBarNotification, picKey: String, source: GraphicSource): HyperPicture {
        return when (source) {
            GraphicSource.APP_ICON -> try {
                val bitmap = context.packageManager.getApplicationIcon(sbn.packageName).toBitmap()
                HyperPicture(picKey, bitmap)
            } catch (e: Exception) {
                resolveIcon(sbn, picKey)
            }
            // STATIC_ASSET is not yet backed by asset storage (scope cut, #272 v1); falls back to
            // the notification's own bitmap like LARGE_ICON until a later phase adds it.
            GraphicSource.LARGE_ICON, GraphicSource.STATIC_ASSET -> resolveIcon(sbn, picKey)
        }
    }

    private fun resolveField(binding: FieldBinding, sbn: StatusBarNotification, title: String, text: String): String {
        return when (binding.field) {
            NotificationField.TITLE -> title
            NotificationField.TEXT -> text
            NotificationField.SUBTEXT -> sbn.notification.extras.getString(Notification.EXTRA_SUB_TEXT) ?: ""
            NotificationField.PROGRESS -> "${resolveProgressPercent(binding, sbn, title, text)}%"
            NotificationField.STATIC -> binding.staticValue ?: ""
            NotificationField.APP_ICON, NotificationField.LARGE_ICON -> "" // image fields, not text
        }
    }

    private fun resolveProgressPercent(binding: FieldBinding, sbn: StatusBarNotification, title: String, text: String): Int {
        if (binding.field == NotificationField.STATIC) {
            return binding.staticValue?.toIntOrNull()?.coerceIn(0, 100) ?: 0
        }
        val extras = sbn.notification.extras
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val current = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        if (max > 0) return ((current.toFloat() / max.toFloat()) * 100).toInt().coerceIn(0, 100)
        return (extractTextPercentage(title, text) ?: 0).coerceIn(0, 100)
    }

    private fun resolveActionButton(
        buttonDef: com.d4viddf.hyperbridge.models.composer.ComposerActionButton,
        index: Int,
        nativeActions: List<com.d4viddf.hyperbridge.models.BridgeAction>,
        sbn: StatusBarNotification
    ): HyperAction? {
        if (buttonDef.useNotificationAction) {
            return nativeActions.getOrNull(buttonDef.notificationActionIndex)?.action?.let { native ->
                HyperAction(
                    key = native.key,
                    title = if (buttonDef.label.isNotBlank()) buttonDef.label else native.title,
                    icon = native.icon,
                    pendingIntent = native.pendingIntent,
                    actionIntentType = native.actionIntentType,
                    actionBgColor = buttonDef.bgColor ?: native.actionBgColor,
                    titleColor = native.titleColor
                )
            }
        }
        val contentIntent = sbn.notification.contentIntent ?: return null
        return HyperAction(
            key = "composer_action_${sbn.key.hashCode()}_$index",
            title = buttonDef.label,
            icon = null,
            pendingIntent = contentIntent,
            actionIntentType = 1,
            actionBgColor = buttonDef.bgColor,
            titleColor = "#FFFFFF"
        )
    }
}
