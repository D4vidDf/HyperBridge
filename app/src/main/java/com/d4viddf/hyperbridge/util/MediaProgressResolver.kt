package com.d4viddf.hyperbridge.util

import android.app.Notification
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import java.util.Locale

data class MediaProgressResult(
    val progressPercent: Int?,
    val currentFormatted: String? = null,
    val durationFormatted: String? = null,
    val currentMs: Long? = null,
    val durationMs: Long? = null
)

object MediaProgressResolver {

    private val TIME_SLASH_TIME_REGEX = Regex(
        """(\d{1,2}:\d{2}(?::\d{2})?)\s*(?:/|of|\|)\s*(\d{1,2}:\d{2}(?::\d{2})?)""",
        RegexOption.IGNORE_CASE
    )

    fun resolveMediaProgress(
        sbn: StatusBarNotification,
        context: Context? = null
    ): MediaProgressResult {
        val extras = sbn.notification.extras ?: return MediaProgressResult(null)

        // 1. Try resolving via MediaSession.Token & MediaController if context is provided
        if (context != null) {
            val sessionResult = extractFromMediaSession(extras, context)
            if (sessionResult?.progressPercent != null) {
                return sessionResult
            }
        }

        // 2. Try notification extras with explicit progress/max or custom duration keys
        val extrasProgress = extractFromExtras(extras)
        if (extrasProgress?.progressPercent != null) {
            return extrasProgress
        }

        // 3. Try parsing text timestamps like "01:23 / 03:45" in text, subText, infoText, or title
        val textCandidates = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        )
        for (candidate in textCandidates) {
            val textResult = extractProgressFromText(candidate)
            if (textResult?.progressPercent != null) {
                return textResult
            }
        }

        return MediaProgressResult(null)
    }

    fun extractFromMediaSession(extras: Bundle, context: Context): MediaProgressResult? {
        return runCatching {
            val token: MediaSession.Token? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION) as? MediaSession.Token
            }
            if (token == null) return null

            val controller = MediaController(context, token)
            val metadata = controller.metadata
            val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

            val playbackState = controller.playbackState
            val rawPosition = playbackState?.position ?: 0L

            val position = if (playbackState?.state == PlaybackState.STATE_PLAYING && playbackState.lastPositionUpdateTime > 0) {
                val speed = playbackState.playbackSpeed.takeIf { it > 0f } ?: 1f
                val elapsed = SystemClock.elapsedRealtime() - playbackState.lastPositionUpdateTime
                (rawPosition + (elapsed * speed).toLong()).coerceAtLeast(0L)
            } else {
                rawPosition
            }

            if (duration > 0L) {
                val clampedPos = position.coerceIn(0L, duration)
                val percent = ((clampedPos.toDouble() / duration) * 100).toInt().coerceIn(0, 100)
                MediaProgressResult(
                    progressPercent = percent,
                    currentFormatted = formatMillisToTime(clampedPos),
                    durationFormatted = formatMillisToTime(duration),
                    currentMs = clampedPos,
                    durationMs = duration
                )
            } else null
        }.getOrNull()
    }

    fun extractFromExtras(extras: Bundle): MediaProgressResult? {
        val current = extras.getInt(Notification.EXTRA_PROGRESS, -1).takeIf { it >= 0 }
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, -1).takeIf { it > 0 }
        if (current != null && max != null) {
            val percent = ((current.toDouble() / max) * 100).toInt().coerceIn(0, 100)
            return MediaProgressResult(
                progressPercent = percent,
                currentMs = current.toLong(),
                durationMs = max.toLong()
            )
        }

        // Custom time keys (in ms or seconds)
        val posLong = listOf("android.playbackPosition", "playbackPosition", "position", "current_time")
            .firstNotNullOfOrNull { key -> if (extras.containsKey(key)) extras.getLong(key, -1L).takeIf { it >= 0 } else null }
        val durLong = listOf("android.duration", "duration", "total_time", "max_time")
            .firstNotNullOfOrNull { key -> if (extras.containsKey(key)) extras.getLong(key, -1L).takeIf { it > 0 } else null }

        if (posLong != null && durLong != null && durLong > 0L) {
            val percent = ((posLong.toDouble() / durLong) * 100).toInt().coerceIn(0, 100)
            return MediaProgressResult(
                progressPercent = percent,
                currentFormatted = formatMillisToTime(posLong),
                durationFormatted = formatMillisToTime(durLong),
                currentMs = posLong,
                durationMs = durLong
            )
        }

        return null
    }

    fun calculateProgress(current: Long, total: Long): MediaProgressResult? {
        if (current < 0 || total <= 0) return null
        val percent = ((current.toDouble() / total) * 100).toInt().coerceIn(0, 100)
        return MediaProgressResult(
            progressPercent = percent,
            currentFormatted = formatMillisToTime(current),
            durationFormatted = formatMillisToTime(total),
            currentMs = current,
            durationMs = total
        )
    }

    fun extractProgressFromText(text: CharSequence?): MediaProgressResult? {
        if (text.isNullOrBlank()) return null
        val match = TIME_SLASH_TIME_REGEX.find(text) ?: return null
        val curStr = match.groupValues[1]
        val durStr = match.groupValues[2]

        val curSec = parseTimeStringToSeconds(curStr) ?: return null
        val durSec = parseTimeStringToSeconds(durStr) ?: return null

        if (durSec <= 0L) return null
        val percent = ((curSec.toDouble() / durSec) * 100).toInt().coerceIn(0, 100)
        return MediaProgressResult(
            progressPercent = percent,
            currentFormatted = curStr,
            durationFormatted = durStr,
            currentMs = curSec * 1000L,
            durationMs = durSec * 1000L
        )
    }

    fun parseTimeStringToSeconds(timeStr: String): Long? {
        val parts = timeStr.trim().split(":")
        return when (parts.size) {
            2 -> {
                val m = parts[0].toLongOrNull() ?: return null
                val s = parts[1].toLongOrNull() ?: return null
                m * 60L + s
            }
            3 -> {
                val h = parts[0].toLongOrNull() ?: return null
                val m = parts[1].toLongOrNull() ?: return null
                val s = parts[2].toLongOrNull() ?: return null
                h * 3600L + m * 60L + s
            }
            else -> null
        }
    }

    fun formatMillisToTime(millis: Long): String {
        val totalSec = millis / 1000L
        val hours = totalSec / 3600L
        val mins = (totalSec % 3600L) / 60L
        val secs = totalSec % 60L
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }
}
