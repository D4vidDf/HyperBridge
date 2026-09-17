package com.d4viddf.hyperbridge.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeIslandYieldPolicyTest {

    private fun focus(island: String?) =
        """{"param_v2":{"protocol":1,"business":"test"${if (island != null) ",\"param_island\":$island" else ""}}}"""

    @Test
    fun ordinaryNotificationIsNotNative() {
        assertNull(NativeIslandYieldPolicy.yieldMsFor(hasFocusParam = false, focusParam = null, isMediaPlayer = false))
    }

    @Test
    fun mediaPlayerIsTheOnlyIslandWithATimeLimit() {
        assertEquals(
            NativeIslandYieldPolicy.MEDIA_YIELD_MS,
            NativeIslandYieldPolicy.yieldMsFor(hasFocusParam = false, focusParam = null, isMediaPlayer = true)
        )
    }

    @Test
    fun mediaPlayerWinsOverItsFocusParam() {
        // A player that also declares a long island (e.g. drag-to-share data) is still a player:
        // HyperOS drops its island on pause while the notification stays, so the short window applies.
        val json = focus("""{"islandTimeout":43200}""")
        assertEquals(
            NativeIslandYieldPolicy.MEDIA_YIELD_MS,
            NativeIslandYieldPolicy.yieldMsFor(hasFocusParam = true, focusParam = json, isMediaPlayer = true)
        )
    }

    @Test
    fun focusIslandHidesThePillForTheLifetimeItDeclares() {
        // A stopwatch-style island that declares a 12 h life keeps the pill away for 12 h (#335).
        assertEquals(43_200_000L, NativeIslandYieldPolicy.focusYieldMs(focus("""{"islandTimeout":43200,"bigIslandArea":{}}""")))
        assertEquals(5_000L, NativeIslandYieldPolicy.focusYieldMs(focus("""{"islandTimeout":"5"}""")))
        assertEquals(5_000L, NativeIslandYieldPolicy.yieldMsFor(true, focus("""{"islandTimeout":5}"""), false))
    }

    @Test
    fun missingIslandTimeoutUsesXiaomiDefaultOfOneHour() {
        assertEquals(3_600_000L, NativeIslandYieldPolicy.focusYieldMs(focus("""{"bigIslandArea":{}}""")))
    }

    @Test
    fun nonPositiveIslandTimeoutUsesTheDefault() {
        assertEquals(3_600_000L, NativeIslandYieldPolicy.focusYieldMs(focus("""{"islandTimeout":0}""")))
        assertEquals(3_600_000L, NativeIslandYieldPolicy.focusYieldMs(focus("""{"islandTimeout":-1}""")))
    }

    @Test
    fun focusNotificationWithoutIslandNeverYields() {
        assertEquals(0L, NativeIslandYieldPolicy.focusYieldMs(focus(null)))
    }

    @Test
    fun unreadablePayloadHidesThePillUntilTheNotificationGoes() {
        val unbounded = NativeIslandYieldPolicy.UNBOUNDED_YIELD_MS
        assertEquals(unbounded, NativeIslandYieldPolicy.focusYieldMs(null))
        assertEquals(unbounded, NativeIslandYieldPolicy.focusYieldMs(""))
        assertEquals(unbounded, NativeIslandYieldPolicy.focusYieldMs("{not json"))
        assertEquals(unbounded, NativeIslandYieldPolicy.focusYieldMs("[1,2]"))
        assertEquals(unbounded, NativeIslandYieldPolicy.focusYieldMs("""{"param_v1":{}}"""))
        assertEquals(unbounded, NativeIslandYieldPolicy.yieldMsFor(true, null, false))
    }

    @Test
    fun malformedIslandTimeoutUsesTheDefault() {
        assertEquals(3_600_000L, NativeIslandYieldPolicy.focusYieldMs(focus("""{"islandTimeout":"soon"}""")))
        assertEquals(3_600_000L, NativeIslandYieldPolicy.focusYieldMs(focus("""{"islandTimeout":{"x":1}}""")))
    }

    @Test
    fun absurdIslandTimeoutDoesNotOverflow() {
        val ms = NativeIslandYieldPolicy.focusYieldMs(focus("""{"islandTimeout":${Long.MAX_VALUE}}"""))
        assert(ms > 0L)
    }
}
