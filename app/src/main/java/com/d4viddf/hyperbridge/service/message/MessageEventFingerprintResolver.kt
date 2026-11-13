package com.d4viddf.hyperbridge.service.message

import com.d4viddf.hyperbridge.models.MessageEventFingerprint
import com.d4viddf.hyperbridge.models.MessageEventFingerprintSource
import java.util.LinkedHashMap

data class MessageEventSignals(
    val latestMessageTimestamp: Long? = null,
    val messageCount: Int = 0,
    val notificationWhen: Long? = null,
    val notificationWhenIsReliable: Boolean = true,
    val sourcePostTime: Long? = null
)

/** Selects the strongest event tier, retaining source generation only when `when` may be reused. */
object MessageEventFingerprintResolver {
    fun resolve(signals: MessageEventSignals): MessageEventFingerprint? {
        signals.latestMessageTimestamp.positiveOrNull()?.let { timestamp ->
            return MessageEventFingerprint(
                source = MessageEventFingerprintSource.MESSAGING_STYLE,
                primaryValue = timestamp,
                messageCount = signals.messageCount.takeIf { it > 0 }
            )
        }

        if (signals.notificationWhenIsReliable) {
            signals.notificationWhen.positiveOrNull()?.let { timestamp ->
                return MessageEventFingerprint(
                    source = MessageEventFingerprintSource.NOTIFICATION_WHEN,
                    primaryValue = timestamp,
                    // Preserve the preferred Notification.when identity while allowing a new
                    // source generation to disambiguate apps that keep `when` constant.
                    secondaryValue = signals.sourcePostTime.positiveOrNull()
                )
            }
        }

        signals.sourcePostTime.positiveOrNull()?.let { postTime ->
            return MessageEventFingerprint(
                source = MessageEventFingerprintSource.SOURCE_POST_TIME,
                primaryValue = postTime
            )
        }

        return null
    }

    private fun Long?.positiveOrNull(): Long? = this?.takeIf { it > 0L }
}

/**
 * Last-resort identity for apps that expose no timestamp at all.
 *
 * A callback generation is accepted only outside a tiny duplicate-dispatch window. Stronger
 * metadata always bypasses this coalescing, so genuinely distinct rapid messages with different
 * timestamps or post times cannot be swallowed here. Recovery never invents a fresh event.
 */
class MessageEventFallbackTracker(
    private val duplicateWindowMs: Long = 120L,
    private val retentionMs: Long = 6 * 60 * 60 * 1000L,
    private val maxEntries: Int = 256
) {
    private data class State(
        val contentHash: Int,
        val signalFingerprint: MessageEventFingerprint?,
        val eventFingerprint: MessageEventFingerprint,
        val lastObservedAt: Long
    )

    private val states = LinkedHashMap<String, State>(16, 0.75f, true)
    private var fallbackSequence = 0L

    init {
        require(duplicateWindowMs >= 0L)
        require(retentionMs >= duplicateWindowMs)
        require(maxEntries > 0)
    }

    @Synchronized
    fun resolve(
        sourceKey: String,
        contentHash: Int,
        signals: MessageEventSignals,
        callbackGeneration: Long?,
        observedAt: Long,
        recovery: Boolean
    ): MessageEventFingerprint? {
        prune(observedAt)
        val previous = states[sourceKey]
        val strongFingerprint = MessageEventFingerprintResolver.resolve(signals)
        val elapsed = previous?.let { observedAt - it.lastObservedAt }

        if (strongFingerprint?.source == MessageEventFingerprintSource.MESSAGING_STYLE) {
            remember(sourceKey, State(contentHash, strongFingerprint, strongFingerprint, observedAt))
            return strongFingerprint
        }

        // Grouped social and messaging apps commonly rebuild already-active child notifications
        // when another child is added to the group. That refresh can replace both Notification.when
        // and StatusBarNotification.postTime even though the child's semantic payload is unchanged.
        // Neither timestamp is sufficient evidence of a new user-visible event in that case.
        // MessagingStyle is excluded because its latest-message timestamp is app-provided event
        // identity and must continue to distinguish repeated messages with identical visible text.
        if (previous != null &&
            previous.contentHash == contentHash &&
            strongFingerprint != null &&
            previous.signalFingerprint != strongFingerprint
        ) {
            val stableEvent = previous.eventFingerprint
            remember(sourceKey, State(contentHash, strongFingerprint, stableEvent, observedAt))
            return stableEvent
        }

        if (strongFingerprint?.source == MessageEventFingerprintSource.NOTIFICATION_WHEN) {
            val previousSignal = previous?.signalFingerprint
            val sameNotificationWhen = previousSignal?.source == MessageEventFingerprintSource.NOTIFICATION_WHEN &&
                    previousSignal.primaryValue == strongFingerprint.primaryValue
            if (!sameNotificationWhen) {
                remember(sourceKey, State(contentHash, strongFingerprint, strongFingerprint, observedAt))
                return strongFingerprint
            }
            val prior = requireNotNull(previous)

            if (previousSignal == strongFingerprint) {
                val stableEvent = prior.eventFingerprint
                remember(sourceKey, State(contentHash, strongFingerprint, stableEvent, observedAt))
                return stableEvent
            }

            // SystemUI and some apps repost the same notification after its initial floating
            // animation, changing only StatusBarNotification.postTime. Keeping the event stable
            // while its authoritative Notification.when and visible content are unchanged stops
            // the already-present Island from floating a second time.
            if (prior.contentHash == contentHash) {
                val stableEvent = prior.eventFingerprint
                remember(sourceKey, State(contentHash, strongFingerprint, stableEvent, observedAt))
                return stableEvent
            }

            if (recovery) return prior.eventFingerprint

            remember(sourceKey, State(contentHash, strongFingerprint, strongFingerprint, observedAt))
            return strongFingerprint
        }

        if (strongFingerprint != null && previous?.signalFingerprint != strongFingerprint) {
            remember(sourceKey, State(contentHash, strongFingerprint, strongFingerprint, observedAt))
            return strongFingerprint
        }

        if (recovery) return previous?.eventFingerprint ?: strongFingerprint

        val isImmediateDuplicate = previous != null &&
                previous.signalFingerprint == strongFingerprint &&
                previous.contentHash == contentHash &&
                elapsed != null && elapsed in 0L..duplicateWindowMs
        if (isImmediateDuplicate) {
            remember(sourceKey, previous.copy(lastObservedAt = observedAt))
            return previous.eventFingerprint
        }

        val fingerprint = MessageEventFingerprint(
            source = MessageEventFingerprintSource.CALLBACK_GENERATION,
            primaryValue = nextFallbackValue(callbackGeneration, previous),
            sourceScope = sourceKey.hashCode()
        )
        remember(sourceKey, State(contentHash, strongFingerprint, fingerprint, observedAt))
        return fingerprint
    }

    @Synchronized
    fun clear() {
        states.clear()
        fallbackSequence = 0L
    }

    @Synchronized
    fun prune(now: Long) {
        states.entries.removeIf { now - it.value.lastObservedAt > retentionMs }
    }

    private fun nextFallbackValue(callbackGeneration: Long?, previous: State?): Long {
        val supplied = callbackGeneration?.takeIf { it > 0L }
        val previousValue = previous?.eventFingerprint
            ?.takeIf { it.source == MessageEventFingerprintSource.CALLBACK_GENERATION }
            ?.primaryValue
        if (supplied != null && supplied != previousValue) return supplied

        do {
            fallbackSequence += 1L
        } while (fallbackSequence == previousValue)
        return fallbackSequence
    }

    private fun remember(sourceKey: String, state: State) {
        states[sourceKey] = state
        while (states.size > maxEntries) {
            states.entries.iterator().run {
                next()
                remove()
            }
        }
    }
}

data class MessagingEventSignals(
    val packageName: String,
    val isMessageNotificationType: Boolean = false,
    val isStandardNotificationType: Boolean = false,
    val hasMessageCategory: Boolean = false,
    val hasMessagingStyleTemplate: Boolean = false,
    val extractedMessageCount: Int = 0,
    val hasConversationShortcut: Boolean = false,
    val hasConversationLocus: Boolean = false,
    val hasMessagePersonMetadata: Boolean = false,
    val hasRemoteInputReply: Boolean = false,
    val hasEmailCategory: Boolean = false,
    val hasUsefulContent: Boolean = false
)

/** Messaging-like lifecycle detection; this does not alter display classification. */
fun isMessagingEvent(signals: MessagingEventSignals): Boolean {
    if (!signals.isMessageNotificationType && !signals.isStandardNotificationType) return false

    if (signals.isMessageNotificationType ||
        signals.hasMessageCategory ||
        signals.hasMessagingStyleTemplate ||
        signals.extractedMessageCount > 0 ||
        signals.hasConversationShortcut ||
        signals.hasConversationLocus ||
        (signals.hasRemoteInputReply && signals.hasMessagePersonMetadata && signals.hasUsefulContent)
    ) {
        return true
    }

    // Compatibility is intentionally narrow and affects only lifecycle/event handling. It does
    // not inspect localized text and does not suppress WhatsApp aggregate notifications.
    return signals.isStandardNotificationType &&
            signals.hasUsefulContent &&
            (signals.packageName in WHATSAPP_PACKAGES ||
                    (signals.packageName in GMAIL_PACKAGES && signals.hasEmailCategory))
}

private val WHATSAPP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")
private val GMAIL_PACKAGES = setOf("com.google.android.gm")
