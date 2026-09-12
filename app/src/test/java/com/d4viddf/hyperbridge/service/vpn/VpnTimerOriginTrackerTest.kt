package com.d4viddf.hyperbridge.service.vpn

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnTimerOriginTrackerTest {
    @Test
    fun `uses observation time when an existing VPN has no known start`() {
        val tracker = VpnTimerOriginTracker()

        assertEquals(2_000L, tracker.resolve(1L, null, 2_000L))
    }

    @Test
    fun `keeps fallback origin stable across renders in the same session`() {
        val tracker = VpnTimerOriginTracker()

        tracker.resolve(1L, null, 2_000L)

        assertEquals(2_000L, tracker.resolve(1L, null, 9_000L))
    }

    @Test
    fun `authoritative notification time replaces fallback origin`() {
        val tracker = VpnTimerOriginTracker()

        tracker.resolve(1L, null, 2_000L)

        assertEquals(1_000L, tracker.resolve(1L, 1_000L, 9_000L))
        assertEquals(1_000L, tracker.resolve(1L, null, 10_000L))
    }

    @Test
    fun `new VPN generation receives a new fallback origin`() {
        val tracker = VpnTimerOriginTracker()

        tracker.resolve(1L, null, 2_000L)

        assertEquals(8_000L, tracker.resolve(2L, null, 8_000L))
    }
}

