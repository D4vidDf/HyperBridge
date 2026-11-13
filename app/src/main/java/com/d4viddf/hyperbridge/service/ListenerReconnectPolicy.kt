package com.d4viddf.hyperbridge.service

/**
 * Decides how HyperBridge reacts when the system unbinds the notification listener (#330).
 *
 * A disconnected listener usually comes back after an official `requestRebind`. When it does
 * not, toggling the component's enabled state (what `BootReceiver` already does after every
 * boot) forces the system to re-evaluate enabled listeners. Only after every step has failed
 * is the user told, so a transient unbind (package update, user switch, system pressure) never
 * produces a notification.
 *
 * Pure Kotlin so the schedule and the guards are unit-testable without Android.
 */
object ListenerReconnectPolicy {
    enum class Step { REQUEST_REBIND, TOGGLE_AND_REBIND }

    /** One reconnect attempt, [delayMs] after the previous attempt (or after the trigger). */
    data class Attempt(val delayMs: Long, val step: Step)

    /** Attempts run after the system disconnects the listener on its own. */
    val automaticAttempts: List<Attempt> = listOf(
        Attempt(3_000L, Step.REQUEST_REBIND),
        Attempt(12_000L, Step.REQUEST_REBIND),
        Attempt(30_000L, Step.TOGGLE_AND_REBIND)
    )

    /** Attempts run when the user taps "Reconnect". The first one is immediate. */
    val manualAttempts: List<Attempt> = listOf(
        Attempt(0L, Step.REQUEST_REBIND),
        Attempt(4_000L, Step.TOGGLE_AND_REBIND)
    )

    /** Time after the last attempt before the cycle is declared failed. */
    const val VERDICT_DELAY_MS = 15_000L

    /** Whether reconnect work should happen at all. Revoked access is the user's choice. */
    fun shouldReconnect(listenerAccessGranted: Boolean, connected: Boolean): Boolean =
        listenerAccessGranted && !connected

    /** Whether to tell the user once a cycle ran out without the listener coming back. */
    fun shouldNotifyUser(
        listenerAccessGranted: Boolean,
        connected: Boolean,
        canPostNotifications: Boolean
    ): Boolean = shouldReconnect(listenerAccessGranted, connected) && canPostNotifications

    /** Total time from trigger to verdict for a list of attempts. */
    fun cycleDurationMs(attempts: List<Attempt>): Long = attempts.sumOf { it.delayMs } + VERDICT_DELAY_MS
}
