package com.d4viddf.hyperbridge.util

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri

/**
 * Opens the hidden Xiaomi Autostart management screen.
 */
fun openAutoStartSettings(context: Context) {
    try {
        val intent = Intent()
        intent.component = ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = "package:${context.packageName}".toUri()
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Settings not found", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Opens the Battery Optimization screen for this app.
 */
@SuppressLint("BatteryLife")
fun openBatterySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        intent.data = "package:${context.packageName}".toUri()
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = "package:${context.packageName}".toUri()
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Settings not found", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Checks if Notification Listener permission is granted.
 */
fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(pkgName)
}

/**
 * Checks if Post Notification permission (Android 13+) is granted.
 */
fun isPostNotificationsEnabled(context: Context): Boolean {
    return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}