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
import com.d4viddf.hyperbridge.models.theme.ActionConfig
import com.d4viddf.hyperbridge.models.theme.AppThemeOverride
import com.d4viddf.hyperbridge.models.theme.CallModule
import com.d4viddf.hyperbridge.models.theme.GlobalConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.theme.ResourceType
import com.d4viddf.hyperbridge.models.theme.ThemeMetadata
import com.d4viddf.hyperbridge.models.theme.ThemeResource
import com.d4viddf.hyperbridge.service.NotificationReaderService
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

    private val _appOverrides = MutableStateFlow<Map<String, AppThemeOverride>>(emptyMap())
    val appOverrides: StateFlow<Map<String, AppThemeOverride>> = _appOverrides

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    val installedApps: StateFlow<List<AppItem>> = _installedApps

    // --- GLOBAL VISUAL STATE ---
    var currentEditingThemeId: String? by mutableStateOf(null)

    var themeName by mutableStateOf("")
    var themeAuthor by mutableStateOf("")
    var themeDescription by mutableStateOf("")
    var themeIconUri by mutableStateOf<Uri?>(null)
    var isLockedForEditing by mutableStateOf(false)
    var customShareLink by mutableStateOf("")

    var selectedColorHex by mutableStateOf("#3DDA82")
    var useAppColors by mutableStateOf(false)
    var isDarkThemePreview by mutableStateOf(true)

    var selectedShapeId by mutableStateOf("circle")
    var iconPaddingPercent by mutableIntStateOf(15)

    var callAnswerUri by mutableStateOf<Uri?>(null)
    var callDeclineUri by mutableStateOf<Uri?>(null)
    var callAnswerColor by mutableStateOf("#34C759")
    var callDeclineColor by mutableStateOf("#FF3B30")
    var callAnswerShapeId by mutableStateOf("circle")
    var callDeclineShapeId by mutableStateOf("circle")

    var themeDefaultActions by mutableStateOf<Map<String, ActionConfig>>(emptyMap())

    // --- APP-SPECIFIC EDITING STATE (Buffers) ---
    var editingAppPackage by mutableStateOf<String?>(null)
    var editingAppLabel by mutableStateOf("")

    var appHighlightColor by mutableStateOf<String?>(null)
    var appUseAppColors by mutableStateOf<Boolean?>(null)

    var appShapeId by mutableStateOf<String?>(null)
    var appPaddingPercent by mutableStateOf<Int?>(null)

    var appCallAnswerColor by mutableStateOf<String?>(null)
    var appCallDeclineColor by mutableStateOf<String?>(null)
    var appCallAnswerUri by mutableStateOf<Uri?>(null)
    var appCallDeclineUri by mutableStateOf<Uri?>(null)

    var appCallAnswerShapeId by mutableStateOf<String?>(null)
    var appCallDeclineShapeId by mutableStateOf<String?>(null)

    var appActions by mutableStateOf<Map<String, ActionConfig>>(emptyMap())

    private val _tempAssets = mutableMapOf<String, Uri>()

    // [FIX] Use application context to get string resource
    val shareTheme: String = application.getString(R.string.share_theme)

    init {
        refreshThemes()
        loadInstalledApps()
    }

    enum class ShapeOption(val id: String, @StringRes val labelRes: Int) {
        CIRCLE("circle", R.string.shape_circle),
        SQUARE("square", R.string.shape_square),
        COOKIE_4("cookie", R.string.shape_cookie),
        ARCH("arch", R.string.shape_arch),
        CLOVER_8("clover8", R.string.shape_clover)
    }

    fun importTheme(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repo.installThemeFromUri(uri)
                refreshThemes()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun refreshThemes() {
        viewModelScope.launch {
            _installedThemes.value = repo.getAvailableThemes()
            val currentId = activeThemeId.value
            if (currentId != null) repo.activateTheme(currentId)
        }
    }

    fun applyTheme(theme: HyperTheme) {
        viewModelScope.launch {
            prefs.setActiveThemeId(theme.id)
            repo.activateTheme(theme.id)
            reloadNotificationService()
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            prefs.setActiveThemeId(null)
            reloadNotificationService()
        }
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
                // [FIX] Use the pre-loaded string
                val chooser = Intent.createChooser(intent, shareTheme)
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

    fun updateAppOverride(pkg: String, override: AppThemeOverride) {
        val newMap = _appOverrides.value.toMutableMap()
        newMap[pkg] = override
        _appOverrides.value = newMap

        saveTheme(currentEditingThemeId, apply = false)
    }

    fun removeAppOverride(pkg: String) {
        val newMap = _appOverrides.value.toMutableMap()
        newMap.remove(pkg)
        _appOverrides.value = newMap
        saveTheme(currentEditingThemeId, apply = false)
    }

    fun getThemeById(id: String): HyperTheme? = _installedThemes.value.find { it.id == id }

    // --- APP EDITOR LOGIC ---

    fun loadAppForEditing(pkg: String, label: String) {
        val override = _appOverrides.value[pkg]
        editingAppPackage = pkg
        editingAppLabel = label

        appHighlightColor = override?.highlightColor
        appUseAppColors = override?.useAppColors

        appShapeId = override?.iconShapeId
        appPaddingPercent = override?.iconPaddingPercent

        appCallAnswerColor = override?.callConfig?.answerColor
        appCallDeclineColor = override?.callConfig?.declineColor
        appCallAnswerShapeId = override?.callConfig?.answerShapeId
        appCallDeclineShapeId = override?.callConfig?.declineShapeId

        appCallAnswerUri = null
        appCallDeclineUri = null

        appActions = override?.actions ?: emptyMap()
    }

    fun saveAppChanges() {
        val pkg = editingAppPackage ?: return

        val existingOverride = _appOverrides.value[pkg]

        val hasCallChanges = appCallAnswerColor != null || appCallDeclineColor != null ||
                appCallAnswerUri != null || appCallDeclineUri != null ||
                appCallAnswerShapeId != null || appCallDeclineShapeId != null

        val callModule = if (hasCallChanges) {
            CallModule(
                answerColor = appCallAnswerColor,
                declineColor = appCallDeclineColor,
                answerShapeId = appCallAnswerShapeId ?: "circle",
                declineShapeId = appCallDeclineShapeId ?: "circle",
                answerIcon = if (appCallAnswerUri != null) {
                    val key = "app_${pkg}_answer"
                    stageAsset(key, appCallAnswerUri!!)
                    ThemeResource(ResourceType.LOCAL_FILE, "icons/$key.png")
                } else existingOverride?.callConfig?.answerIcon,
                declineIcon = if (appCallDeclineUri != null) {
                    val key = "app_${pkg}_decline"
                    stageAsset(key, appCallDeclineUri!!)
                    ThemeResource(ResourceType.LOCAL_FILE, "icons/$key.png")
                } else existingOverride?.callConfig?.declineIcon
            )
        } else null

        val newOverride = AppThemeOverride(
            highlightColor = appHighlightColor,
            useAppColors = appUseAppColors,
            iconShapeId = appShapeId,
            iconPaddingPercent = appPaddingPercent,
            callConfig = callModule,
            actions = appActions.ifEmpty { null },
            progress = existingOverride?.progress,
            navigation = existingOverride?.navigation
        )

        updateAppOverride(pkg, newOverride)
        editingAppPackage = null
    }

    fun cancelAppEditing() {
        editingAppPackage = null
    }

    fun updateAppAction(keyword: String, config: ActionConfig) {
        appActions = appActions + (keyword to config)
    }

    fun removeAppAction(keyword: String) {
        appActions = appActions - keyword
    }

    // --- MAIN THEME LOADING & SAVING ---

    fun loadThemeForEditing(id: String) {
        currentEditingThemeId = id
        val theme = _installedThemes.value.find { it.id == id }
        if (theme != null) {
            themeName = theme.meta.name
            themeAuthor = theme.meta.author
            themeDescription = theme.meta.description
            isLockedForEditing = theme.meta.lockedForEditing
            customShareLink = theme.meta.customShareLink ?: ""

            selectedColorHex = theme.global.highlightColor ?: "#3DDA82"
            useAppColors = theme.global.useAppColors
            selectedShapeId = theme.global.iconShapeId
            iconPaddingPercent = theme.global.iconPaddingPercent
            callAnswerColor = theme.callConfig.answerColor ?: "#34C759"
            callDeclineColor = theme.callConfig.declineColor ?: "#FF3B30"
            callAnswerShapeId = theme.callConfig.answerShapeId
            callDeclineShapeId = theme.callConfig.declineShapeId

            _appOverrides.value = theme.apps
            themeDefaultActions = theme.defaultActions
        }
    }

    fun clearCreatorState() {
        currentEditingThemeId = UUID.randomUUID().toString()

        // [FIX] Extract "My Theme" string
        themeName = "" // Let the UI handle the "My Theme" placeholder or logic
        themeAuthor = ""
        themeDescription = ""
        themeIconUri = null
        isLockedForEditing = false
        customShareLink = ""

        selectedColorHex = "#3DDA82"
        useAppColors = false
        selectedShapeId = "circle"
        iconPaddingPercent = 15
        callAnswerUri = null
        callDeclineUri = null
        callAnswerColor = "#34C759"
        callDeclineColor = "#FF3B30"
        callAnswerShapeId = "circle"
        callDeclineShapeId = "circle"

        appHighlightColor = null
        appUseAppColors = null
        appCallAnswerShapeId = null
        appCallDeclineShapeId = null

        _appOverrides.value = emptyMap()
        themeDefaultActions = emptyMap()
        _tempAssets.clear()
        isDarkThemePreview = true
    }

    fun updateDefaultAction(keyword: String, config: ActionConfig) {
        val current = themeDefaultActions.toMutableMap()
        current[keyword] = config
        themeDefaultActions = current
    }

    fun removeDefaultAction(keyword: String) {
        val current = themeDefaultActions.toMutableMap()
        current.remove(keyword)
        themeDefaultActions = current
    }

    fun saveTheme(existingId: String? = null, apply: Boolean = false) {
        val themeId = existingId ?: currentEditingThemeId ?: UUID.randomUUID().toString()
        if (currentEditingThemeId == null) currentEditingThemeId = themeId

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val iconsDir = File(File(repo.getThemesDir(), themeId), "icons")
                if (!iconsDir.exists()) iconsDir.mkdirs()

                var themeIconRes = existingId?.let { getThemeById(it)?.meta?.customIcon }
                if (themeIconUri != null) {
                    try {
                        context.contentResolver.openInputStream(themeIconUri!!)?.use { input ->
                            val originalBitmap = BitmapFactory.decodeStream(input)
                            if (originalBitmap != null) {
                                val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 512, 512, true)
                                val iconFile = File(iconsDir, "theme_thumb.png")
                                iconFile.outputStream().use { out -> scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
                                themeIconRes = ThemeResource(ResourceType.LOCAL_FILE, "icons/theme_thumb.png")
                                if (originalBitmap != scaledBitmap) originalBitmap.recycle()
                            }
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }

                if (_tempAssets.isNotEmpty()) {
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

                val answerRes = if (callAnswerUri != null) ThemeResource(ResourceType.LOCAL_FILE, "icons/call_answer.png")
                else getThemeById(themeId)?.callConfig?.answerIcon
                val declineRes = if (callDeclineUri != null) ThemeResource(ResourceType.LOCAL_FILE, "icons/call_decline.png")
                else getThemeById(themeId)?.callConfig?.declineIcon

                // [FIX] Use extracted string for default theme name
                val defaultThemeName = getApplication<Application>().getString(R.string.my_theme)

                val newTheme = HyperTheme(
                    id = themeId,
                    meta = ThemeMetadata(
                        name = themeName.ifBlank { defaultThemeName },
                        author = themeAuthor,
                        description = themeDescription,
                        customIcon = themeIconRes,
                        lockedForEditing = isLockedForEditing,
                        customShareLink = customShareLink.ifBlank { null }
                    ),
                    global = GlobalConfig(
                        highlightColor = selectedColorHex,
                        useAppColors = useAppColors,
                        iconShapeId = selectedShapeId,
                        iconPaddingPercent = iconPaddingPercent,
                        backgroundColor = "#202124"
                    ),
                    callConfig = CallModule(
                        answerIcon = answerRes,
                        declineIcon = declineRes,
                        answerColor = callAnswerColor,
                        declineColor = callDeclineColor,
                        answerShapeId = callAnswerShapeId,
                        declineShapeId = callDeclineShapeId
                    ),
                    defaultActions = themeDefaultActions,
                    apps = _appOverrides.value
                )

                repo.saveTheme(newTheme)

                if (apply || activeThemeId.value == themeId) {
                    if(apply) prefs.setActiveThemeId(themeId)
                    repo.activateTheme(themeId)
                    reloadNotificationService()
                }
            }
            refreshThemes()
        }
    }

    private fun reloadNotificationService() {
        val intent = Intent(context, NotificationReaderService::class.java).apply {
            action = NotificationReaderService.ACTION_RELOAD_THEME
        }
        context.startService(intent)
    }
}

data class AppItem(val packageName: String, val label: String)