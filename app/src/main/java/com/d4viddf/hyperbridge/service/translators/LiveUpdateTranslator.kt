package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.toColorInt
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.NavContent
import com.d4viddf.hyperbridge.models.NotificationType

class LiveUpdateTranslator(
    context: Context,
    repo: ThemeRepository
) : BaseTranslator(context, repo) {

    fun translateToLiveUpdate(
        sbn: StatusBarNotification,
        channelId: String,
        type: NotificationType,
        navRight: NavContent? = null
    ): NotificationCompat.Builder {
        val original = sbn.notification
        val extras = original.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val progress = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(original.category)

        original.contentIntent?.let { builder.setContentIntent(it) }

        // --- THEME COLOR & ICON INJECTION ---
        val theme = repository?.activeTheme?.value

        if (type == NotificationType.NAVIGATION) {
            // 1. Inject Theme Nav Color
            val themeColorStr = theme?.defaultNavigation?.progressBarColor
                ?: resolveColor(theme, sbn.packageName, "#34C759") // Green fallback

            val themeColorInt = try {
                themeColorStr.toColorInt()
            } catch (_: Exception) {
                original.color
            }
            builder.setColor(themeColorInt)

            // 2. Inject Theme Nav Icon
            val navStartBitmap = getThemeBitmap(theme, "nav_start")
            if (navStartBitmap != null) {
                builder.setSmallIcon(IconCompat.createWithBitmap(navStartBitmap))
            } else {
                builder.setSmallIcon(original.smallIcon?.let { IconCompat.createFromIcon(context, it) } ?: IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground))
            }
        } else {
            // Standard fallback for non-navigation
            builder.setColor(original.color)
            builder.setSmallIcon(original.smallIcon?.let { IconCompat.createFromIcon(context, it) } ?: IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground))
        }

        // --- ACTIONS ---
        val rawActions = original.actions ?: emptyArray()
        rawActions.forEach { action ->
            val iconCompat = if (action.getIcon() != null) {
                IconCompat.createFromIcon(context, action.getIcon()!!)
            } else {
                IconCompat.createWithResource(context, action.icon)
            }
            builder.addAction(NotificationCompat.Action.Builder(iconCompat, action.title, action.actionIntent).build())
        }

        // --- APPLY STYLES ---
        // BigTextStyle ensures text isn't completely hidden by the progress bar
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(text).setBigContentTitle(title))

        // Add back the progress bar to show the user where they are
        if (progressMax > 0 || indeterminate) {
            builder.setProgress(progressMax, progress, indeterminate)
        }

        // --- ANDROID 16 LIVE UPDATE INJECTION ---
        val shortAlertText = generateCriticalShortText(title, text, progress, progressMax, type, navRight, sbn)

        builder.setRequestPromotedOngoing(true)
        builder.setShortCriticalText(shortAlertText)

        return builder
    }

    private fun generateCriticalShortText(
        title: String,
        text: String,
        progress: Int,
        max: Int,
        type: NotificationType,
        navRight: NavContent?,
        sbn: StatusBarNotification
    ): String {

        if (type == NotificationType.MEDIA) return title.ifBlank { "Media" }

        // Advanced Extraction for Navigation Layouts
        if (type == NotificationType.NAVIGATION) {
            val extras = sbn.notification.extras
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.replace("\n", " ")?.trim() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.replace("\n", " ")?.trim() ?: ""

            val timeRegex = Regex("(\\d{1,2}:\\d{2})|(\\d+h\\s*\\d+m)", RegexOption.IGNORE_CASE)
            val distanceRegex = Regex("^\\d+([,.]\\d+)?\\s*(m|km|ft|mi|yd|yards|miles|meters)", RegexOption.IGNORE_CASE)

            var distance = ""
            var eta = ""

            // Extract ETA
            if (timeRegex.containsMatchIn(subText)) eta = subText
            else if (timeRegex.containsMatchIn(text) && !distanceRegex.containsMatchIn(text)) eta = text

            // Extract Distance
            val candidates = listOf(bigText, title, text).filter { it.isNotEmpty() }
            val contentSource = candidates.firstOrNull { str -> distanceRegex.containsMatchIn(str) } ?: title.ifEmpty { text }

            if (distanceRegex.containsMatchIn(contentSource)) {
                distanceRegex.find(contentSource)?.let { distance = it.value }
            }

            // Return the value based on the user's customized Right Side layout!
            return when (navRight) {
                NavContent.ETA -> eta.ifEmpty { distance }
                NavContent.DISTANCE -> distance.ifEmpty { eta }
                NavContent.DISTANCE_ETA -> listOf(distance, eta).filter { it.isNotEmpty() }.joinToString(" • ")
                NavContent.INSTRUCTION -> title
                else -> eta.ifEmpty { distance }.ifEmpty { title } // Fallback
            }
        }

        // Standard Progress Fallback (Only applied if NOT Navigation)
        if (max > 0) return "${(progress * 100) / max}%"

        // Timer Fallback
        val timeRegex = Regex("(\\d+\\s*(min|m))", RegexOption.IGNORE_CASE)
        timeRegex.find(text)?.let { return it.groupValues[1] }

        return title
    }
}