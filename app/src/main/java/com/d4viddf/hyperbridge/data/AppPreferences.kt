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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// We keep this ONLY for the one-time migration logic
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

    // --- HELPERS FOR ROOM ---
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

    // --- CORE ---
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

    // --- LIMITS ---
    val limitModeFlow: Flow<IslandLimitMode> = dao.getSettingFlow("limit_mode").map {
        try { IslandLimitMode.valueOf(it ?: IslandLimitMode.MOST_RECENT.name) } catch(e: Exception) { IslandLimitMode.MOST_RECENT }
    }
    val appPriorityListFlow: Flow<List<String>> = dao.getSettingFlow(SettingsKeys.PRIORITY_ORDER).map { it.deserializeList() }

    suspend fun setLimitMode(mode: IslandLimitMode) = save("limit_mode", mode.name)
    suspend fun setAppPriorityOrder(order: List<String>) = save(SettingsKeys.PRIORITY_ORDER, order.joinToString(","))

    // --- TYPE CONFIG ---
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

    // --- ISLAND CONFIG ---
    private fun sanitizeTimeout(raw: Long?): Long {
        val value = raw ?: 5L
        return if (value > 60) value / 1000 else value
    }

    val globalConfigFlow: Flow<IslandConfig> = combine(
        dao.getSettingFlow(SettingsKeys.GLOBAL_FLOAT),
        dao.getSettingFlow(SettingsKeys.GLOBAL_SHADE),
        dao.getSettingFlow(SettingsKeys.GLOBAL_TIMEOUT)
    ) { f, s, t ->
        IslandConfig(f.toBoolean(true), s.toBoolean(true), sanitizeTimeout(t?.toLongOrNull()))
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
                if (t != null) sanitizeTimeout(t.toLong()) else null
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

    // --- NAVIGATION & BLOCKED TERMS ---
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

    // --- WIDGET CONFIG ---
    private val WIDGET_IDS_DB_KEY = "saved_widget_ids_list" // Changed key name

    // Note: We keep these keys singular for now.
    // Ideally, you'd want "widget_{id}_timeout", but for now, they share settings or overwrite.
    private val WIDGET_SHOWN_DB_KEY = "saved_widget_shown"
    private val WIDGET_TIMEOUT_DB_KEY = "saved_widget_timeout"

    // Flow returns a LIST of Integers
    val savedWidgetIdsFlow: Flow<List<Int>> = dao.getSettingFlow(WIDGET_IDS_DB_KEY).map { str ->
        str?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
    }

    val savedWidgetConfigFlow: Flow<IslandConfig> = combine(
        dao.getSettingFlow(WIDGET_SHOWN_DB_KEY),
        dao.getSettingFlow(WIDGET_TIMEOUT_DB_KEY)
    ) { shown, timeout ->
        IslandConfig(
            isFloat = true,
            isShowShade = shown?.toBoolean() ?: false,
            timeout = timeout?.toLongOrNull() ?: 5000L
        )
    }

    suspend fun saveWidgetConfig(id: Int, isShowShade: Boolean, timeout: Long) {
        // 1. Update the List of IDs (Add if not exists)
        val currentStr = dao.getSetting(WIDGET_IDS_DB_KEY) ?: ""
        val currentIds = currentStr.split(",").filter { it.isNotEmpty() }.toMutableSet()
        currentIds.add(id.toString())

        save(WIDGET_IDS_DB_KEY, currentIds.joinToString(","))

        // 2. Save Config options
        save(WIDGET_SHOWN_DB_KEY, isShowShade.toString())
        save(WIDGET_TIMEOUT_DB_KEY, timeout.toString())
    }

    suspend fun removeWidgetId(id: Int) {
        val currentStr = dao.getSetting(WIDGET_IDS_DB_KEY) ?: ""
        val currentIds = currentStr.split(",").filter { it.isNotEmpty() }.toMutableList()
        currentIds.remove(id.toString())
        save(WIDGET_IDS_DB_KEY, currentIds.joinToString(","))
    }
}