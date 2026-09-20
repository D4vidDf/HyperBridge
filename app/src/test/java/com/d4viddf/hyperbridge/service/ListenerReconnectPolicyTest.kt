package com.d4viddf.hyperbridge.service

import com.d4viddf.hyperbridge.service.ListenerReconnectPolicy.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenerReconnectPolicyTest {
    @Test
    fun automaticCycleRetriesAtLeastTwiceBeforeGivingUp() {
        val attempts = ListenerReconnectPolicy.automaticAttempts
        assertTrue(attempts.size >= 2)
        assertTrue(attempts.count { it.step == Step.REQUEST_REBIND } >= 2)
        // The heavy component toggle is a last resort, never the first thing tried.
        assertEquals(Step.REQUEST_REBIND, attempts.first().step)
        assertEquals(Step.TOGGLE_AND_REBIND, attempts.last().step)
    }

    @Test
    fun automaticCycleWaitsBeforeTheFirstAttempt() {
        // A disconnect is usually followed by an immediate system rebind; racing it is pointless.
        assertTrue(ListenerReconnectPolicy.automaticAttempts.first().delayMs > 0)
    }

    @Test
    fun manualCycleStartsImmediatelyAndEscalates() {
        val attempts = ListenerReconnectPolicy.manualAttempts
        assertEquals(0L, attempts.first().delayMs)
        assertEquals(Step.REQUEST_REBIND, attempts.first().step)
        assertTrue(attempts.any { it.step == Step.TOGGLE_AND_REBIND })
    }

    @Test
    fun cyclesFinishWithinAMinuteAndAHalf() {
        assertTrue(ListenerReconnectPolicy.cycleDurationMs(ListenerReconnectPolicy.automaticAttempts) <= 90_000L)
        assertTrue(ListenerReconnectPolicy.cycleDurationMs(ListenerReconnectPolicy.manualAttempts) <= 90_000L)
    }

    @Test
    fun revokedAccessNeverTriggersReconnectOrNotification() {
        assertFalse(ListenerReconnectPolicy.shouldReconnect(listenerAccessGranted = false, connected = false))
        assertFalse(
            ListenerReconnectPolicy.shouldNotifyUser(
                listenerAccessGranted = false, connected = false, canPostNotifications = true
            )
        )
    }

    @Test
    fun connectedListenerNeedsNothing() {
        assertFalse(ListenerReconnectPolicy.shouldReconnect(listenerAccessGranted = true, connected = true))
        assertFalse(
            ListenerReconnectPolicy.shouldNotifyUser(
                listenerAccessGranted = true, connected = true, canPostNotifications = true
            )
        )
    }

    @Test
    fun userIsNotifiedOnlyWhenNotificationsCanBePosted() {
        assertTrue(
            ListenerReconnectPolicy.shouldNotifyUser(
                listenerAccessGranted = true, connected = false, canPostNotifications = true
            )
        )
        assertFalse(
            ListenerReconnectPolicy.shouldNotifyUser(
                listenerAccessGranted = true, connected = false, canPostNotifications = false
            )
        )
    }
}
