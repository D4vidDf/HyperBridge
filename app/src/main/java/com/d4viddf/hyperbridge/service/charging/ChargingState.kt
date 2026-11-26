package com.d4viddf.hyperbridge.service.charging

import com.d4viddf.hyperbridge.models.ChargingSpeedMode
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
    private const val PLUGGED_USB = 2
    private const val PLUGGED_WIRELESS = 4
    private const val PLUGGED_DOCK = 8

    private const val MAX_PLAUSIBLE_WATTS = 250.0
    private const val MIN_SHOWN_WATTS = 1.0
    private const val MAX_PLAUSIBLE_CHARGE_TIME_MS = 10 * 3_600_000L

    /** Returns null when the phone is not on a charger. */
    fun resolve(
        plugged: Int,
        status: Int,
        level: Int,
        scale: Int,
        voltageMillivolts: Int,
        currentNow: Int?,
        chargeTimeRemainingMillis: Long,
        powerMaxWatts: Int = 0,
        quickChargeType: Int = 0,
        maxChargingCurrentMicroamps: Int = 0,
        maxChargingVoltageMicrovolts: Int = 0,
        chargeCounterMicroampHours: Int = 0,
        designCapacityMah: Int = 0,
        speedMode: ChargingSpeedMode = ChargingSpeedMode.REAL_TIME
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
        val negotiatedMax = if (charging) {
            negotiatedMaxWatts(
                powerMaxWatts = powerMaxWatts,
                maxChargingCurrentMicroamps = maxChargingCurrentMicroamps,
                maxChargingVoltageMicrovolts = maxChargingVoltageMicrovolts
            )
        } else null
        val liveWatts = if (charging) {
            watts(
                voltageMillivolts = voltageMillivolts,
                currentNow = currentNow,
                negotiatedMaxWatts = negotiatedMax
            )
        } else null
        val resolvedWatts = if (charging) {
            when (speedMode) {
                ChargingSpeedMode.MAX_NEGOTIATED -> negotiatedMax ?: liveWatts
                ChargingSpeedMode.REAL_TIME -> liveWatts ?: negotiatedMax
            }
        } else null
        val remainingMillis = if (charging) {
            estimateChargeTimeRemainingMillis(
                plugged = plugged,
                percent = percent,
                voltageMillivolts = voltageMillivolts,
                currentNow = currentNow,
                systemChargeTimeRemainingMillis = chargeTimeRemainingMillis,
                powerMaxWatts = negotiatedMax ?: powerMaxWatts.coerceAtLeast(0),
                quickChargeType = quickChargeType,
                chargeCounterMicroampHours = chargeCounterMicroampHours,
                designCapacityMah = designCapacityMah
            )
        } else -1L
        return ChargingState(
            percent = percent,
            isFull = isFull,
            isCharging = charging,
            source = source,
            watts = resolvedWatts,
            minutesToFull = remainingMillis
                .takeIf { charging && it > 0 }
                ?.let { ((it + 59_999) / 60_000).toInt().coerceAtLeast(1) }
        )
    }

    /**
     * Resolves the charger's negotiated maximum power in watts.
     * Prioritizes Xiaomi's sticky broadcast extra `miui.intent.extra.POWER_MAX` and falls back to
     * Android's standard `max_charging_current` / `max_charging_voltage` extras.
     */
    fun negotiatedMaxWatts(
        powerMaxWatts: Int,
        maxChargingCurrentMicroamps: Int = 0,
        maxChargingVoltageMicrovolts: Int = 0
    ): Int? {
        if (powerMaxWatts > 0 && powerMaxWatts <= MAX_PLAUSIBLE_WATTS) {
            return powerMaxWatts
        }
        if (maxChargingCurrentMicroamps <= 0) return null
        val amps = if (maxChargingCurrentMicroamps >= 50_000) {
            maxChargingCurrentMicroamps / 1_000_000.0
        } else {
            maxChargingCurrentMicroamps / 1_000.0
        }
        val volts = when {
            maxChargingVoltageMicrovolts >= 500_000 -> maxChargingVoltageMicrovolts / 1_000_000.0
            maxChargingVoltageMicrovolts >= 1_000 -> maxChargingVoltageMicrovolts / 1_000.0
            else -> 5.0
        }
        val watts = amps * volts
        return watts.takeIf { it in MIN_SHOWN_WATTS..MAX_PLAUSIBLE_WATTS }?.roundToInt()
    }

    /**
     * BATTERY_PROPERTY_CURRENT_NOW is documented in microamps, but vendors disagree on both the
     * unit (some kernels report milliamps) and the sign while charging. Take the magnitude, try
     * microamps first, fall back to milliamps, compensate for dual-cell packs when Xiaomi's
     * negotiated max power indicates a high-wattage dual-cell charger, and drop anything that is
     * not a believable charger.
     */
    fun watts(
        voltageMillivolts: Int,
        currentNow: Int?,
        negotiatedMaxWatts: Int? = null
    ): Int? {
        val volts = normalizedVolts(voltageMillivolts) ?: return null
        val amps = currentAmps(volts, currentNow) ?: return null
        var watts = volts * amps
        if (
            negotiatedMaxWatts != null &&
            negotiatedMaxWatts >= 50 &&
            volts in 3.0..4.65 &&
            watts >= 4.0 &&
            watts < negotiatedMaxWatts * 0.58 &&
            watts * 2.0 <= negotiatedMaxWatts * 1.15
        ) {
            // Dual-cell Xiaomi battery reporting single-cell voltage (~3.8-4.5V) and per-cell current.
            watts *= 2.0
        }
        if (negotiatedMaxWatts != null && watts > negotiatedMaxWatts * 1.15) {
            watts = negotiatedMaxWatts.toDouble()
        }
        return watts.takeIf { it in MIN_SHOWN_WATTS..MAX_PLAUSIBLE_WATTS }?.roundToInt()
    }

    /**
     * Estimates remaining charge duration in milliseconds.
     *
     * On Xiaomi HyperOS, `BatteryManager.computeChargeTimeRemaining()` often returns stale
     * multi-hour historical averages from `batterystats`, whereas Xiaomi's Security app
     * (`com.miui.powercenter.batteryhistory.b` / `BatteryChargeTimeHelper`) computes remaining
     * time using a two-phase CC/CV rate curve based on `POWER_MAX`, `quick_charge_type`, plug
     * type, and battery capacity.
     */
    internal fun estimateChargeTimeRemainingMillis(
        plugged: Int,
        percent: Int,
        voltageMillivolts: Int,
        currentNow: Int?,
        systemChargeTimeRemainingMillis: Long,
        powerMaxWatts: Int,
        quickChargeType: Int,
        chargeCounterMicroampHours: Int,
        designCapacityMah: Int
    ): Long {
        val remainingPercent = (100 - percent).coerceIn(0, 100)
        if (remainingPercent == 0) return -1L

        val hasXiaomiOrCapacityContext =
            powerMaxWatts > 0 || quickChargeType > 0 || designCapacityMah > 0 || chargeCounterMicroampHours > 0

        if (!hasXiaomiOrCapacityContext) {
            return systemChargeTimeRemainingMillis.takeIf { it in 1..MAX_PLAUSIBLE_CHARGE_TIME_MS } ?: -1L
        }

        val capacityMah = resolveCapacityMah(
            designCapacityMah = designCapacityMah,
            chargeCounterMicroampHours = chargeCounterMicroampHours,
            percent = percent
        )

        val profileRemainingMs = xiaomiProfileRemainingMillis(
            plugged = plugged,
            percent = percent,
            capacityMah = capacityMah,
            powerMaxWatts = powerMaxWatts,
            quickChargeType = quickChargeType
        )

        val volts = normalizedVolts(voltageMillivolts) ?: 4.0
        val currentMa = currentAmps(volts, currentNow)?.times(1000.0)
        val currentBasedRemainingMs = if (currentMa != null && currentMa >= 250.0) {
            val remainingMah = capacityMah * (remainingPercent / 100.0)
            val taperFactor = if (percent > 90) 1.35 else 1.20
            ((remainingMah / currentMa) * taperFactor * 3_600_000.0).toLong()
        } else -1L

        val hasFastChargeContext = powerMaxWatts > 0 || quickChargeType > 0
        if (hasFastChargeContext) {
            // Replicate Xiaomi SecurityCenter's BatteryChargeTimeHelper primary curve,
            // with light live-current adaptation when the measured current is in family.
            return if (
                currentBasedRemainingMs > 0 &&
                currentBasedRemainingMs in (profileRemainingMs / 2)..(profileRemainingMs * 2)
            ) {
                (profileRemainingMs * 0.75 + currentBasedRemainingMs * 0.25).toLong()
            } else {
                profileRemainingMs
            }
        }

        // Standard/normal charging with capacity context: reject stale battery stats outliers.
        val validSystemMs = systemChargeTimeRemainingMillis.takeIf { it in 1..MAX_PLAUSIBLE_CHARGE_TIME_MS }
        return when {
            validSystemMs != null && currentBasedRemainingMs > 0 &&
                validSystemMs in (currentBasedRemainingMs / 2)..(currentBasedRemainingMs * 2) ->
                ((validSystemMs + currentBasedRemainingMs) / 2)
            currentBasedRemainingMs in 60_000L..MAX_PLAUSIBLE_CHARGE_TIME_MS ->
                currentBasedRemainingMs
            validSystemMs != null && validSystemMs <= profileRemainingMs * 2 ->
                validSystemMs
            else -> profileRemainingMs
        }
    }

    private fun resolveCapacityMah(
        designCapacityMah: Int,
        chargeCounterMicroampHours: Int,
        percent: Int
    ): Int {
        if (designCapacityMah in 2_000..15_000) return designCapacityMah
        if (chargeCounterMicroampHours > 0 && percent >= 15) {
            val derived = ((chargeCounterMicroampHours / 1000.0) / (percent / 100.0)).roundToInt()
            if (derived in 2_000..15_000) return derived
        }
        return 5_000
    }

    /**
     * Replicates `com.miui.powercenter.batteryhistory.b.f`, `b.h`, and `b.i` from Xiaomi's
     * SecurityCenter (`BatteryChargeTimeHelper`):
     * - `fastRateMa` (`b.i`): Constant-Current (CC) rate in mA
     * - `cvRateMa` (`b.h`): Constant-Voltage (CV) taper rate in mA
     * - Above 90% (`percent > 90`), only `maxChargeTime` (`cvRateMa`) is used; at or below 90%,
     *   the average of `minChargeTime` and `maxChargeTime` is scaled by `(100 - percent) / 100f`.
     */
    private fun xiaomiProfileRemainingMillis(
        plugged: Int,
        percent: Int,
        capacityMah: Int,
        powerMaxWatts: Int,
        quickChargeType: Int
    ): Long {
        val (fastRateMa, cvRateMa) = when {
            plugged and PLUGGED_WIRELESS != 0 -> when {
                powerMaxWatts >= 50 -> 11_700 to 4_500
                powerMaxWatts >= 30 -> 5_400 to 3_600
                quickChargeType >= 1 || powerMaxWatts >= 15 -> 2_700 to 1_800
                else -> 900 to 450
            }
            plugged == PLUGGED_USB && quickChargeType == 0 && powerMaxWatts <= 10 -> 1_300 to 450
            else -> when {
                powerMaxWatts in 140..250 -> 36_000 to 7_200
                powerMaxWatts in 89..139 || quickChargeType >= 4 -> 14_400 to 7_200
                quickChargeType == 3 || powerMaxWatts in 50..88 -> 9_000 to 5_400
                quickChargeType == 2 || powerMaxWatts in 28..49 -> 4_500 to 2_700
                quickChargeType == 1 || powerMaxWatts in 15..27 -> 3_600 to 1_800
                else -> 1_800 to 900
            }
        }
        val minFullTimeMs = capacityMah.toLong() * 3_600_000L / fastRateMa
        val maxFullTimeMs = capacityMah.toLong() * 3_600_000L / cvRateMa
        val fullChargeDurationMs = if (percent > 90) {
            maxFullTimeMs
        } else {
            (minFullTimeMs / 2) + (maxFullTimeMs / 2)
        }
        val remainingFraction = (100 - percent).coerceIn(0, 100) / 100.0
        return (fullChargeDurationMs * remainingFraction).toLong()
    }

    private fun normalizedVolts(voltageMillivolts: Int): Double? {
        if (voltageMillivolts <= 0) return null
        val mv = if (voltageMillivolts in 1..20) voltageMillivolts * 1000 else voltageMillivolts
        return mv / 1000.0
    }

    private fun currentAmps(volts: Double, currentNow: Int?): Double? {
        if (currentNow == null || currentNow == 0 || currentNow == Int.MIN_VALUE || currentNow == Int.MAX_VALUE) {
            return null
        }
        val raw = abs(currentNow.toLong()).toDouble()
        val candidates = listOf(raw / 1_000_000.0, raw / 1000.0)
        return candidates.firstOrNull { amps ->
            val w = volts * amps
            w in MIN_SHOWN_WATTS..MAX_PLAUSIBLE_WATTS
        }
    }
}

