package com.d4viddf.hyperbridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import com.d4viddf.hyperbridge.data.widget.SourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * "Island content sources" (issue #273 add-on): any allow-listed app can drive a micro-widget's
 * `{source.<id>.text}` / `{source.<id>.icon}` tokens by broadcasting [ACTION_UPDATE_SOURCE].
 *
 * SECURITY NOTE: [android.content.BroadcastReceiver.onReceive] has no first-class API to learn
 * the *actual* sending app. The custom [PERMISSION_UPDATE_SOURCE] (OS-enforced, real) only proves
 * the sender declared that permission - it does not identify *which* app sent it. Per-app
 * allow/revoke here is therefore trust-on-first-use: the sender self-reports its package in
 * [EXTRA_OWNER_PACKAGE], and that name is what the user allows or revokes in Settings. This is
 * weaker than "verified sender identity" and is called out here deliberately rather than silently.
 */
class UpdateSourceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_UPDATE_SOURCE) return
        val sourceId = intent.getStringExtra(EXTRA_SOURCE_ID) ?: return
        val ownerPackage = intent.getStringExtra(EXTRA_OWNER_PACKAGE) ?: return
        val text = intent.getStringExtra(EXTRA_TEXT)
        val icon = getIconExtra(intent)
        val ttlMs = intent.getLongExtra(EXTRA_TTL_MS, -1L).takeIf { it > 0 }

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = SourceRepository(appContext)
                // The owner package is self-reported (see class comment); at minimum it must be
                // an installed app, otherwise the Settings allow-list would show phantom entries.
                val label = try {
                    appContext.packageManager.getApplicationLabel(
                        appContext.packageManager.getApplicationInfo(ownerPackage, 0)
                    ).toString()
                } catch (_: Exception) {
                    Log.w(TAG, "Dropped source update claiming uninstalled package: $ownerPackage")
                    return@launch
                }
                repository.recordSighting(ownerPackage, label)

                if (repository.isAllowed(ownerPackage)) {
                    if (!repository.update(sourceId, ownerPackage, text, icon, ttlMs)) {
                        Log.i(TAG, "Source '$sourceId' is owned by another package; update from $ownerPackage ignored")
                    }
                } else {
                    Log.i(TAG, "Dropped source update from not-yet-allowed package: $ownerPackage")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process UPDATE_SOURCE broadcast", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun getIconExtra(intent: Intent): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_ICON, Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_ICON)
        }
    }

    companion object {
        private const val TAG = "HyperBridgeSources"

        const val ACTION_UPDATE_SOURCE = "com.d4viddf.hyperbridge.action.UPDATE_SOURCE"
        const val PERMISSION_UPDATE_SOURCE = "com.d4viddf.hyperbridge.permission.UPDATE_SOURCE"

        const val EXTRA_SOURCE_ID = "source_id"
        const val EXTRA_OWNER_PACKAGE = "owner_package"
        const val EXTRA_TEXT = "text"
        const val EXTRA_ICON = "icon"
        const val EXTRA_TTL_MS = "ttl_ms"
    }
}
