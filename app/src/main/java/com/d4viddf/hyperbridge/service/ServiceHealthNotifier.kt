package com.d4viddf.hyperbridge.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.d4viddf.hyperbridge.MainActivity
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.receiver.ListenerReconnectReceiver

/**
 * The one notification HyperBridge posts about itself: the listener is down and could not be
 * reconnected. It opens the diagnostics screen and offers a one-tap reconnect (#330).
 */
object ServiceHealthNotifier {
    private const val TAG = "HyperBridgeDebug"

    /** Negative so it can never collide with hash-derived bridge ids or the widget id range. */
    const val LISTENER_DOWN_ID = -20301
    const val EXTRA_OPEN_DIAGNOSTICS = "open_diagnostics"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            BridgeNotificationChannels.SERVICE_HEALTH,
            context.getString(R.string.channel_service_health),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { setShowBadge(false) }
        manager.createNotificationChannel(channel)
    }

    fun showListenerDown(context: Context) {
        ensureChannel(context)
        val openDiagnostics = PendingIntent.getActivity(
            context,
            LISTENER_DOWN_ID,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_OPEN_DIAGNOSTICS, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val reconnect = PendingIntent.getBroadcast(
            context,
            LISTENER_DOWN_ID,
            Intent(context, ListenerReconnectReceiver::class.java).apply {
                action = ListenerReconnectReceiver.ACTION_RECONNECT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = context.getString(R.string.listener_down_text)
        val notification = NotificationCompat.Builder(context, BridgeNotificationChannels.SERVICE_HEALTH)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.listener_down_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setContentIntent(openDiagnostics)
            .addAction(0, context.getString(R.string.listener_down_action_reconnect), reconnect)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(LISTENER_DOWN_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot post service health notification: ${e.message}")
        }
    }

    fun cancel(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(LISTENER_DOWN_ID)
        } catch (_: Exception) {
        }
    }
}
