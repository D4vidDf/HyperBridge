package com.d4viddf.hyperbridge.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.data.db.AppSetting
import com.d4viddf.hyperbridge.data.db.SettingsDao
import com.d4viddf.hyperbridge.data.db.SettingsKeys
import com.d4viddf.hyperbridge.models.CallStage
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.IslandLimitMode
import com.d4viddf.hyperbridge.models.NavContent
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.WidgetConfig
import com.d4viddf.hyperbridge.models.WidgetRenderMode
import com.d4viddf.hyperbridge.models.WidgetSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

private val Context.legacyDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPreferences internal constructor(
    private val dao: SettingsDao,
    private val legacyDataStore: DataStore<Preferences>?,
    context: Context?,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    constructor(context: Context) : this(
        dao = AppDatabase.getDatabase(context).settingsDao(),
        legacyDataStore = context.applicationContext.legacyDataStore,
        context = context
    )

    private val memoryCache = ConcurrentHashMap<String, String>()

    init {
        // --- MEMORY CACHE LOGIC ---
        scope.launch {
            dao.getAllFlow().collect { list ->
                val newCache = ConcurrentHashMap<String, String>()
                list.forEach { newCache[it.key] = it.value }
                memoryCache.clear()
                memoryCache.putAll(newCache)
            }
        }

        // --- MIGRATION LOGIC ---
        if (context != null && legacyDataStore != null) {
            scope.launch {
                try {
                    // Wait for user unlock before attempting to migrate from legacy DataStore (CE storage)
                    val userManager = context.getSystemService(Context.USER_SERVICE) as? android.os.UserManager
                    if (userManager != null && !userManager.isUserUnlocked) {
                        return@launch 
                    }

                // Force Onboarding reset for new permissions
                val lastResetVersion = dao.getSetting("onboarding_reset_version")?.toIntOrNull() ?: 0
                if (lastResetVersion < 19) {
                    dao.insert(AppSetting(SettingsKeys.SETUP_COMPLETE, "false"))
                    dao.insert(AppSetting("onboarding_reset_version", "19"))
                }

                val isMigrated = dao.getSetting(SettingsKeys.MIGRATION_COMPLETE) == "true"
                if (!isMigrated) {
                    val legacyPrefs = legacyDataStore.data.first().asMap()
                    if (legacyPrefs.isNotEmpty()) {
                        legacyPrefs.forEach { (key, value) ->
                            val strValue = when (value) {
                                is Set<*> -> value.joinToString(",")
                                else -> value.toString()
                            }
                            dao.insert(AppSetting(key.name, strValue))
                        }
                        legacyDataStore.edit { it.clear() }
                    }
                    dao.insert(AppSetting(SettingsKeys.MIGRATION_COMPLETE, "true"))
                }

                if (dao.getSetting(SettingsKeys.FLOATING_SETUP_NOTICE_PENDING) == null) {
                    val setupComplete = dao.getSetting(SettingsKeys.SETUP_COMPLETE).toBoolean(false)
                    val hasSelectedApps = !dao.getSetting(SettingsKeys.ALLOWED_PACKAGES).isNullOrBlank()
                    dao.insert(
                        AppSetting(
                            SettingsKeys.FLOATING_SETUP_NOTICE_PENDING,
                            (setupComplete && hasSelectedApps).toString()
                        )
                    )
                }

                // Grant DOWNLOAD notification type if PROGRESS was previously enabled
                val isDownloadMigrated = dao.getSetting("download_type_migration_complete") == "true"
                if (!isDownloadMigrated) {
                    // 1. Global notification types migration
                    val globalTypesStr = dao.getSetting(GLOBAL_NOTIFICATION_TYPES_KEY)
                    if (globalTypesStr != null) {
                        val globalTypes = globalTypesStr.deserializeSet()
                        if (globalTypes.contains("PROGRESS") && !globalTypes.contains("DOWNLOAD")) {
                            val newGlobalTypes = globalTypes + "DOWNLOAD"
                            dao.insert(AppSetting(GLOBAL_NOTIFICATION_TYPES_KEY, newGlobalTypes.serialize()))
                        }
                    }

                    // 2. App-specific notification types migration
                    val suffixes = listOf("_float", "_shade", "_timeout", "_float_timeout", "_remove_notif", "_blocked", "_nav_left", "_nav_right", "_use_native")
                    val allSettings = dao.getAllSync()
                    allSettings.forEach { setting ->
                        val key = setting.key
                        if (key.startsWith("config_") && suffixes.none { key.endsWith(it) }) {
                            val types = setting.value.deserializeSet()
                            if (types.contains("PROGRESS") && !types.contains("DOWNLOAD")) {
                                val newTypes = types + "DOWNLOAD"
                                dao.insert(AppSetting(key, newTypes.serialize()))
                            }
                        }
                    }

                    dao.insert(AppSetting("download_type_migration_complete", "true"))
                }

                // Grant DOWNLOAD and MESSAGE to all active apps and globally
                val isDownloadMessageMigrated = dao.getSetting("download_message_migration_complete") == "true"
                if (!isDownloadMessageMigrated) {
                    // 1. Global notification types migration
                    val globalTypesStr = dao.getSetting(GLOBAL_NOTIFICATION_TYPES_KEY)
                    if (globalTypesStr != null) {
                        val globalTypes = globalTypesStr.deserializeSet()
                        val newGlobalTypes = globalTypes + "DOWNLOAD" + "MESSAGE"
                        dao.insert(AppSetting(GLOBAL_NOTIFICATION_TYPES_KEY, newGlobalTypes.serialize()))
                    }

                    // 2. Active apps migration
                    val allowedPackagesStr = dao.getSetting(SettingsKeys.ALLOWED_PACKAGES)
                    val allowedPackages = allowedPackagesStr.deserializeSet()
                    
                    allowedPackages.forEach { packageName ->
                        val key = "config_$packageName"
                        val configStr = dao.getSetting(key)
                        if (configStr != null) {
                            val types = configStr.deserializeSet()
                            val newTypes = types + "DOWNLOAD" + "MESSAGE"
                            dao.insert(AppSetting(key, newTypes.serialize()))
                        }
                    }

                    dao.insert(AppSetting("download_message_migration_complete", "true"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

    // --- HELPERS ---
    private fun String?.toBoolean(default: Boolean = false): Boolean = this?.toBooleanStrictOrNull() ?: default
    private fun String?.toInt(default: Int = 0): Int = this?.toIntOrNull() ?: default
    private fun String?.toLong(default: Long = 0L): Long = this?.toLongOrNull() ?: default

    private fun Set<String>.serialize(): String = this.joinToString(",")
    private fun String?.deserializeSet(): Set<String> = this?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
    private fun String?.deserializeList(): List<String> = this?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    private fun String?.deserializeCallStages(default: Set<CallStage>): Set<CallStage> {
        if (this == null) return default
        return deserializeSet().mapNotNull { value ->
            try { CallStage.valueOf(value) } catch (_: IllegalArgumentException) { null }
        }.toSet()
    }

    private suspend fun save(key: String, value: String) {
        memoryCache[key] = value
        dao.insert(AppSetting(key, value))
    }

    private suspend fun remove(key: String) {
        memoryCache.remove(key)
        dao.delete(key)
    }

    // --- CORE SETTINGS ---
    val allowedPackagesFlow: Flow<Set<String>> = dao.getSettingFlow(SettingsKeys.ALLOWED_PACKAGES).map { it.deserializeSet() }
    val vpnIslandEnabledFlow: Flow<Boolean> = dao.getSettingFlow("vpn_island_enabled").map { it.toBoolean(true) }
    val isSetupComplete: Flow<Boolean> = dao.getSettingFlow(SettingsKeys.SETUP_COMPLETE).map { it.toBoolean(false) }
    val lastSeenVersion: Flow<Int> = dao.getSettingFlow(SettingsKeys.LAST_VERSION).map { it.toInt(0) }

    suspend fun setSetupComplete(isComplete: Boolean) = save(SettingsKeys.SETUP_COMPLETE, isComplete.toString())
    suspend fun setLastSeenVersion(versionCode: Int) = save(SettingsKeys.LAST_VERSION, versionCode.toString())
    suspend fun setVpnIslandEnabled(enabled: Boolean) = save("vpn_island_enabled", enabled.toString())
    suspend fun setPriorityEduShown(shown: Boolean) = save(SettingsKeys.PRIORITY_EDU, shown.toString())

    val featuredPermissionWarningFlow: Flow<Boolean> = dao.getSettingFlow(SettingsKeys.FEATURED_PERMISSION_WARNING).map { it.toBoolean(false) }
    suspend fun setFeaturedPermissionWarning(show: Boolean) = save(SettingsKeys.FEATURED_PERMISSION_WARNING, show.toString())

    val floatingSetupNoticePendingFlow: Flow<Boolean> =
        dao.getSettingFlow(SettingsKeys.FLOATING_SETUP_NOTICE_PENDING).map { it.toBoolean(false) }

    val floatingSetupConfirmedPackagesFlow: Flow<Set<String>> =
        dao.getSettingFlow(SettingsKeys.FLOATING_SETUP_CONFIRMED_PACKAGES).map { it.deserializeSet() }

    suspend fun setFloatingSetupNoticePending(show: Boolean) =
        save(SettingsKeys.FLOATING_SETUP_NOTICE_PENDING, show.toString())

    suspend fun setFloatingSetupConfirmed(packageName: String, confirmed: Boolean) {
        val current = dao.getSetting(SettingsKeys.FLOATING_SETUP_CONFIRMED_PACKAGES).deserializeSet()
        val updated = if (confirmed) current + packageName else current - packageName
        save(SettingsKeys.FLOATING_SETUP_CONFIRMED_PACKAGES, updated.serialize())
    }

    suspend fun toggleApp(packageName: String, isEnabled: Boolean) {
        val currentString = dao.getSetting(SettingsKeys.ALLOWED_PACKAGES)
        val currentSet = currentString.deserializeSet()
        val newSet = if (isEnabled) currentSet + packageName else currentSet - packageName
        save(SettingsKeys.ALLOWED_PACKAGES, newSet.serialize())
        if (isEnabled && packageName !in dao.getSetting(SettingsKeys.FLOATING_SETUP_CONFIRMED_PACKAGES).deserializeSet()) {
            save(SettingsKeys.FLOATING_SETUP_NOTICE_PENDING, "true")
        }
    }

    // ========================================================================
    //                        THEME ENGINE
    // ========================================================================

    val activeThemeIdFlow: Flow<String?> = dao.getSettingFlow("active_theme_id")

    suspend fun setActiveThemeId(id: String?) {
        if (id == null) {
            remove("active_theme_id")
        } else {
            save("active_theme_id", id)
        }
    }

    // --- LIMITS & PRIORITY ---
    val limitModeFlow: Flow<IslandLimitMode> = dao.getSettingFlow("limit_mode").map {
        try { IslandLimitMode.valueOf(it ?: IslandLimitMode.MOST_RECENT.name) } catch(_: Exception) { IslandLimitMode.MOST_RECENT }
    }
    val appPriorityListFlow: Flow<List<String>> = dao.getSettingFlow(SettingsKeys.PRIORITY_ORDER).map { it.deserializeList() }

    suspend fun setLimitMode(mode: IslandLimitMode) = save("limit_mode", mode.name)
    suspend fun setAppPriorityOrder(order: List<String>) = save(SettingsKeys.PRIORITY_ORDER, order.joinToString(","))

    // --- NOTIFICATION TYPES ---
    fun getAppConfig(packageName: String): Flow<Set<String>> {
        val legacyKey = "config_$packageName"
        return dao.getSettingFlow(legacyKey).map { str ->
            str?.deserializeSet() ?: NotificationType.entries.map { t -> t.name }.toSet()
        }
    }

    // --- ISLAND CONFIG (Standard Notifications) ---
    private fun sanitizeTimeout(raw: Long?): Long {
        val value = raw ?: 5L
        return if (value > 60) value / 1000 else value
    }

    val globalConfigFlow: Flow<IslandConfig> = combine(
        dao.getSettingFlow(SettingsKeys.GLOBAL_FLOAT),
        dao.getSettingFlow(SettingsKeys.GLOBAL_SHADE),
        dao.getSettingFlow(SettingsKeys.GLOBAL_TIMEOUT),
        dao.getSettingFlow(SettingsKeys.GLOBAL_FLOAT_TIMEOUT),
        dao.getSettingFlow(SettingsKeys.GLOBAL_REMOVE_NOTIF),
        dao.getSettingFlow(SettingsKeys.GLOBAL_DISMISS_WITH_ORIGINAL),
        dao.getSettingFlow(SettingsKeys.GLOBAL_ENABLE_INLINE_REPLY)
    ) { args: Array<String?> ->
        IslandConfig(
            args[0].toBoolean(true),
            args[1].toBoolean(false),
            args[2]?.toIntOrNull(),
            args[3]?.toIntOrNull(),
            args[4]?.toBooleanStrictOrNull(),
            args[5]?.toBooleanStrictOrNull() ?: true,
            args[6]?.toBooleanStrictOrNull()
        )
    }

    suspend fun updateGlobalConfig(config: IslandConfig) {
        config.isFloat?.let { save(SettingsKeys.GLOBAL_FLOAT, it.toString()) }
        config.isShowShade?.let { save(SettingsKeys.GLOBAL_SHADE, it.toString()) }
        config.timeout?.let { save(SettingsKeys.GLOBAL_TIMEOUT, it.toString()) }
        config.floatTimeout?.let { save(SettingsKeys.GLOBAL_FLOAT_TIMEOUT, it.toString()) }
        config.removeOriginalNotification?.let { save(SettingsKeys.GLOBAL_REMOVE_NOTIF, it.toString()) }
        config.dismissWithOriginal?.let { save(SettingsKeys.GLOBAL_DISMISS_WITH_ORIGINAL, it.toString()) }
        config.enableInlineReply?.let { save(SettingsKeys.GLOBAL_ENABLE_INLINE_REPLY, it.toString()) }
    }

    fun getAppIslandConfig(packageName: String): Flow<IslandConfig> {
        return combine(
            dao.getSettingFlow("config_${packageName}_float"),
            dao.getSettingFlow("config_${packageName}_shade"),
            dao.getSettingFlow("config_${packageName}_timeout"),
            dao.getSettingFlow("config_${packageName}_float_timeout"),
            dao.getSettingFlow("config_${packageName}_remove_notif"),
            dao.getSettingFlow("config_${packageName}_dismiss_with_original"),
            dao.getSettingFlow("config_${packageName}_enable_inline_reply")
        ) { args: Array<String?> ->
            IslandConfig(
                args[0]?.toBooleanStrictOrNull(),
                args[1]?.toBooleanStrictOrNull(),
                args[2]?.toIntOrNull(),
                args[3]?.toIntOrNull(),
                args[4]?.toBooleanStrictOrNull(),
                args[5]?.toBooleanStrictOrNull(),
                args[6]?.toBooleanStrictOrNull()
            )
        }
    }

    suspend fun updateAppIslandConfig(packageName: String, config: IslandConfig) {
        val fKey = "config_${packageName}_float"
        val sKey = "config_${packageName}_shade"
        val tKey = "config_${packageName}_timeout"
        val ftKey = "config_${packageName}_float_timeout"
        val rnKey = "config_${packageName}_remove_notif"
        val dwoKey = "config_${packageName}_dismiss_with_original"
        val eirKey = "config_${packageName}_enable_inline_reply"

        if (config.isFloat != null) save(fKey, config.isFloat.toString()) else remove(fKey)
        if (config.isShowShade != null) save(sKey, config.isShowShade.toString()) else remove(sKey)
        if (config.timeout != null) save(tKey, config.timeout.toString()) else remove(tKey)
        if (config.floatTimeout != null) save(ftKey, config.floatTimeout.toString()) else remove(ftKey)
        if (config.removeOriginalNotification != null) save(rnKey, config.removeOriginalNotification.toString()) else remove(rnKey)
        if (config.dismissWithOriginal != null) save(dwoKey, config.dismissWithOriginal.toString()) else remove(dwoKey)
        if (config.enableInlineReply != null) save(eirKey, config.enableInlineReply.toString()) else remove(eirKey)
    }

    // --- SYSTEM ISLAND: SCREEN RECORDING ---
    val screenRecordingTimeoutFlow: Flow<Int> =
        dao.getSettingFlow(SettingsKeys.SCREEN_RECORDING_TIMEOUT).map { it.toInt(SYSTEM_ISLAND_DEFAULT_TIMEOUT) }

    suspend fun setScreenRecordingTimeout(seconds: Int) =
        save(SettingsKeys.SCREEN_RECORDING_TIMEOUT, seconds.toString())

    val screenRecordingLeftDesignFlow: Flow<com.d4viddf.hyperbridge.models.ScreenRecordingLeftDesign> =
        dao.getSettingFlow(SettingsKeys.SCREEN_RECORDING_LEFT_DESIGN).map { value ->
            value?.let { runCatching { com.d4viddf.hyperbridge.models.ScreenRecordingLeftDesign.valueOf(it) }.getOrNull() }
                ?: com.d4viddf.hyperbridge.models.ScreenRecordingLeftDesign.ICON_AND_TEXT
        }

    val screenRecordingRightDesignFlow: Flow<com.d4viddf.hyperbridge.models.ScreenRecordingRightDesign> =
        dao.getSettingFlow(SettingsKeys.SCREEN_RECORDING_RIGHT_DESIGN).map { value ->
            value?.let { runCatching { com.d4viddf.hyperbridge.models.ScreenRecordingRightDesign.valueOf(it) }.getOrNull() }
                ?: com.d4viddf.hyperbridge.models.ScreenRecordingRightDesign.TIMER
        }

    val screenRecordingDesignFlow: Flow<com.d4viddf.hyperbridge.models.ScreenRecordingDesignConfig> =
        combine(screenRecordingLeftDesignFlow, screenRecordingRightDesignFlow) { left, right ->
            com.d4viddf.hyperbridge.models.ScreenRecordingDesignConfig(left = left, right = right)
        }

    suspend fun setScreenRecordingLeftDesign(design: com.d4viddf.hyperbridge.models.ScreenRecordingLeftDesign) =
        save(SettingsKeys.SCREEN_RECORDING_LEFT_DESIGN, design.name)

    suspend fun setScreenRecordingRightDesign(design: com.d4viddf.hyperbridge.models.ScreenRecordingRightDesign) =
        save(SettingsKeys.SCREEN_RECORDING_RIGHT_DESIGN, design.name)


    // --- NAVIGATION ---
    val globalBlockedTermsFlow: Flow<Set<String>> = dao.getSettingFlow(SettingsKeys.GLOBAL_BLOCKED_TERMS).map { it.deserializeSet() }
    suspend fun setGlobalBlockedTerms(terms: Set<String>) = save(SettingsKeys.GLOBAL_BLOCKED_TERMS, terms.serialize())

    fun getAppBlockedTerms(packageName: String): Flow<Set<String>> {
        return dao.getSettingFlow("config_${packageName}_blocked").map { it.deserializeSet() }
    }
    suspend fun setAppBlockedTerms(packageName: String, terms: Set<String>) {
        save("config_${packageName}_blocked", terms.serialize())
    }

    val globalNavLayoutFlow: Flow<Pair<NavContent, NavContent>> = combine(
        dao.getSettingFlow(SettingsKeys.NAV_LEFT),
        dao.getSettingFlow(SettingsKeys.NAV_RIGHT)
    ) { l, r ->
        val left = try { NavContent.valueOf(l ?: NavContent.DISTANCE_ETA.name) } catch (_: Exception) { NavContent.DISTANCE_ETA }
        val right = try { NavContent.valueOf(r ?: NavContent.INSTRUCTION.name) } catch (_: Exception) { NavContent.INSTRUCTION }
        left to right
    }

    suspend fun setGlobalNavLayout(left: NavContent, right: NavContent) {
        save(SettingsKeys.NAV_LEFT, left.name)
        save(SettingsKeys.NAV_RIGHT, right.name)
    }

    fun getAppNavLayout(packageName: String): Flow<Pair<NavContent?, NavContent?>> {
        return combine(
            dao.getSettingFlow("config_${packageName}_nav_left"),
            dao.getSettingFlow("config_${packageName}_nav_right")
        ) { l, r ->
            val left = l?.let { try { NavContent.valueOf(it) } catch(_: Exception){null} }
            val right = r?.let { try { NavContent.valueOf(it) } catch(_: Exception){null} }
            left to right
        }
    }

    fun getEffectiveNavLayout(packageName: String): Flow<Pair<NavContent, NavContent>> {
        return combine(
            dao.getSettingFlow("config_${packageName}_nav_left"),
            dao.getSettingFlow("config_${packageName}_nav_right"),
            globalNavLayoutFlow
        ) { appL, appR, global ->
            val left = appL?.let { try { NavContent.valueOf(it) } catch(_: Exception){null} } ?: global.first
            val right = appR?.let { try { NavContent.valueOf(it) } catch(_: Exception){null} } ?: global.second
            left to right
        }
    }

    suspend fun updateAppNavLayout(packageName: String, left: NavContent?, right: NavContent?) {
        val lKey = "config_${packageName}_nav_left"
        val rKey = "config_${packageName}_nav_right"
        if (left != null) save(lKey, left.name) else remove(lKey)
        if (right != null) save(rKey, right.name) else remove(rKey)
    }

    // ========================================================================
    //                         WIDGET CONFIGURATION
    // ========================================================================

    private val WIDGET_IDS_DB_KEY = "saved_widget_ids_list"

    val savedWidgetIdsFlow: Flow<List<Int>> = dao.getSettingFlow(WIDGET_IDS_DB_KEY).map { str ->
        str?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
    }

    fun getWidgetConfigFlow(id: Int): Flow<WidgetConfig> {
        return combine(
            dao.getSettingFlow("widget_${id}_shown"),
            dao.getSettingFlow("widget_${id}_timeout"),
            dao.getSettingFlow("widget_${id}_size"),
            dao.getSettingFlow("widget_${id}_mode"),
            dao.getSettingFlow("widget_${id}_auto_update"),
            dao.getSettingFlow("widget_${id}_update_interval")
        ) { args: Array<String?> ->
            val shown = args[0]
            val timeout = args[1]
            val sizeStr = args[2]
            val modeStr = args[3]
            val autoStr = args[4]
            val intervalStr = args[5]

            val sizeEnum = try { WidgetSize.valueOf(sizeStr ?: WidgetSize.MEDIUM.name) } catch (_: Exception) { WidgetSize.MEDIUM }
            val modeEnum = try { WidgetRenderMode.valueOf(modeStr ?: WidgetRenderMode.INTERACTIVE.name) } catch (_: Exception) { WidgetRenderMode.INTERACTIVE }

            WidgetConfig(
                isShowShade = shown.toBoolean(true),
                timeout = timeout.toInt(10),
                size = sizeEnum,
                renderMode = modeEnum,
                autoUpdate = autoStr.toBoolean(false),
                updateIntervalMinutes = intervalStr.toInt(15)
            )
        }
    }

    suspend fun saveWidgetConfig(
        id: Int,
        config: WidgetConfig
    ) {
        val currentStr = dao.getSetting(WIDGET_IDS_DB_KEY) ?: ""
        val currentIds = currentStr.split(",").filter { it.isNotEmpty() }.toMutableSet()
        currentIds.add(id.toString())
        save(WIDGET_IDS_DB_KEY, currentIds.joinToString(","))

        save("widget_${id}_shown", config.isShowShade.toString())
        save("widget_${id}_timeout", config.timeout.toString())
        save("widget_${id}_size", config.size.name)
        save("widget_${id}_mode", config.renderMode.name)
        save("widget_${id}_auto_update", config.autoUpdate.toString())
        save("widget_${id}_update_interval", config.updateIntervalMinutes.toString())
    }

    suspend fun removeWidgetId(id: Int) {
        val currentStr = dao.getSetting(WIDGET_IDS_DB_KEY) ?: ""
        val currentIds = currentStr.split(",").filter { it.isNotEmpty() }.toMutableList()
        currentIds.remove(id.toString())
        save(WIDGET_IDS_DB_KEY, currentIds.joinToString(","))

        dao.delete("widget_${id}_shown")
        dao.delete("widget_${id}_timeout")
        dao.delete("widget_${id}_size")
        dao.delete("widget_${id}_mode")
        dao.delete("widget_${id}_auto_update")
        dao.delete("widget_${id}_update_interval")
    }

    // ========================================================================
    //                        FAVORITE WIDGET APPS
    // ========================================================================

    val favoriteWidgetAppsFlow: Flow<Set<String>> = dao.getSettingFlow("favorite_widget_apps").map { it.deserializeSet() }

    suspend fun toggleFavoriteWidgetApp(packageName: String, isFavorite: Boolean) {
        val currentStr = dao.getSetting("favorite_widget_apps")
        val currentSet = currentStr.deserializeSet()
        val newSet = if (isFavorite) currentSet + packageName else currentSet - packageName
        save("favorite_widget_apps", newSet.serialize())
    }

    // ========================================================================
    //                        Global Notification Types
    // ========================================================================

    val GLOBAL_NOTIFICATION_TYPES_KEY = "global_notification_types"

    val globalNotificationTypesFlow: Flow<Set<String>> = dao.getSettingFlow(GLOBAL_NOTIFICATION_TYPES_KEY).map { str ->
        str?.deserializeSet() ?: NotificationType.entries.map { it.name }.toSet()
    }

    suspend fun updateGlobalNotificationType(type: NotificationType, isEnabled: Boolean) {
        val currentStr = dao.getSetting(GLOBAL_NOTIFICATION_TYPES_KEY)
        val currentSet = currentStr?.deserializeSet() ?: NotificationType.entries.map { it.name }.toSet()
        val newSet = if (isEnabled) currentSet + type.name else currentSet - type.name
        save(GLOBAL_NOTIFICATION_TYPES_KEY, newSet.serialize())
    }

    // --- APP-SPECIFIC NOTIFICATION TYPES ---

    fun getAppConfigFlow(packageName: String): Flow<Set<String>?> {
        val legacyKey = "config_$packageName"
        return dao.getSettingFlow(legacyKey).map { str ->
            str?.deserializeSet()
        }
    }

    suspend fun updateAppConfig(packageName: String, type: NotificationType, isEnabled: Boolean) {
        val key = "config_$packageName"
        val currentStr = dao.getSetting(key)
        val currentSet = currentStr?.deserializeSet() ?: NotificationType.entries.map { it.name }.toSet()
        val newSet = if (isEnabled) currentSet + type.name else currentSet - type.name
        save(key, newSet.serialize())
    }

    // ========================================================================
    //                        Call Stages Configuration
    // ========================================================================

    val GLOBAL_CALL_STAGES_KEY = "global_call_stages"

    val globalCallStagesFlow: Flow<Set<CallStage>> = dao.getSettingFlow(GLOBAL_CALL_STAGES_KEY).map { raw ->
        raw.deserializeCallStages(CallStage.entries.toSet())
    }

    suspend fun updateGlobalCallStage(stage: CallStage, isEnabled: Boolean) {
        val current = dao.getSetting(GLOBAL_CALL_STAGES_KEY)
            .deserializeCallStages(CallStage.entries.toSet())
        val updated = if (isEnabled) current + stage else current - stage
        save(GLOBAL_CALL_STAGES_KEY, updated.map { it.name }.toSet().serialize())
    }

    fun getAppCallStagesFlow(packageName: String): Flow<Set<CallStage>?> {
        return dao.getSettingFlow("config_${packageName}_call_stages").map { raw ->
            raw?.deserializeCallStages(emptySet())
        }
    }

    suspend fun updateAppCallStage(packageName: String, stage: CallStage, isEnabled: Boolean) {
        val key = "config_${packageName}_call_stages"
        val appValue = dao.getSetting(key)
        val inherited = dao.getSetting(GLOBAL_CALL_STAGES_KEY)
            .deserializeCallStages(CallStage.entries.toSet())
        val current = appValue.deserializeCallStages(inherited)
        val updated = if (isEnabled) current + stage else current - stage
        save(key, updated.map { it.name }.toSet().serialize())
    }

    // ========================================================================
    //                        THEME ENGINE CONFIGURATION
    // ========================================================================

    private val USE_NATIVE_ENGINE = "use_native_live_updates"
    private val IS_SHIZUKU_WORKAROUND_ENABLED = "is_shizuku_workaround_enabled"

    val useNativeLiveUpdates: Flow<Boolean> = dao.getSettingFlow(USE_NATIVE_ENGINE)
        .map { it?.toBoolean() ?: false }

    val isShizukuWorkaroundEnabled: Flow<Boolean> = dao.getSettingFlow(IS_SHIZUKU_WORKAROUND_ENABLED)
        .map { it?.toBoolean() ?: false }

    suspend fun setUseNativeLiveUpdates(value: Boolean) {
        save(USE_NATIVE_ENGINE, value.toString())
    }

    suspend fun setShizukuWorkaroundEnabled(value: Boolean) {
        save(IS_SHIZUKU_WORKAROUND_ENABLED, value.toString())
    }

    // ========================================================================
    //                        DND / GAME MODE CONFIGURATION
    // ========================================================================

    val isDndModeEnabledFlow: Flow<Boolean> = dao.getSettingFlow("dnd_mode_enabled").map { it.toBoolean(false) }
    suspend fun setDndModeEnabled(isEnabled: Boolean) = save("dnd_mode_enabled", isEnabled.toString())

    val autoDetectDndFlow: Flow<Boolean> = dao.getSettingFlow("auto_detect_dnd").map { it.toBoolean(false) }
    suspend fun setAutoDetectDnd(autoDetect: Boolean) = save("auto_detect_dnd", autoDetect.toString())

    // --- APP-SPECIFIC ENGINE OVERRIDES ---

    fun getAppEnginePreferenceFlow(packageName: String): Flow<Boolean?> {
        val key = "config_${packageName}_use_native"
        return dao.getSettingFlow(key).map { it?.toBooleanStrictOrNull() }
    }

    suspend fun updateAppEnginePreference(packageName: String, useNative: Boolean?) {
        val key = "config_${packageName}_use_native"
        if (useNative != null) {
            save(key, useNative.toString())
        } else {
            remove(key)
        }
    }

    // ========================================================================
    //                        PERMANENT ISLAND CONFIGURATION
    // ========================================================================

    private val SHOW_PERMANENT_ISLAND = "show_permanent_island"
    private val PERMANENT_ISLAND_WIDTH = "permanent_island_width"
    private val HIDE_PERMANENT_ISLAND_LANDSCAPE = "hide_permanent_island_landscape"

    val isPermanentIslandEnabledFlow: Flow<Boolean> = dao.getSettingFlow(SHOW_PERMANENT_ISLAND)
        .map { it?.toBoolean() ?: false }

    val permanentIslandWidthFlow: Flow<Int> = dao.getSettingFlow(PERMANENT_ISLAND_WIDTH)
        .map { it?.toIntOrNull() ?: 0 }

    val hidePermanentIslandLandscapeFlow: Flow<Boolean> = dao.getSettingFlow(HIDE_PERMANENT_ISLAND_LANDSCAPE)
        .map { it?.toBoolean() ?: false }

    suspend fun setPermanentIslandEnabled(value: Boolean) {
        save(SHOW_PERMANENT_ISLAND, value.toString())
    }

    suspend fun setPermanentIslandWidth(value: Int) {
        save(PERMANENT_ISLAND_WIDTH, value.toString())
    }

    suspend fun setHidePermanentIslandLandscape(value: Boolean) {
        save(HIDE_PERMANENT_ISLAND_LANDSCAPE, value.toString())
    }

    fun hidePermanentIslandLandscapeSync(): Boolean {
        return memoryCache[HIDE_PERMANENT_ISLAND_LANDSCAPE]?.toBoolean() ?: false
    }

    // ========================================================================
    //                        SYNCHRONOUS CACHE GETTERS
    // ========================================================================

    fun getAppBlockedTermsSync(packageName: String): Set<String> {
        return memoryCache["config_${packageName}_blocked"].deserializeSet()
    }

    fun getAppIslandConfigSync(packageName: String): IslandConfig {
        return IslandConfig(
            memoryCache["config_${packageName}_float"]?.toBooleanStrictOrNull(),
            memoryCache["config_${packageName}_shade"]?.toBooleanStrictOrNull(),
            memoryCache["config_${packageName}_timeout"]?.toIntOrNull(),
            memoryCache["config_${packageName}_float_timeout"]?.toIntOrNull(),
            memoryCache["config_${packageName}_remove_notif"]?.toBooleanStrictOrNull(),
            memoryCache["config_${packageName}_dismiss_with_original"]?.toBooleanStrictOrNull(),
            memoryCache["config_${packageName}_enable_inline_reply"]?.toBooleanStrictOrNull()
        )
    }

    fun getGlobalConfigSync(): IslandConfig {
        return IslandConfig(
            memoryCache[SettingsKeys.GLOBAL_FLOAT].toBoolean(true),
            memoryCache[SettingsKeys.GLOBAL_SHADE].toBoolean(false),
            memoryCache[SettingsKeys.GLOBAL_TIMEOUT]?.toIntOrNull(),
            memoryCache[SettingsKeys.GLOBAL_FLOAT_TIMEOUT]?.toIntOrNull(),
            memoryCache[SettingsKeys.GLOBAL_REMOVE_NOTIF]?.toBooleanStrictOrNull(),
            memoryCache[SettingsKeys.GLOBAL_DISMISS_WITH_ORIGINAL]?.toBooleanStrictOrNull() ?: true,
            memoryCache[SettingsKeys.GLOBAL_ENABLE_INLINE_REPLY]?.toBooleanStrictOrNull()
        )
    }

    fun getGlobalNavLayoutSync(): Pair<NavContent, NavContent> {
        val l = memoryCache[SettingsKeys.NAV_LEFT]
        val r = memoryCache[SettingsKeys.NAV_RIGHT]
        val left = try { NavContent.valueOf(l ?: NavContent.DISTANCE_ETA.name) } catch (_: Exception) { NavContent.DISTANCE_ETA }
        val right = try { NavContent.valueOf(r ?: NavContent.INSTRUCTION.name) } catch (_: Exception) { NavContent.INSTRUCTION }
        return left to right
    }

    fun getEffectiveNavLayoutSync(packageName: String): Pair<NavContent, NavContent> {
        val appL = memoryCache["config_${packageName}_nav_left"]
        val appR = memoryCache["config_${packageName}_nav_right"]
        val global = getGlobalNavLayoutSync()
        val left = appL?.let { try { NavContent.valueOf(it) } catch(_: Exception){null} } ?: global.first
        val right = appR?.let { try { NavContent.valueOf(it) } catch(_: Exception){null} } ?: global.second
        return left to right
    }

    fun getGlobalNotificationTypesSync(): Set<String> {
        val str = memoryCache[GLOBAL_NOTIFICATION_TYPES_KEY]
        return str?.deserializeSet() ?: NotificationType.entries.map { it.name }.toSet()
    }

    fun getAppConfigSync(packageName: String): Set<String>? {
        val str = memoryCache["config_$packageName"]
        return str?.deserializeSet()
    }

    fun getEffectiveCallStagesSync(packageName: String): Set<CallStage> {
        val appValue = memoryCache["config_${packageName}_call_stages"]
        val globalValue = memoryCache[GLOBAL_CALL_STAGES_KEY]
        return appValue.deserializeCallStages(
            globalValue.deserializeCallStages(CallStage.entries.toSet())
        )
    }

    fun getAppEnginePreferenceSync(packageName: String): Boolean? {
        return memoryCache["config_${packageName}_use_native"]?.toBooleanStrictOrNull()
    }

    fun useNativeLiveUpdatesSync(): Boolean {
        return memoryCache[USE_NATIVE_ENGINE]?.toBoolean() ?: false
    }

    fun isAppAllowedSync(packageName: String): Boolean {
        val raw = memoryCache[SettingsKeys.ALLOWED_PACKAGES] ?: return false
        return raw.deserializeSet().contains(packageName)
    }

    fun getAppPriorityOrderSync(): List<String> {
        val raw = memoryCache[SettingsKeys.PRIORITY_ORDER]
        return raw.deserializeList()
    }

    fun getAppPriorityFast(packageName: String): Int {
        val priorityList = getAppPriorityOrderSync()
        val index = priorityList.indexOf(packageName)
        return if (index == -1) Int.MAX_VALUE else index
    }

    fun getLimitModeSync(): IslandLimitMode {
        val raw = memoryCache["limit_mode"]
        return try {
            IslandLimitMode.valueOf(raw ?: IslandLimitMode.MOST_RECENT.name)
        } catch (_: Exception) {
            IslandLimitMode.MOST_RECENT
        }
    }

    fun getGlobalBlockedTermsSync(): Set<String> {
        return memoryCache[SettingsKeys.GLOBAL_BLOCKED_TERMS].deserializeSet()
    }

    fun isBlockedTermFast(packageName: String, title: String, text: String): Boolean {
        val appBlocked = getAppBlockedTermsSync(packageName)
        val globalBlocked = getGlobalBlockedTermsSync()
        if (appBlocked.isEmpty() && globalBlocked.isEmpty()) return false

        val combinedContent = "$title $text"
        if (appBlocked.isNotEmpty() && appBlocked.any { combinedContent.contains(it, ignoreCase = true) }) {
            return true
        }
        if (globalBlocked.isNotEmpty() && globalBlocked.any { combinedContent.contains(it, ignoreCase = true) }) {
            return true
        }
        return false
    }

    fun isDndModeEnabledSync(): Boolean {
        return memoryCache["dnd_mode_enabled"]?.toBoolean() ?: false
    }

    fun autoDetectDndSync(): Boolean {
        return memoryCache["auto_detect_dnd"]?.toBoolean() ?: false
    }

    fun getScreenRecordingTimeoutSync(): Int =
        memoryCache[SettingsKeys.SCREEN_RECORDING_TIMEOUT].toInt(SYSTEM_ISLAND_DEFAULT_TIMEOUT)

    fun getScreenRecordingLeftDesignSync(): com.d4viddf.hyperbridge.models.ScreenRecordingLeftDesign =
        memoryCache[SettingsKeys.SCREEN_RECORDING_LEFT_DESIGN]?.let {
            runCatching { com.d4viddf.hyperbridge.models.ScreenRecordingLeftDesign.valueOf(it) }.getOrNull()
        } ?: com.d4viddf.hyperbridge.models.ScreenRecordingLeftDesign.ICON_AND_TEXT

    fun getScreenRecordingRightDesignSync(): com.d4viddf.hyperbridge.models.ScreenRecordingRightDesign =
        memoryCache[SettingsKeys.SCREEN_RECORDING_RIGHT_DESIGN]?.let {
            runCatching { com.d4viddf.hyperbridge.models.ScreenRecordingRightDesign.valueOf(it) }.getOrNull()
        } ?: com.d4viddf.hyperbridge.models.ScreenRecordingRightDesign.TIMER

    fun getScreenRecordingDesignSync(): com.d4viddf.hyperbridge.models.ScreenRecordingDesignConfig =
        com.d4viddf.hyperbridge.models.ScreenRecordingDesignConfig(
            left = getScreenRecordingLeftDesignSync(),
            right = getScreenRecordingRightDesignSync()
        )

    fun isVpnIslandEnabledSync(): Boolean = memoryCache["vpn_island_enabled"]?.toBoolean(true) ?: true


    companion object {
        const val SYSTEM_ISLAND_DEFAULT_TIMEOUT = 4
    }

    @androidx.annotation.VisibleForTesting
    internal fun putInCacheForTesting(key: String, value: String) {
        memoryCache[key] = value
    }

    @androidx.annotation.VisibleForTesting
    internal fun removeFromCacheForTesting(key: String) {
        memoryCache.remove(key)
    }

    @androidx.annotation.VisibleForTesting
    internal fun clearCacheForTesting() {
        memoryCache.clear()
    }
}