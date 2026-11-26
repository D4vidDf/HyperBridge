package com.d4viddf.hyperbridge.models

enum class ScreenRecordingLeftDesign {
    ICON_ONLY,
    ICON_AND_TEXT,
    TEXT_ONLY
}

enum class ScreenRecordingRightDesign {
    TIMER,
    NONE
}

data class ScreenRecordingDesignConfig(
    val left: ScreenRecordingLeftDesign = ScreenRecordingLeftDesign.ICON_AND_TEXT,
    val right: ScreenRecordingRightDesign = ScreenRecordingRightDesign.TIMER
)
