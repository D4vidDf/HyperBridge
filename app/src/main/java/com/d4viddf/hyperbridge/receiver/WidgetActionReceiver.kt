package com.d4viddf.hyperbridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/** Handles a [com.d4viddf.hyperbridge.models.widget.ButtonAction.Dismiss] button inside a custom micro-widget. */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DISMISS) return
        val bridgeId = intent.getIntExtra(EXTRA_BRIDGE_ID, -1)
        if (bridgeId != -1) {
            NotificationManagerCompat.from(context).cancel(bridgeId)
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.d4viddf.hyperbridge.action.WIDGET_DISMISS"
        const val EXTRA_BRIDGE_ID = "bridge_id"
    }
}
