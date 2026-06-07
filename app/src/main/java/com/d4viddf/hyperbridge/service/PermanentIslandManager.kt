package com.d4viddf.hyperbridge.service

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.util.ShizukuManager
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.TextInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class PermanentIslandManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val preferences: AppPreferences
) {
    private val TAG = "HyperBridgeDebug"
    private val PERMANENT_BRIDGE_ID = 9999

    private var isPermanentIslandEnabled = false
    private var isIslandActive = false
    private var currentRealNotifications = 0
    private var currentWidth = 0

    init {
        scope.launch {
            preferences.isPermanentIslandEnabledFlow.collectLatest { enabled ->
                if (isPermanentIslandEnabled != enabled) {
                    isPermanentIslandEnabled = enabled
                    updateState()
                }
            }
        }
        scope.launch  {
            preferences.permanentIslandWidthFlow.collectLatest { width ->
                if (currentWidth != width) {
                    currentWidth = width
                    if (isIslandActive) {
                        dispatchPermanentIsland()
                    }
                }
            }
        }
    }

    fun onActiveNotificationsChanged(count: Int) {
        currentRealNotifications = count
        updateState()
    }

    private fun updateState() {
        if (isPermanentIslandEnabled && currentRealNotifications == 0) {
            if (!isIslandActive) {
                dispatchPermanentIsland()
                isIslandActive = true
            }
        } else {
            if (isIslandActive) {
                removePermanentIsland()
                isIslandActive = false
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun dispatchPermanentIsland() {
        try {
            Log.d(TAG, "Dispatching permanent island")
            
            val builder = HyperIslandNotification.Builder(context, "permanent_island", "Permanent Island")
            
            // Should not be dismissible and shouldn't show in shade
            builder.setEnableFloat(false)
            builder.setIslandConfig(timeout = 0, dismissible = false, highlightColor = "#FFFFFF", expandedTimeMs = 0)
            builder.setShowNotification(false)
            builder.setReopen(false)
            builder.setIslandFirstFloat(false)

            // Only big paramislands with empty values for textonleft and picKey = ""
            // Use width spaces to change width
            val emptyString = "\u00A0".repeat(currentWidth)
            builder.setBigIslandInfo(
                left = ImageTextInfoLeft(1, null, TextInfo(emptyString, emptyString)),
                right = null
            )

            val data = HyperIslandData(builder.buildResourceBundle(), builder.buildJsonParam())

            val notifBuilder = NotificationCompat.Builder(context, "hyper_bridge_notification_channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Permanent Island")
                .setContentText("Empty Island")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)

            notifBuilder.addExtras(data.resources)

            val notification = notifBuilder.build()
            notification.extras.putString("miui.focus.param", data.jsonParam)

            ShizukuManager.notify(context, PERMANENT_BRIDGE_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching permanent island", e)
        }
    }

    private fun removePermanentIsland() {
        try {
            Log.d(TAG, "Removing permanent island")
            ShizukuManager.cancel(context, PERMANENT_BRIDGE_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing permanent island", e)
        }
    }
}
