package com.d4viddf.hyperbridge.models

data class WidgetConfig(
    // Appearance
    val size: WidgetSize = WidgetSize.MEDIUM,
    val renderMode: WidgetRenderMode = WidgetRenderMode.INTERACTIVE,

    // Behavior
    val isShowShade: Boolean = true,
    val timeout: Long? = 5L,

    // Automation
    val autoUpdate: Boolean = false,
    val updateIntervalMinutes: Int = 15
)