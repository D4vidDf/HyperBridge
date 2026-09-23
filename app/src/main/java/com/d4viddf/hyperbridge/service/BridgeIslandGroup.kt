package com.d4viddf.hyperbridge.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.service.BridgeIslandGroupPolicy.OwnNotification

/**
 * Keeps every bridged island inside one app-provided notification group so Android 16+ never
 * force-groups (and silences) them. See [BridgeIslandGroupPolicy] for the why, and for why this
 * is a no-op below Android 16 (#358).
 *
 * Usage: [asChild] on the builder, [ensureSummaryFor] right before `notify`, and
 * [scheduleRelease] whenever one of our notifications is removed.
 */
object BridgeIslandGroup {
    private const val TAG = "HyperBridgeDebug"
    const val GROUP_KEY = BridgeIslandGroupPolicy.GROUP_KEY
    const val SUMMARY_ID = BridgeIslandGroupPolicy.SUMMARY_ID

    private val handler = Handler(Looper.getMainLooper())
    private val releaseToken = Any()

    private val enabled: Boolean get() = BridgeIslandGroupPolicy.appliesTo(Build.VERSION.SDK_INT)

    /**
     * Marks a bridged notification as a child. Children alert; the summary never does.
     * Below Android 16 the builder is returned untouched: no group, no summary.
     */
    fun asChild(builder: NotificationCompat.Builder): NotificationCompat.Builder =
        if (!enabled) builder
        else builder.setGroup(GROUP_KEY).setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)

    /** Posts the summary if [notification] is a child and no summary is active. */
    fun ensureSummaryFor(context: Context, notification: Notification) {
        if (!enabled || notification.group != GROUP_KEY) return
        val active = ownNotifications(context) ?: return
        if (!BridgeIslandGroupPolicy.needsSummary(active)) return
        val summary = NotificationCompat.Builder(context, BridgeNotificationChannels.ACTIVE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.channel_active_islands))
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(SUMMARY_ID, summary)
            Log.d(TAG, "Island group summary posted")
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot post island group summary: ${e.message}")
        }
    }

    /**
     * Cancels the summary once no child is left. Debounced: a Shizuku-style cancel+repost of the
     * only child must not tear the summary down in between.
     */
    fun scheduleRelease(context: Context) {
        val app = context.applicationContext
        handler.removeCallbacksAndMessages(releaseToken)
        handler.postDelayed({ releaseIfEmpty(app) }, releaseToken, BridgeIslandGroupPolicy.RELEASE_DELAY_MS)
    }

    private fun releaseIfEmpty(context: Context) {
        val active = ownNotifications(context) ?: return
        if (!BridgeIslandGroupPolicy.shouldReleaseSummary(active)) return
        try {
            NotificationManagerCompat.from(context).cancel(SUMMARY_ID)
            Log.d(TAG, "Island group summary released")
        } catch (_: Exception) {
        }
    }

    /** Our own active notifications (an app may always list those). */
    private fun ownNotifications(context: Context): List<OwnNotification>? = try {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.activeNotifications?.map { OwnNotification(it.id, it.notification.group) }
    } catch (e: Exception) {
        Log.w(TAG, "Cannot read own notifications: ${e.message}")
        null
    }
}
