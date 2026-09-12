package com.d4viddf.hyperbridge.data

import com.d4viddf.hyperbridge.data.db.AppSetting
import com.d4viddf.hyperbridge.data.db.SettingsDao
import com.d4viddf.hyperbridge.data.db.SettingsKeys
import com.d4viddf.hyperbridge.models.IslandLimitMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppPreferencesCacheTest {

    private class FakeSettingsDao : SettingsDao {
        val settingsMap = mutableMapOf<String, String>()
        val flow = MutableStateFlow<List<AppSetting>>(emptyList())

        override fun getSettingFlow(key: String): Flow<String?> =
            MutableStateFlow(settingsMap[key])

        override suspend fun getSetting(key: String): String? =
            settingsMap[key]

        override suspend fun insert(setting: AppSetting) {
            settingsMap[setting.key] = setting.value
            flow.value = settingsMap.map { AppSetting(it.key, it.value) }
        }

        override suspend fun delete(key: String) {
            settingsMap.remove(key)
            flow.value = settingsMap.map { AppSetting(it.key, it.value) }
        }

        override fun getAllFlow(): Flow<List<AppSetting>> = flow

        override suspend fun getAllSync(): List<AppSetting> =
            settingsMap.map { AppSetting(it.key, it.value) }

        override suspend fun insertAll(settings: List<AppSetting>) {
            settings.forEach { settingsMap[it.key] = it.value }
            flow.value = settingsMap.map { AppSetting(it.key, it.value) }
        }
    }

    private lateinit var fakeDao: FakeSettingsDao
    private lateinit var preferences: AppPreferences

    @Before
    fun setUp() {
        fakeDao = FakeSettingsDao()
        preferences = AppPreferences(
            dao = fakeDao,
            legacyDataStore = null,
            context = null,
            scope = CoroutineScope(Dispatchers.Unconfined)
        )
        preferences.clearCacheForTesting()
    }

    @Test
    fun isAppAllowedSyncEvaluatesInMemoryImmediately() {
        assertFalse(preferences.isAppAllowedSync("com.whatsapp"))

        preferences.putInCacheForTesting(SettingsKeys.ALLOWED_PACKAGES, "com.whatsapp,com.telegram")
        assertTrue(preferences.isAppAllowedSync("com.whatsapp"))
        assertTrue(preferences.isAppAllowedSync("com.telegram"))
        assertFalse(preferences.isAppAllowedSync("com.other.app"))

        preferences.putInCacheForTesting(SettingsKeys.ALLOWED_PACKAGES, "com.telegram")
        assertFalse(preferences.isAppAllowedSync("com.whatsapp"))
        assertTrue(preferences.isAppAllowedSync("com.telegram"))
    }

    @Test
    fun getAppPriorityFastAssignsZeroBasedRankOrMaxInt() {
        preferences.putInCacheForTesting(SettingsKeys.PRIORITY_ORDER, "com.whatsapp,com.slack,com.spotify")

        assertEquals(0, preferences.getAppPriorityFast("com.whatsapp"))
        assertEquals(1, preferences.getAppPriorityFast("com.slack"))
        assertEquals(2, preferences.getAppPriorityFast("com.spotify"))
        assertEquals(Int.MAX_VALUE, preferences.getAppPriorityFast("com.unknown.app"))
    }

    @Test
    fun getLimitModeSyncFallsBackToMostRecentAndParsesCorrectly() {
        assertEquals(IslandLimitMode.MOST_RECENT, preferences.getLimitModeSync())

        preferences.putInCacheForTesting("limit_mode", IslandLimitMode.FIRST_COME.name)
        assertEquals(IslandLimitMode.FIRST_COME, preferences.getLimitModeSync())

        preferences.putInCacheForTesting("limit_mode", IslandLimitMode.PRIORITY.name)
        assertEquals(IslandLimitMode.PRIORITY, preferences.getLimitModeSync())

        preferences.putInCacheForTesting("limit_mode", "INVALID_VALUE")
        assertEquals(IslandLimitMode.MOST_RECENT, preferences.getLimitModeSync())
    }

    @Test
    fun isBlockedTermFastChecksBothAppSpecificAndGlobalTermsCaseInsensitively() {
        preferences.putInCacheForTesting(SettingsKeys.GLOBAL_BLOCKED_TERMS, "promo,discount")
        preferences.putInCacheForTesting("config_com.shopping.app_blocked", "sale,clearance")

        assertTrue(preferences.isBlockedTermFast("com.other.app", "Special Promo!", "Check it out"))
        assertTrue(preferences.isBlockedTermFast("com.other.app", "Special Offer", "Get your DISCOUNT now"))
        assertFalse(preferences.isBlockedTermFast("com.other.app", "Hello", "Normal text"))

        assertTrue(preferences.isBlockedTermFast("com.shopping.app", "Summer SALE", "Items are 50% off"))
        assertTrue(preferences.isBlockedTermFast("com.shopping.app", "Clearance Event", "Everything must go"))
        assertFalse(preferences.isBlockedTermFast("com.other.app", "Summer Sale", "Not blocked for other apps"))
    }

    @Test
    fun isDndModeEnabledAndAutoDetectDndReadFast() {
        assertFalse(preferences.isDndModeEnabledSync())
        assertFalse(preferences.autoDetectDndSync())

        preferences.putInCacheForTesting("dnd_mode_enabled", "true")
        preferences.putInCacheForTesting("auto_detect_dnd", "true")

        assertTrue(preferences.isDndModeEnabledSync())
        assertTrue(preferences.autoDetectDndSync())
    }
}
