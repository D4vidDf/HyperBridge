package com.d4viddf.hyperbridge.ui.screens.theme

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.theme.AppThemeOverride
import com.d4viddf.hyperbridge.models.theme.GlobalConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.theme.ThemeMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ThemeRepository(application)
    private val prefs = AppPreferences(application)
    private val context = application.applicationContext
    private val pm = context.packageManager

    private val _installedThemes = MutableStateFlow<List<HyperTheme>>(emptyList())
    val installedThemes: StateFlow<List<HyperTheme>> = _installedThemes

    val activeThemeId = prefs.activeThemeIdFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // --- CREATOR STATE ---
    private val _tempAssets = mutableMapOf<String, Uri>()

    // Key = Package Name, Value = Override Configuration
    private val _appOverrides = MutableStateFlow<Map<String, AppThemeOverride>>(emptyMap())
    val appOverrides: StateFlow<Map<String, AppThemeOverride>> = _appOverrides

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps

    init {
        refreshThemes()
        loadInstalledApps()
    }

    // --- BASIC OPERATIONS ---
    fun refreshThemes() {
        viewModelScope.launch {
            _installedThemes.value = repo.getAvailableThemes()
            prefs.activeThemeIdFlow.collect { id -> if (id != null) repo.activateTheme(id) }
        }
    }

    fun applyTheme(theme: HyperTheme) {
        viewModelScope.launch {
            prefs.setActiveThemeId(theme.id)
            repo.activateTheme(theme.id)
        }
    }

    fun resetToDefault() {
        viewModelScope.launch { prefs.setActiveThemeId(null) }
    }

    fun deleteTheme(theme: HyperTheme) {
        viewModelScope.launch {
            repo.deleteTheme(theme.id)
            refreshThemes()
        }
    }

    fun exportAndShareTheme(theme: HyperTheme) {
        viewModelScope.launch {
            val zipFile = repo.exportTheme(theme.id)
            if (zipFile != null) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", zipFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share Theme")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        }
    }

    // --- CREATOR HELPERS ---

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 }
                .map { AppItem(it.packageName, it.loadLabel(pm).toString()) }
                .sortedBy { it.label }
            _installedApps.value = apps
        }
    }

    fun stageAsset(key: String, uri: Uri) {
        _tempAssets[key] = uri
    }

    fun updateAppOverride(pkg: String, override: AppThemeOverride) {
        _appOverrides.value = _appOverrides.value + (pkg to override)
    }

    fun removeAppOverride(pkg: String) {
        _appOverrides.value = _appOverrides.value - pkg
    }

    fun loadThemeForEditing(id: String) {
        val theme = _installedThemes.value.find { it.id == id }
        if (theme != null) {
            _appOverrides.value = theme.apps
        }
    }

    fun clearCreatorState() {
        _appOverrides.value = emptyMap()
        _tempAssets.clear()
    }

    // --- SAVE LOGIC ---

    fun saveTheme(
        existingId: String?,
        name: String,
        author: String,
        highlightColor: String
    ) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val themeId = existingId ?: UUID.randomUUID().toString()

                // 1. Construct HyperTheme using the new model
                val newTheme = HyperTheme(
                    id = themeId,
                    meta = ThemeMetadata(name, author, 1, "Custom Theme"),
                    global = GlobalConfig(highlightColor = highlightColor, backgroundColor = "#000000", textColor = "#FFFFFF"),
                    apps = _appOverrides.value,
                    // Default values for other modules (can be expanded later)
                    defaultActions = emptyMap(),
                    rules = emptyList()
                )

                repo.saveTheme(newTheme)

                // 2. Save Assets
                if (_tempAssets.isNotEmpty()) {
                    val themeDir = File(repo.getThemesDir(), themeId)
                    val iconsDir = File(themeDir, "icons")
                    if (!iconsDir.exists()) iconsDir.mkdirs()

                    _tempAssets.forEach { (key, uri) ->
                        try {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                val bitmap = BitmapFactory.decodeStream(input)
                                if (bitmap != null) {
                                    // Save as PNG matching the key used in ThemeResource value
                                    val destFile = File(iconsDir, "$key.png")
                                    destFile.outputStream().use { output ->
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                                    }
                                }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    _tempAssets.clear()
                }
            }
            refreshThemes()
        }
    }
    // [NEW] Get data for editing
    fun getThemeById(id: String): HyperTheme? {
        return _installedThemes.value.find { it.id == id }
    }
}

data class AppItem(val packageName: String, val label: String)