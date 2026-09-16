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
    val sourceLookup: (sourceId: String, field: String) -> String? = { _, _ -> null }
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
