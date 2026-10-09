package com.d4viddf.hyperbridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/** Handles a [com.d4viddf.hyperbridge.models.widget.ButtonAction.Dismiss] button inside a custom micro-widget. */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_NOOP) return
        if (intent.action == ACTION_DISMISS) {
            val bridgeId = intent.getIntExtra(EXTRA_BRIDGE_ID, -1)
            if (bridgeId != -1) {
                NotificationManagerCompat.from(context).cancel(bridgeId)
            }
            return
        }
        if (intent.action == ACTION_MEDIA_CONTROL) {
            val command = intent.getStringExtra(EXTRA_MEDIA_COMMAND) ?: "play_pause"
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            val keyCode = when (command.lowercase()) {
                "play" -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY
                "pause" -> android.view.KeyEvent.KEYCODE_MEDIA_PAUSE
                "next", "skip_next" -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
                "previous", "skip_previous", "prev" -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
                "stop" -> android.view.KeyEvent.KEYCODE_MEDIA_STOP
                else -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            }
            audioManager?.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode))
            audioManager?.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode))
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.d4viddf.hyperbridge.action.WIDGET_DISMISS"
        const val ACTION_MEDIA_CONTROL = "com.d4viddf.hyperbridge.action.MEDIA_CONTROL"
        const val ACTION_NOOP = "com.d4viddf.hyperbridge.action.WIDGET_NOOP"
        const val EXTRA_BRIDGE_ID = "bridge_id"
        const val EXTRA_MEDIA_COMMAND = "media_command"
    }
}
