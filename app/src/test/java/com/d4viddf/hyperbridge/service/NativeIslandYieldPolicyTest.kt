package com.d4viddf.hyperbridge.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeIslandYieldPolicyTest {

    private fun focus(island: String?) =
        """{"param_v2":{"protocol":1,"business":"test"${if (island != null) ",\"param_island\":$island" else ""}}}"""

    @Test
    fun ordinaryNotificationIsNotNative() {
        assertNull(NativeIslandYieldPolicy.yieldMsFor(hasFocusParam = false, focusParam = null, isMediaStyle = false))
    }

    @Test
    fun mediaPlayerKeepsTheShortWindow() {
        assertEquals(
            NativeIslandYieldPolicy.MEDIA_YIELD_MS,
            NativeIslandYieldPolicy.yieldMsFor(hasFocusParam = false, focusParam = null, isMediaStyle = true)
        )
    }

    @Test
    fun focusParamWinsOverMediaStyle() {
        val json = focus("""{"islandTimeout":120}""")
        assertEquals(120_000L, NativeIslandYieldPolicy.yieldMsFor(hasFocusParam = true, focusParam = json, isMediaStyle = true))
    }

    @Test
    fun declaredIslandTimeoutIsSeconds() {
        // A stopwatch-style island that declares a 12 h life keeps the pill away for 12 h (#335).
        assertEquals(43_200_000L, NativeIslandYieldPolicy.focusYieldMs(focus("""{"islandTimeout":43200,"bigIslandArea":{}}""")))
        assertEquals(5_000L, NativeIslandYieldPolicy.focusYieldMs(focus("""{"islandTimeout":"5"}""")))
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
    fun unreadablePayloadFallsBackToTheFlatWindow() {
        assertEquals(NativeIslandYieldPolicy.FALLBACK_YIELD_MS, NativeIslandYieldPolicy.focusYieldMs(null))
        assertEquals(NativeIslandYieldPolicy.FALLBACK_YIELD_MS, NativeIslandYieldPolicy.focusYieldMs(""))
        assertEquals(NativeIslandYieldPolicy.FALLBACK_YIELD_MS, NativeIslandYieldPolicy.focusYieldMs("{not json"))
        assertEquals(NativeIslandYieldPolicy.FALLBACK_YIELD_MS, NativeIslandYieldPolicy.focusYieldMs("[1,2]"))
        assertEquals(NativeIslandYieldPolicy.FALLBACK_YIELD_MS, NativeIslandYieldPolicy.focusYieldMs("""{"param_v1":{}}"""))
        assertEquals(NativeIslandYieldPolicy.FALLBACK_YIELD_MS, NativeIslandYieldPolicy.yieldMsFor(true, null, false))
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
