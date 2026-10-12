package com.d4viddf.hyperbridge.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Base64
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.data.db.AppSetting
import com.d4viddf.hyperbridge.data.db.SettingsKeys
import com.d4viddf.hyperbridge.data.db.TranslatorEntity
import com.d4viddf.hyperbridge.data.model.AppSettingBackup
import com.d4viddf.hyperbridge.data.model.BackupMetadata
import com.d4viddf.hyperbridge.data.model.CustomWidgetBackupItem
import com.d4viddf.hyperbridge.data.model.HyperBridgeBackup
import com.d4viddf.hyperbridge.data.model.TranslatorBackupItem
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// UI selection state for backup and restore
data class BackupSelection(
    val includeSettings: Boolean = true,
    val includeBlocklist: Boolean = true,
    val includePriorities: Boolean = true,
    val includeTranslators: Boolean = true,
    val includeCustomWidgets: Boolean = true
)

class BackupManager(
    private val context: Context,
    private val preferences: AppPreferences,
    private val database: AppDatabase,
    private val gson: Gson = Gson()
) {

    @Suppress("DEPRECATION")
    suspend fun performExport(uri: Uri, selection: BackupSelection): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 1. Metadata
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val metadata = BackupMetadata(
                packageName = context.packageName,
                versionCode = pInfo.longVersionCode.toInt(),
                versionName = pInfo.versionName ?: "Unknown",
                timestamp = System.currentTimeMillis(),
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
            )

            // 2. Fetch Settings Data
            val allSettings = database.settingsDao().getAllSync()
            val filteredSettings = allSettings.filter { setting ->
                val key = setting.key
                when {
                    key == SettingsKeys.GLOBAL_BLOCKED_TERMS || key.endsWith("_blocked") -> selection.includeBlocklist
                    key == SettingsKeys.PRIORITY_ORDER -> selection.includePriorities
                    else -> selection.includeSettings
                }
            }.map { AppSettingBackup(it.key, it.value) }

            // 3. Fetch Translators Data
            val translatorItems = if (selection.includeTranslators) {
                val entities = database.translatorDao().getAllTranslators()
                val translatorsStorageDir = File(context.filesDir, "translators")
                entities.map { entity ->
                    val iconDir = File(translatorsStorageDir, "${entity.id}/icons")
                    val iconsMap = if (iconDir.exists() && iconDir.isDirectory) {
                        iconDir.listFiles()?.filter { it.isFile }?.associate { file ->
                            file.name to Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                        }
                    } else null

                    TranslatorBackupItem(
                        id = entity.id,
                        jsonContent = entity.jsonContent,
                        isEnabled = entity.isEnabled,
                        priority = entity.priority,
                        icons = iconsMap
                    )
                }
            } else null

            // 4. Fetch Custom Widgets / Designs Data
            val widgetItems = if (selection.includeCustomWidgets) {
                val widgetsDir = File(context.filesDir, "custom_widgets")
                if (widgetsDir.exists() && widgetsDir.isDirectory) {
                    widgetsDir.listFiles()?.filter { it.isDirectory }?.mapNotNull { folder ->
                        val configFile = File(folder, "widget.json")
                        if (!configFile.exists()) return@mapNotNull null
                        val configJson = configFile.readText(Charsets.UTF_8)
                        val translatorFile = File(folder, "translator.json").takeIf { it.exists() }
                        val translatorJson = translatorFile?.readText(Charsets.UTF_8)

                        val assetsDir = File(folder, "assets")
                        val assetsMap = if (assetsDir.exists() && assetsDir.isDirectory) {
                            assetsDir.listFiles()?.filter { it.isFile }?.associate { file ->
                                file.name to Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
                            }
                        } else null

                        CustomWidgetBackupItem(
                            id = folder.name,
                            configJson = configJson,
                            translatorJson = translatorJson,
                            assets = assetsMap
                        )
                    } ?: emptyList()
                } else emptyList()
            } else null

            // 5. Write Backup
            val backup = HyperBridgeBackup(
                metadata = metadata,
                settings = filteredSettings,
                translators = translatorItems,
                widgets = widgetItems
            )
            val jsonString = gson.toJson(backup)

            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(jsonString.toByteArray())
            }

            Result.success(true)
        } catch (e: Throwable) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun readBackupFile(uri: Uri): Result<HyperBridgeBackup> = withContext(Dispatchers.IO) {
        try {
            val backup = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                java.io.InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                    gson.fromJson(reader, HyperBridgeBackup::class.java)
                }
            } ?: return@withContext Result.failure(Exception("Could not open input stream"))

            if (backup.metadata.packageName != context.packageName) {
                return@withContext Result.failure(Exception("Package mismatch"))
            }

            Result.success(backup)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(backup: HyperBridgeBackup, selection: BackupSelection): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            // 1. Settings
            val entitiesToRestore = backup.settings.filter { item ->
                val key = item.key
                when {
                    key == SettingsKeys.GLOBAL_BLOCKED_TERMS || key.endsWith("_blocked") -> selection.includeBlocklist
                    key == SettingsKeys.PRIORITY_ORDER -> selection.includePriorities
                    else -> selection.includeSettings
                }
            }.map { AppSetting(it.key, it.value) }

            if (entitiesToRestore.isNotEmpty()) {
                database.settingsDao().insertAll(entitiesToRestore)
            }

            // 2. Translators
            if (selection.includeTranslators && !backup.translators.isNullOrEmpty()) {
                val translatorsStorageDir = File(context.filesDir, "translators")
                val entities = mutableListOf<TranslatorEntity>()

                backup.translators.forEach { item ->
                    val parsed = CustomTranslator.fromJson(item.jsonContent).getOrNull()
                    if (parsed != null) {
                        val updated = parsed.copy(isEnabled = item.isEnabled, priority = item.priority)
                        entities.add(TranslatorEntity.fromCustomTranslator(updated))

                        if (!item.icons.isNullOrEmpty()) {
                            val iconDir = File(translatorsStorageDir, "${item.id}/icons")
                            if (!iconDir.exists()) iconDir.mkdirs()
                            item.icons.forEach { (fileName, base64) ->
                                try {
                                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                                    File(iconDir, fileName).writeBytes(bytes)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
                if (entities.isNotEmpty()) {
                    database.translatorDao().insertAll(entities)
                }
            }

            // 3. Custom Widgets & Designs
            if (selection.includeCustomWidgets && !backup.widgets.isNullOrEmpty()) {
                val widgetsDir = File(context.filesDir, "custom_widgets")
                if (!widgetsDir.exists()) widgetsDir.mkdirs()

                backup.widgets.forEach { item ->
                    try {
                        val widgetFolder = File(widgetsDir, item.id)
                        if (!widgetFolder.exists()) widgetFolder.mkdirs()

                        File(widgetFolder, "widget.json").writeText(item.configJson, Charsets.UTF_8)

                        if (item.translatorJson != null) {
                            File(widgetFolder, "translator.json").writeText(item.translatorJson, Charsets.UTF_8)
                            val parsedTrans = CustomTranslator.fromJson(item.translatorJson).getOrNull()
                            if (parsedTrans != null) {
                                database.translatorDao().insertTranslator(TranslatorEntity.fromCustomTranslator(parsedTrans))
                            }
                        }

                        if (!item.assets.isNullOrEmpty()) {
                            val assetsDir = File(widgetFolder, "assets")
                            if (!assetsDir.exists()) assetsDir.mkdirs()
                            item.assets.forEach { (fileName, base64) ->
                                try {
                                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                                    File(assetsDir, fileName).writeBytes(bytes)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            Result.success(true)
        } catch (e: Throwable) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    companion object {
        fun generateFileName(): String {
            val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            return "HyperBridge_Backup_$date.hbr"
        }
    }
}