package com.d4viddf.hyperbridge.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

object NotificationSettingsNavigator {
    fun openForApp(context: Context, packageName: String): Boolean {
        val intents = listOf(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:$packageName".toUri()
            },
            Intent(Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS)
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {
                // Try the next public settings screen.
            }
        }
        return false
    }
}
