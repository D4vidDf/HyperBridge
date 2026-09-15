package com.d4viddf.hyperbridge.service.translators

import android.content.Context
import android.service.notification.StatusBarNotification
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.data.widget.CustomWidgetRepository
import com.d4viddf.hyperbridge.data.widget.SourceRepository
import com.d4viddf.hyperbridge.data.widget.VariableContext
import com.d4viddf.hyperbridge.data.widget.WidgetVariableEngine
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.service.widget.CustomWidgetRenderer
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
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
    // notification (mirrors AppPreferences' memoryCache pattern); invalidated on save/delete.
    private val bindingCache = ConcurrentHashMap<String, String?>()

    suspend fun hasBinding(packageName: String): Boolean = resolveWidgetId(packageName) != null

    private suspend fun resolveWidgetId(packageName: String): String? {
        if (bindingCache.containsKey(packageName)) return bindingCache[packageName]
        val doc = widgetRepo.getWidgetForPackage(packageName)
        bindingCache[packageName] = doc?.id
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
            sourceLookup = { id, field -> runBlocking { sourceRepo.lookup(id, field) } }
        )

        val rv = renderer.render(doc, ctx)

        val builder = HyperIslandNotification.Builder(context, "custom_widget_channel", effectiveTitle)
        builder.setCustomRemoteView(rv)
        builder.setIslandConfig(timeout = config.timeout, dismissible = true)
        builder.setEnableFloat(config.isFloat == true)
        builder.setShowNotification(config.isShowShade == true)
        builder.setReopen(true)

        return HyperIslandData(builder.buildCustomExtras(), builder.buildJsonParam())
    }
}
