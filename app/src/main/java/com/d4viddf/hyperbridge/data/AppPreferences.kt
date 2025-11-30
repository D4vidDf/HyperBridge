package com.d4viddf.hyperbridge.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.IslandLimitMode
import com.d4viddf.hyperbridge.models.NavContent
import com.d4viddf.hyperbridge.models.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.IOException

// 1. SINGLETON DATASTORE SETUP
// Using a private extension on Context that we invoke on applicationContext ensures valid singleton behavior.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPreferences(context: Context) {

    // Ensure we always use Application Context to avoid memory leaks and context mismatches
    private val dataStore = context.applicationContext.dataStore

    companion object {
        // Keys
        private val ALLOWED_PACKAGES_KEY = stringSetPreferencesKey("allowed_packages")
        private val SETUP_COMPLETE_KEY = booleanPreferencesKey("setup_complete")
        private val LAST_VERSION_CODE_KEY = intPreferencesKey("last_version_code")
        private val PRIORITY_EDU_KEY = booleanPreferencesKey("priority_edu_shown")
        private val LIMIT_MODE_KEY = stringPreferencesKey("limit_mode")
        private val PRIORITY_ORDER_KEY = stringPreferencesKey("priority_app_order")
        private val GLOBAL_FLOAT_KEY = booleanPreferencesKey("global_float")
        private val GLOBAL_SHADE_KEY = booleanPreferencesKey("global_shade")
        private val GLOBAL_TIMEOUT_KEY = longPreferencesKey("global_timeout")
        private val NAV_LEFT_CONTENT_KEY = stringPreferencesKey("nav_left_content")
        private val NAV_RIGHT_CONTENT_KEY = stringPreferencesKey("nav_right_content")
    }

    // Helper to catch IOExceptions (e.g. during boot or file corruption)
    private val safeData: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("AppPreferences", "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    // --- CORE ---
    val allowedPackagesFlow: Flow<Set<String>> = safeData.map { it[ALLOWED_PACKAGES_KEY] ?: emptySet() }

    // FIX: Returns FALSE by default (First Install), never null.
    val isSetupComplete: Flow<Boolean> = safeData.map { it[SETUP_COMPLETE_KEY] ?: false }

    val lastSeenVersion: Flow<Int> = safeData.map { it[LAST_VERSION_CODE_KEY] ?: 0 }
    val isPriorityEduShown: Flow<Boolean> = safeData.map { it[PRIORITY_EDU_KEY] ?: false }

    suspend fun setSetupComplete(isComplete: Boolean) { dataStore.edit { it[SETUP_COMPLETE_KEY] = isComplete } }
    suspend fun setLastSeenVersion(versionCode: Int) { dataStore.edit { it[LAST_VERSION_CODE_KEY] = versionCode } }
    suspend fun setPriorityEduShown(shown: Boolean) { dataStore.edit { it[PRIORITY_EDU_KEY] = shown } }

    suspend fun toggleApp(packageName: String, isEnabled: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[ALLOWED_PACKAGES_KEY] ?: emptySet()
            prefs[ALLOWED_PACKAGES_KEY] = if (isEnabled) current + packageName else current - packageName
        }
    }

    // --- LIMITS ---
    val limitModeFlow: Flow<IslandLimitMode> = safeData.map {
        try { IslandLimitMode.valueOf(it[LIMIT_MODE_KEY] ?: IslandLimitMode.MOST_RECENT.name) } catch(e: Exception) { IslandLimitMode.MOST_RECENT }
    }
    val appPriorityListFlow: Flow<List<String>> = safeData.map { it[PRIORITY_ORDER_KEY]?.split(",") ?: emptyList() }

    suspend fun setLimitMode(mode: IslandLimitMode) { dataStore.edit { it[LIMIT_MODE_KEY] = mode.name } }
    suspend fun setAppPriorityOrder(order: List<String>) { dataStore.edit { it[PRIORITY_ORDER_KEY] = order.joinToString(",") } }

    // --- TYPE CONFIG ---
    fun getAppConfig(packageName: String): Flow<Set<String>> {
        val key = stringSetPreferencesKey("config_$packageName")
        return safeData.map { it[key] ?: NotificationType.entries.map { t -> t.name }.toSet() }
    }
    suspend fun updateAppConfig(packageName: String, type: NotificationType, isEnabled: Boolean) {
        val key = stringSetPreferencesKey("config_$packageName")
        dataStore.edit { prefs ->
            val current = prefs[key] ?: NotificationType.entries.map { it.name }.toSet()
            prefs[key] = if (isEnabled) current + type.name else current - type.name
        }
    }

    // --- ISLAND CONFIG ---
    val globalConfigFlow: Flow<IslandConfig> = safeData.map {
        IslandConfig(it[GLOBAL_FLOAT_KEY] ?: true, it[GLOBAL_SHADE_KEY] ?: true, it[GLOBAL_TIMEOUT_KEY] ?: 5000L)
    }
    suspend fun updateGlobalConfig(config: IslandConfig) {
        dataStore.edit {
            config.isFloat?.let { v -> it[GLOBAL_FLOAT_KEY] = v }
            config.isShowShade?.let { v -> it[GLOBAL_SHADE_KEY] = v }
            config.timeout?.let { v -> it[GLOBAL_TIMEOUT_KEY] = v }
        }
    }
    fun getAppIslandConfig(packageName: String): Flow<IslandConfig> {
        return safeData.map {
            IslandConfig(
                it[booleanPreferencesKey("config_${packageName}_float")],
                it[booleanPreferencesKey("config_${packageName}_shade")],
                it[longPreferencesKey("config_${packageName}_timeout")]
            )
        }
    }
    suspend fun updateAppIslandConfig(packageName: String, config: IslandConfig) {
        dataStore.edit { prefs ->
            val f = booleanPreferencesKey("config_${packageName}_float")
            val s = booleanPreferencesKey("config_${packageName}_shade")
            val t = longPreferencesKey("config_${packageName}_timeout")
            if (config.isFloat != null) prefs[f] = config.isFloat else prefs.remove(f)
            if (config.isShowShade != null) prefs[s] = config.isShowShade else prefs.remove(s)
            if (config.timeout != null) prefs[t] = config.timeout else prefs.remove(t)
        }
    }

    // --- NAVIGATION LAYOUT ---
    val globalNavLayoutFlow: Flow<Pair<NavContent, NavContent>> = safeData.map { prefs ->
        val left = try { NavContent.valueOf(prefs[NAV_LEFT_CONTENT_KEY] ?: NavContent.DISTANCE_ETA.name) } catch (e: Exception) { NavContent.DISTANCE_ETA }
        val right = try { NavContent.valueOf(prefs[NAV_RIGHT_CONTENT_KEY] ?: NavContent.INSTRUCTION.name) } catch (e: Exception) { NavContent.INSTRUCTION }
        left to right
    }

    suspend fun setGlobalNavLayout(left: NavContent, right: NavContent) {
        dataStore.edit {
            it[NAV_LEFT_CONTENT_KEY] = left.name
            it[NAV_RIGHT_CONTENT_KEY] = right.name
        }
    }

    fun getAppNavLayout(packageName: String): Flow<Pair<NavContent?, NavContent?>> {
        return safeData.map { prefs ->
            val lKey = stringPreferencesKey("config_${packageName}_nav_left")
            val rKey = stringPreferencesKey("config_${packageName}_nav_right")
            val l = prefs[lKey]?.let { try { NavContent.valueOf(it) } catch(e: Exception){null} }
            val r = prefs[rKey]?.let { try { NavContent.valueOf(it) } catch(e: Exception){null} }
            l to r
        }
    }

    fun getEffectiveNavLayout(packageName: String): Flow<Pair<NavContent, NavContent>> {
        return combine(getAppNavLayout(packageName), globalNavLayoutFlow) { app, global ->
            (app.first ?: global.first) to (app.second ?: global.second)
        }
    }

    suspend fun updateAppNavLayout(packageName: String, left: NavContent?, right: NavContent?) {
        dataStore.edit { prefs ->
            val lKey = stringPreferencesKey("config_${packageName}_nav_left")
            val rKey = stringPreferencesKey("config_${packageName}_nav_right")
            if (left != null) prefs[lKey] = left.name else prefs.remove(lKey)
            if (right != null) prefs[rKey] = right.name else prefs.remove(rKey)
        }
    }
}