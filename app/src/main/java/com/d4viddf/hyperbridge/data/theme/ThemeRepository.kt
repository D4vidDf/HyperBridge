package com.d4viddf.hyperbridge.data.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.d4viddf.hyperbridge.models.theme.ActionConfig
import com.d4viddf.hyperbridge.models.theme.AppThemeOverride
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.theme.ResourceType
import com.d4viddf.hyperbridge.models.theme.ThemeResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream
import androidx.core.net.toUri

class ThemeRepository(private val context: Context) {

    private val TAG = "HyperBridgeTheme"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val _activeTheme = MutableStateFlow<HyperTheme?>(null)
    val activeTheme: StateFlow<HyperTheme?> = _activeTheme.asStateFlow()

    private val themesDir = File(context.filesDir, "themes")

    init {
        if (!themesDir.exists()) themesDir.mkdirs()
    }

    /**
     * Loads a theme from disk into memory by ID.
     */
    suspend fun activateTheme(themeId: String) {
        withContext(Dispatchers.IO) {
            try {
                val themeFile = File(themesDir, "$themeId/theme_config.json") // Standard name
                if (themeFile.exists()) {
                    val content = themeFile.readText()
                    val theme = json.decodeFromString<HyperTheme>(content)
                    _activeTheme.value = theme
                    Log.i(TAG, "Theme Activated: ${theme.meta.name}")
                } else {
                    Log.w(TAG, "Theme file not found: $themeId")
                    _activeTheme.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load theme", e)
                _activeTheme.value = null
            }
        }
    }

    // ==========================================
    //           INSTALLATION LOGIC
    // ==========================================

    /**
     * Unzips a .hbr file, validates it, and installs it to internal storage.
     * Returns the ID of the installed theme, or throws Exception.
     */
    suspend fun installThemeFromUri(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            val tempId = UUID.randomUUID().toString()
            val tempDir = File(context.cacheDir, "theme_import_$tempId")
            if (!tempDir.exists()) tempDir.mkdirs()

            try {
                // 1. UNZIP
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            val file = File(tempDir, entry.name)

                            // Security: Zip Slip Check
                            if (!file.canonicalPath.startsWith(tempDir.canonicalPath)) {
                                throw SecurityException("Invalid Zip Path: ${entry.name}")
                            }

                            if (entry.isDirectory) {
                                file.mkdirs()
                            } else {
                                file.parentFile?.mkdirs()
                                file.outputStream().use { output ->
                                    zip.copyTo(output)
                                }
                            }
                            entry = zip.nextEntry
                        }
                    }
                }

                // 2. VALIDATION
                val configFile = File(tempDir, "theme_config.json")
                if (!configFile.exists()) {
                    throw IllegalArgumentException("Invalid Theme: Missing theme_config.json")
                }

                // Parse to check validity and get real ID if present
                val content = configFile.readText()
                val theme = try {
                    json.decodeFromString<HyperTheme>(content)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid JSON structure")
                }

                // 3. INSTALLATION
                // We use the ID defined in JSON, or generate one if missing (shouldn't happen with strict spec)
                val finalId = if (theme.id.isNotEmpty()) theme.id else tempId
                val targetDir = File(themesDir, finalId)

                // If updating, clear old version
                if (targetDir.exists()) {
                    targetDir.deleteRecursively()
                }

                // Move temp to final
                if (!tempDir.renameTo(targetDir)) {
                    // Fallback if rename fails (filesystems differ)
                    tempDir.copyRecursively(targetDir, overwrite = true)
                    tempDir.deleteRecursively()
                }

                Log.i(TAG, "Theme Installed Successfully: $finalId")
                return@withContext finalId

            } catch (e: Exception) {
                Log.e(TAG, "Installation failed", e)
                tempDir.deleteRecursively() // Cleanup
                throw e
            }
        }
    }

    // ==========================================
    //           HELPER LOGIC
    // ==========================================

    fun getHighlightColor(packageName: String, default: String = "#000000"): String {
        val theme = _activeTheme.value ?: return default
        theme.apps[packageName]?.highlightColor?.let { return it }
        return theme.global.highlightColor ?: default
    }

    fun getActionConfig(packageName: String, actionKey: String): ActionConfig? {
        val theme = _activeTheme.value ?: return null
        val appActions = theme.apps[packageName]?.actions
        if (appActions != null && appActions.containsKey(actionKey)) {
            return appActions[actionKey]
        }
        if (theme.defaultActions.containsKey(actionKey)) {
            return theme.defaultActions[actionKey]
        }
        return theme.global.defaultActionStyle
    }

    fun getAppOverride(packageName: String): AppThemeOverride? {
        return _activeTheme.value?.apps?.get(packageName)
    }

    // ==========================================
    //           ASSET LOADING
    // ==========================================

    fun getResourceBitmap(resource: ThemeResource?): Bitmap? {
        if (resource == null) return null
        val themeId = _activeTheme.value?.id ?: return null

        return try {
            when (resource.type) {
                ResourceType.LOCAL_FILE -> {
                    val themeFolder = File(themesDir, themeId)
                    val file = File(themeFolder, resource.value)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                }
                ResourceType.URI_CONTENT -> {
                    val uri = resource.value.toUri()
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
                ResourceType.PRESET_DRAWABLE -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error loading bitmap resource: ${resource.value}", e)
            null
        }
    }

    fun getThemesDir(): File = themesDir


    /**
     * Scans the internal themes folder and returns metadata for all installed themes.
     */
    suspend fun getAvailableThemes(): List<HyperTheme> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<HyperTheme>()

            if (themesDir.exists()) {
                themesDir.listFiles()?.forEach { themeFolder ->
                    try {
                        val configFile = File(themeFolder, "theme_config.json")
                        if (configFile.exists()) {
                            val content = configFile.readText()
                            // We decode the whole object to get the ID/Meta
                            // (In a real app, maybe cache this to avoid reading 50 files on startup)
                            val theme = json.decodeFromString<HyperTheme>(content)
                            list.add(theme)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Corrupt theme found in: ${themeFolder.name}")
                    }
                }
            }
            return@withContext list
        }
    }

    /**
     * Deletes a theme from storage.
     */
    suspend fun deleteTheme(themeId: String) {
        withContext(Dispatchers.IO) {
            val themeFolder = File(themesDir, themeId)
            if (themeFolder.exists()) {
                themeFolder.deleteRecursively()
            }
            // If the deleted theme was active, reset to default
            if (_activeTheme.value?.id == themeId) {
                _activeTheme.value = null
                // You should also clear the preference in AppPreferences here or in ViewModel
            }
        }
    }

    /**
     * Saves a new or updated theme object to disk.
     */
    suspend fun saveTheme(theme: HyperTheme) {
        withContext(Dispatchers.IO) {
            // Ensure folder exists
            val themeFolder = File(themesDir, theme.id)
            if (!themeFolder.exists()) themeFolder.mkdirs()

            // Write JSON
            val configFile = File(themeFolder, "theme_config.json")
            val content = json.encodeToString(HyperTheme.serializer(), theme)
            configFile.writeText(content)
        }
    }

    /**
     * Zips the theme folder into a .hbr file for sharing.
     * Returns the file path of the zip.
     */
    suspend fun exportTheme(themeId: String): File? {
        return withContext(Dispatchers.IO) {
            val sourceFolder = File(themesDir, themeId)
            if (!sourceFolder.exists()) return@withContext null

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val zipFile = File(exportDir, "${themeId}_v1.hbr")
            try {
                java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
                    sourceFolder.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val entryName = file.relativeTo(sourceFolder).path
                            zos.putNextEntry(java.util.zip.ZipEntry(entryName))
                            file.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
                return@withContext zipFile
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                return@withContext null
            }
        }
    }
}