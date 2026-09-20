package com.d4viddf.hyperbridge.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.util.Log
import com.d4viddf.hyperbridge.service.NotificationReaderService

/**
 * The two ways HyperBridge can ask the system to bind [NotificationReaderService] again.
 * Shared by the boot receiver, the listener watchdog and the diagnostics screen so every
 * caller uses the same component name and the same error handling.
 */
object NotificationListenerBinding {
    private const val TAG = "HyperBridgeDebug"

    fun component(context: Context): ComponentName =
        ComponentName(context, NotificationReaderService::class.java)

    /** Official API: asks NotificationManager to rebind an enabled listener. */
    fun requestRebind(context: Context): Boolean = try {
        NotificationListenerService.requestRebind(component(context))
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to request listener re-bind", e)
        false
    }

    /**
     * Disables and immediately re-enables the listener component. The system reacts to the
     * component change by re-evaluating enabled listeners, which rebinds a listener that
     * ignored [requestRebind]. Notification access is keyed by component name and survives.
     */
    fun toggleComponent(context: Context): Boolean = try {
        val pm = context.packageManager
        val component = component(context)
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to toggle listener component", e)
        false
    }
}
