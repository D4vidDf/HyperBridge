package com.d4viddf.hyperbridge.service.charging

import kotlin.math.abs
import kotlin.math.roundToInt

enum class ChargingSource { WIRED, WIRELESS, DOCK }

/** What the charging island shows; two equal states never need a re-render. */
data class ChargingState(
    val percent: Int,
    val isFull: Boolean,
    /** False when plugged in but held (e.g. a charge limit or a weak USB port). */
    val isCharging: Boolean,
    val source: ChargingSource,
    val watts: Int?,
    val minutesToFull: Int?
)

object ChargingStateResolver {
    // android.os.BatteryManager constants, inlined so the resolver stays a plain-JVM unit.
    private const val STATUS_CHARGING = 2
    private const val STATUS_FULL = 5
    private const val PLUGGED_WIRELESS = 4
    private const val PLUGGED_DOCK = 8

    private const val MAX_PLAUSIBLE_WATTS = 250.0
    private const val MIN_SHOWN_WATTS = 1.0

    /** Returns null when the phone is not on a charger. */
    fun resolve(
        plugged: Int,
        status: Int,
        level: Int,
        scale: Int,
        voltageMillivolts: Int,
        currentNow: Int?,
        chargeTimeRemainingMillis: Long
    ): ChargingState? {
        if (plugged == 0) return null
        val percent = if (scale > 0) (level * 100f / scale).roundToInt().coerceIn(0, 100) else level.coerceIn(0, 100)
        val isFull = status == STATUS_FULL || percent >= 100
        val source = when {
            plugged and PLUGGED_WIRELESS != 0 -> ChargingSource.WIRELESS
            plugged and PLUGGED_DOCK != 0 -> ChargingSource.DOCK
            else -> ChargingSource.WIRED
        }
        val charging = status == STATUS_CHARGING && !isFull
        return ChargingState(
            percent = percent,
            isFull = isFull,
            isCharging = charging,
            source = source,
            watts = if (charging) watts(voltageMillivolts, currentNow) else null,
            minutesToFull = chargeTimeRemainingMillis
                .takeIf { charging && it > 0 }
                ?.let { ((it + 59_999) / 60_000).toInt() }
        )
    }

    /**
     * BATTERY_PROPERTY_CURRENT_NOW is documented in microamps, but vendors disagree on both the
     * unit (some kernels report milliamps) and the sign while charging. Take the magnitude, try
     * microamps first, fall back to milliamps, and drop anything that is not a believable charger.
     */
    fun watts(voltageMillivolts: Int, currentNow: Int?): Int? {
        if (currentNow == null || currentNow == 0 || currentNow == Int.MIN_VALUE || voltageMillivolts <= 0) return null
        val volts = voltageMillivolts / 1000.0
        val amps = abs(currentNow.toLong()).toDouble()
        val candidates = listOf(volts * amps / 1_000_000.0, volts * amps / 1000.0)
        val watts = candidates.firstOrNull { it in MIN_SHOWN_WATTS..MAX_PLAUSIBLE_WATTS } ?: return null
        return watts.roundToInt()
    }
}
