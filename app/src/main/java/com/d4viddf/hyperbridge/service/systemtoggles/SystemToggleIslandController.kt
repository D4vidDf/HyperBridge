package com.d4viddf.hyperbridge.service.systemtoggles

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.service.BridgeNotificationChannels
import com.d4viddf.hyperbridge.service.translators.SystemToggleTranslator
import com.d4viddf.hyperbridge.util.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * System toggles island: a short pill ("Battery saver on", "Vibrate") when the user flips a system
 * switch. Status only, nothing here changes a setting. Like the privacy island it owns its
 * notification, and it registers its listeners only while the toggle is on and the listener
 * service runs.
 *
 * Signals, none of them needing a permission:
 * - broadcasts: battery saver, airplane mode, Do Not Disturb, ringer mode, Wi-Fi, NFC, location
 *   and the hotspot (hidden action, best effort);
 * - Settings observers: Bluetooth (Settings.Global bluetooth_on, which avoids BLUETOOTH_CONNECT),
 *   mobile data and auto brightness.
 *
 * What counts as a change is decided by [SystemToggleTracker]; this class only wires it up.
 */
class SystemToggleIslandController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val preferences: AppPreferences,
    private val themeRepository: ThemeRepository,
    private val onIslandActiveChanged: (Boolean) -> Unit = {}
) {
    private val translator = SystemToggleTranslator(context, themeRepository)
    private val tracker = SystemToggleTracker()
    private val mutex = Mutex()
    private val handler = Handler(Looper.getMainLooper())
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val locationManager = context.getSystemService(LocationManager::class.java)
    private val resolver = context.contentResolver

    @Volatile private var enabled = false
    private var registered = false
    private val settleJobs = HashMap<SystemToggle, Job>()
    private var hideJob: Job? = null
    private var shownEvent: SystemToggleEvent? = null
    private var reportedActive = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context, intent: Intent) {
            when (intent.action) {
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED ->
                    report(SystemToggle.BATTERY_SAVER, SystemToggleSignals.fromBoolean(powerManager?.isPowerSaveMode == true))
                Intent.ACTION_AIRPLANE_MODE_CHANGED ->
                    report(SystemToggle.AIRPLANE_MODE, SystemToggleSignals.fromBoolean(intent.getBooleanExtra("state", airplaneOn())))
                NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED ->
                    report(SystemToggle.DO_NOT_DISTURB, interruptionFilterValue())
                AudioManager.RINGER_MODE_CHANGED_ACTION ->
                    report(SystemToggle.RINGER, SystemToggleSignals.fromRingerMode(intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1)))
                WifiManager.WIFI_STATE_CHANGED_ACTION ->
                    report(SystemToggle.WIFI, SystemToggleSignals.fromWifiState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)))
                NfcAdapter.ACTION_ADAPTER_STATE_CHANGED ->
                    report(SystemToggle.NFC, SystemToggleSignals.fromNfcState(intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, -1)))
                LocationManager.MODE_CHANGED_ACTION ->
                    report(SystemToggle.LOCATION, locationValue())
                HOTSPOT_STATE_ACTION ->
                    report(SystemToggle.HOTSPOT, SystemToggleSignals.fromHotspotState(intent.getIntExtra(HOTSPOT_STATE_EXTRA, -1)))
            }
        }
    }

    /** One observer for the three settings. A null uri (some devices) just re-reads all of them. */
    private val settingsObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            report(SystemToggle.BLUETOOTH, globalFlag(BLUETOOTH_ON))
            report(SystemToggle.MOBILE_DATA, globalFlag(MOBILE_DATA))
            report(SystemToggle.AUTO_BRIGHTNESS, autoBrightnessValue())
        }
    }

    fun start() {
        scope.launch {
            preferences.systemTogglesSelectionFlow.collectLatest { tracker.setEnabledToggles(it) }
        }
        scope.launch {
            preferences.systemTogglesIslandEnabledFlow.collectLatest { shouldEnable ->
                enabled = shouldEnable
                if (shouldEnable) {
                    register()
                } else {
                    unregister()
                    mutex.withLock { clear() }
                }
            }
        }
    }

    fun stop() {
        enabled = false
        unregister()
        hideJob?.cancel()
        shownEvent = null
        cancelIsland()
    }

    private fun register() {
        if (registered) return
        registered = true
        // Seed first: a change that lands between seeding and registering still shows up as a
        // difference, while the other order would swallow it into the baseline.
        SystemToggle.entries.forEach { toggle -> currentValue(toggle)?.let { tracker.seed(toggle, it) } }
        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
            addAction(LocationManager.MODE_CHANGED_ACTION)
            addAction(HOTSPOT_STATE_ACTION)
        }
        // System broadcasts reach a not-exported receiver; the return value (a sticky intent for
        // some actions) is deliberately ignored, the initial state comes from the seeding above.
        runCatching { context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED) }
        runCatching {
            resolver.registerContentObserver(Settings.Global.getUriFor(BLUETOOTH_ON), false, settingsObserver)
            resolver.registerContentObserver(Settings.Global.getUriFor(MOBILE_DATA), false, settingsObserver)
            resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), false, settingsObserver)
        }
    }

    private fun unregister() {
        if (!registered) return
        registered = false
        runCatching { context.unregisterReceiver(receiver) }
        runCatching { resolver.unregisterContentObserver(settingsObserver) }
        synchronized(settleJobs) {
            settleJobs.values.forEach { it.cancel() }
            settleJobs.clear()
        }
        hideJob?.cancel()
        tracker.clear()
    }

    private fun report(toggle: SystemToggle, value: ToggleValue?) {
        if (!enabled) return
        if (!tracker.submit(toggle, value, SystemClock.elapsedRealtime())) return
        synchronized(settleJobs) {
            settleJobs[toggle]?.cancel()
            settleJobs[toggle] = scope.launch {
                delay(toggle.settleMillis)
                val event = tracker.settle(toggle, SystemClock.elapsedRealtime()) ?: return@launch
                mutex.withLock { if (enabled) render(event) }
            }
        }
    }

    // --- current state readers (seeding and re-reads) ---

    private fun currentValue(toggle: SystemToggle): ToggleValue? = runCatching {
        when (toggle) {
            SystemToggle.BATTERY_SAVER -> powerManager?.let { SystemToggleSignals.fromBoolean(it.isPowerSaveMode) }
            SystemToggle.AIRPLANE_MODE -> SystemToggleSignals.fromBoolean(airplaneOn())
            SystemToggle.DO_NOT_DISTURB -> interruptionFilterValue()
            SystemToggle.RINGER -> audioManager?.let { SystemToggleSignals.fromRingerMode(it.ringerMode) }
            SystemToggle.BLUETOOTH -> globalFlag(BLUETOOTH_ON)
            SystemToggle.WIFI -> globalInt(WIFI_ON)?.let { SystemToggleSignals.fromWifiSetting(it) }
            SystemToggle.MOBILE_DATA -> globalFlag(MOBILE_DATA)
            // No public way to read the hotspot state: assume off, and let the first broadcast correct it.
            SystemToggle.HOTSPOT -> ToggleValue.OFF
            SystemToggle.NFC -> NfcAdapter.getDefaultAdapter(context)?.let { SystemToggleSignals.fromBoolean(it.isEnabled) }
            SystemToggle.LOCATION -> locationValue()
            SystemToggle.AUTO_BRIGHTNESS -> autoBrightnessValue()
        }
    }.getOrNull()

    private fun airplaneOn(): Boolean = globalInt(Settings.Global.AIRPLANE_MODE_ON) == 1

    private fun interruptionFilterValue(): ToggleValue? =
        notificationManager?.let { SystemToggleSignals.fromInterruptionFilter(it.currentInterruptionFilter) }

    private fun locationValue(): ToggleValue? =
        locationManager?.let { SystemToggleSignals.fromBoolean(it.isLocationEnabled) }

    private fun autoBrightnessValue(): ToggleValue? = runCatching {
        SystemToggleSignals.fromBoolean(
            Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        )
    }.getOrNull()

    private fun globalInt(name: String): Int? =
        runCatching { Settings.Global.getInt(resolver, name) }.getOrNull()

    private fun globalFlag(name: String): ToggleValue? =
        globalInt(name)?.let { SystemToggleSignals.fromBoolean(it != 0) }

    // --- island ---

    private suspend fun render(event: SystemToggleEvent) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        // A change made while the screen is off (a schedule, a tile from a watch) has nobody to show it to.
        if (powerManager?.isInteractive == false) return
        val data = translator.translate(event, preferences.getGlobalConfigSync())
        val notification = NotificationCompat.Builder(context, BridgeNotificationChannels.ACTIVE)
            .setSmallIcon(R.drawable.ic_system_toggles)
            .setContentTitle(translator.title())
            .setContentText(translator.content(event))
            .setContentIntent(settingsIntent(event.toggle))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setDefaults(0)
            .setSound(null)
            .setVibrate(null)
            .addExtras(data.resources)
            .build()
        notification.extras.putString("miui.focus.param", data.jsonParam)
        if (shownEvent != null) NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        else ShizukuManager.notifyInPlace(context, NOTIFICATION_ID, notification)
        shownEvent = event
        setReportedActive(true)

        hideJob?.cancel()
        hideJob = scope.launch {
            delay(SystemToggleTracker.DISPLAY_MILLIS)
            mutex.withLock { clear() }
        }
    }

    /**
     * The Settings page for the toggle, or the Settings root when it has none. The intent is pinned
     * to the package that handles it so the PendingIntent is never implicit.
     */
    private fun settingsIntent(toggle: SystemToggle): PendingIntent {
        val intent = Intent(toggle.settingsAction ?: Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val settingsPackage = intent.resolveActivity(context.packageManager)?.packageName ?: SETTINGS_PACKAGE
        intent.setPackage(settingsPackage)
        return PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun clear() {
        if (shownEvent == null && !reportedActive) return
        shownEvent = null
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
        val NOTIFICATION_ID = SystemToggleTranslator.BUSINESS.hashCode()

        /** Fallback when the settings activity can't be resolved (package visibility). */
        private const val SETTINGS_PACKAGE = "com.android.settings"

        // Hidden constants: the keys and the action are stable since long before API 35.
        private const val BLUETOOTH_ON = "bluetooth_on"
        private const val MOBILE_DATA = "mobile_data"
        private const val WIFI_ON = "wifi_on"
        private const val HOTSPOT_STATE_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        private const val HOTSPOT_STATE_EXTRA = "wifi_state"
    }
}
