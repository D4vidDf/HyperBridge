package com.d4viddf.hyperbridge.service.widget

import android.app.PendingIntent

/**
 * The tappable things a notification brings with it, handed to [CustomWidgetRenderer] so a design
 * can fire the app's own buttons instead of only its own (#328).
 *
 * Kept out of [com.d4viddf.hyperbridge.data.widget.VariableContext] on purpose: that one stays a
 * plain data holder the variable engine and the condition evaluator can be unit tested against.
 */
data class WidgetActionIntents(
    val notificationActions: List<PendingIntent?> = emptyList(),
    val inlineReply: PendingIntent? = null,
    /** Keyed by Smart Action type name: OTP, URL, PHONE, TRACKING. */
    val smartActions: Map<String, PendingIntent> = emptyMap()
)
