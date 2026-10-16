package com.d4viddf.hyperbridge.service.charging

import android.Manifest
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.integration.xiaomi.KEYGUARD_ISLAND_PROPERTY
import com.d4viddf.hyperbridge.service.BridgeNotificationChannels
import com.d4viddf.hyperbridge.service.translators.ChargingTranslator
import com.d4viddf.hyperbridge.util.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Charging status island. There is no source notification to bridge, so like the VPN island
 * this controller owns its notification: it follows the sticky battery broadcast, polls the
 * charge current while plugged in (the broadcast does not fire for current changes), and
 * cancels the island as soon as the charger is removed.
 *
 * HyperOS hides islands while the keyguard is up. With "show on lock screen" on, a second
 * notification with islandProperty 0 is posted while locked: SystemUI keeps that one at the
 * cutout. It only works when added with the screen on. While it is up the regular island is
 * withdrawn, and on unlock the two swap back.
 */
class ChargingIslandController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val preferences: AppPreferences,
    private val themeRepository: ThemeRepository,
    private val onIslandActiveChanged: (Boolean) -> Unit = {}
) {
    private val batteryManager = context.getSystemService(BatteryManager::class.java)
    private val keyguardManager = context.getSystemService(KeyguardManager::class.java)
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val translator = ChargingTranslator(context, themeRepository)
    private val mutex = Mutex()

    @Volatile private var enabled = false
    @Volatile private var lockscreenEnabled = false
    private var receiverRegistered = false
    private var lastBatteryIntent: Intent? = null
    private var shownState: ChargingState? = null
    private var notificationPosted = false
    private var lockscreenShownState: ChargingState? = null
    private var reportedActive = false
    private var pollJob: Job? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) lastBatteryIntent = intent
            val woke = intent.action == Intent.ACTION_SCREEN_ON
            scope.launch { refresh(recheckLockscreen = woke) }
        }
    }

    fun start() {
        scope.launch {
            preferences.chargingIslandEnabledFlow.collectLatest { shouldEnable ->
                enabled = shouldEnable
                if (shouldEnable) registerReceiver() else {
                    unregisterReceiver()
                    mutex.withLock { clear() }
                }
            }
        }
        scope.launch {
            preferences.chargingIslandLockscreenFlow.collectLatest { show ->
                lockscreenEnabled = show
                refresh()
            }
        }
    }

    fun stop() {
        enabled = false
        unregisterReceiver()
        pollJob?.cancel()
        pollJob = null
        shownState = null
        cancelIsland()
        cancelLockscreenIsland()
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        // ACTION_BATTERY_CHANGED is sticky: registering hands back the current state at once.
        lastBatteryIntent = ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
        scope.launch { refresh() }
    }

    private fun unregisterReceiver() {
        if (receiverRegistered) runCatching { context.unregisterReceiver(receiver) }
        receiverRegistered = false
        lastBatteryIntent = null
    }

    private fun currentState(): ChargingState? {
        val intent = lastBatteryIntent ?: return null
        val currentNow = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        return ChargingStateResolver.resolve(
            plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
            status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN),
            level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0),
            scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100),
            voltageMillivolts = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0),
            currentNow = currentNow,
            chargeTimeRemainingMillis = batteryManager?.computeChargeTimeRemaining() ?: -1L
        )
    }

    private suspend fun refresh(recheckLockscreen: Boolean = false) = mutex.withLock {
        if (!enabled) return@withLock
        // SystemUI drops keyguard islands at lock time when AOD is on; repost on wake if so.
        if (recheckLockscreen && lockscreenShownState != null && !lockscreenIslandAlive()) {
            lockscreenShownState = null
        }
        val state = currentState()
        if (state == null) {
            clear()
            return@withLock
        }
        ensurePolling()
        if (showLockscreenIsland(state)) return@withLock
        if (state != shownState || !notificationPosted) render(state)
    }

    private fun ensurePolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                refresh()
            }
        }
    }

    private suspend fun render(state: ChargingState) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPosted = false
            setReportedActive(false)
            return
        }
        val data = translator.translate(state, preferences.getGlobalConfigSync(), themeRepository.activeTheme.value)
        val notification = NotificationCompat.Builder(context, BridgeNotificationChannels.ACTIVE)
            .setSmallIcon(R.drawable.ic_charging_bolt)
            .setContentTitle(translator.title(state))
            .setContentText(translator.content(state))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setDefaults(0)
            .setSound(null)
            .setVibrate(null)
            .addExtras(data.resources)
            .build()
        notification.extras.putString("miui.focus.param", data.jsonParam)
        if (notificationPosted) NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        else ShizukuManager.notifyInPlace(context, NOTIFICATION_ID, notification)
        shownState = state
        notificationPosted = true
        setReportedActive(true)
    }

    /** Returns true while the lock-screen island stands in for the regular one. */
    private suspend fun showLockscreenIsland(state: ChargingState): Boolean {
        if (!lockscreenEnabled || keyguardManager?.isKeyguardLocked != true) {
            cancelLockscreenIsland()
            return false
        }
        // Posting with the screen off leaves the pill hidden after wake: wait for SCREEN_ON.
        if (lockscreenShownState == null && powerManager?.isInteractive != true) return false
        if (state != lockscreenShownState) postLockscreenIsland(state)
        // With the regular island still posted SystemUI keeps it first in line and hides the
        // whole island on the keyguard, so withdraw it until unlock (USER_PRESENT reposts it).
        if (notificationPosted) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            notificationPosted = false
            shownState = null
        }
        setReportedActive(true)
        return true
    }

    private suspend fun postLockscreenIsland(state: ChargingState) {
        val data = translator.translate(
            state, preferences.getGlobalConfigSync(), themeRepository.activeTheme.value,
            islandProperty = KEYGUARD_ISLAND_PROPERTY
        )
        val notification = NotificationCompat.Builder(context, BridgeNotificationChannels.ACTIVE)
            .setSmallIcon(R.drawable.ic_charging_bolt)
            .setContentTitle(translator.title(state))
            .setContentText(translator.content(state))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addExtras(data.resources)
            .build()
        notification.extras.putString("miui.focus.param", data.jsonParam)
        NotificationManagerCompat.from(context).notify(LOCKSCREEN_NOTIFICATION_ID, notification)
        lockscreenShownState = state
    }

    private fun lockscreenIslandAlive(): Boolean = runCatching {
        context.getSystemService(NotificationManager::class.java).activeNotifications
            .any { it.id == LOCKSCREEN_NOTIFICATION_ID }
    }.getOrDefault(false)

    private fun cancelLockscreenIsland() {
        if (lockscreenShownState == null) return
        NotificationManagerCompat.from(context).cancel(LOCKSCREEN_NOTIFICATION_ID)
        lockscreenShownState = null
    }

    private fun clear() {
        pollJob?.cancel()
        pollJob = null
        shownState = null
        notificationPosted = false
        cancelIsland()
        cancelLockscreenIsland()
    }

    private fun cancelIsland() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        setReportedActive(false)
    }

    private fun setReportedActive(active: Boolean) {
        if (reportedActive == active) return
        reportedActive = active
        onIslandActiveChanged(active)
    }

    companion object {
        /** Reserved controller-owned ID; periodic orphan cleanup must leave this notification alone. */
        val NOTIFICATION_ID = ChargingTranslator.BUSINESS.hashCode()
        val LOCKSCREEN_NOTIFICATION_ID = (ChargingTranslator.BUSINESS + "_lockscreen").hashCode()
        private const val POLL_INTERVAL_MS = 5_000L
    }
}
