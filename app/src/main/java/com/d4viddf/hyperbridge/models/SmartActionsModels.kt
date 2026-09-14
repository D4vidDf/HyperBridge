package com.d4viddf.hyperbridge.models

/** Kinds of actionable content Smart Actions can pull out of notification text. */
enum class SmartActionType { OTP, TRACKING, NAVIGATION, URL, PHONE }

/**
 * One extracted entity plus the target its button acts on.
 *
 * - OTP: [value] = digits only, [target] = the same digits (copied to the clipboard)
 * - URL: [value] = the text as written, [target] = absolute URL (https:// prepended when missing)
 * - PHONE: [value] = the number as written, [target] = dialable number (`+34612345678`)
 * - TRACKING: [value] = tracking id, [target] = carrier tracking page URL, [carrier] = human name
 * - NAVIGATION: [value] = the address or map link as written, [target] = what to open: the map link
 *   itself (so the owning app handles it) or a `geo:0,0?q=` search for a street address
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
    val navigation: Boolean = true,
    val excludedPackages: Set<String> = emptySet(),
    // When true, the OTP button never prints the code itself (island or shade), only "Copy code".
    val hideOtpCode: Boolean = false
) {
    fun isActiveFor(packageName: String): Boolean =
        enabled && packageName !in excludedPackages && (otp || url || phone || tracking || navigation)

    /**
     * Applies a per-app [override] on top of this (global) config: a non-null field in [override]
     * wins in both directions (it can turn a type on OR off relative to the global value), a null
     * field inherits the global value as-is. [enabled], [hideOtpCode] and [excludedPackages] are
     * global-only and pass through unchanged — per-app on/off lives in [excludedPackages] instead.
     */
    fun applyOverride(override: AppSmartActionsOverride?): SmartActionsConfig {
        if (override == null) return this
        return copy(
            otp = override.otp ?: otp,
            url = override.url ?: url,
            phone = override.phone ?: phone,
            tracking = override.tracking ?: tracking,
            navigation = override.navigation ?: navigation
        )
    }

    companion object {
        val DISABLED = SmartActionsConfig()
    }
}

/**
 * Per-app, per-type overrides of the global [SmartActionsConfig], for privacy: a user may want
 * OTP buttons everywhere except one banking app, for example. A null field means "inherit the
 * global setting"; whether Smart Actions run at all for this app is controlled separately by
 * [SmartActionsConfig.excludedPackages] (the single source of truth for per-app on/off).
 */
data class AppSmartActionsOverride(
    val otp: Boolean? = null,
    val url: Boolean? = null,
    val phone: Boolean? = null,
    val tracking: Boolean? = null,
    val navigation: Boolean? = null
) {
    val isEmpty: Boolean
        get() = otp == null && url == null && phone == null && tracking == null && navigation == null

    fun get(type: SmartActionType): Boolean? = when (type) {
        SmartActionType.OTP -> otp
        SmartActionType.URL -> url
        SmartActionType.PHONE -> phone
        SmartActionType.TRACKING -> tracking
        SmartActionType.NAVIGATION -> navigation
    }

    fun with(type: SmartActionType, value: Boolean?): AppSmartActionsOverride = when (type) {
        SmartActionType.OTP -> copy(otp = value)
        SmartActionType.URL -> copy(url = value)
        SmartActionType.PHONE -> copy(phone = value)
        SmartActionType.TRACKING -> copy(tracking = value)
        SmartActionType.NAVIGATION -> copy(navigation = value)
    }
}
