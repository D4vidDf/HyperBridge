package com.d4viddf.hyperbridge.models

enum class PrivacyIndicatorStyle {
    ICON,
    DOT
}

data class PrivacyIndicatorDesignConfig(
    val style: PrivacyIndicatorStyle = PrivacyIndicatorStyle.ICON,
    val showText: Boolean = true
)
