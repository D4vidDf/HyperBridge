package com.d4viddf.hyperbridge.service.debugging

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.service.BridgeNotificationChannels
import com.d4viddf.hyperbridge.service.translators.DebuggingTranslator
import com.d4viddf.hyperbridge.util.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.Inet4Address

/**
 * USB / wireless debugging island. Like the VPN and charging islands it owns its notification:
 * the adb toggles come from Settings.Global, and "a computer is attached" comes from the
 * system's own ongoing notifications, which the listener service forwards here.
 */
class DebuggingIslandController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val preferences: AppPreferences,
    private val themeRepository: ThemeRepository,
    private val initialNotifications: () -> Array<StatusBarNotification> = { emptyArray() },
    private val onIslandActiveChanged: (Boolean) -> Unit = {}
) {
    private val translator = DebuggingTranslator(context, themeRepository)
    private val mutex = Mutex()

    @Volatile private var enabled = false
    @Volatile private var usbConnected = false
    @Volatile private var wirelessConnected = false
    @Volatile private var systemContentIntent: PendingIntent? = null
    private var observerRegistered = false
    private var shownState: DebuggingState? = null
    private var reportedActive = false

    private val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            scope.launch { refresh() }
        }
    }

    fun start() {
        scope.launch {
            preferences.debuggingIslandEnabledFlow.collectLatest { shouldEnable ->
                enabled = shouldEnable
                if (shouldEnable) {
                    registerObserver()
                    seedFromActiveNotifications()
                    refresh()
                } else {
                    unregisterObserver()
                    mutex.withLock { clear() }
                }
            }
        }
    }

    /** The service may start before the listener is bound; catch up once it is. */
    fun onListenerConnected() {
        if (!enabled) return
        scope.launch {
            seedFromActiveNotifications()
            refresh()
        }
    }

    fun stop() {
        enabled = false
        unregisterObserver()
        shownState = null
        cancelIsland()
    }

    fun onSourceNotificationPosted(sbn: StatusBarNotification) {
        if (!enabled || !track(sbn, present = true)) return
        scope.launch { refresh() }
    }

    fun onSourceNotificationRemoved(sbn: StatusBarNotification) {
        if (!enabled || !track(sbn, present = false)) return
        scope.launch { refresh() }
    }

    /** Returns true when [sbn] is one of the system's debugging notifications. */
    private fun track(sbn: StatusBarNotification, present: Boolean): Boolean {
        val tag = sbn.tag
        when {
            DebuggingStateResolver.isUsbNotification(sbn.packageName, sbn.id, tag) -> usbConnected = present
            DebuggingStateResolver.isWirelessNotification(sbn.packageName, sbn.id, tag) -> wirelessConnected = present
            else -> return false
        }
        // Its tap target is the exact settings page that turns debugging off.
        if (present) systemContentIntent = sbn.notification.contentIntent
        else if (!usbConnected && !wirelessConnected) systemContentIntent = null
        return true
    }

    private fun seedFromActiveNotifications() {
        val active = runCatching { initialNotifications() }.getOrDefault(emptyArray())
        usbConnected = false
        wirelessConnected = false
        systemContentIntent = null
        active.forEach { track(it, present = true) }
    }

    private fun registerObserver() {
        if (observerRegistered) return
        val resolver = context.contentResolver
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false, settingsObserver)
        resolver.registerContentObserver(Settings.Global.getUriFor(ADB_WIFI_ENABLED), false, settingsObserver)
        observerRegistered = true
    }

    private fun unregisterObserver() {
        if (observerRegistered) runCatching { context.contentResolver.unregisterContentObserver(settingsObserver) }
        observerRegistered = false
    }

    private fun globalFlag(name: String): Boolean =
        runCatching { Settings.Global.getInt(context.contentResolver, name, 0) != 0 }.getOrDefault(false)

    private fun currentState(): DebuggingState? {
        val wirelessEnabled = globalFlag(ADB_WIFI_ENABLED)
        return DebuggingStateResolver.resolve(
            usbEnabled = globalFlag(Settings.Global.ADB_ENABLED),
            wirelessEnabled = wirelessEnabled,
            usbConnected = usbConnected,
            wirelessConnected = wirelessConnected,
            wifiAddress = if (wirelessEnabled) wifiAddress() else null
        )
    }

    private fun wifiAddress(): String? = runCatching {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val network = connectivity.activeNetwork ?: return null
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return null
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
        connectivity.getLinkProperties(network)?.linkAddresses
            ?.map { it.address }
            ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    }.getOrNull()

    private suspend fun refresh() = mutex.withLock {
        if (!enabled) return@withLock
        val state = currentState()
        if (state == null) {
            clear()
            return@withLock
        }
        if (state != shownState) render(state)
    }

    private suspend fun render(state: DebuggingState) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            setReportedActive(false)
            return
        }
        val data = translator.translate(
            state = state,
            config = preferences.getGlobalConfigSync(),
            theme = themeRepository.activeTheme.value
        )
        val notification = NotificationCompat.Builder(context, BridgeNotificationChannels.ACTIVE)
            .setSmallIcon(R.drawable.ic_debugging_adb)
            .setContentTitle(translator.title(state))
            .setContentText(translator.content(state))
            .setContentIntent(systemContentIntent ?: developerOptionsIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setDefaults(0)
            .setSound(null)
            .setVibrate(null)
            .addExtras(data.resources)
            .build()
        notification.extras.putString("miui.focus.param", data.jsonParam)
        if (shownState != null) NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        else ShizukuManager.notifyInPlace(context, NOTIFICATION_ID, notification)
        shownState = state
        setReportedActive(true)
    }

    private fun developerOptionsIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        NOTIFICATION_ID,
        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun clear() {
        if (shownState == null && !reportedActive) return
        shownState = null
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
        val NOTIFICATION_ID = DebuggingTranslator.BUSINESS.hashCode()
        /** Settings.Global.ADB_WIFI_ENABLED is @hide; the key itself is stable since Android 11. */
        private const val ADB_WIFI_ENABLED = "adb_wifi_enabled"
    }
}
