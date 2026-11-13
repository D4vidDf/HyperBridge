package com.d4viddf.hyperbridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.d4viddf.hyperbridge.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_PACKAGE_ADDED) return

        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        if (isReplacing) return

        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName == context.packageName) return
        if (packageName == com.d4viddf.hyperbridge.service.recording.ScreenRecordingClassifier.PACKAGE_NAME) return

        // Verify that this package is a launchable app (meaning it can be displayed in Library)
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            logDebug("PackageChangeReceiver", "Ignoring non-launchable package: $packageName")
            return
        }

        logDebug("PackageChangeReceiver", "New library app installed: $packageName")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleNewPackage(context, packageName)
            } finally {
                pendingResult.finish()
            }
        }
    }

    suspend fun handleNewPackage(
        context: Context?,
        packageName: String,
        preferences: AppPreferences? = context?.let { AppPreferences(it) },
        isLibraryApp: Boolean = context?.let { ctx ->
            packageName != ctx.packageName &&
            packageName != com.d4viddf.hyperbridge.service.recording.ScreenRecordingClassifier.PACKAGE_NAME &&
            ctx.packageManager.getLaunchIntentForPackage(packageName) != null
        } ?: true
    ) {
        val prefs = preferences ?: return
        if (!isLibraryApp) return
        if (prefs.isBridgeAllAppsEnabledSync() && prefs.isAutoAddNewAppsEnabledSync()) {
            logDebug("PackageChangeReceiver", "Auto-adding library app $packageName to bridged apps")
            prefs.toggleApp(packageName, true)
        }
    }

    private fun logDebug(tag: String, message: String) {
        try {
            Log.d(tag, message)
        } catch (_: Throwable) {
            // Ignored in unit tests
        }
    }
}
