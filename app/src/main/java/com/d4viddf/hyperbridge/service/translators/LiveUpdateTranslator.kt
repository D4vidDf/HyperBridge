package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.IslandConfig

class LiveUpdateTranslator(
    context: Context,
    repo: ThemeRepository
) : BaseTranslator(context, repo) {

    fun translateToLiveUpdate(
        sbn: StatusBarNotification,
        config: IslandConfig,
        channelId: String
    ): NotificationCompat.Builder {
        val original = sbn.notification
        val extras = original.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val progress = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)

        // [CRITICAL FIX] Android 16 STRICTLY filters Live Updates by category.
        // It actively blocks "CATEGORY_SERVICE". We must force it to PROGRESS or TRANSPORT.
        val validCategory = if (original.category.isNullOrEmpty() || original.category == NotificationCompat.CATEGORY_SERVICE) {
            if (progressMax > 0 || indeterminate) {
                NotificationCompat.CATEGORY_PROGRESS
            } else {
                NotificationCompat.CATEGORY_TRANSPORT
            }
        } else {
            original.category
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(original.smallIcon?.let { IconCompat.createFromIcon(context, it) } ?: IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground))
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(validCategory) // Must use the strictly valid category!
            .setContentIntent(original.contentIntent)

        // [CRITICAL FIX] Carry over the original timestamp so the OS can calculate time active!
        if (original.`when` > 0) {
            builder.setWhen(original.`when`)
            builder.setShowWhen(true)
        }

        // 1. Copy Original Actions Using IconCompat
        original.actions?.forEach { action ->
            val iconCompat = if (action.getIcon() != null) {
                IconCompat.createFromIcon(context, action.getIcon()!!)
            } else {
                IconCompat.createWithResource(context, action.icon)
            }

            val compatAction = NotificationCompat.Action.Builder(
                iconCompat,
                action.title,
                action.actionIntent
            ).build()
            builder.addAction(compatAction)
        }

        // 2. Apply Progress if it exists
        if (progressMax > 0 || indeterminate) {
            builder.setProgress(progressMax, progress, indeterminate)
        }

        // 3. ANDROID 16 LIVE UPDATE PROMOTION
        val shortAlertText = generateCriticalShortText(title, text, progress, progressMax)

        // [CRITICAL FIX] Use bundle extras to force the new Android 16 behavior natively!
        builder.extras.putBoolean("android.requestPromotedOngoing", true)
        builder.extras.putString("android.shortCriticalText", shortAlertText)

        return builder
    }

    /**
     * The Status Chip / Right-side Island text only supports ~7 characters!
     * We dynamically extract the most critical piece of info.
     */
    private fun generateCriticalShortText(title: String, text: String, progress: Int, max: Int): String {
        // 1. If it's a progress notification, return the percentage (e.g. "45%")
        if (max > 0) {
            val percent = (progress * 100) / max
            return "$percent%"
        }

        // 2. Try to extract an ETA or time (e.g. "5 min", "12m")
        val timeRegex = Regex("(\\d+\\s*(min|m))", RegexOption.IGNORE_CASE)
        timeRegex.find(text)?.let { return it.groupValues[1] }
        timeRegex.find(title)?.let { return it.groupValues[1] }

        // 3. Fallback: Take the first word of the title, max 7 chars
        val firstWord = title.split(" ").firstOrNull() ?: "Active"
        return if (firstWord.length > 7) firstWord.substring(0, 6) + "…" else firstWord
    }
}