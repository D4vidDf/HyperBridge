package com.d4viddf.hyperbridge.receiver

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.PersistableBundle
import android.widget.Toast
import com.d4viddf.hyperbridge.R

/**
 * Handles Smart Action buttons that need no Activity: today that is only "copy code".
 * Opening links, dialling and tracking use direct Activity PendingIntents instead.
 */
class SmartActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_COPY -> copyToClipboard(context, intent)
        }
    }

    private fun copyToClipboard(context: Context, intent: Intent) {
        val text = intent.getStringExtra(EXTRA_TEXT)?.takeIf { it.isNotBlank() } ?: return
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        val clip = ClipData.newPlainText(CLIP_LABEL, text)
        if (intent.getBooleanExtra(EXTRA_SENSITIVE, false)) {
            // Keeps the code out of the system clipboard preview overlay.
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(R.string.smart_action_copied), Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ACTION_COPY = "com.d4viddf.hyperbridge.action.SMART_COPY"
        const val EXTRA_TEXT = "smart_text"
        const val EXTRA_SENSITIVE = "smart_sensitive"
        private const val CLIP_LABEL = "HyperBridge"
    }
}
