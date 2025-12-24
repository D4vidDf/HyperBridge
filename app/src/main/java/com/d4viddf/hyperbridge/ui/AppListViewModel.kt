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
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    // --- PREFERENCE ACTIONS ---

    fun toggleApp(packageName: String, isEnabled: Boolean) {
        viewModelScope.launch {
            preferences.toggleApp(packageName, isEnabled)
        }
    }

    fun getAppConfig(packageName: String) = preferences.getAppConfig(packageName)

    fun updateAppConfig(pkg: String, type: NotificationType, enabled: Boolean) {
        viewModelScope.launch { preferences.updateAppConfig(pkg, type, enabled) }
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
}