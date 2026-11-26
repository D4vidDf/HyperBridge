package com.d4viddf.hyperbridge.service.smartactions

import android.app.Notification
import androidx.core.app.NotificationCompat

/**
 * Gathers every piece of user-visible text a notification carries into one string for
 * [SmartActionsExtractor]. Only called once Smart Actions are known to be enabled for the app.
 */
object SmartActionNotificationText {

    fun collect(notification: Notification): String {
        val extras = notification.extras ?: return ""
        val parts = LinkedHashSet<String>()
        fun add(value: CharSequence?) {
            value?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { parts.add(it) }
        }

        add(extras.getCharSequence(Notification.EXTRA_TITLE))
        add(extras.getCharSequence(Notification.EXTRA_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
        add(extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { add(it) }

        // Chats: the newest message is the one the user is acting on.
        runCatching {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
                ?.messages?.lastOrNull()?.text
        }.getOrNull()?.let { add(it) }

        return parts.joinToString("\n")
    }
}
