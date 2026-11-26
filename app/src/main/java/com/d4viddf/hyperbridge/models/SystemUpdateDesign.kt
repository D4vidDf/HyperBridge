package com.d4viddf.hyperbridge.models

enum class SystemUpdateLeftDesign {
    ICON_AND_TEXT,
    ICON_ONLY,
    TEXT_ONLY
}

enum class SystemUpdateRightDesign {
    PERCENTAGE,
    PROGRESS_CIRCLE,
    NONE
}

enum class SystemUpdateIconSource {
    NOTIFICATION_ICON,
    APP_PACKAGE_ICON,
    PROVIDED_SYSTEM_UPDATE,
    PROVIDED_DOWNLOAD
}

data class SystemUpdateDesignConfig(
    val left: SystemUpdateLeftDesign = SystemUpdateLeftDesign.ICON_AND_TEXT,
    val right: SystemUpdateRightDesign = SystemUpdateRightDesign.PERCENTAGE,
    val iconSource: SystemUpdateIconSource = SystemUpdateIconSource.NOTIFICATION_ICON
)
