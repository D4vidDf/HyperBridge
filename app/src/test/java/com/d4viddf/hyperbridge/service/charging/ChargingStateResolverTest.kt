package com.d4viddf.hyperbridge.service.charging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargingStateResolverTest {

    private fun resolve(
        plugged: Int = 1,
        status: Int = 2,
        level: Int = 57,
        scale: Int = 100,
        voltage: Int = 4200,
        current: Int? = 4_000_000,
        remaining: Long = -1
    ) = ChargingStateResolver.resolve(plugged, status, level, scale, voltage, current, remaining)

    @Test
    fun `unplugged phone has no charging state`() {
        assertNull(resolve(plugged = 0, status = 3))
    }

    @Test
    fun `wired charging reports percent and watts`() {
        val state = resolve()!!
        assertEquals(57, state.percent)
        assertFalse(state.isFull)
        assertEquals(ChargingSource.WIRED, state.source)
        assertEquals(17, state.watts)
    }

    @Test
    fun `percent uses the battery scale`() {
        assertEquals(50, resolve(level = 25, scale = 50)!!.percent)
    }

    @Test
    fun `wireless and dock sources are told apart`() {
        assertEquals(ChargingSource.WIRELESS, resolve(plugged = 4)!!.source)
        assertEquals(ChargingSource.DOCK, resolve(plugged = 8)!!.source)
    }

    @Test
    fun `full battery drops watts and time to full`() {
        val state = resolve(status = 5, level = 100, remaining = 600_000)!!
        assertTrue(state.isFull)
        assertNull(state.watts)
        assertNull(state.minutesToFull)
    }

    @Test
    fun `plugged in but held below full is not charging`() {
        val state = resolve(status = 4, level = 80, remaining = 600_000)!!
        assertFalse(state.isCharging)
        assertFalse(state.isFull)
        assertNull(state.watts)
        assertNull(state.minutesToFull)
    }

    @Test
    fun `time to full rounds up to whole minutes`() {
        assertEquals(2, resolve(remaining = 61_000)!!.minutesToFull)
        assertNull(resolve(remaining = -1)!!.minutesToFull)
    }

    @Test
    fun `negative current while charging still gives watts`() {
        assertEquals(17, ChargingStateResolver.watts(4200, -4_000_000))
    }

    @Test
    fun `milliamp kernels are recognised`() {
        assertEquals(8, ChargingStateResolver.watts(4000, 2000))
    }

    @Test
    fun `missing or implausible current hides watts`() {
        assertNull(ChargingStateResolver.watts(4200, null))
        assertNull(ChargingStateResolver.watts(4200, 0))
        assertNull(ChargingStateResolver.watts(4200, Int.MIN_VALUE))
        assertNull(ChargingStateResolver.watts(0, 4_000_000))
        assertNull(ChargingStateResolver.watts(4200, Int.MAX_VALUE))
    }
}
