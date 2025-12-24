package com.d4viddf.hyperbridge.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.edit

/**
 * Persists App Names and Icons so they can be displayed even after the app is uninstalled.
 */
class AppCacheManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("app_metadata_cache", Context.MODE_PRIVATE)
    private val iconsDir = File(context.filesDir, "cached_icons").apply { mkdirs() }

    fun cacheAppInfo(packageName: String, name: String, icon: Bitmap?) {
        // 1. Save Name
        prefs.edit { putString(packageName, name) }

        // 2. Save Icon to Disk
        if (icon != null) {
            try {
                val file = File(iconsDir, "$packageName.png")
                // Only save if it doesn't exist or we want to update it (optional optimization)
                if (!file.exists()) {
                    FileOutputStream(file).use { out ->
                        icon.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppCacheManager", "Failed to cache icon for $packageName", e)
            }
        }
    }

    fun getCachedAppName(packageName: String): String {
        return prefs.getString(packageName, null) ?: packageName
    }

    fun getCachedAppIcon(packageName: String): Bitmap? {
        val file = File(iconsDir, "$packageName.png")
        return if (file.exists()) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun removeCachedData(packageName: String) {
        prefs.edit { remove(packageName) }
        File(iconsDir, "$packageName.png").delete()
    }
}