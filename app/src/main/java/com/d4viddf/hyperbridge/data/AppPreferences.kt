package com.d4viddf.hyperbridge.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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

// Singleton DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPreferences(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    companion object {
        // ... (Keys remain the same)
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

    // ... (Core Flows remain the same) ...
    private val safeData: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }

    val allowedPackagesFlow: Flow<Set<String>> = safeData.map { it[ALLOWED_PACKAGES_KEY] ?: emptySet() }
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

    // ... (Limits & Types remain the same) ...
    val limitModeFlow: Flow<IslandLimitMode> = safeData.map {
        try { IslandLimitMode.valueOf(it[LIMIT_MODE_KEY] ?: IslandLimitMode.MOST_RECENT.name) } catch(e: Exception) { IslandLimitMode.MOST_RECENT }
    }
    val appPriorityListFlow: Flow<List<String>> = safeData.map { it[PRIORITY_ORDER_KEY]?.split(",") ?: emptyList() }

    suspend fun setLimitMode(mode: IslandLimitMode) { dataStore.edit { it[LIMIT_MODE_KEY] = mode.name } }
    suspend fun setAppPriorityOrder(order: List<String>) { dataStore.edit { it[PRIORITY_ORDER_KEY] = order.joinToString(",") } }

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

    // --- ISLAND CONFIG (WITH MIGRATION LOGIC) ---

    // Helper to migrate legacy MS values (e.g. 5000) to Seconds (5)
    private fun sanitizeTimeout(raw: Long?): Long {
        val value = raw ?: 5L // Default to 5s
        return if (value > 60) {
            // If value > 60, it's definitely legacy Milliseconds (e.g. 5000)
            value / 1000
        } else {
            value
        }
    }

    val globalConfigFlow: Flow<IslandConfig> = safeData.map {
        IslandConfig(
            isFloat = it[GLOBAL_FLOAT_KEY] ?: true,
            isShowShade = it[GLOBAL_SHADE_KEY] ?: true,
            // Fix: Apply migration logic
            timeout = sanitizeTimeout(it[GLOBAL_TIMEOUT_KEY])
        )
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
            val timeout = it[longPreferencesKey("config_${packageName}_timeout")]

            IslandConfig(
                isFloat = it[booleanPreferencesKey("config_${packageName}_float")],
                isShowShade = it[booleanPreferencesKey("config_${packageName}_shade")],
                // Fix: Apply migration logic if override exists
                timeout = if (timeout != null) sanitizeTimeout(timeout) else null
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

    // --- NAVIGATION LAYOUT (Existing code) ---
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

    // --- BLOCKED TERMS (Existing code) ---
    private val GLOBAL_BLOCKED_TERMS_KEY = stringSetPreferencesKey("global_blocked_terms")

    val globalBlockedTermsFlow: Flow<Set<String>> = safeData.map { it[GLOBAL_BLOCKED_TERMS_KEY] ?: emptySet() }

    suspend fun setGlobalBlockedTerms(terms: Set<String>) {
        dataStore.edit { it[GLOBAL_BLOCKED_TERMS_KEY] = terms }
    }

    fun getAppBlockedTerms(packageName: String): Flow<Set<String>> {
        val key = stringSetPreferencesKey("config_${packageName}_blocked")
        return safeData.map { it[key] ?: emptySet() }
    }

    suspend fun setAppBlockedTerms(packageName: String, terms: Set<String>) {
        val key = stringSetPreferencesKey("config_${packageName}_blocked")
        dataStore.edit { it[key] = terms }
    }
}