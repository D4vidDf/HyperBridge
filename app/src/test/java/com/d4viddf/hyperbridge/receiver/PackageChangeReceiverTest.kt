package com.d4viddf.hyperbridge.receiver

import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.db.AppSetting
import com.d4viddf.hyperbridge.data.db.SettingsDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PackageChangeReceiverTest {

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
    private lateinit var receiver: PackageChangeReceiver

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
        receiver = PackageChangeReceiver()
    }

    @Test
    fun handleNewPackageAddsAppWhenBridgeAllAndAutoAddAreEnabled() = runBlocking {
        preferences.setBridgeAllAppsEnabled(true)
        assertTrue(preferences.isBridgeAllAppsEnabledSync())
        assertTrue(preferences.isAutoAddNewAppsEnabledSync())

        receiver.handleNewPackage(null, "com.new.app", preferences)

        assertTrue(preferences.isAppAllowedSync("com.new.app"))
    }

    @Test
    fun handleNewPackageDoesNotAddAppWhenAutoAddIsDisabled() = runBlocking {
        preferences.setBridgeAllAppsEnabled(true)
        preferences.setAutoAddNewApps(false)
        assertTrue(preferences.isBridgeAllAppsEnabledSync())
        assertFalse(preferences.isAutoAddNewAppsEnabledSync())

        receiver.handleNewPackage(null, "com.new.app", preferences)

        val allowedPackages = fakeDao.getSetting("allowed_packages") ?: ""
        assertFalse(allowedPackages.contains("com.new.app"))
    }

    @Test
    fun handleNewPackageDoesNotAddAppWhenBridgeAllIsDisabled() = runBlocking {
        preferences.setBridgeAllAppsEnabled(false)
        preferences.setAutoAddNewApps(true)

        receiver.handleNewPackage(null, "com.new.app", preferences)

        val allowedPackages = fakeDao.getSetting("allowed_packages") ?: ""
        assertFalse(allowedPackages.contains("com.new.app"))
    }

    @Test
    fun handleNewPackageDoesNotAddAppWhenNotLibraryApp() = runBlocking {
        preferences.setBridgeAllAppsEnabled(true)
        preferences.setAutoAddNewApps(true)

        receiver.handleNewPackage(
            context = null,
            packageName = "com.hidden.system.daemon",
            preferences = preferences,
            isLibraryApp = false
        )

        assertFalse(preferences.isAppAllowedSync("com.hidden.system.daemon"))
        val allowedPackages = fakeDao.getSetting("allowed_packages") ?: ""
        assertFalse(allowedPackages.contains("com.hidden.system.daemon"))
    }
}
