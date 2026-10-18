package com.d4viddf.hyperbridge.models

enum class ChargingLeftDesign {
    CHARGING_SPEED,
    REMAINING_TIME,
    BATTERY_PERCENTAGE,
    ICON_ONLY
}

enum class ChargingRightDesign {
    BATTERY_PERCENTAGE,
    PROGRESS_CIRCLE,
    CHARGING_SPEED,
    REMAINING_TIME,
    NONE
}

data class ChargingDesignConfig(
    val left: ChargingLeftDesign = ChargingLeftDesign.CHARGING_SPEED,
    val right: ChargingRightDesign = ChargingRightDesign.BATTERY_PERCENTAGE,
    val showExpandedProgressBar: Boolean = true
)
