package com.d4viddf.hyperbridge.data.widget

/**
 * Values available to a [com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument] at render
 * time. Battery/time are read by the caller (translator or [com.d4viddf.hyperbridge.service.PermanentIslandManager])
 * and passed in here, never read by the engine itself, so this class stays plain-JUnit testable.
 *
 * [sourceLookup] is how the "island content sources" feature (an allow-listed app driving
 * `{source.<id>.text}` / `{source.<id>.icon}` via the UPDATE_SOURCE broadcast) is wired in without
 * this engine depending on Room directly.
 */
data class VariableContext(
    val notifTitle: String? = null,
    val notifText: String? = null,
    val notifProgress: Int? = null,
    val notifPackage: String? = null,
    val deviceBatteryPercent: Int? = null,
    val timeNowFormatted: String? = null,
    /** Titles of the notification's own action buttons, in order. */
    val notificationActionTitles: List<String> = emptyList(),
    val hasInlineReply: Boolean = false,
    /** Smart Action types (#270) detected on this notification: OTP, URL, PHONE, TRACKING. */
    val smartActionTypes: Set<String> = emptySet(),
    val sourceLookup: (sourceId: String, field: String) -> String? = { _, _ -> null },
    val themePrimary: String? = "#3DDA82",
    val themeAccent: String? = "#00E5FF",
    val themeSurface: String? = "#1E1E1E",
    val notifAvatarBitmap: android.graphics.Bitmap? = null,
    val notifPictureBitmap: android.graphics.Bitmap? = null,
    val notifSmallIconBitmap: android.graphics.Bitmap? = null,
    val mediaProgress: Int? = null,
    val mediaTrack: String? = null,
    val mediaArtist: String? = null,
    val mediaDuration: String? = null,
    val mediaPosition: String? = null,
    val isMediaPlaying: Boolean = false,
    val mediaState: String? = null,
    val colorScheme: com.d4viddf.hyperbridge.models.colorscheme.ColorSchemeDefinition? = null
)

/**
 * Resolves `{token}` placeholders inside a widget node's template string. Unknown or unavailable
 * tokens resolve to an empty string rather than throwing, so a widget never crashes rendering
 * because a value hasn't arrived yet.
 */
class WidgetVariableEngine {

    fun resolve(template: String, ctx: VariableContext): String {
        return TOKEN_REGEX.replace(template) { match ->
            resolveToken(match.groupValues[1], ctx) ?: ""
        }
    }

    private fun resolveToken(token: String, ctx: VariableContext): String? {
        return when {
            token == "notif.title" -> ctx.notifTitle
            token == "notif.text" -> ctx.notifText
            token == "notif.progress" -> ctx.notifProgress?.toString()
            token == "notif.package" -> ctx.notifPackage
            token == "device.battery" -> ctx.deviceBatteryPercent?.toString()
            token == "time.now" -> ctx.timeNowFormatted
            token == "media.progress" -> ctx.mediaProgress?.toString() ?: ctx.notifProgress?.toString()
            token == "media.track" -> ctx.mediaTrack ?: ctx.notifTitle
            token == "media.artist" -> ctx.mediaArtist ?: ctx.notifText
            token == "media.duration" -> ctx.mediaDuration
            token == "media.position" -> ctx.mediaPosition
            token == "media.is_playing" -> ctx.isMediaPlaying.toString()
            token == "media.state" -> ctx.mediaState
            token == "theme.primary" -> ctx.colorScheme?.getHex(com.d4viddf.hyperbridge.models.colorscheme.ColorSchemeRole.PRIMARY) ?: ctx.themePrimary
            token == "theme.accent" -> ctx.colorScheme?.getHex(com.d4viddf.hyperbridge.models.colorscheme.ColorSchemeRole.SECONDARY) ?: ctx.themeAccent
            token == "theme.highlight" -> ctx.colorScheme?.getHex(com.d4viddf.hyperbridge.models.colorscheme.ColorSchemeRole.PRIMARY) ?: ctx.themePrimary ?: ctx.themeAccent
            token == "theme.surface" -> ctx.colorScheme?.getHex(com.d4viddf.hyperbridge.models.colorscheme.ColorSchemeRole.SURFACE) ?: ctx.themeSurface
            token.startsWith("scheme.") -> {
                val roleKey = token.removePrefix("scheme.")
                val role = com.d4viddf.hyperbridge.models.colorscheme.ColorSchemeRole.fromKey(roleKey)
                if (role != null) {
                    ctx.colorScheme?.getHex(role)
                } else {
                    ctx.colorScheme?.roles?.get(roleKey)
                }
            }
            else -> {
                val parts = token.split(".")
                if (parts.size == 3 && parts[0] == "source") {
                    ctx.sourceLookup(parts[1], parts[2])
                } else {
                    null
                }
            }
        }
    }

    companion object {
        private val TOKEN_REGEX = Regex("\\{([a-zA-Z0-9_.]+)\\}")
    }
}
