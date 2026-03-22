package com.d4viddf.hyperbridge.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.hyperbridge.data.AppCacheManager
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.NavContent
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.theme.NavigationModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- DATA MODELS ---
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Bitmap?,
    val isBridged: Boolean = false,
    val isInstalled: Boolean = true,
    val category: AppCategory = AppCategory.OTHER
)

enum class AppCategory(val label: String) {
    ALL("All"), MUSIC("Music"), MAPS("Navigation"), TIMER("Productivity"), OTHER("Other")
}

enum class SortOption { NAME_AZ, NAME_ZA }

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val packageManager = application.packageManager
    private val preferences = AppPreferences(application)
    private val cacheManager = AppCacheManager(application)

    // [NEW] Theme Repository to resolve behavior overrides
    private val themeRepo = ThemeRepository(application)
    val activeTheme: StateFlow<HyperTheme?> = themeRepo.activeTheme

    private val _installedApps = MutableStateFlow<List<AppInfo>?>(null)

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    // Filters
    val activeSearch = MutableStateFlow("")
    val activeCategory = MutableStateFlow(AppCategory.ALL)
    val activeSort = MutableStateFlow(SortOption.NAME_AZ)
    val librarySearch = MutableStateFlow("")
    val libraryCategory = MutableStateFlow(AppCategory.ALL)
    val librarySort = MutableStateFlow(SortOption.NAME_AZ)

    // Helpers (Keyword Fallback)
    private val MUSIC_KEYS = listOf("music", "spotify", "youtube", "deezer", "tidal", "sound", "audio", "podcast", "radio")
    private val MAPS_KEYS = listOf("map", "nav", "waze", "gps", "transit", "uber", "cabify", "moovit")
    private val TIMER_KEYS = listOf("clock", "timer", "alarm", "stopwatch", "calendar", "todo", "task", "productivity")

    private val baseAppsFlow = combine(_installedApps, preferences.allowedPackagesFlow) { installed, allowedSet ->
        if (installed == null) {
            return@combine emptyList<AppInfo>()
        }

        // 1. Process Installed Apps
        val result = installed.map { app ->
            if (allowedSet.contains(app.packageName)) {
                cacheManager.cacheAppInfo(app.packageName, app.name, app.icon)
            }
            app.copy(isBridged = allowedSet.contains(app.packageName))
        }.toMutableList()

        // 2. Identify Missing (Uninstalled) Apps
        val installedPkgSet = installed.map { it.packageName }.toSet()
        val uninstalledPkgs = allowedSet.filter { !installedPkgSet.contains(it) }

        // 3. Reconstruct Uninstalled Apps from Cache
        uninstalledPkgs.forEach { pkg ->
            val cachedName = cacheManager.getCachedAppName(pkg)
            val cachedIcon = cacheManager.getCachedAppIcon(pkg)

            result.add(
                AppInfo(
                    name = cachedName,
                    packageName = pkg,
                    icon = cachedIcon,
                    isBridged = true,
                    isInstalled = false,
                    category = AppCategory.OTHER
                )
            )
        }
        result.toList()
    }

    val activeAppsState: StateFlow<List<AppInfo>> = combine(
        baseAppsFlow, activeSearch, activeCategory, activeSort
    ) { apps, query, category, sort ->
        applyFilters(apps.filter { it.isBridged }, query, category, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val libraryAppsState: StateFlow<List<AppInfo>> = combine(
        baseAppsFlow, librarySearch, libraryCategory, librarySort
    ) { apps, query, category, sort ->
        applyFilters(apps, query, category, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun applyFilters(list: List<AppInfo>, query: String, category: AppCategory, sort: SortOption): List<AppInfo> {
        var result = list
        if (query.isNotEmpty()) {
            result = result.filter {
                it.name.contains(query, true) || it.packageName.contains(query, true)
            }
        }
        if (category != AppCategory.ALL) {
            result = result.filter { it.category == category }
        }
        result = when (sort) {
            SortOption.NAME_AZ -> result.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            SortOption.NAME_ZA -> result.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
        }
        return result
    }

    init { refreshApps() }

    fun refreshApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val apps = getLaunchableApps()
            _installedApps.value = apps
            _isLoading.value = false
        }
    }

// ========================================================================
    //               EFFECTIVE BEHAVIOR LOGIC (THEME vs PREFS)
    // ========================================================================

    data class EffectiveAppConfig(
        val isManagedByTheme: Boolean,
        val activeTypes: Set<String>,
        val useNativeEngine: Boolean,
        val navigationOverride: NavigationModule?,
        val localNavContent: Pair<NavContent, NavContent> // Added for the bottom sheet
    )

    /**
     * Resolves the "true" settings for an app by layering:
     * Active Theme > Local App Preferences > Global Fallbacks
     */
    fun getEffectiveAppConfigFlow(packageName: String): Flow<EffectiveAppConfig> {
        return combine(
            preferences.getAppConfigFlow(packageName),
            preferences.globalNotificationTypesFlow,
            preferences.getEffectiveNavLayout(packageName), // Gets the fallback-resolved NavContent
            activeTheme
        ) { appPrefTypes, globalTypes, effectiveNavContent, theme ->

            val themeOverride = theme?.apps?.get(packageName)
            val isManaged = themeOverride != null

            // 1. Resolve Types (Theme -> AppPref -> Global)
            val effectiveTypes = when {
                themeOverride?.activeNotificationTypes != null -> themeOverride.activeNotificationTypes
                appPrefTypes != null -> appPrefTypes
                else -> globalTypes
            }

            // 2. Resolve Engine
            val effectiveEngine = when {
                themeOverride?.useNativeLiveUpdates != null -> themeOverride.useNativeLiveUpdates
                else -> true // Global Default
            }

            // 3. Resolve Navigation Visuals (Theme completely overrides local nav preferences)
            val effectiveNavVisuals = themeOverride?.navigation

            EffectiveAppConfig(
                isManagedByTheme = isManaged,
                activeTypes = effectiveTypes,
                useNativeEngine = effectiveEngine,
                navigationOverride = effectiveNavVisuals,
                localNavContent = effectiveNavContent
            )
        }
    }

    // --- PREFERENCE ACTIONS ---

    fun toggleApp(packageName: String, isEnabled: Boolean) {
        viewModelScope.launch {
            preferences.toggleApp(packageName, isEnabled)
        }
    }

    // Standard non-flow getter if needed by other parts of the app
    suspend fun getAppConfig(packageName: String) = preferences.getAppConfigFlow(packageName).first()

    /**
     * Updates notification types. Intelligently routes the save to the
     * Active Theme JSON if the theme is managing the app, otherwise saves locally.
     */
    fun updateAppConfig(pkg: String, type: NotificationType, enabled: Boolean) {
        viewModelScope.launch {
            val currentTheme = activeTheme.value
            val themeOverride = currentTheme?.apps?.get(pkg)

            if (currentTheme != null && themeOverride != null) {
                // The Theme is managing this app! Update the Theme JSON directly.
                val currentTypes = themeOverride.activeNotificationTypes ?: emptySet()
                val newTypes = if (enabled) currentTypes + type.name else currentTypes - type.name

                val updatedOverride = themeOverride.copy(activeNotificationTypes = newTypes)
                val updatedAppsMap = currentTheme.apps.toMutableMap().apply { put(pkg, updatedOverride) }
                val updatedTheme = currentTheme.copy(apps = updatedAppsMap)

                // Save to disk and reload the active theme state
                themeRepo.saveTheme(updatedTheme)
                themeRepo.activateTheme(updatedTheme.id)
            } else {
                // Not managed by theme. Save normally to local AppPreferences.
                preferences.updateAppConfig(pkg, type, enabled)
            }
        }
    }

    /**
     * Updates the per-app navigation content layout (Distance vs ETA, etc.)
     */
    fun updateAppNavLayout(pkg: String, left: NavContent?, right: NavContent?) {
        viewModelScope.launch {
            preferences.updateAppNavLayout(pkg, left, right)
        }
    }

    // --- ISLAND CONFIG ---
    val globalConfigFlow = preferences.globalConfigFlow
    fun getAppIslandConfig(packageName: String) = preferences.getAppIslandConfig(packageName)
    fun updateAppIslandConfig(packageName: String, config: IslandConfig) {
        viewModelScope.launch { preferences.updateAppIslandConfig(packageName, config) }
    }
    fun updateGlobalConfig(config: IslandConfig) {
        viewModelScope.launch { preferences.updateGlobalConfig(config) }
    }

    // --- BLOCKED TERMS ---
    val globalBlockedTermsFlow = preferences.globalBlockedTermsFlow
    fun setGlobalBlockedTerms(terms: Set<String>) {
        viewModelScope.launch { preferences.setGlobalBlockedTerms(terms) }
    }
    fun getAppBlockedTerms(packageName: String) = preferences.getAppBlockedTerms(packageName)
    fun updateAppBlockedTerms(packageName: String, terms: Set<String>) {
        viewModelScope.launch { preferences.setAppBlockedTerms(packageName, terms) }
    }

    // App Loader
    private suspend fun getLaunchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)

        resolveInfos.mapNotNull { resolveInfo ->
            try {
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg == getApplication<Application>().packageName) return@mapNotNull null

                val name = resolveInfo.loadLabel(packageManager).toString()
                val icon = resolveInfo.loadIcon(packageManager).toBitmap()

                // [NEW] Hybrid Category Detection
                var cat = AppCategory.OTHER

                // 1. Try Android Manifest Category (API 26+)
                val appInfo = resolveInfo.activityInfo.applicationInfo
                cat = when (appInfo.category) {
                    ApplicationInfo.CATEGORY_AUDIO -> AppCategory.MUSIC
                    ApplicationInfo.CATEGORY_MAPS -> AppCategory.MAPS
                    ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.TIMER
                    else -> AppCategory.OTHER
                }

                // 2. Fallback to Keywords if Manifest failed (returned OTHER or -1)
                if (cat == AppCategory.OTHER) {
                    cat = when {
                        MUSIC_KEYS.any { pkg.contains(it, true) || name.contains(it, true) } -> AppCategory.MUSIC
                        MAPS_KEYS.any { pkg.contains(it, true) || name.contains(it, true) } -> AppCategory.MAPS
                        TIMER_KEYS.any { pkg.contains(it, true) || name.contains(it, true) } -> AppCategory.TIMER
                        else -> AppCategory.OTHER
                    }
                }

                AppInfo(name, pkg, icon, category = cat, isInstalled = true)
            } catch (e: Exception) { null }
        }.distinctBy { it.packageName }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable) return this.bitmap
        val width = if (intrinsicWidth > 0) intrinsicWidth else 1
        val height = if (intrinsicHeight > 0) intrinsicHeight else 1
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }

    fun updateAppEngine(pkg: String, useNative: Boolean) {
        viewModelScope.launch {
            val currentTheme = activeTheme.value
            val themeOverride = currentTheme?.apps?.get(pkg)

            if (currentTheme != null && themeOverride != null) {
                // The Theme is managing this app! Update the Theme JSON directly.
                val updatedOverride = themeOverride.copy(useNativeLiveUpdates = useNative)
                val updatedAppsMap = currentTheme.apps.toMutableMap().apply { put(pkg, updatedOverride) }
                val updatedTheme = currentTheme.copy(apps = updatedAppsMap)

                // Save to disk and reload the active theme state
                themeRepo.saveTheme(updatedTheme)
                themeRepo.activateTheme(updatedTheme.id)
            } else {
                // Not managed by theme. Save normally to local AppPreferences.
                // NOTE: Make sure you have `updateAppEnginePreference(pkg, useNative)` in your AppPreferences!
                preferences.updateAppEnginePreference(pkg, useNative)
            }
        }
    }
}