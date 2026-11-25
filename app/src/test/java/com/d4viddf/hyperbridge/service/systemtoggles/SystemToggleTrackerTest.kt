package com.d4viddf.hyperbridge.service.systemtoggles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemToggleTrackerTest {

    private fun tracker(
        enabled: Set<SystemToggle> = SystemToggle.entries.toSet(),
        seeds: Array<Pair<SystemToggle, ToggleValue>> = emptyArray()
    ) = SystemToggleTracker(enabled).also { t -> seeds.forEach { (toggle, value) -> t.seed(toggle, value) } }

    /** submit + settle in one step, as the controller does after the settle delay. */
    private fun SystemToggleTracker.report(toggle: SystemToggle, value: ToggleValue?, at: Long): SystemToggleEvent? =
        if (submit(toggle, value, at)) settle(toggle) else null

    @Test
    fun `a real change shows an event`() {
        val t = tracker(seeds = arrayOf(SystemToggle.BATTERY_SAVER to ToggleValue.OFF))
        assertEquals(
            SystemToggleEvent(SystemToggle.BATTERY_SAVER, ToggleValue.ON),
            t.report(SystemToggle.BATTERY_SAVER, ToggleValue.ON, 0)
        )
    }

    @Test
    fun `seeding never produces an event`() {
        val t = SystemToggleTracker()
        t.seed(SystemToggle.BLUETOOTH, ToggleValue.ON)
        assertNull(t.settle(SystemToggle.BLUETOOTH))
    }

    @Test
    fun `the first report of an unseeded toggle is only the starting state`() {
        val t = SystemToggleTracker()
        assertFalse(t.submit(SystemToggle.NFC, ToggleValue.ON, 0))
        assertNull(t.settle(SystemToggle.NFC))
        assertEquals(
            SystemToggleEvent(SystemToggle.NFC, ToggleValue.OFF),
            t.report(SystemToggle.NFC, ToggleValue.OFF, 1_000)
        )
    }

    @Test
    fun `reporting the known value again shows nothing`() {
        val t = tracker(seeds = arrayOf(SystemToggle.WIFI to ToggleValue.ON))
        assertNull(t.report(SystemToggle.WIFI, ToggleValue.ON, 0))
    }

    @Test
    fun `a repeated broadcast after the island was shown shows nothing`() {
        val t = tracker(seeds = arrayOf(SystemToggle.LOCATION to ToggleValue.OFF))
        assertEquals(ToggleValue.ON, t.report(SystemToggle.LOCATION, ToggleValue.ON, 0)?.value)
        assertNull(t.report(SystemToggle.LOCATION, ToggleValue.ON, 50))
    }

    @Test
    fun `transitional states are ignored`() {
        val t = tracker(seeds = arrayOf(SystemToggle.WIFI to ToggleValue.OFF))
        assertFalse(t.submit(SystemToggle.WIFI, SystemToggleSignals.fromWifiState(2), 0)) // enabling
        assertNull(t.settle(SystemToggle.WIFI))
        assertEquals(
            SystemToggleEvent(SystemToggle.WIFI, ToggleValue.ON),
            t.report(SystemToggle.WIFI, SystemToggleSignals.fromWifiState(3), 100)
        )
    }

    @Test
    fun `flipping on and straight back ends with no island`() {
        val t = tracker(seeds = arrayOf(SystemToggle.BATTERY_SAVER to ToggleValue.OFF))
        assertTrue(t.submit(SystemToggle.BATTERY_SAVER, ToggleValue.ON, 0))
        assertFalse(t.submit(SystemToggle.BATTERY_SAVER, ToggleValue.OFF, 100))
        // The settle that was scheduled by the first report finds the value back where it began.
        assertNull(t.settle(SystemToggle.BATTERY_SAVER))
    }

    @Test
    fun `several flips settle on the last value only`() {
        val t = tracker(seeds = arrayOf(SystemToggle.BATTERY_SAVER to ToggleValue.OFF))
        t.submit(SystemToggle.BATTERY_SAVER, ToggleValue.ON, 0)
        t.submit(SystemToggle.BATTERY_SAVER, ToggleValue.OFF, 50)
        t.submit(SystemToggle.BATTERY_SAVER, ToggleValue.ON, 100)
        assertEquals(ToggleValue.ON, t.settle(SystemToggle.BATTERY_SAVER)?.value)
        assertNull(t.settle(SystemToggle.BATTERY_SAVER))
    }

    @Test
    fun `a toggle switched off in settings shows nothing but keeps its state`() {
        val t = tracker(
            enabled = setOf(SystemToggle.WIFI),
            seeds = arrayOf(SystemToggle.NFC to ToggleValue.OFF)
        )
        assertNull(t.report(SystemToggle.NFC, ToggleValue.ON, 0))
        // Enabling it later does not replay the old change.
        t.setEnabledToggles(setOf(SystemToggle.NFC))
        assertNull(t.report(SystemToggle.NFC, ToggleValue.ON, 10_000))
        assertEquals(ToggleValue.OFF, t.report(SystemToggle.NFC, ToggleValue.OFF, 20_000)?.value)
    }

    @Test
    fun `radios switched off by airplane mode do not get their own island`() {
        val t = tracker(
            seeds = arrayOf(
                SystemToggle.AIRPLANE_MODE to ToggleValue.OFF,
                SystemToggle.WIFI to ToggleValue.ON,
                SystemToggle.BLUETOOTH to ToggleValue.ON,
                SystemToggle.NFC to ToggleValue.ON
            )
        )
        assertEquals(ToggleValue.ON, t.report(SystemToggle.AIRPLANE_MODE, ToggleValue.ON, 0)?.value)
        assertNull(t.report(SystemToggle.WIFI, ToggleValue.OFF, 800))
        assertNull(t.report(SystemToggle.BLUETOOTH, ToggleValue.OFF, 1_200))
        assertNull(t.report(SystemToggle.NFC, ToggleValue.OFF, 2_000))
    }

    @Test
    fun `turning wifi back on a while after airplane mode shows an island`() {
        val t = tracker(
            seeds = arrayOf(
                SystemToggle.AIRPLANE_MODE to ToggleValue.OFF,
                SystemToggle.WIFI to ToggleValue.ON
            )
        )
        t.report(SystemToggle.AIRPLANE_MODE, ToggleValue.ON, 0)
        assertNull(t.report(SystemToggle.WIFI, ToggleValue.OFF, 500))
        assertEquals(ToggleValue.ON, t.report(SystemToggle.WIFI, ToggleValue.ON, 30_000)?.value)
    }

    @Test
    fun `the hotspot taking wifi down is not reported as wifi off`() {
        val t = tracker(
            seeds = arrayOf(
                SystemToggle.HOTSPOT to ToggleValue.OFF,
                SystemToggle.WIFI to ToggleValue.ON
            )
        )
        assertEquals(ToggleValue.ON, t.report(SystemToggle.HOTSPOT, ToggleValue.ON, 0)?.value)
        assertNull(t.report(SystemToggle.WIFI, ToggleValue.OFF, 1_000))
    }

    @Test
    fun `do not disturb moving the ringer shows only do not disturb`() {
        val t = tracker(
            seeds = arrayOf(
                SystemToggle.DO_NOT_DISTURB to ToggleValue.OFF,
                SystemToggle.RINGER to ToggleValue.RING
            )
        )
        assertEquals(ToggleValue.ON, t.report(SystemToggle.DO_NOT_DISTURB, ToggleValue.ON, 0)?.value)
        assertNull(t.report(SystemToggle.RINGER, ToggleValue.SILENT, 20))
    }

    @Test
    fun `the ringer arriving before do not disturb is still dropped`() {
        val t = tracker(
            seeds = arrayOf(
                SystemToggle.DO_NOT_DISTURB to ToggleValue.OFF,
                SystemToggle.RINGER to ToggleValue.RING
            )
        )
        // Both are recorded before the ringer's settle delay runs out.
        assertTrue(t.submit(SystemToggle.RINGER, ToggleValue.VIBRATE, 0))
        assertTrue(t.submit(SystemToggle.DO_NOT_DISTURB, ToggleValue.ON, 30))
        assertNull(t.settle(SystemToggle.RINGER))
        assertEquals(ToggleValue.ON, t.settle(SystemToggle.DO_NOT_DISTURB)?.value)
    }

    @Test
    fun `a ringer change on its own shows an island`() {
        val t = tracker(seeds = arrayOf(SystemToggle.RINGER to ToggleValue.RING))
        assertEquals(
            SystemToggleEvent(SystemToggle.RINGER, ToggleValue.VIBRATE),
            t.report(SystemToggle.RINGER, ToggleValue.VIBRATE, 0)
        )
        assertEquals(ToggleValue.SILENT, t.report(SystemToggle.RINGER, ToggleValue.SILENT, 5_000)?.value)
    }

    @Test
    fun `a ringer change long after do not disturb shows an island`() {
        val t = tracker(
            seeds = arrayOf(
                SystemToggle.DO_NOT_DISTURB to ToggleValue.OFF,
                SystemToggle.RINGER to ToggleValue.RING
            )
        )
        t.report(SystemToggle.DO_NOT_DISTURB, ToggleValue.ON, 0)
        assertEquals(ToggleValue.SILENT, t.report(SystemToggle.RINGER, ToggleValue.SILENT, 20_000)?.value)
    }

    @Test
    fun `clear forgets everything so a restart starts from a fresh baseline`() {
        val t = tracker(seeds = arrayOf(SystemToggle.WIFI to ToggleValue.ON))
        t.clear()
        assertFalse(t.submit(SystemToggle.WIFI, ToggleValue.OFF, 0))
        assertNull(t.settle(SystemToggle.WIFI))
    }

    @Test
    fun `ringer waits longer than a plain switch`() {
        assertTrue(SystemToggle.RINGER.settleMillis > SystemToggle.WIFI.settleMillis)
    }

    @Test
    fun `stored toggle list round trips and drops unknown names`() {
        val set = setOf(SystemToggle.WIFI, SystemToggle.NFC)
        assertEquals(set, SystemToggle.parseSet(SystemToggle.serialize(set)))
        assertEquals(setOf(SystemToggle.WIFI), SystemToggle.parseSet("WIFI,FUTURE_TOGGLE"))
        assertEquals(emptySet<SystemToggle>(), SystemToggle.parseSet(""))
        assertNull(SystemToggle.parseSet(null))
    }

    @Test
    fun `every toggle names a settings action or falls back to the root`() {
        SystemToggle.entries.forEach { toggle ->
            toggle.settingsAction?.let { assertTrue(toggle.name, it.startsWith("android.settings.")) }
        }
    }
}

class SystemToggleSignalsTest {

    @Test
    fun `wifi maps only the settled states`() {
        assertEquals(ToggleValue.OFF, SystemToggleSignals.fromWifiState(1))
        assertEquals(ToggleValue.ON, SystemToggleSignals.fromWifiState(3))
        assertNull(SystemToggleSignals.fromWifiState(0)) // disabling
        assertNull(SystemToggleSignals.fromWifiState(2)) // enabling
        assertNull(SystemToggleSignals.fromWifiState(4)) // unknown
    }

    @Test
    fun `wifi setting counts the airplane override as on and airplane off as off`() {
        assertEquals(ToggleValue.OFF, SystemToggleSignals.fromWifiSetting(0))
        assertEquals(ToggleValue.ON, SystemToggleSignals.fromWifiSetting(1))
        assertEquals(ToggleValue.ON, SystemToggleSignals.fromWifiSetting(2))
        assertEquals(ToggleValue.OFF, SystemToggleSignals.fromWifiSetting(3))
    }

    @Test
    fun `nfc maps only the settled states`() {
        assertEquals(ToggleValue.OFF, SystemToggleSignals.fromNfcState(1))
        assertNull(SystemToggleSignals.fromNfcState(2)) // turning on
        assertEquals(ToggleValue.ON, SystemToggleSignals.fromNfcState(3))
        assertNull(SystemToggleSignals.fromNfcState(4)) // turning off
    }

    @Test
    fun `hotspot maps only the settled states`() {
        assertNull(SystemToggleSignals.fromHotspotState(10)) // disabling
        assertEquals(ToggleValue.OFF, SystemToggleSignals.fromHotspotState(11))
        assertNull(SystemToggleSignals.fromHotspotState(12)) // enabling
        assertEquals(ToggleValue.ON, SystemToggleSignals.fromHotspotState(13))
        assertNull(SystemToggleSignals.fromHotspotState(14)) // failed
    }

    @Test
    fun `ringer modes map to silent vibrate and ring`() {
        assertEquals(ToggleValue.SILENT, SystemToggleSignals.fromRingerMode(0))
        assertEquals(ToggleValue.VIBRATE, SystemToggleSignals.fromRingerMode(1))
        assertEquals(ToggleValue.RING, SystemToggleSignals.fromRingerMode(2))
        assertNull(SystemToggleSignals.fromRingerMode(7))
    }

    @Test
    fun `every do not disturb mode but allow all counts as on`() {
        assertNull(SystemToggleSignals.fromInterruptionFilter(0))
        assertEquals(ToggleValue.OFF, SystemToggleSignals.fromInterruptionFilter(1))
        assertEquals(ToggleValue.ON, SystemToggleSignals.fromInterruptionFilter(2))
        assertEquals(ToggleValue.ON, SystemToggleSignals.fromInterruptionFilter(3))
        assertEquals(ToggleValue.ON, SystemToggleSignals.fromInterruptionFilter(4))
    }

    @Test
    fun `attention colour is only for silent and vibrate`() {
        assertEquals("#34C759", ToggleValue.ON.colorHex)
        assertEquals("#8E8E93", ToggleValue.OFF.colorHex)
        assertEquals("#FF9500", ToggleValue.SILENT.colorHex)
        assertEquals("#FF9500", ToggleValue.VIBRATE.colorHex)
        assertEquals("#34C759", ToggleValue.RING.colorHex)
    }
}
