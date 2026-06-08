package com.d4viddf.hyperbridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import com.d4viddf.hyperbridge.service.InlineReplyService

class InlineReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Settings.canDrawOverlays(context)) {
            Toast.makeText(context, "Please grant 'Display over other apps' permission to use inline reply", Toast.LENGTH_LONG).show()
            
            // Optionally launch settings
            val settingsIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(settingsIntent)
            return
        }

        val serviceIntent = Intent(context, InlineReplyService::class.java).apply {
            putExtras(intent)
        }
        
        try {
            context.startService(serviceIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Failed to start reply service from background", Toast.LENGTH_SHORT).show()
        }
    }
}
