package com.d4viddf.hyperbridge.service.charging

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
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
 */
class ChargingIslandController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val preferences: AppPreferences,
    private val themeRepository: ThemeRepository,
    private val onIslandActiveChanged: (Boolean) -> Unit = {}
) {
    private val batteryManager = context.getSystemService(BatteryManager::class.java)
    private val translator = ChargingTranslator(context, themeRepository)
    private val mutex = Mutex()

    @Volatile private var enabled = false
    private var receiverRegistered = false
    private var lastBatteryIntent: Intent? = null
    private var shownState: ChargingState? = null
    private var notificationPosted = false
    private var reportedActive = false
    private var pollJob: Job? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) lastBatteryIntent = intent
            scope.launch { refresh() }
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
    }

    fun stop() {
        enabled = false
        unregisterReceiver()
        pollJob?.cancel()
        pollJob = null
        shownState = null
        cancelIsland()
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
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

    private suspend fun refresh() = mutex.withLock {
        if (!enabled) return@withLock
        val state = currentState()
        if (state == null) {
            clear()
            return@withLock
        }
        ensurePolling()
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

    private fun clear() {
        pollJob?.cancel()
        pollJob = null
        shownState = null
        notificationPosted = false
        cancelIsland()
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
        private const val POLL_INTERVAL_MS = 5_000L
    }
}
