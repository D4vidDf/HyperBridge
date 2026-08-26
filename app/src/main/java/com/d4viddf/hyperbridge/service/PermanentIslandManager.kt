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
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds


class PermanentIslandManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val preferences: AppPreferences
) {
    private val TAG = "HyperBridgeDebug"

    companion object {
        const val PERMANENT_BRIDGE_ID = 9999
        // The dismiss path posts 9999 right after cancelling the previous focus
        // island. Delaying the post lets HyperOS finish tearing that island down,
        // otherwise it can swallow the re-post and leave 9999 posted but hidden.
        private const val DISPATCH_DELAY_MS = 700L
    }

    private var isPermanentIslandEnabled = false
    private var isIslandActive = false
    private var currentRealNotifications = 0

    fun isIslandActive(): Boolean = isIslandActive
    private var hasNativeIsland = false
    private var currentWidth = 0
    private var isHideInLandscapeEnabled = false
    private var pendingDispatchJob: Job? = null
    private var showIslandOnLockscreen = true

    private var lockscreenOverlayView: View? = null
    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    }

    init {
        scope.launch {
            preferences.isPermanentIslandEnabledFlow.collectLatest { enabled ->
                synchronized(this@PermanentIslandManager) {
                    if (isPermanentIslandEnabled != enabled) {
                        isPermanentIslandEnabled = enabled
                        updateStateLocked()
                    }
                }
            }
        }
        scope.launch {
            preferences.hidePermanentIslandLandscapeFlow.collectLatest { hide ->
                synchronized(this@PermanentIslandManager) {
                    if (isHideInLandscapeEnabled != hide) {
                        isHideInLandscapeEnabled = hide
                        updateStateLocked()
                    }
                }
            }
        }
        scope.launch {
            preferences.permanentIslandWidthFlow.collectLatest { width ->
                synchronized(this@PermanentIslandManager) {
                    if (currentWidth != width) {
                        currentWidth = width
                        if (isIslandActive) {
                            dispatchPermanentIsland()
                            if (showIslandOnLockscreen) {
                                showLockscreenOverlayLocked()
                            }
                        }
                    }
                }
            }
        }
        scope.launch {
            preferences.showIslandOnLockscreenFlow.collectLatest { show ->
                synchronized(this@PermanentIslandManager) {
                    showIslandOnLockscreen = show
                    if (isIslandActive) {
                        dispatchPermanentIsland()
                        if (show) {
                            showLockscreenOverlayLocked()
                        } else {
                            removeLockscreenOverlayLocked()
                        }
                    }
                }
            }
        }
    }

    @Synchronized
    fun onActiveNotificationsChanged(count: Int, hasNative: Boolean = false) {
        currentRealNotifications = count
        hasNativeIsland = hasNative
        updateStateLocked()
    }

    @Synchronized
    fun onOrientationChanged() {
        updateStateLocked()
    }

    private fun desiredActive(): Boolean {
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        return isPermanentIslandEnabled && !hasNativeIsland && !(isHideInLandscapeEnabled && isLandscape)
    }

    @Synchronized
    fun reconcile(count: Int, hasNative: Boolean, isIslandPresent: Boolean, refresh: Boolean) {
        currentRealNotifications = count
        hasNativeIsland = hasNative
        val shouldShow = desiredActive()
        val isLandscape = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        if (shouldShow && isIslandPresent && refresh) {
            val jobToCancel = pendingDispatchJob
            pendingDispatchJob = null
            jobToCancel?.cancel()
            dispatchPermanentIsland()
            isIslandActive = true
            if (showIslandOnLockscreen) {
                showLockscreenOverlayLocked()
            }
            return
        }
        isIslandActive = isIslandPresent
        Log.d(TAG, "updateState: shouldShow=$shouldShow, isLandscape=$isLandscape, isHideInLandscapeEnabled=$isHideInLandscapeEnabled")
        updateStateLocked()
    }

    private fun updateStateLocked() {
        if (desiredActive()) {
            if (!isIslandActive) {
                isIslandActive = true
                scheduleDispatchLocked()
            }
            if (showIslandOnLockscreen) {
                showLockscreenOverlayLocked()
            } else {
                removeLockscreenOverlayLocked()
            }
        } else {
            if (isIslandActive) {
                isIslandActive = false
                val jobToCancel = pendingDispatchJob
                pendingDispatchJob = null
                jobToCancel?.cancel()
                removePermanentIsland()
            }
            removeLockscreenOverlayLocked()
        }
    }

    private fun showLockscreenOverlayLocked() {
        if (!showIslandOnLockscreen || !Settings.canDrawOverlays(context)) {
            removeLockscreenOverlayLocked()
            return
        }
        scope.launch(Dispatchers.Main) {
            try {
                val density = context.resources.displayMetrics.density
                val widthPx = ((120 + currentWidth * 5) * density).toInt()
                val heightPx = (26 * density).toInt()

                val statusBarResId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
                val statusBarHeight = if (statusBarResId > 0) context.resources.getDimensionPixelSize(statusBarResId) else (28 * density).toInt()
                val topMarginPx = max(0, (statusBarHeight - heightPx) / 2)

                if (lockscreenOverlayView == null) {
                    val pillView = View(context).apply {
                        val shape = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = 50 * density
                            setColor(Color.BLACK)
                        }
                        background = shape
                    }
                    val params = WindowManager.LayoutParams(
                        widthPx,
                        heightPx,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        y = topMarginPx
                    }
                    windowManager?.addView(pillView, params)
                    lockscreenOverlayView = pillView
                } else {
                    val params = lockscreenOverlayView?.layoutParams as? WindowManager.LayoutParams
                    if (params != null) {
                        params.width = widthPx
                        params.height = heightPx
                        params.y = topMarginPx
                        windowManager?.updateViewLayout(lockscreenOverlayView, params)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying lockscreen overlay", e)
            }
        }
    }

    private fun removeLockscreenOverlayLocked() {
        scope.launch(Dispatchers.Main) {
            try {
                lockscreenOverlayView?.let {
                    windowManager?.removeView(it)
                    lockscreenOverlayView = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing lockscreen overlay", e)
            }
        }
    }

    private fun scheduleDispatchLocked() {
        val jobToCancel = pendingDispatchJob
        pendingDispatchJob = null
        jobToCancel?.cancel()
        pendingDispatchJob = scope.launch {
            delay(DISPATCH_DELAY_MS.milliseconds)
            synchronized(this@PermanentIslandManager) {
                pendingDispatchJob = null
                // Re-check under the lock: the desired state may have flipped during the delay.
                if (desiredActive()) {
                    dispatchPermanentIsland()
                }
            }
        }
    }
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun dispatchPermanentIsland() {
        try {
            Log.d(TAG, "Dispatching permanent island")
            
            val builder = HyperIslandNotification.Builder(context, "permanent_island", "Permanent Island")
            
            // When show on lockscreen is enabled, tell HyperOS focus protocol to display the notification on keyguard
            builder.setEnableFloat(false)
            builder.setIslandConfig(timeout = 86400000, dismissible = false, highlightColor = "#FFFFFF", expandedTimeMs = 0)
            builder.setShowNotification(showIslandOnLockscreen)
            builder.setReopen(true)
            builder.setIslandFirstFloat(false)

            // Only big paramislands with empty values for textonleft and picKey = null
            // Use width spaces to change width
            val emptyString = "\u00A0".repeat(currentWidth)
            builder.setBigIslandInfo(
                left = ImageTextInfoLeft(1, null, TextInfo(emptyString, emptyString)),
                right = null
            )
            builder.setSmallIsland(emptyString)

            val data = HyperIslandData(builder.buildResourceBundle(), builder.buildJsonParam())

            val notifPriority = if (showIslandOnLockscreen) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_MIN
            val notifBuilder = NotificationCompat.Builder(context, "hyper_bridge_notification_channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Permanent Island")
                .setContentText("Empty Island")
                .setPriority(notifPriority)
                .setOngoing(true)

            // Lockscreen visibility: show permanent island on lock screen if user enabled it
            if (showIslandOnLockscreen) {
                notifBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            }

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
