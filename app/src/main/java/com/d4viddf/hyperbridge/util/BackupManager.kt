package com.d4viddf.hyperbridge.util

import android.content.Context
import android.net.Uri
import android.os.Build
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.data.db.AppSetting
import com.d4viddf.hyperbridge.data.db.SettingsKeys
import com.d4viddf.hyperbridge.data.model.AppSettingBackup
import com.d4viddf.hyperbridge.data.model.BackupMetadata
import com.d4viddf.hyperbridge.data.model.HyperBridgeBackup
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Kept this for UI selection state
data class BackupSelection(
    val includeSettings: Boolean = true,
    val includeBlocklist: Boolean = true,
    val includePriorities: Boolean = true
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

            // 2. Fetch ALL Data
            val allSettings = database.settingsDao().getAllSync()

            // 3. Filter based on Selection
            val filteredSettings = allSettings.filter { setting ->
                val key = setting.key
                when {
                    // Blocklist keys
                    key == SettingsKeys.GLOBAL_BLOCKED_TERMS || key.endsWith("_blocked") -> selection.includeBlocklist

                    // Priority keys
                    key == SettingsKeys.PRIORITY_ORDER -> selection.includePriorities

                    // Everything else is considered "App Settings" (Config, Setup, etc.)
                    else -> selection.includeSettings
                }
            }.map { AppSettingBackup(it.key, it.value) }

            // 4. Write
            val backup = HyperBridgeBackup(metadata, filteredSettings)
            val jsonString = gson.toJson(backup)

            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(jsonString.toByteArray())
            }

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun readBackupFile(uri: Uri): Result<HyperBridgeBackup> = withContext(Dispatchers.IO) {
        try {
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }
            val backup = gson.fromJson(stringBuilder.toString(), HyperBridgeBackup::class.java)

            if (backup.metadata.packageName != context.packageName) {
                return@withContext Result.failure(Exception("Package mismatch"))
            }

            Result.success(backup)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(backup: HyperBridgeBackup, selection: BackupSelection): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
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

            Result.success(true)
        } catch (e: Exception) {
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