package com.d4viddf.hyperbridge.util

import android.app.ActivityOptions
import android.app.PendingIntent
import android.os.Build

/**
 * Sends another app's PendingIntent with our background-activity-launch privilege lent to it.
 *
 * Whenever *we* are the sender (a tap that reaches us as a broadcast, a VPN disconnect button),
 * an activity PendingIntent is silently dropped on API 34+ unless the sender opts in, and since
 * API 35 creators deny it by default (#359). Harmless for broadcast/service PendingIntents.
 *
 * @throws PendingIntent.CanceledException like [PendingIntent.send].
 */
fun PendingIntent.sendAllowingBackgroundLaunch() {
    val mode = if (Build.VERSION.SDK_INT >= 36) {
        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
    } else {
        @Suppress("DEPRECATION")
        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
    }
    val options = ActivityOptions.makeBasic()
        .setPendingIntentBackgroundActivityStartMode(mode)
    send(options.toBundle())
}
