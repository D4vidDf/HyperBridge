package com.d4viddf.hyperbridge.service

import com.d4viddf.hyperbridge.models.MessageEventFingerprint
import com.d4viddf.hyperbridge.models.NotificationType
import java.util.LinkedHashMap

enum class IslandPresentationKind {
    NEW,
    UPDATE,
    UNCHANGED
}

enum class IslandPresentationReason {
    NEW_EVENT,
    /** A richer alias replaced an aggregate source before HyperOS displayed the first payload. */
    SOURCE_PROMOTION,
    CONTENT_UPDATE,
    RESTORE,
    RECONCILE;

    val mayAutoExpand: Boolean
        get() = this == NEW_EVENT || this == SOURCE_PROMOTION
}

data class PreviousIslandPresentation(
    val logicalId: String,
    val bridgeId: Int,
    val contentHash: Int,
    val messageEventFingerprint: MessageEventFingerprint? = null
)

data class IslandUpdateDecision(
    val kind: IslandPresentationKind,
    val bridgeId: Int,
    val onlyAlertOnce: Boolean,
    val presentationReason: IslandPresentationReason,
    val cancelBeforeNotify: Boolean = false
)

object IslandUpdateResolver {
    fun decide(
        logicalId: String,
        candidateBridgeId: Int,
        contentHash: Int,
        previous: PreviousIslandPresentation?,
        notificationType: NotificationType = NotificationType.STANDARD,
        presentationReason: IslandPresentationReason = if (previous == null || previous.logicalId != logicalId) {
            IslandPresentationReason.NEW_EVENT
        } else {
            IslandPresentationReason.CONTENT_UPDATE
        },
        isMessagingEvent: Boolean = notificationType == NotificationType.MESSAGE,
        messageEventFingerprint: MessageEventFingerprint? = null
    ): IslandUpdateDecision {
        if (previous == null || previous.logicalId != logicalId) {
            return IslandUpdateDecision(
                kind = IslandPresentationKind.NEW,
                bridgeId = candidateBridgeId,
                onlyAlertOnce = !presentationReason.mayAutoExpand,
                presentationReason = presentationReason
            )
        }

        val contentChanged = previous.contentHash != contentHash
        val previousMessageEvent = previous.messageEventFingerprint
        val sameKnownMessageEvent = isMessagingEvent &&
                previousMessageEvent != null &&
                messageEventFingerprint != null &&
                previousMessageEvent.representsSameEventAs(
                    messageEventFingerprint,
                    contentUnchanged = !contentChanged
                )
        val messageEventChanged = isMessagingEvent &&
                !sameKnownMessageEvent &&
                (previousMessageEvent != null || messageEventFingerprint != null)

        if (!contentChanged && !messageEventChanged) {
            return IslandUpdateDecision(
                kind = IslandPresentationKind.UNCHANGED,
                bridgeId = previous.bridgeId,
                onlyAlertOnce = true,
                presentationReason = presentationReason
            )
        }

        if (isMessagingEvent && !sameKnownMessageEvent) {
            val newEventReason = when (presentationReason) {
                IslandPresentationReason.RECONCILE,
                IslandPresentationReason.RESTORE -> presentationReason
                else -> IslandPresentationReason.NEW_EVENT
            }
            return IslandUpdateDecision(
                kind = IslandPresentationKind.NEW,
                bridgeId = candidateBridgeId,
                onlyAlertOnce = !newEventReason.mayAutoExpand,
                presentationReason = newEventReason,
                cancelBeforeNotify = true
            )
        }

        return IslandUpdateDecision(
            kind = IslandPresentationKind.UPDATE,
            bridgeId = previous.bridgeId,
            onlyAlertOnce = !presentationReason.mayAutoExpand,
            presentationReason = presentationReason
        )
    }
}

object MessageBridgeIdPolicy {
    private const val RANGE_START = -1_900_000_000
    private const val RANGE_SIZE = 800_000_000

    /** Message replacements use a private negative band, away from permanent/widget/watch ids. */
    fun candidate(
        logicalId: String,
        generation: Long,
        contentHash: Int,
        attempt: Int = 0,
        messageEventFingerprint: MessageEventFingerprint? = null
    ): Int {
        val mixed = listOf(
            logicalId,
            generation,
            contentHash,
            messageEventFingerprint?.stableHash,
            attempt
        ).hashCode().toLong()
        return RANGE_START + Math.floorMod(mixed, RANGE_SIZE.toLong()).toInt()
    }
}

data class InternalBridgeReplacement(
    val logicalId: String,
    val generation: Long,
    val markedAt: Long
)

class InternalBridgeReplacementRegistry(
    private val ttlMs: Long = 10_000L,
    private val maxEntries: Int = 64
) {
    private val entries = LinkedHashMap<Int, InternalBridgeReplacement>()

    @Synchronized
    fun mark(bridgeId: Int, logicalId: String, generation: Long, now: Long) {
        prune(now)
        entries[bridgeId] = InternalBridgeReplacement(logicalId, generation, now)
        while (entries.size > maxEntries) {
            entries.remove(entries.entries.first().key)
        }
    }

    @Synchronized
    fun consume(bridgeId: Int, now: Long): InternalBridgeReplacement? {
        prune(now)
        return entries.remove(bridgeId)
    }

    @Synchronized
    fun prune(now: Long) {
        entries.entries.removeIf { now - it.value.markedAt > ttlMs }
    }

    @Synchronized
    fun clear() = entries.clear()
}

object PermanentIslandVisibilityPolicy {
    fun desiredActive(
        enabled: Boolean,
        realNotificationCount: Int,
        hasNativeIsland: Boolean,
        hideInLandscape: Boolean,
        isLandscape: Boolean
    ): Boolean = enabled &&
            realNotificationCount == 0 &&
            !hasNativeIsland &&
            !(hideInLandscape && isLandscape)
}

object NotificationLifecyclePolicy {
    fun dismissesWithSource(type: NotificationType?): Boolean = when (type) {
        NotificationType.CALL,
        NotificationType.MEDIA,
        NotificationType.NAVIGATION,
        NotificationType.SCREEN_RECORDING -> true
        else -> false
    }

    fun canIntentionallyMirrorSource(type: NotificationType): Boolean =
        type == NotificationType.MESSAGE || type == NotificationType.STANDARD

    fun shouldDismissSourceAfterBridgeRemoval(
        dismissSourceOnContentClick: Boolean,
        wasContentClick: Boolean
    ): Boolean = dismissSourceOnContentClick && wasContentClick

    fun presentationReason(hasPrevious: Boolean, recovery: Boolean): IslandPresentationReason = when {
        recovery -> IslandPresentationReason.RECONCILE
        hasPrevious -> IslandPresentationReason.CONTENT_UPDATE
        else -> IslandPresentationReason.NEW_EVENT
    }

    fun isCurrentRemoval(
        activeSourceKey: String,
        activeSourcePostTime: Long,
        removedSourceKey: String,
        removedSourcePostTime: Long
    ): Boolean {
        return activeSourceKey == removedSourceKey && activeSourcePostTime <= removedSourcePostTime
    }
}
