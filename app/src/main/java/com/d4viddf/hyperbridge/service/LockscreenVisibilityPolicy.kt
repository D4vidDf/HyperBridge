package com.d4viddf.hyperbridge.service

import android.app.Notification

/**
 * Decides whether a bridge notification has to stay off the secure lock screen.
 *
 * The bridge copies the title and text of the source notification into a notification posted
 * by this app, so the lock screen rules the user set for the source app (or the source app set
 * for itself) no longer apply to that copy. When the source content would be hidden or redacted
 * on the lock screen, the copy is posted as [Notification.VISIBILITY_SECRET]: the system keeps
 * it off the lock screen and shows it normally once the device is unlocked. The source
 * notification is untouched and still shows in whatever redacted form the system gives it.
 */
object LockscreenVisibilityPolicy {

    /** Same value as `NotificationListenerService.Ranking.VISIBILITY_NO_OVERRIDE`. */
    const val NO_OVERRIDE = -1000

    /**
     * @param sourceVisibility `Notification.visibility` of the source notification.
     * @param rankingOverride the user's lock screen choice for the source app/channel, from
     *   `Ranking.getLockscreenVisibilityOverride()`, or [NO_OVERRIDE].
     * @param privateContentAllowed false when the system-wide lock screen setting hides
     *   sensitive notification content.
     * @param explicitLockscreen true when the user asked for this island on the lock screen.
     * @return the visibility to set on the bridge notification, or null to leave the default.
     */
    fun bridgeVisibility(
        sourceVisibility: Int,
        rankingOverride: Int,
        privateContentAllowed: Boolean,
        explicitLockscreen: Boolean
    ): Int? {
        if (explicitLockscreen) return null
        val hidden = sourceVisibility == Notification.VISIBILITY_SECRET ||
            rankingOverride == Notification.VISIBILITY_SECRET
        val redacted = rankingOverride == Notification.VISIBILITY_PRIVATE ||
            (sourceVisibility == Notification.VISIBILITY_PRIVATE && !privateContentAllowed)
        return if (hidden || redacted) Notification.VISIBILITY_SECRET else null
    }
}
