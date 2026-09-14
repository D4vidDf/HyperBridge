package com.d4viddf.hyperbridge.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeIslandTrackerTest {

    private var now = 1_000_000L
    private val tracker = NativeIslandTracker(yieldMs = 30_000L, clock = { now })

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
    fun newNativeIslandOpensItsOwnWindow() {
        tracker.note("media|1")
        now += 40_000L
        assertFalse(tracker.hasFresh())
        assertTrue(tracker.note("focus|2"))
        assertTrue(tracker.hasFresh())
        assertEquals(30_000L, tracker.remainingYieldMs())
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
