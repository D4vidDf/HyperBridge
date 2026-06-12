package com.d4viddf.hyperbridge.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.util.Log
import com.d4viddf.hyperbridge.service.NotificationReaderService

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.d4viddf.hyperbridge.data.db.AppDatabase

class BootReceiver : BroadcastReceiver() {

    companion object {
        private var lastToggleTime = 0L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        // 1. Determine Importance
        val isMajor = action == Intent.ACTION_BOOT_COMPLETED ||
                action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
                action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                action == "android.intent.action.QUICKBOOT_POWERON"

        val isMinor = action == Intent.ACTION_USER_UNLOCKED ||
                action == Intent.ACTION_USER_PRESENT ||
                action == "android.intent.action.USER_SWITCHED"

        val isTest = action == "com.d4viddf.hyperbridge.ACTION_TEST_MIGRATION"

        if (!isMajor && !isMinor && !isTest) return

        Log.d("HyperBridge", "Trigger event detected: $action")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 2. Migration Logic (Major or Test only)
                if (isMajor || isTest) {
                    Log.d("HyperBridge", "Major trigger: Performing database migration.")
                    AppDatabase.performMigration(context) { progress ->
                        Log.d("HyperBridge", "Migration progress: $progress%")
                    }
                    // Trigger AppPreferences initialization to run SharedPreferences migrations
                    com.d4viddf.hyperbridge.data.AppPreferences(context)
                }
            } catch (e: Exception) {
                Log.e("HyperBridge", "Error during migration in BootReceiver", e)
            } finally {
                // 3. Re-bind Logic
                withContext(Dispatchers.Main) {
                    try {
                        if (isMajor) {
                            val now = System.currentTimeMillis()
                            if (now - lastToggleTime > 5000) {
                                lastToggleTime = now
                                Log.d("HyperBridge", "Major trigger: Toggling NLS component state.")
                                toggleNotificationListener(context)
                            } else {
                                Log.d("HyperBridge", "Major trigger: Cooldown active, skipping toggle.")
                            }
                        } else if (isMinor) {
                            Log.d("HyperBridge", "Minor trigger: Requesting official re-bind.")
                            requestRebind(context)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun requestRebind(context: Context) {
        try {
            val component = ComponentName(context, NotificationReaderService::class.java)
            NotificationListenerService.requestRebind(component)
        } catch (e: Exception) {
            Log.e("HyperBridge", "Failed to request official re-bind", e)
        }
    }

    private fun toggleNotificationListener(context: Context) {
        val pm = context.packageManager
        val componentName = ComponentName(context, NotificationReaderService::class.java)

        // Disable
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        // Enable
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}