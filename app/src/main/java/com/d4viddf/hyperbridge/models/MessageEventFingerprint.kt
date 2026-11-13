package com.d4viddf.hyperbridge.models

/**
 * Stable identity for one received message event.
 *
 * This is deliberately separate from the rendered-content hash: two messages may have the
 * same sender and text while still being distinct events.
 */
data class MessageEventFingerprint(
    val source: MessageEventFingerprintSource,
    val primaryValue: Long,
    val messageCount: Int? = null,
    val secondaryValue: Long? = null,
    val sourceScope: Int? = null
) {
    /** A compact seed for bridge-id generation. Equality must still use the full value. */
    val stableHash: Int
        get() = listOf(source, primaryValue, messageCount, secondaryValue, sourceScope).hashCode()

    /**
     * Returns whether two fingerprints still describe the same app event.
     *
     * Android may refresh a StatusBarNotification's post time while retaining the original
     * Notification.when value. When the visible payload is unchanged, that secondary-time churn
     * is a framework repost rather than a new message.
     */
    fun representsSameEventAs(
        other: MessageEventFingerprint,
        contentUnchanged: Boolean
    ): Boolean {
        if (this == other) return true
        return contentUnchanged &&
                source == MessageEventFingerprintSource.NOTIFICATION_WHEN &&
                other.source == MessageEventFingerprintSource.NOTIFICATION_WHEN &&
                primaryValue == other.primaryValue
    }
}

enum class MessageEventFingerprintSource {
    MESSAGING_STYLE,
    NOTIFICATION_WHEN,
    SOURCE_POST_TIME,
    CALLBACK_GENERATION
}
