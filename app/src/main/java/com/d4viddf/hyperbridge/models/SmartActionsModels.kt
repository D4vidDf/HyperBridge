package com.d4viddf.hyperbridge.models

/** Kinds of actionable content Smart Actions can pull out of notification text. */
enum class SmartActionType { OTP, TRACKING, URL, PHONE }

/**
 * One extracted entity plus the target its button acts on.
 *
 * - OTP: [value] = digits only, [target] = the same digits (copied to the clipboard)
 * - URL: [value] = the text as written, [target] = absolute URL (https:// prepended when missing)
 * - PHONE: [value] = the number as written, [target] = dialable number (`+34612345678`)
 * - TRACKING: [value] = tracking id, [target] = carrier tracking page URL, [carrier] = human name
 */
data class SmartAction(
    val type: SmartActionType,
    val value: String,
    val target: String,
    val carrier: String? = null
)

/**
 * User preferences for the Smart Actions engine. Disabled by default so that users who never
 * enable it pay zero cost: no text is collected or scanned while [enabled] is false.
 */
data class SmartActionsConfig(
    val enabled: Boolean = false,
    val otp: Boolean = true,
    val url: Boolean = true,
    val phone: Boolean = true,
    val tracking: Boolean = true,
    val excludedPackages: Set<String> = emptySet()
) {
    fun isActiveFor(packageName: String): Boolean =
        enabled && packageName !in excludedPackages && (otp || url || phone || tracking)

    companion object {
        val DISABLED = SmartActionsConfig()
    }
}
