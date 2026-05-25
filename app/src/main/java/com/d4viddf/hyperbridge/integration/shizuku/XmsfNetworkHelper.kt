package com.d4viddf.hyperbridge.integration.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.os.DeadObjectException
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

object XmsfNetworkHelper {
    
    private const val TAG = "XmsfNetworkHelper"
    private const val XMSF_PACKAGE = "com.xiaomi.xmsf"
    private const val MAX_RETRIES = 2
    private const val RETRY_DELAY_MS = 500L

    suspend fun setXmsfNetworkingEnabled(context: Context, enabled: Boolean): Boolean {
        
        try {
            val pm = context.packageManager
            val uid = try {
                pm.getPackageUid(XMSF_PACKAGE, 0)
            } catch (e: Exception) {
                Log.w(TAG, "XMSF package not found (UID lookup failed)")
                return false
            }
            
            Log.d(TAG, "🚀 setXmsfNetworkingEnabled called: enabled=$enabled, uid=$uid")

            // Use the utility to ensure permission and then perform the action
            return try {
                requireShizukuPermissionGranted {
                    Log.d(TAG, "✓ Shizuku permission granted, entering retry loop")
                    var lastError: Exception? = null
                    
                    for (attempt in 0 until MAX_RETRIES) {
                        try {
                            Log.d(TAG, "📡 Attempt ${attempt + 1}/$MAX_RETRIES: Getting privileged service...")
                            val service = ShizukuUserServiceRecycler.getPrivilegedService()
                            Log.d(TAG, "✓ Got privileged service, calling setPackageNetworkingEnabled...")

                            val success = service.setPackageNetworkingEnabled(uid, enabled)
                            if (!success) {
                                throw IllegalStateException("Privileged service returned failure for uid=$uid")
                            }

                            Log.d(TAG, "✓ Successfully set XMSF networking to $enabled via privileged service")
                            return@requireShizukuPermissionGranted true
                        } catch (e: CancellationException) {
                            Log.w(TAG, "⚠️ Operation cancelled")
                            return@requireShizukuPermissionGranted false
                        } catch (e: DeadObjectException) {
                            lastError = e
                            Log.w(TAG, "⚠️ DeadObjectException on attempt ${attempt + 1}")
                            if (attempt + 1 < MAX_RETRIES) {
                                delay(RETRY_DELAY_MS)
                            }
                        } catch (e: Exception) {
                            lastError = e
                            Log.w(TAG, "⚠️ Privileged service path failed on attempt ${attempt + 1}: ${e.message}")
                            try {
                                Log.d(TAG, "🪝 Attempt ${attempt + 1}/$MAX_RETRIES: Falling back to hooked binder...")
                                ShizukuHook.setPackageNetworkingEnabled(uid, enabled)
                                Log.d(TAG, "✓ Successfully set XMSF networking to $enabled via hooked binder fallback")
                                return@requireShizukuPermissionGranted true
                            } catch (hookError: Exception) {
                                lastError = hookError
                                Log.e(
                                    TAG,
                                    "❌ Error on attempt ${attempt + 1}: service=${e.message}; hook=${hookError.message}"
                                )
                            }
                            if (attempt + 1 < MAX_RETRIES) {
                                delay(RETRY_DELAY_MS)
                            }
                        }
                    }
                    lastError?.let { Log.e(TAG, "❌ All XMSF networking paths failed after retries: ${it.message}") }
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Shizuku permission or logic failed: ${e.message}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error in setXmsfNetworkingEnabled: ${e.message}")
            return false
        }
    }
}


