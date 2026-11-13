package com.d4viddf.hyperbridge.service.charging

import com.d4viddf.hyperbridge.models.ChargingSpeedMode
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
        remaining: Long = -1,
        powerMaxWatts: Int = 0,
        quickChargeType: Int = 0,
        maxChargingCurrentMicroamps: Int = 0,
        maxChargingVoltageMicrovolts: Int = 0,
        chargeCounterMicroampHours: Int = 0,
        designCapacityMah: Int = 0,
        speedMode: ChargingSpeedMode = ChargingSpeedMode.REAL_TIME
    ) = ChargingStateResolver.resolve(
        plugged = plugged,
        status = status,
        level = level,
        scale = scale,
        voltageMillivolts = voltage,
        currentNow = current,
        chargeTimeRemainingMillis = remaining,
        powerMaxWatts = powerMaxWatts,
        quickChargeType = quickChargeType,
        maxChargingCurrentMicroamps = maxChargingCurrentMicroamps,
        maxChargingVoltageMicrovolts = maxChargingVoltageMicrovolts,
        chargeCounterMicroampHours = chargeCounterMicroampHours,
        designCapacityMah = designCapacityMah,
        speedMode = speedMode
    )

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

    @Test
    fun `max negotiated speed mode prefers Xiaomi POWER_MAX`() {
        val state = resolve(
            voltage = 4100,
            current = 5_000_000,
            powerMaxWatts = 67,
            quickChargeType = 3,
            speedMode = ChargingSpeedMode.MAX_NEGOTIATED
        )!!
        assertEquals(67, state.watts)
    }

    @Test
    fun `real time speed mode compensates for dual-cell battery on high wattage Xiaomi charger`() {
        // 4.1V single-cell * 7.5A per-cell = 30.75W single cell -> ~61W dual-cell pack on 67W charger
        val state = resolve(
            voltage = 4100,
            current = 7_500_000,
            powerMaxWatts = 67,
            quickChargeType = 3,
            speedMode = ChargingSpeedMode.REAL_TIME
        )!!
        assertEquals(61, state.watts)
    }

    @Test
    fun `real time speed mode falls back to negotiated max watts when current is zero`() {
        val state = resolve(
            voltage = 4100,
            current = 0,
            powerMaxWatts = 33,
            quickChargeType = 2,
            speedMode = ChargingSpeedMode.REAL_TIME
        )!!
        assertEquals(33, state.watts)
    }

    @Test
    fun `USB-PD max current and voltage provide negotiated watts when Xiaomi POWER_MAX is absent`() {
        val maxW = ChargingStateResolver.negotiatedMaxWatts(
            powerMaxWatts = 0,
            maxChargingCurrentMicroamps = 3_000_000,
            maxChargingVoltageMicrovolts = 9_000_000
        )
        assertEquals(27, maxW)
    }

    @Test
    fun `Xiaomi quick charge profile overrides stale multi-hour batterystats ETA`() {
        // 50% on a 5000mAh battery with 67W (quickChargeType=3), while AOSP batterystats claims 19 hours
        val state = resolve(
            level = 50,
            voltage = 4100,
            current = 0,
            remaining = 19 * 3_600_000L,
            powerMaxWatts = 67,
            quickChargeType = 3,
            designCapacityMah = 5000
        )!!
        // Xiaomi BatteryChargeTimeHelper at 50% for 67W / 5000mAh is ~23 minutes
        assertEquals(23, state.minutesToFull)
    }

    @Test
    fun `Xiaomi CV phase above 90 percent uses slower CV taper rate`() {
        val state = resolve(
            level = 95,
            voltage = 4450,
            current = 0,
            remaining = 5 * 3_600_000L,
            powerMaxWatts = 67,
            quickChargeType = 3,
            designCapacityMah = 5000
        )!!
        // 5% of maxFullTimeMs (5000 * 3600s / 5400mA = 3333s) = 166s -> rounds up to 3 minutes
        assertEquals(3, state.minutesToFull)
    }
}

