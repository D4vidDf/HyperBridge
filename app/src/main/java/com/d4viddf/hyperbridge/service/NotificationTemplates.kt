package com.d4viddf.hyperbridge.service

/**
 * Helpers around the value of `Notification.EXTRA_TEMPLATE`.
 *
 * The platform stores the style's binary class name there, so a MessagingStyle notification
 * carries `android.app.Notification$MessagingStyle` (dollar sign, nested class), and one built
 * with the compat library carries `androidx.core.app.NotificationCompat$MessagingStyle`.
 * A comparison against the dotted spelling `android.app.Notification.MessagingStyle` can never
 * match, which silently downgraded every MessagingStyle notification without a `msg` category
 * to a STANDARD island (#331).
 *
 * Pure Kotlin so it can be unit-tested without an Android runtime.
 */
object NotificationTemplates {
    const val PLATFORM_MESSAGING_STYLE = "android.app.Notification\$MessagingStyle"
    const val COMPAT_MESSAGING_STYLE = "androidx.core.app.NotificationCompat\$MessagingStyle"

    private const val MESSAGING_STYLE_SUFFIX = "MessagingStyle"

    /** True for platform and compat MessagingStyle templates, whatever the outer class spelling. */
    fun isMessagingStyle(template: String?): Boolean =
        template?.endsWith(MESSAGING_STYLE_SUFFIX) == true
}
