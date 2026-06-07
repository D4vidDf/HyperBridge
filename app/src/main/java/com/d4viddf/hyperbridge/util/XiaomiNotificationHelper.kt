package com.d4viddf.hyperbridge.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import java.lang.reflect.Method
import androidx.core.net.toUri

object XiaomiNotificationHelper {

    /**
     * Checks if the device's OS supports the Xiaomi Island feature.
     */
    fun isSupportIsland(): Boolean {
        return getSystemPropertyBoolean("persist.sys.feature.island", false)
    }

    /**
     * Returns the focus protocol version:
     * 1: OS1 (OS1 focus notification templates)
     * 2: OS2 (OS2 focus notification templates)
     * 3: OS3 (OS3 Hyper Island notification templates)
     * 0: Not supported or unknown
     */
    fun getFocusProtocolVersion(context: Context): Int {
        return try {
            Settings.System.getInt(
                context.contentResolver,
                "notification_focus_protocol", 0
            )
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Checks if the app has the "Focus Notification" permission enabled.
     * Note: This is an expensive operation and calls a ContentProvider.
     * 
     * Returns false on OS versions prior to OS1.
     * On OS1, OS2, OS3 returns true if permission is granted, false if not.
     */
    fun hasFocusPermission(context: Context): Boolean {
        var canShowFocus = false
        try {
            val uri = "content://miui.statusbar.notification.public".toUri()
            val extras = Bundle()
            extras.putString("package", context.packageName)
            val bundle = context.contentResolver.call(uri, "canShowFocus", null, extras)
            if (bundle != null) {
                canShowFocus = bundle.getBoolean("canShowFocus", false)
            }
        } catch (_: Exception) {
            // Permission provider not found or failed
            canShowFocus = false
        }
        return canShowFocus
    }

    @SuppressLint("PrivateApi")
    private fun getSystemPropertyBoolean(key: String, defaultValue: Boolean): Boolean {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method: Method = clazz.getDeclaredMethod("getBoolean", String::class.java, Boolean::class.javaPrimitiveType)
            val result = method.invoke(null, key, false)
            result as? Boolean ?: defaultValue
        } catch (_: Exception) {
            defaultValue
        }
    }
}
