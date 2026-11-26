package com.d4viddf.hyperbridge.service.visual

import android.graphics.Bitmap

enum class NotificationVisualSource {
    PERSON,
    PICTURE,
    LARGE_ICON,
    SMALL_ICON,
    APP_ICON,
    FALLBACK;

    val shouldShowAppBadge: Boolean
        get() = this == PERSON || this == PICTURE || this == LARGE_ICON
}

data class ResolvedNotificationVisual(
    val bitmap: Bitmap,
    val source: NotificationVisualSource
) {
    val shouldShowAppBadge: Boolean
        get() = source.shouldShowAppBadge
}
