package com.d4viddf.hyperbridge.service.translators

import android.content.Context
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import android.util.Log
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
import com.d4viddf.hyperbridge.service.widget.CustomWidgetRenderer
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

/**
 * Minimal "translator registry" hook for Phase 5 (#273) custom micro-widgets. Phase 3's real
 * `TranslatorRegistry` doesn't exist yet (see ground rules), so this class is consulted directly
 * from `NotificationReaderService`, before the built-in `when(type)` dispatch, exactly like any
 * other translator - [hasBinding] doing a package-keyed lookup backed by Room/files IS the
 * minimal registry for now.
 */
class CustomWidgetTranslator(
    context: Context,
    repository: ThemeRepository? = null
) : BaseTranslator(context, repository) {

    private val widgetRepo = CustomWidgetRepository(context)
    private val sourceRepo = SourceRepository(context)
    private val renderer = CustomWidgetRenderer(context, WidgetVariableEngine(), widgetRepo)

    // Cheap in-memory package -> widget id cache so hasBinding() is safe to call on every posted
    // notification. "No binding" is cached too (widgetId == null) — never store a null VALUE in
    // the ConcurrentHashMap itself, that throws NPE and used to kill island processing for every
    // package without a binding. Entries expire so a widget bound in the Studio (a different
    // process/screen) is picked up without restarting the service.
    private data class CachedBinding(val widgetId: String?, val cachedAt: Long)
    private val bindingCache = ConcurrentHashMap<String, CachedBinding>()

    /**
     * True when a custom widget is bound to [packageName]. Never throws: island processing must
     * not depend on this lookup succeeding, so any failure is logged and treated as "no binding".
     */
    suspend fun hasBinding(packageName: String): Boolean = try {
        resolveWidgetId(packageName) != null
    } catch (e: Exception) {
        Log.w(TAG, "Custom widget binding lookup failed for $packageName; treating as unbound", e)
        false
    }

    private suspend fun resolveWidgetId(packageName: String): String? {
        val now = System.currentTimeMillis()
        val cached = bindingCache[packageName]
        if (cached != null && now - cached.cachedAt < BINDING_CACHE_TTL_MS) return cached.widgetId
        val doc = widgetRepo.getWidgetForPackage(packageName)
        bindingCache[packageName] = CachedBinding(doc?.id, now)
        return doc?.id
    }

    fun invalidateCache() {
        bindingCache.clear()
    }

    suspend fun translate(
        sbn: StatusBarNotification,
        picKey: String,
        effectiveTitle: String,
        effectiveText: String,
        config: IslandConfig,
        theme: HyperTheme?
    ): HyperIslandData {
        val doc = requireNotNull(widgetRepo.getWidgetForPackage(sbn.packageName)) {
            "No custom widget bound to package ${sbn.packageName}"
        }

        val progress = sbn.notification.extras.getInt("android.progress", -1).takeIf { it >= 0 }
        val ctx = VariableContext(
            notifTitle = effectiveTitle,
            notifText = effectiveText,
            notifProgress = progress,
            notifPackage = sbn.packageName,
            deviceBatteryPercent = DeviceVariables.batteryPercent(context),
            timeNowFormatted = DeviceVariables.timeNow(),
            sourceLookup = { id, field -> runBlocking { sourceRepo.lookup(id, field) } }
        )

        val rv = renderer.render(doc, ctx)

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

    companion object {
        private const val TAG = "CustomWidgetTranslator"
        private const val BINDING_CACHE_TTL_MS = 15_000L
    }
}
