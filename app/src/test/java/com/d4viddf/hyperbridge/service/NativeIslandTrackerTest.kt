package com.d4viddf.hyperbridge.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeIslandTrackerTest {

    private var now = 1_000_000L
    private val tracker = NativeIslandTracker(defaultYieldMs = 30_000L, clock = { now })

    @Test
    fun freshNativeIslandMakesPermanentIslandYield() {
        assertTrue(tracker.note("media|1"))
        assertTrue(tracker.hasFresh())
        assertEquals(30_000L, tracker.remainingYieldMs())
    }

    @Test
    fun yieldEndsAfterWindowEvenIfNotificationStaysPosted() {
        tracker.note("media|1")
        now += 29_999L
        assertTrue(tracker.hasFresh())
        now += 1L
        assertFalse(tracker.hasFresh())
        assertEquals(0L, tracker.remainingYieldMs())
        assertFalse(tracker.isEmpty()) // still tracked, just no longer fresh
    }

    @Test
    fun updatesDoNotRenewTheWindow() {
        tracker.note("media|1")
        now += 20_000L
        assertFalse(tracker.note("media|1")) // progress update of the same notification
        now += 10_000L
        assertFalse(tracker.hasFresh())
    }

    @Test
    fun updatesDoNotChangeTheWindowEither() {
        tracker.note("focus|1", yieldMs = 10_000L)
        assertFalse(tracker.note("focus|1", yieldMs = 3_600_000L))
        now += 10_000L
        assertFalse(tracker.hasFresh())
    }

    @Test
    fun newNativeIslandOpensItsOwnWindow() {
        tracker.note("media|1")
        now += 40_000L
        assertFalse(tracker.hasFresh())
        assertTrue(tracker.note("focus|2"))
        assertTrue(tracker.hasFresh())
        assertEquals(30_000L, tracker.remainingYieldMs())
    }

    @Test
    fun focusIslandYieldsForItsDeclaredLifetime() {
        tracker.note("focus|stopwatch", yieldMs = 3_600_000L)
        now += 30_001L
        assertTrue(tracker.hasFresh()) // the flat window would have ended here (#335)
        now += 3_600_000L - 30_001L - 1L
        assertTrue(tracker.hasFresh())
        now += 1L
        assertFalse(tracker.hasFresh())
    }

    @Test
    fun remainingYieldFollowsTheLastWindowToClose() {
        tracker.note("focus|long", yieldMs = 3_600_000L)
        now += 10_000L
        tracker.note("media|short", yieldMs = 30_000L)
        assertEquals(3_600_000L - 10_000L, tracker.remainingYieldMs())
        // The long-lived island goes away first: only the short window remains.
        assertTrue(tracker.remove("focus|long"))
        assertEquals(30_000L, tracker.remainingYieldMs())
        now += 30_000L
        assertFalse(tracker.hasFresh())
        assertEquals(0L, tracker.remainingYieldMs())
    }

    @Test
    fun zeroWindowNeverYields() {
        assertTrue(tracker.note("focus|no-island", yieldMs = 0L))
        assertFalse(tracker.hasFresh())
        assertEquals(0L, tracker.remainingYieldMs())
        assertFalse(tracker.isEmpty())
    }

    @Test
    fun hugeWindowDoesNotOverflow() {
        tracker.note("focus|forever", yieldMs = Long.MAX_VALUE)
        assertTrue(tracker.hasFresh())
        assertEquals(Long.MAX_VALUE, tracker.remainingYieldMs())
        now += 1_000_000_000L
        assertTrue(tracker.hasFresh())
    }

    @Test
    fun removeReportsWhetherKeyWasTracked() {
        tracker.note("media|1")
        assertTrue(tracker.remove("media|1"))
        assertFalse(tracker.remove("media|1"))
        assertTrue(tracker.isEmpty())
        assertFalse(tracker.hasFresh())
    }

    @Test
    fun keysListsEverythingTracked() {
        tracker.note("a")
        tracker.note("b")
        assertEquals(setOf("a", "b"), tracker.keys().toSet())
    }
}
