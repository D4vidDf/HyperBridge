package com.d4viddf.hyperbridge.util

import android.content.Context
import android.app.Notification
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.integration.shizuku.XmsfNetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ShizukuManager {
    
    private val _isShizukuRunning = MutableStateFlow(false)
    val isShizukuRunning: StateFlow<Boolean> = _isShizukuRunning.asStateFlow()

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        _isShizukuRunning.value = true
        updatePermissionState()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isShizukuRunning.value = false
        _isPermissionGranted.value = false
    }

    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        _isPermissionGranted.value = grantResult == PackageManager.PERMISSION_GRANTED
    }

    init {
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        
        // Initial state check
        try {
            if (Shizuku.pingBinder()) {
                _isShizukuRunning.value = true
                updatePermissionState()
            }
        } catch (_: Exception) {
            _isShizukuRunning.value = false
        }
    }

    fun isShizukuInstalled(context: Context): Boolean {
        if (rikka.sui.Sui.isSui()) return true
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun updatePermissionState() {
        _isPermissionGranted.value = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }

    fun requestPermission(context: Context, requestCode: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                com.d4viddf.hyperbridge.integration.shizuku.requireShizukuPermissionGranted {
                    _isPermissionGranted.value = true
                    android.widget.Toast.makeText(context, "Shizuku Permission Granted!", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Error: " + e.message, android.widget.Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private var restoreNetworkJob: Job? = null
    private val notifyMutex = Mutex()

    @androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    fun notify(context: Context, id: Int, notification: Notification) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = AppPreferences(context)
            val workaroundEnabled = prefs.isShizukuWorkaroundEnabled.first()
            
            if (_isPermissionGranted.value && workaroundEnabled) {
                notifyMutex.withLock  {
                    // Cancel any pending restore job so we don't enable the network too early 
                    // if another notification comes in before the 1 second delay is up.
                    restoreNetworkJob?.cancel()

                    // Briefly disable XMSF network to bypass MIUI/HyperOS interception
                    XmsfNetworkHelper.setXmsfNetworkingEnabled(context, false)
                    
                    // Wait briefly to ensure the network command takes effect
                    delay(50)
                    
                    // Dispatch notification
                    NotificationManagerCompat.from(context).notify(id, notification)
                    
                    // Schedule network restore after 1 second
                    restoreNetworkJob = launch {
                        delay(1000)
                        XmsfNetworkHelper.setXmsfNetworkingEnabled(context, true)
                    }
                }
            } else {
                // Fallback to standard NotificationManagerCompat
                NotificationManagerCompat.from(context).notify(id, notification)
            }
        }
    }

    fun cancel(context: Context, id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = AppPreferences(context)
            val workaroundEnabled = prefs.isShizukuWorkaroundEnabled.first()
            
            if (_isPermissionGranted.value && workaroundEnabled) {
                notifyMutex.withLock {
                    restoreNetworkJob?.cancel()
                    XmsfNetworkHelper.setXmsfNetworkingEnabled(context, false)
                    delay(50)
                    NotificationManagerCompat.from(context).cancel(id)
                    restoreNetworkJob = launch {
                        delay(1000)
                        XmsfNetworkHelper.setXmsfNetworkingEnabled(context, true)
                    }
                }
            } else {
                NotificationManagerCompat.from(context).cancel(id)
            }
        }
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    fun notifyWithCancel(context: Context, id: Int, notification: Notification) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = AppPreferences(context)
            val workaroundEnabled = prefs.isShizukuWorkaroundEnabled.first()
            
            if (_isPermissionGranted.value && workaroundEnabled) {
                notifyMutex.withLock {
                    restoreNetworkJob?.cancel()
                    XmsfNetworkHelper.setXmsfNetworkingEnabled(context, false)
                    delay(50)
                    NotificationManagerCompat.from(context).cancel(id)
                    delay(20) // Give it a moment to clear
                    NotificationManagerCompat.from(context).notify(id, notification)
                    restoreNetworkJob = launch {
                        delay(1000)
                        XmsfNetworkHelper.setXmsfNetworkingEnabled(context, true)
                    }
                }
            } else {
                NotificationManagerCompat.from(context).cancel(id)
                delay(20)
                NotificationManagerCompat.from(context).notify(id, notification)
            }
        }
    }
}
