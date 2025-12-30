package com.d4viddf.hyperbridge.ui.screens.theme

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.theme.AppThemeOverride
import com.d4viddf.hyperbridge.models.theme.CallModule
import com.d4viddf.hyperbridge.models.theme.GlobalConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.theme.ResourceType
import com.d4viddf.hyperbridge.models.theme.ThemeMetadata
import com.d4viddf.hyperbridge.models.theme.ThemeResource
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
    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext
    private val pm = context.packageManager

    private val _installedThemes = MutableStateFlow<List<HyperTheme>>(emptyList())
    val installedThemes: StateFlow<List<HyperTheme>> = _installedThemes
    val activeThemeId = prefs.activeThemeIdFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)
    private val _tempAssets = mutableMapOf<String, Uri>()
    private val _appOverrides = MutableStateFlow<Map<String, AppThemeOverride>>(emptyMap())
    val appOverrides: StateFlow<Map<String, AppThemeOverride>> = _appOverrides
    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps

    // --- VISUAL STATE ---
    var themeName by mutableStateOf("")
    var themeAuthor by mutableStateOf("")
    var selectedColorHex by mutableStateOf("#3DDA82")
    var useAppColors by mutableStateOf(false)

    var isDarkThemePreview by mutableStateOf(true)

    // Icons
    var selectedShapeId by mutableStateOf("circle")
    var iconPaddingPercent by mutableIntStateOf(15)

    // Call
    var callAnswerUri by mutableStateOf<Uri?>(null)
    var callDeclineUri by mutableStateOf<Uri?>(null)
    var callAnswerColor by mutableStateOf("#34C759")
    var callDeclineColor by mutableStateOf("#FF3B30")

    init {
        refreshThemes()
        loadInstalledApps()
    }

    // --- SHAPE DEFINITIONS ---
    // [UPDATED] Uses Resource IDs for localization
    enum class ShapeOption(val id: String, @StringRes val labelRes: Int) {
        CIRCLE("circle", R.string.shape_circle),
        SQUARE("square", R.string.shape_square),
        COOKIE_4("cookie", R.string.shape_cookie),
        ARCH("arch", R.string.shape_arch),
        CLOVER_8("clover8", R.string.shape_clover)
    }

    // --- ACTIONS ---
    fun importTheme(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.installThemeFromUri(uri)
                refreshThemes()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 }
                .map { AppItem(it.packageName, it.loadLabel(pm).toString()) }
                .sortedBy { it.label }
            _installedApps.value = apps
        }
    }

    fun stageAsset(key: String, uri: Uri) { _tempAssets[key] = uri }
    fun updateAppOverride(pkg: String, override: AppThemeOverride) { _appOverrides.value = _appOverrides.value + (pkg to override) }
    fun removeAppOverride(pkg: String) { _appOverrides.value = _appOverrides.value - pkg }
    fun getThemeById(id: String): HyperTheme? = _installedThemes.value.find { it.id == id }

    fun loadThemeForEditing(id: String) {
        val theme = _installedThemes.value.find { it.id == id }
        if (theme != null) {
            themeName = theme.meta.name
            themeAuthor = theme.meta.author
            selectedColorHex = theme.global.highlightColor ?: "#3DDA82"
            useAppColors = theme.global.useAppColors
            selectedShapeId = theme.global.iconShapeId
            iconPaddingPercent = theme.global.iconPaddingPercent
            callAnswerColor = theme.callConfig.answerColor ?: "#34C759"
            callDeclineColor = theme.callConfig.declineColor ?: "#FF3B30"
            _appOverrides.value = theme.apps
        }
    }

    fun clearCreatorState() {
        themeName = ""
        themeAuthor = ""
        selectedColorHex = "#3DDA82"
        useAppColors = false
        selectedShapeId = "circle"
        iconPaddingPercent = 15
        callAnswerUri = null
        callDeclineUri = null
        callAnswerColor = "#34C759"
        callDeclineColor = "#FF3B30"
        _appOverrides.value = emptyMap()
        _tempAssets.clear()
        isDarkThemePreview = true
    }

    fun saveTheme(existingId: String?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val themeId = existingId ?: UUID.randomUUID().toString()

                val answerRes = if (callAnswerUri != null) ThemeResource(ResourceType.LOCAL_FILE, "icons/call_answer.png") else null
                val declineRes = if (callDeclineUri != null) ThemeResource(ResourceType.LOCAL_FILE, "icons/call_decline.png") else null

                val newTheme = HyperTheme(
                    id = themeId,
                    meta = ThemeMetadata(themeName.ifBlank { "My Theme" }, themeAuthor),
                    global = GlobalConfig(
                        highlightColor = selectedColorHex,
                        useAppColors = useAppColors,
                        iconShapeId = selectedShapeId,
                        iconPaddingPercent = iconPaddingPercent,
                        backgroundColor = "#202124"
                    ),
                    callConfig = CallModule(answerRes, declineRes, callAnswerColor, callDeclineColor),
                    apps = _appOverrides.value
                )

                repo.saveTheme(newTheme)

                if (_tempAssets.isNotEmpty()) {
                    val iconsDir = File(File(repo.getThemesDir(), themeId), "icons")
                    if (!iconsDir.exists()) iconsDir.mkdirs()
                    _tempAssets.forEach { (key, uri) ->
                        try {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                val bitmap = BitmapFactory.decodeStream(input)
                                if (bitmap != null) {
                                    File(iconsDir, "$key.png").outputStream().use {
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
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
}

data class AppItem(val packageName: String, val label: String)