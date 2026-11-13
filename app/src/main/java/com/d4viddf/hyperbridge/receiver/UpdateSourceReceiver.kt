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
 * SECURITY: the custom [PERMISSION_UPDATE_SOURCE] is `normal`, so any app can hold it; it only
 * gates who may broadcast at all. Which app sent the update comes from the platform instead: the
 * sender must opt in to sharing its identity (`BroadcastOptions.setShareIdentityEnabled(true)`,
 * API 34+), and [getSentFromPackage] is what the Settings allow-list is checked against. A
 * self-reported package could otherwise be spoofed by any app to write an allowed app's source.
 * [EXTRA_OWNER_PACKAGE] is optional and, when present, must match. The source id is used as a
 * file name, so it is restricted to [SourceRepository.isValidSourceId].
 */
class UpdateSourceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_UPDATE_SOURCE) return
        val sourceId = intent.getStringExtra(EXTRA_SOURCE_ID) ?: return
        if (!SourceRepository.isValidSourceId(sourceId)) {
            Log.w(TAG, "Dropped source update with an invalid source id")
            return
        }
        val ownerPackage = sentFromPackage
        if (ownerPackage == null) {
            Log.w(TAG, "Dropped source update from a sender that did not share its identity")
            return
        }
        val claimed = intent.getStringExtra(EXTRA_OWNER_PACKAGE)
        if (claimed != null && claimed != ownerPackage) {
            Log.w(TAG, "Dropped source update: $ownerPackage claimed to be $claimed")
            return
        }
        val text = intent.getStringExtra(EXTRA_TEXT)?.take(SourceRepository.MAX_TEXT_LENGTH)
        val icon = getIconExtra(intent)
        val ttlMs = intent.getLongExtra(EXTRA_TTL_MS, -1L).takeIf { it > 0 }

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = SourceRepository(appContext)
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
