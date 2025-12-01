package com.d4viddf.hyperbridge.ui

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- DATA MODELS ---
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Bitmap,
    val isBridged: Boolean = false,
    val category: AppCategory = AppCategory.OTHER
)

enum class AppCategory(val label: String) {
    ALL("All"), MUSIC("Music"), MAPS("Navigation"), TIMER("Productivity"), OTHER("Other")
}

enum class SortOption { NAME_AZ, NAME_ZA }

// --- VIEW MODEL ---
class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val packageManager = application.packageManager
    private val preferences = AppPreferences(application)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    // Filters
    val activeSearch = MutableStateFlow("")
    val activeCategory = MutableStateFlow(AppCategory.ALL)
    val activeSort = MutableStateFlow(SortOption.NAME_AZ)
    val librarySearch = MutableStateFlow("")
    val libraryCategory = MutableStateFlow(AppCategory.ALL)
    val librarySort = MutableStateFlow(SortOption.NAME_AZ)

    // Helpers
    private val MUSIC_KEYS = listOf("music", "spotify", "youtube", "deezer", "tidal", "sound", "audio", "podcast")
    private val MAPS_KEYS = listOf("map", "nav", "waze", "gps", "transit", "uber", "cabify")
    private val TIMER_KEYS = listOf("clock", "timer", "alarm", "stopwatch", "calendar", "todo")

    private val baseAppsFlow = combine(_installedApps, preferences.allowedPackagesFlow) { apps, allowedSet ->
        apps.map { app -> app.copy(isBridged = allowedSet.contains(app.packageName)) }
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

    init { loadInstalledApps() }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _installedApps.value = getLaunchableApps()
            _isLoading.value = false
        }
    }

    // --- PREFERENCE ACTIONS ---

    fun toggleApp(packageName: String, isEnabled: Boolean) {
        viewModelScope.launch { preferences.toggleApp(packageName, isEnabled) }
    }

    fun getAppConfig(packageName: String) = preferences.getAppConfig(packageName)

    fun updateAppConfig(pkg: String, type: NotificationType, enabled: Boolean) {
        viewModelScope.launch { preferences.updateAppConfig(pkg, type, enabled) }
    }

    // --- ISLAND APPEARANCE BRIDGES ---
    val globalConfigFlow = preferences.globalConfigFlow

    fun getAppIslandConfig(packageName: String) = preferences.getAppIslandConfig(packageName)

    fun updateAppIslandConfig(packageName: String, config: IslandConfig) {
        viewModelScope.launch { preferences.updateAppIslandConfig(packageName, config) }
    }

    fun updateGlobalConfig(config: IslandConfig) {
        viewModelScope.launch { preferences.updateGlobalConfig(config) }
    }

    // --- BLOCKED TERMS (FIX FOR ISSUE #6) ---
    val globalBlockedTermsFlow = preferences.globalBlockedTermsFlow

    fun setGlobalBlockedTerms(terms: Set<String>) {
        viewModelScope.launch { preferences.setGlobalBlockedTerms(terms) }
    }

    fun getAppBlockedTerms(packageName: String) = preferences.getAppBlockedTerms(packageName)

    fun updateAppBlockedTerms(packageName: String, terms: Set<String>) {
        viewModelScope.launch { preferences.setAppBlockedTerms(packageName, terms) }
    }
    // ----------------------------------------

    // UI State Setters
    fun setCategory(cat: AppCategory) { }
    fun setSort(option: SortOption) { }
    fun clearSearch() { }

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
                val cat = when {
                    MUSIC_KEYS.any { pkg.contains(it, true) } -> AppCategory.MUSIC
                    MAPS_KEYS.any { pkg.contains(it, true) } -> AppCategory.MAPS
                    TIMER_KEYS.any { pkg.contains(it, true) } -> AppCategory.TIMER
                    else -> AppCategory.OTHER
                }
                AppInfo(name, pkg, icon, category = cat)
            } catch (e: Exception) { null }
        }.distinctBy { it.packageName }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable) return this.bitmap
        val width = if (intrinsicWidth > 0) intrinsicWidth else 1
        val height = if (intrinsicHeight > 0) intrinsicHeight else 1
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }
}