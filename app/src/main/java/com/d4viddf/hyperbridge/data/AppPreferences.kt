package com.d4viddf.hyperbridge.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.data.db.AppSetting
import com.d4viddf.hyperbridge.data.db.SettingsKeys
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

private val Context.legacyDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPreferences(context: Context) {

    private val dao = AppDatabase.getDatabase(context).settingsDao()
    private val legacyDataStore = context.applicationContext.legacyDataStore

    init {
        // --- MIGRATION LOGIC ---
        CoroutineScope(Dispatchers.IO).launch {
            try {
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
            } catch (e: Exception) {
                e.printStackTrace()
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

    private suspend fun save(key: String, value: String) {
        dao.insert(AppSetting(key, value))
    }

    private suspend fun remove(key: String) {
        dao.delete(key)
    }

    // --- CORE SETTINGS ---
    val allowedPackagesFlow: Flow<Set<String>> = dao.getSettingFlow(SettingsKeys.ALLOWED_PACKAGES).map { it.deserializeSet() }
    val isSetupComplete: Flow<Boolean> = dao.getSettingFlow(SettingsKeys.SETUP_COMPLETE).map { it.toBoolean(false) }
    val lastSeenVersion: Flow<Int> = dao.getSettingFlow(SettingsKeys.LAST_VERSION).map { it.toInt(0) }
    val isPriorityEduShown: Flow<Boolean> = dao.getSettingFlow(SettingsKeys.PRIORITY_EDU).map { it.toBoolean(false) }

    suspend fun setSetupComplete(isComplete: Boolean) = save(SettingsKeys.SETUP_COMPLETE, isComplete.toString())
    suspend fun setLastSeenVersion(versionCode: Int) = save(SettingsKeys.LAST_VERSION, versionCode.toString())
    suspend fun setPriorityEduShown(shown: Boolean) = save(SettingsKeys.PRIORITY_EDU, shown.toString())

    suspend fun toggleApp(packageName: String, isEnabled: Boolean) {
        val currentString = dao.getSetting(SettingsKeys.ALLOWED_PACKAGES)
        val currentSet = currentString.deserializeSet()
        val newSet = if (isEnabled) currentSet + packageName else currentSet - packageName
        save(SettingsKeys.ALLOWED_PACKAGES, newSet.serialize())
    }

    // ========================================================================
    //                        THEME ENGINE (NEW)
    // ========================================================================

    /**
     * Holds the ID (folder name) of the currently active theme.
     * Null means the system default (no theme).
     */
    val activeThemeIdFlow: Flow<String?> = dao.getSettingFlow("active_theme_id")

    suspend fun setActiveThemeId(id: String?) {
        if (id == null) {
            remove("active_theme_id")
        } else {
            save("active_theme_id", id)
        }
    }

    // ========================================================================

    // --- LIMITS & PRIORITY ---
    val limitModeFlow: Flow<IslandLimitMode> = dao.getSettingFlow("limit_mode").map {
        try { IslandLimitMode.valueOf(it ?: IslandLimitMode.MOST_RECENT.name) } catch(e: Exception) { IslandLimitMode.MOST_RECENT }
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

    suspend fun updateAppConfig(packageName: String, type: NotificationType, isEnabled: Boolean) {
        val key = "config_$packageName"
        val currentStr = dao.getSetting(key)
        val currentSet = currentStr?.deserializeSet() ?: NotificationType.entries.map { it.name }.toSet()
        val newSet = if (isEnabled) currentSet + type.name else currentSet - type.name
        save(key, newSet.serialize())
    }

    // --- ISLAND CONFIG (Standard Notifications) ---
    private fun sanitizeTimeout(raw: Long?): Long {
        val value = raw ?: 5L
        return if (value > 60) value / 1000 else value
    }

    val globalConfigFlow: Flow<IslandConfig> = combine(
        dao.getSettingFlow(SettingsKeys.GLOBAL_FLOAT),
        dao.getSettingFlow(SettingsKeys.GLOBAL_SHADE),
        dao.getSettingFlow(SettingsKeys.GLOBAL_TIMEOUT)
    ) { f, s, t ->
        IslandConfig(f.toBoolean(true), s.toBoolean(true), t?.toIntOrNull())
    }

    suspend fun updateGlobalConfig(config: IslandConfig) {
        config.isFloat?.let { save(SettingsKeys.GLOBAL_FLOAT, it.toString()) }
        config.isShowShade?.let { save(SettingsKeys.GLOBAL_SHADE, it.toString()) }
        config.timeout?.let { save(SettingsKeys.GLOBAL_TIMEOUT, it.toString()) }
    }

    fun getAppIslandConfig(packageName: String): Flow<IslandConfig> {
        return combine(
            dao.getSettingFlow("config_${packageName}_float"),
            dao.getSettingFlow("config_${packageName}_shade"),
            dao.getSettingFlow("config_${packageName}_timeout")
        ) { f, s, t ->
            IslandConfig(
                f?.toBoolean(),
                s?.toBoolean(),
                t?.toIntOrNull()
            )
        }
    }

    suspend fun updateAppIslandConfig(packageName: String, config: IslandConfig) {
        val fKey = "config_${packageName}_float"
        val sKey = "config_${packageName}_shade"
        val tKey = "config_${packageName}_timeout"

        if (config.isFloat != null) save(fKey, config.isFloat.toString()) else remove(fKey)
        if (config.isShowShade != null) save(sKey, config.isShowShade.toString()) else remove(sKey)
        if (config.timeout != null) save(tKey, config.timeout.toString()) else remove(tKey)
    }

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
        val left = try { NavContent.valueOf(l ?: NavContent.DISTANCE_ETA.name) } catch (e: Exception) { NavContent.DISTANCE_ETA }
        val right = try { NavContent.valueOf(r ?: NavContent.INSTRUCTION.name) } catch (e: Exception) { NavContent.INSTRUCTION }
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
            val left = l?.let { try { NavContent.valueOf(it) } catch(e: Exception){null} }
            val right = r?.let { try { NavContent.valueOf(it) } catch(e: Exception){null} }
            left to right
        }
    }

    fun getEffectiveNavLayout(packageName: String): Flow<Pair<NavContent, NavContent>> {
        return combine(
            dao.getSettingFlow("config_${packageName}_nav_left"),
            dao.getSettingFlow("config_${packageName}_nav_right"),
            globalNavLayoutFlow
        ) { appL, appR, global ->
            val left = appL?.let { try { NavContent.valueOf(it) } catch(e: Exception){null} } ?: global.first
            val right = appR?.let { try { NavContent.valueOf(it) } catch(e: Exception){null} } ?: global.second
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

    /**
     * Provides a Flow of all currently saved Widget IDs.
     */
    val savedWidgetIdsFlow: Flow<List<Int>> = dao.getSettingFlow(WIDGET_IDS_DB_KEY).map { str ->
        str?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
    }

    /**
     * Gets configuration specific to a single Widget ID.
     * Returns the clean WidgetConfig class.
     */
    fun getWidgetConfigFlow(id: Int): Flow<WidgetConfig> {
        // [FIX] 'combine' only supports up to 5 args nicely. For 6+, it returns an Array<T>.
        return combine(
            dao.getSettingFlow("widget_${id}_shown"),
            dao.getSettingFlow("widget_${id}_timeout"),
            dao.getSettingFlow("widget_${id}_size"),
            dao.getSettingFlow("widget_${id}_mode"),
            dao.getSettingFlow("widget_${id}_auto_update"),
            dao.getSettingFlow("widget_${id}_update_interval")
        ) { args: Array<String?> ->
            // Manually unpack the array
            val shown = args[0]
            val timeout = args[1]
            val sizeStr = args[2]
            val modeStr = args[3]
            val autoStr = args[4]
            val intervalStr = args[5]

            val sizeEnum = try { WidgetSize.valueOf(sizeStr ?: WidgetSize.MEDIUM.name) } catch (e: Exception) { WidgetSize.MEDIUM }
            val modeEnum = try { WidgetRenderMode.valueOf(modeStr ?: WidgetRenderMode.INTERACTIVE.name) } catch (e: Exception) { WidgetRenderMode.INTERACTIVE }

            WidgetConfig(
                isShowShade = shown.toBoolean(true),
                timeout = timeout.toInt(5),
                size = sizeEnum,
                renderMode = modeEnum,
                autoUpdate = autoStr.toBoolean(false),
                updateIntervalMinutes = intervalStr.toInt(15)
            )
        }
    }

    /**
     * Saves configuration for a specific widget ID.
     */
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
}