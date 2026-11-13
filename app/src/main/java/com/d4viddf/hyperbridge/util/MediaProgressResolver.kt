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
    val remainingFormatted: String? = null,
    val currentMs: Long? = null,
    val durationMs: Long? = null,
    val isPlaying: Boolean? = null,
    val playbackStateStr: String? = null
)

object MediaProgressResolver {

    private val TIME_SLASH_TIME_REGEX = Regex(
        """(\d{1,2}:\d{2}(?::\d{2})?)\s*(?:/|of|\|)\s*(\d{1,2}:\d{2}(?::\d{2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val SINGLE_TIME_REGEX = Regex(
        """(-?\b\d{1,2}:\d{2}(?::\d{2})?\b)"""
    )

    fun resolveMediaProgress(
        sbn: StatusBarNotification,
        context: Context? = null
    ): MediaProgressResult {
        val extras = sbn.notification.extras ?: return MediaProgressResult(null)

        var fallbackIsPlaying: Boolean? = null
        var fallbackStateStr: String? = null

        // 1. Try resolving via MediaSession.Token & MediaController if context is provided
        if (context != null) {
            val sessionResult = extractFromMediaSession(extras, context)
            if (sessionResult != null) {
                if (sessionResult.currentFormatted != null || sessionResult.remainingFormatted != null || sessionResult.progressPercent != null) {
                    return sessionResult
                }
                fallbackIsPlaying = sessionResult.isPlaying
                fallbackStateStr = sessionResult.playbackStateStr
            }
        }

        // 2. Try notification extras with explicit progress/max or custom duration keys
        val extrasProgress = extractFromExtras(extras)
        if (extrasProgress != null && (extrasProgress.progressPercent != null || extrasProgress.currentFormatted != null || extrasProgress.remainingFormatted != null)) {
            return extrasProgress.copy(
                isPlaying = extrasProgress.isPlaying ?: fallbackIsPlaying,
                playbackStateStr = extrasProgress.playbackStateStr ?: fallbackStateStr
            )
        }

        // 3. Try parsing text timestamps like "01:23 / 03:45" or "02:45" in text, subText, infoText, or title
        val textCandidates = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        )
        for (candidate in textCandidates) {
            val textResult = extractProgressFromText(candidate)
            if (textResult != null && (textResult.progressPercent != null || textResult.currentFormatted != null || textResult.remainingFormatted != null)) {
                return textResult.copy(
                    isPlaying = textResult.isPlaying ?: fallbackIsPlaying,
                    playbackStateStr = textResult.playbackStateStr ?: fallbackStateStr
                )
            }
        }

        return MediaProgressResult(
            progressPercent = null,
            isPlaying = fallbackIsPlaying,
            playbackStateStr = fallbackStateStr
        )
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
            val rawPosition = playbackState?.position ?: -1L

            val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
            val stateStr = when (playbackState?.state) {
                PlaybackState.STATE_PLAYING -> "playing"
                PlaybackState.STATE_PAUSED -> "paused"
                PlaybackState.STATE_STOPPED -> "stopped"
                PlaybackState.STATE_BUFFERING -> "buffering"
                else -> null
            }

            val position = if (isPlaying && playbackState.lastPositionUpdateTime > 0 && rawPosition >= 0L) {
                val speed = playbackState.playbackSpeed.takeIf { it > 0f } ?: 1f
                val elapsed = SystemClock.elapsedRealtime() - playbackState.lastPositionUpdateTime
                (rawPosition + (elapsed * speed).toLong()).coerceAtLeast(0L)
            } else {
                rawPosition
            }

            if (duration > 0L) {
                val clampedPos = if (position >= 0L) position.coerceIn(0L, duration) else 0L
                val percent = ((clampedPos.toDouble() / duration) * 100).toInt().coerceIn(0, 100)
                val remainingMs = (duration - clampedPos).coerceAtLeast(0L)
                MediaProgressResult(
                    progressPercent = percent,
                    currentFormatted = formatMillisToTime(clampedPos),
                    durationFormatted = formatMillisToTime(duration),
                    remainingFormatted = "-${formatMillisToTime(remainingMs)}",
                    currentMs = clampedPos,
                    durationMs = duration,
                    isPlaying = isPlaying,
                    playbackStateStr = stateStr
                )
            } else if (position >= 0L) {
                MediaProgressResult(
                    progressPercent = null,
                    currentFormatted = formatMillisToTime(position),
                    durationFormatted = null,
                    remainingFormatted = null,
                    currentMs = position,
                    durationMs = null,
                    isPlaying = isPlaying,
                    playbackStateStr = stateStr
                )
            } else {
                MediaProgressResult(
                    progressPercent = null,
                    isPlaying = isPlaying,
                    playbackStateStr = stateStr
                )
            }
        }.getOrNull()
    }

    fun extractFromExtras(extras: Bundle): MediaProgressResult? {
        val current = extras.getInt(Notification.EXTRA_PROGRESS, -1).takeIf { it >= 0 }
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, -1).takeIf { it > 0 }
        if (current != null && max != null) {
            val percent = ((current.toDouble() / max) * 100).toInt().coerceIn(0, 100)
            val isSeconds = max in 1..7200 && max > 100
            val isMillis = max > 7200
            val curMs = if (isMillis) current.toLong() else if (isSeconds) current.toLong() * 1000L else null
            val durMs = if (isMillis) max.toLong() else if (isSeconds) max.toLong() * 1000L else null
            val remMs = if (durMs != null && curMs != null && durMs > 0L) (durMs - curMs).coerceAtLeast(0L) else null

            return MediaProgressResult(
                progressPercent = percent,
                currentFormatted = curMs?.let { formatMillisToTime(it) },
                durationFormatted = durMs?.let { formatMillisToTime(it) },
                remainingFormatted = remMs?.let { "-${formatMillisToTime(it)}" },
                currentMs = curMs ?: current.toLong(),
                durationMs = durMs ?: max.toLong()
            )
        }

        // Custom time keys (in ms or seconds)
        val posLong = listOf("android.playbackPosition", "playbackPosition", "position", "current_time")
            .firstNotNullOfOrNull { key -> if (extras.containsKey(key)) extras.getLong(key, -1L).takeIf { it >= 0 } else null }
        val durLong = listOf("android.duration", "duration", "total_time", "max_time")
            .firstNotNullOfOrNull { key -> if (extras.containsKey(key)) extras.getLong(key, -1L).takeIf { it > 0 } else null }

        if (posLong != null && durLong != null && durLong > 0L) {
            val isSeconds = durLong < 10000L
            val posMs = if (isSeconds) posLong * 1000L else posLong
            val durMs = if (isSeconds) durLong * 1000L else durLong
            val percent = ((posMs.toDouble() / durMs) * 100).toInt().coerceIn(0, 100)
            val remMs = (durMs - posMs).coerceAtLeast(0L)
            return MediaProgressResult(
                progressPercent = percent,
                currentFormatted = formatMillisToTime(posMs),
                durationFormatted = formatMillisToTime(durMs),
                remainingFormatted = "-${formatMillisToTime(remMs)}",
                currentMs = posMs,
                durationMs = durMs
            )
        } else if (posLong != null) {
            val isSeconds = posLong in 1..9999L
            val posMs = if (isSeconds) posLong * 1000L else posLong
            return MediaProgressResult(
                progressPercent = null,
                currentFormatted = formatMillisToTime(posMs),
                durationFormatted = null,
                remainingFormatted = null,
                currentMs = posMs,
                durationMs = null
            )
        }

        return null
    }

    fun calculateProgress(current: Long, total: Long): MediaProgressResult? {
        if (current < 0 || total <= 0) return null
        val percent = ((current.toDouble() / total) * 100).toInt().coerceIn(0, 100)
        val remMs = (total - current).coerceAtLeast(0L)
        return MediaProgressResult(
            progressPercent = percent,
            currentFormatted = formatMillisToTime(current),
            durationFormatted = formatMillisToTime(total),
            remainingFormatted = "-${formatMillisToTime(remMs)}",
            currentMs = current,
            durationMs = total
        )
    }

    fun extractProgressFromText(text: CharSequence?): MediaProgressResult? {
        if (text.isNullOrBlank()) return null
        val match = TIME_SLASH_TIME_REGEX.find(text)
        if (match != null) {
            val curStr = match.groupValues[1]
            val durStr = match.groupValues[2]

            val curSec = parseTimeStringToSeconds(curStr) ?: return null
            val durSec = parseTimeStringToSeconds(durStr) ?: return null

            if (durSec > 0L) {
                val percent = ((curSec.toDouble() / durSec) * 100).toInt().coerceIn(0, 100)
                val remSec = (durSec - curSec).coerceAtLeast(0L)
                return MediaProgressResult(
                    progressPercent = percent,
                    currentFormatted = curStr,
                    durationFormatted = durStr,
                    remainingFormatted = "-${formatMillisToTime(remSec * 1000L)}",
                    currentMs = curSec * 1000L,
                    durationMs = durSec * 1000L
                )
            }
        }

        val singleMatch = SINGLE_TIME_REGEX.find(text)
        if (singleMatch != null) {
            val curStr = singleMatch.groupValues[1]
            val isNegative = curStr.startsWith("-")
            val cleanStr = curStr.removePrefix("-")
            val curSec = parseTimeStringToSeconds(cleanStr)
            if (curSec != null) {
                return MediaProgressResult(
                    progressPercent = null,
                    currentFormatted = if (isNegative) null else cleanStr,
                    durationFormatted = null,
                    remainingFormatted = if (isNegative) curStr else null,
                    currentMs = if (isNegative) null else curSec * 1000L,
                    durationMs = null
                )
            }
        }

        return null
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
