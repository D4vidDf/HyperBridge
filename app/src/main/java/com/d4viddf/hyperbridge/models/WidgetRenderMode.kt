package com.d4viddf.hyperbridge.models

enum class WidgetRenderMode(val label: String, val description: String) {
    INTERACTIVE("Interactive", "Buttons work. Best for Music, Controls."),
    SNAPSHOT("Snapshot", "Fixes empty lists. Best for Calendar, Gmail.")
}