package com.d4viddf.hyperbridge.service

data class NotificationRefreshSignals(
    val packageName: String,
    val title: String,
    val text: String,
    val hasMessageContent: Boolean,
    val hasProgressOrSpecialState: Boolean
)

data class SourceNotificationCandidate(
    val sourceKey: String,
    val packageName: String,
    val notificationId: Int,
    val notificationTag: String?,
    val postTime: Long,
    val quality: SourceCandidateQuality
)

/** App notification slots are deliberately conservative: package + id + tag must all match. */
object SourceNotificationCandidatePolicy {
    fun isSameSlot(first: SourceNotificationCandidate, second: SourceNotificationCandidate): Boolean =
        first.packageName == second.packageName &&
                first.notificationId == second.notificationId &&
                first.notificationTag == second.notificationTag

    /** Quality is monotonic; exact-key preference and recency only break equal-quality ties. */
    fun shouldPrefer(
        current: SourceNotificationCandidate,
        incoming: SourceNotificationCandidate,
        requestedSourceKey: String
    ): Boolean {
        if (!isSameSlot(current, incoming)) return false
        if (incoming.quality.rank != current.quality.rank) {
            return incoming.quality.rank > current.quality.rank
        }
        val currentIsExact = current.sourceKey == requestedSourceKey
        val incomingIsExact = incoming.sourceKey == requestedSourceKey
        if (currentIsExact != incomingIsExact) return incomingIsExact
        return incoming.postTime > current.postTime
    }
}

data class PendingRemovalSignals(
    val removalObserved: Boolean,
    val removalPostTime: Long,
    val candidatePostTime: Long,
    val explicitUserDismissal: Boolean,
    val activeExactOrReplacement: Boolean
)

/** App churn/removal is state, not an instruction to discard a posted event. */
object PendingRemovalPolicy {
    fun shouldSuppress(signals: PendingRemovalSignals): Boolean =
        signals.removalObserved &&
                signals.removalPostTime >= signals.candidatePostTime &&
                signals.explicitUserDismissal &&
                !signals.activeExactOrReplacement
}

object NotificationTypeEnablementPolicy {
    fun isEnabled(effectiveTypes: Set<String>, candidateType: String): Boolean =
        candidateType in effectiveTypes

    /**
     * A direct MessagingStyle event is also a usable general notification. This lets users keep
     * aggregate/chat-summary handling disabled while still accepting the person notification via
     * STANDARD. Inbox/group bookkeeping does not qualify for this fallback.
     */
    fun resolveEnabledType(
        effectiveTypes: Set<String>,
        detectedType: String,
        hasDirectMessagingStyle: Boolean
    ): String? = when {
        isEnabled(effectiveTypes, detectedType) -> detectedType
        detectedType == "MESSAGE" &&
                hasDirectMessagingStyle &&
                isEnabled(effectiveTypes, "STANDARD") -> "STANDARD"
        else -> null
    }
}

/** Bounded refresh policy used after an allowed callback has already claimed a generation. */
object NotificationRefreshPolicy {
    const val MAX_REFRESH_ATTEMPTS = 2
    const val REFRESH_DELAY_MS = 125L

    fun shouldRefresh(signals: NotificationRefreshSignals): Boolean {
        if (signals.hasProgressOrSpecialState) return false
        if (signals.title.equals(signals.packageName, ignoreCase = true) ||
            signals.text.equals(signals.packageName, ignoreCase = true)
        ) return true
        return signals.title.isBlank() && signals.text.isBlank() && !signals.hasMessageContent
    }
}

data class NotificationAcceptanceSignals(
    val packageName: String,
    val title: String,
    val text: String,
    val hasMessageContent: Boolean,
    val hasProgressOrSpecialState: Boolean,
    val containsBlockedTerm: Boolean
)

/** Upstream-compatible junk policy using already resolved title/text content. */
object NotificationAcceptancePolicy {
    fun isJunk(signals: NotificationAcceptanceSignals): Boolean {
        if (signals.hasProgressOrSpecialState) return false
        if (signals.title.isEmpty() && signals.text.isEmpty() && !signals.hasMessageContent) return true
        if (signals.title.equals(signals.packageName, ignoreCase = true) ||
            signals.text.equals(signals.packageName, ignoreCase = true)
        ) return true
        if (signals.containsBlockedTerm) return true
        return false
    }
}

/** Full shade classification is reserved for connect/unlock/screen-on recovery passes. */
object OngoingRecoveryPolicy {
    fun shouldClassifyShadeNotifications(refresh: Boolean): Boolean = refresh
}
