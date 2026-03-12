package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
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

        var progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        var progress = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        var indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)

        // Identify if it's media so we can use the title for the Island chip
        val isMedia = extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true

        // [FIXED] Media apps often leave random progress flags in the background.
        // Force them off so it doesn't show an indeterminate loading bar!
        if (isMedia) {
            progressMax = 0
            progress = 0
            indeterminate = false
        }
        // Force category for Android 16 promotion limits
        val validCategory = if (original.category.isNullOrEmpty() || original.category == NotificationCompat.CATEGORY_SERVICE) {
            if (progressMax > 0 || indeterminate) NotificationCompat.CATEGORY_PROGRESS else NotificationCompat.CATEGORY_TRANSPORT
        } else {
            original.category
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(original.smallIcon?.let { IconCompat.createFromIcon(context, it) } ?: IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground))
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(validCategory)
            .setContentIntent(original.contentIntent)

        // --- ADD PICTURE / LARGE ICON ---
        if (android.os.Build.VERSION.SDK_INT >= 23 && original.getLargeIcon() != null) {
            builder.setLargeIcon(original.getLargeIcon())
        } else {
            @Suppress("DEPRECATION")
            val picture = extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE)
            if (picture != null) builder.setLargeIcon(picture)
        }

        // Carry over the original timestamp (Standard behavior)
        if (original.`when` > 0) {
            builder.setWhen(original.`when`)
            builder.setShowWhen(true)
        }

        // --- COPY BUTTONS ---
        original.actions?.forEach { action ->
            val iconCompat = if ( action.getIcon() != null) {
                IconCompat.createFromIcon(context, action.getIcon()!!)
            } else {
                IconCompat.createWithResource(context, action.icon)
            }
            builder.addAction(NotificationCompat.Action.Builder(iconCompat, action.title, action.actionIntent).build())
        }

        // --- APPLY STYLES ---
        if (progressMax > 0 || indeterminate) {
            builder.setProgress(progressMax, progress, indeterminate)
        } else {
            // Standard notification: Allow expanding text
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(text).setBigContentTitle(title))
        }

        // --- ANDROID 16 LIVE UPDATE INJECTION ---
        val shortAlertText = generateCriticalShortText(title, text, progress, progressMax, isMedia)
        builder.setRequestPromotedOngoing(true)
        builder.setShortCriticalText(shortAlertText)


        return builder
    }

    private fun generateCriticalShortText(title: String, text: String, progress: Int, max: Int, isMedia: Boolean): String {
        if (isMedia) return title.ifBlank { "Media" }

        if (max > 0) return "${(progress * 100) / max}%"

        val timeRegex = Regex("(\\d+\\s*(min|m))", RegexOption.IGNORE_CASE)
        timeRegex.find(text)?.let { return it.groupValues[1] }
        timeRegex.find(title)?.let { return it.groupValues[1] }

        return title.ifBlank { text }.ifBlank { "Active" }
    }
}