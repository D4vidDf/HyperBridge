package com.d4viddf.hyperbridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.d4viddf.hyperbridge.service.ListenerWatchdog

/** "Reconnect" action of the listener-down notification. Not exported. */
class ListenerReconnectReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_RECONNECT = "com.d4viddf.hyperbridge.ACTION_LISTENER_RECONNECT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RECONNECT) return
        ListenerWatchdog.reconnectNow(context, "notification")
    }
}
