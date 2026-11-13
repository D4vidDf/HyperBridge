package com.d4viddf.hyperbridge.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.d4viddf.hyperbridge.service.ListenerReconnectPolicy.Attempt
import com.d4viddf.hyperbridge.service.ListenerReconnectPolicy.Step
import com.d4viddf.hyperbridge.service.diagnostics.DiagnosticsStore
import com.d4viddf.hyperbridge.util.NotificationListenerBinding
import com.d4viddf.hyperbridge.util.isNotificationServiceEnabled
import com.d4viddf.hyperbridge.util.isPostNotificationsEnabled

/**
 * Process-scoped reconnect loop for [NotificationReaderService] (#330).
 *
 * It lives outside the service on purpose: `onListenerDisconnected` is normally followed by
 * `onDestroy`, which cancels the service scope, so any retry scheduled from inside the service
 * would die with it. The main-looper [Handler] survives as long as the process does.
 *
 * Every event is mirrored into [DiagnosticsStore] so a bug report shows when the listener
 * dropped, what was tried and whether it came back.
 */
object ListenerWatchdog {
    private const val TAG = "HyperBridgeDebug"
    const val CLASSIFICATION = "LISTENER"

    private val handler = Handler(Looper.getMainLooper())
    private val cycleToken = Any()

    @Volatile
    private var cycleActive = false

    /** Called from `onListenerConnected`. Ends any running cycle and clears the warning. */
    fun onConnected(context: Context) {
        val recovered = cycleActive
        cancelCycle()
        ServiceHealthNotifier.cancel(context)
        DiagnosticsStore.record(CLASSIFICATION, if (recovered) "reconnected" else "connected")
    }

    /** Called from `onListenerDisconnected`. Starts the automatic reconnect cycle once. */
    fun onDisconnected(context: Context) {
        DiagnosticsStore.record(CLASSIFICATION, "disconnected")
        if (cycleActive) return
        if (!ListenerReconnectPolicy.shouldReconnect(isNotificationServiceEnabled(context), false)) {
            DiagnosticsStore.record(CLASSIFICATION, "ignored", reason = "access-revoked")
            return
        }
        startCycle(context.applicationContext, ListenerReconnectPolicy.automaticAttempts, "auto")
    }

    /** User-driven reconnect (diagnostics button or the warning notification's action). */
    fun reconnectNow(context: Context, source: String) {
        cancelCycle()
        ServiceHealthNotifier.cancel(context)
        DiagnosticsStore.record(CLASSIFICATION, "reconnect-requested", reason = source)
        startCycle(context.applicationContext, ListenerReconnectPolicy.manualAttempts, source)
    }

    /**
     * Cheap foreground check: access is granted but nothing is bound, typically after the
     * process was killed and restarted. One quiet rebind request, no cycle, no notification.
     */
    fun ensureBound(context: Context) {
        if (cycleActive) return
        if (!ListenerReconnectPolicy.shouldReconnect(
                isNotificationServiceEnabled(context),
                NotificationReaderService.isConnected
            )
        ) return
        Log.i(TAG, "Listener access granted but not bound; requesting re-bind")
        DiagnosticsStore.record(CLASSIFICATION, "rebind-requested", reason = "foreground")
        NotificationListenerBinding.requestRebind(context)
    }

    private fun startCycle(app: Context, attempts: List<Attempt>, source: String) {
        cycleActive = true
        var at = SystemClock.uptimeMillis()
        attempts.forEachIndexed { index, attempt ->
            at += attempt.delayMs
            handler.postAtTime({ runAttempt(app, index + 1, attempt.step) }, cycleToken, at)
        }
        at += ListenerReconnectPolicy.VERDICT_DELAY_MS
        handler.postAtTime({ verdict(app, source) }, cycleToken, at)
    }

    private fun cancelCycle() {
        handler.removeCallbacksAndMessages(cycleToken)
        cycleActive = false
    }

    private fun runAttempt(app: Context, number: Int, step: Step) {
        val accessGranted = isNotificationServiceEnabled(app)
        if (!ListenerReconnectPolicy.shouldReconnect(accessGranted, NotificationReaderService.isConnected)) {
            cancelCycle()
            return
        }
        Log.i(TAG, "Listener reconnect attempt $number: $step")
        when (step) {
            Step.REQUEST_REBIND -> {
                DiagnosticsStore.record(CLASSIFICATION, "rebind-requested", reason = "attempt-$number")
                NotificationListenerBinding.requestRebind(app)
            }
            Step.TOGGLE_AND_REBIND -> {
                DiagnosticsStore.record(CLASSIFICATION, "component-toggled", reason = "attempt-$number")
                NotificationListenerBinding.toggleComponent(app)
                NotificationListenerBinding.requestRebind(app)
            }
        }
    }

    private fun verdict(app: Context, source: String) {
        cycleActive = false
        val accessGranted = isNotificationServiceEnabled(app)
        val connected = NotificationReaderService.isConnected
        if (connected) return
        if (ListenerReconnectPolicy.shouldNotifyUser(accessGranted, connected, isPostNotificationsEnabled(app))) {
            Log.w(TAG, "Listener did not come back after reconnect cycle ($source); notifying user")
            DiagnosticsStore.record(CLASSIFICATION, "unrecoverable", reason = source)
            ServiceHealthNotifier.showListenerDown(app)
        } else {
            DiagnosticsStore.record(CLASSIFICATION, "unrecoverable-silent", reason = source)
        }
    }
}
