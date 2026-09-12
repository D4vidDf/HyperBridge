package com.d4viddf.hyperbridge.service.message

import com.d4viddf.hyperbridge.models.MessageEventFingerprint
import com.d4viddf.hyperbridge.models.MessageEventFingerprintSource
import com.d4viddf.hyperbridge.models.NotificationType
import kotlin.math.abs

enum class MessageSourceQuality(val rank: Int) {
    AGGREGATE_SUMMARY(0),
    RICH_NON_SUMMARY(1),
    DIRECT_MESSAGING_STYLE(2)
}

data class MessagePresentationSource(
    val sourceKey: String,
    val packageName: String,
    val proposedLogicalId: String,
    val identitySource: String,
    val notificationType: NotificationType,
    val groupKey: String?,
    val isGroupSummary: Boolean,
    val hasMessagingStyle: Boolean,
    val eventFingerprint: MessageEventFingerprint?,
    val notificationTag: String? = null,
    val sourcePostTime: Long = 0L,
    val contentFingerprint: Int? = null,
    val isEmailCategory: Boolean = false
) {
    val quality: MessageSourceQuality
        get() = when {
            isGroupSummary -> MessageSourceQuality.AGGREGATE_SUMMARY
            hasMessagingStyle -> MessageSourceQuality.DIRECT_MESSAGING_STYLE
            else -> MessageSourceQuality.RICH_NON_SUMMARY
        }
}

data class MessagePresentationResolution(
    val logicalId: String,
    val primarySourceKey: String,
    val identitySource: String,
    val quality: MessageSourceQuality,
    val primaryQuality: MessageSourceQuality,
    val isAlias: Boolean,
    val primaryChanged: Boolean,
    val shouldPresent: Boolean,
    val eventFingerprint: MessageEventFingerprint?
)

data class MessageSourceRemoval(
    val logicalId: String,
    val familyEnded: Boolean,
    val removedPrimary: Boolean,
    val fallbackSourceKey: String? = null
)

/**
 * Owns cross-source identity for one user-visible messaging presentation.
 *
 * Android source keys remain aliases. Cross-identity merging is deliberately limited to sources
 * in the same non-blank Android group with the same structural event timestamp, where at least
 * one side is a summary/slot-style companion. Visible title or message text is never consulted.
 */
class MessagePresentationFamilyTracker {
    private data class Family(
        val logicalId: String,
        val sources: LinkedHashMap<String, MessagePresentationSource>,
        var primarySourceKey: String,
        var eventFingerprint: MessageEventFingerprint?
    )

    private val families = LinkedHashMap<String, Family>()
    private val sourceIndex = HashMap<String, String>()

    @Synchronized
    fun resolve(source: MessagePresentationSource): MessagePresentationResolution {
        var excludedLogicalId: String? = null
        var existingFamily = sourceIndex[source.sourceKey]?.let(families::get)
        val previousSource = existingFamily?.sources?.get(source.sourceKey)
        if (existingFamily != null &&
            existingFamily.primarySourceKey != source.sourceKey &&
            !sameSourceEvent(previousSource, source)
        ) {
            // A reusable summary/companion slot has advanced to another event while the richer
            // child for the prior event remains. Detach only that alias; the old family survives.
            excludedLogicalId = existingFamily.logicalId
            existingFamily.sources.remove(source.sourceKey)
            sourceIndex.remove(source.sourceKey)
            existingFamily = null
        }
        val family = existingFamily
            ?: families[source.proposedLogicalId]?.takeUnless { it.logicalId == excludedLogicalId }
            ?: findCrossSourceFamily(source, excludedLogicalId)
            ?: Family(
                logicalId = availableLogicalId(source, excludedLogicalId),
                sources = linkedMapOf(),
                primarySourceKey = source.sourceKey,
                eventFingerprint = source.eventFingerprint
            ).also { families[it.logicalId] = it }

        val priorPrimaryKey = family.primarySourceKey
        val priorPrimary = family.sources[priorPrimaryKey]
        family.sources[source.sourceKey] = source
        sourceIndex[source.sourceKey] = family.logicalId

        val selectedPrimary = family.sources.values.maxWithOrNull(primaryComparator(priorPrimaryKey))
            ?: source
        family.primarySourceKey = selectedPrimary.sourceKey
        val primaryChanged = priorPrimary != null && priorPrimary.sourceKey != selectedPrimary.sourceKey
        val inputIsPrimary = selectedPrimary.sourceKey == source.sourceKey

        if (priorPrimary == null || (inputIsPrimary && !sameCrossSourceEvent(
                family.eventFingerprint,
                source.eventFingerprint
            ))) {
            family.eventFingerprint = source.eventFingerprint
        }

        return MessagePresentationResolution(
            logicalId = family.logicalId,
            primarySourceKey = selectedPrimary.sourceKey,
            identitySource = source.identitySource,
            quality = source.quality,
            primaryQuality = selectedPrimary.quality,
            isAlias = !inputIsPrimary,
            primaryChanged = primaryChanged,
            shouldPresent = inputIsPrimary,
            eventFingerprint = if (inputIsPrimary) family.eventFingerprint else source.eventFingerprint
        )
    }

    @Synchronized
    fun removeSource(sourceKey: String): MessageSourceRemoval? {
        val logicalId = sourceIndex.remove(sourceKey) ?: return null
        val family = families[logicalId] ?: return null
        val removedPrimary = family.primarySourceKey == sourceKey
        family.sources.remove(sourceKey)
        if (family.sources.isEmpty()) {
            families.remove(logicalId)
            return MessageSourceRemoval(logicalId, familyEnded = true, removedPrimary = removedPrimary)
        }

        if (removedPrimary) {
            val fallback = family.sources.values.maxWithOrNull(primaryComparator(null))
                ?: error("message family must retain a source")
            family.primarySourceKey = fallback.sourceKey
            family.eventFingerprint = fallback.eventFingerprint
        }
        return MessageSourceRemoval(
            logicalId = logicalId,
            familyEnded = false,
            removedPrimary = removedPrimary,
            fallbackSourceKey = family.primarySourceKey
        )
    }

    @Synchronized
    fun logicalIdForSource(sourceKey: String): String? = sourceIndex[sourceKey]

    @Synchronized
    fun isPrimarySource(logicalId: String, sourceKey: String): Boolean =
        families[logicalId]?.primarySourceKey == sourceKey

    @Synchronized
    fun end(logicalId: String) {
        val removed = families.remove(logicalId) ?: return
        removed.sources.keys.forEach(sourceIndex::remove)
    }

    @Synchronized
    fun clear() {
        families.clear()
        sourceIndex.clear()
    }

    @Synchronized
    fun sourceCount(logicalId: String): Int = families[logicalId]?.sources?.size ?: 0

    private fun findCrossSourceFamily(
        source: MessagePresentationSource,
        excludedLogicalId: String? = null
    ): Family? {
        val groupKey = source.groupKey?.takeIf { it.isNotBlank() } ?: return null
        return families.values.filter { family ->
            family.logicalId != excludedLogicalId && family.sources.values.any { candidate ->
                candidate.packageName == source.packageName &&
                        candidate.groupKey == groupKey &&
                        companionRelationship(candidate, source) &&
                        samePresentationEvent(candidate, source)
            }
        }.singleOrNull()
    }

    private fun availableLogicalId(
        source: MessagePresentationSource,
        excludedLogicalId: String?
    ): String {
        if (source.proposedLogicalId !in families && source.proposedLogicalId != excludedLogicalId) {
            return source.proposedLogicalId
        }
        val eventSeed = source.eventFingerprint?.stableHash ?: source.sourceKey.hashCode()
        var candidate = "${source.proposedLogicalId}:event:${eventSeed.toUInt().toString(16)}"
        var suffix = 0
        while (candidate in families) {
            suffix += 1
            candidate = "${source.proposedLogicalId}:event:${eventSeed.toUInt().toString(16)}:$suffix"
        }
        return candidate
    }

    private fun companionRelationship(
        first: MessagePresentationSource,
        second: MessagePresentationSource
    ): Boolean {
        return first.isGroupSummary || second.isGroupSummary ||
                first.identitySource == "notification-slot" ||
                second.identitySource == "notification-slot"
    }

    private fun sameCrossSourceEvent(
        first: MessageEventFingerprint?,
        second: MessageEventFingerprint?
    ): Boolean {
        if (first == null || second == null) return false
        if (first == second) return true
        if (first.source == MessageEventFingerprintSource.CALLBACK_GENERATION ||
            second.source == MessageEventFingerprintSource.CALLBACK_GENERATION
        ) {
            return false
        }
        return first.primaryValue == second.primaryValue
    }

    private fun sameSourceEvent(
        previous: MessagePresentationSource?,
        current: MessagePresentationSource
    ): Boolean {
        if (previous == null) return false
        if (sameCrossSourceEvent(previous.eventFingerprint, current.eventFingerprint)) return true
        return previous.sourceKey == current.sourceKey &&
                previous.sourcePostTime == current.sourcePostTime &&
                previous.contentFingerprint != null &&
                previous.contentFingerprint == current.contentFingerprint
    }

    private fun samePresentationEvent(
        first: MessagePresentationSource,
        second: MessagePresentationSource
    ): Boolean {
        if (sameCrossSourceEvent(first.eventFingerprint, second.eventFingerprint)) return true
        return isGmailEmailCompanion(first, second)
    }

    private fun isGmailEmailCompanion(
        first: MessagePresentationSource,
        second: MessagePresentationSource
    ): Boolean {
        if (first.packageName !in GMAIL_PACKAGES || second.packageName !in GMAIL_PACKAGES) return false
        if (!first.isEmailCategory || !second.isEmailCategory) return false
        if (first.isGroupSummary == second.isGroupSummary) return false
        if (first.notificationTag.isNullOrBlank() || first.notificationTag != second.notificationTag) return false
        if (first.sourcePostTime <= 0L || second.sourcePostTime <= 0L) return false
        if (abs(first.sourcePostTime - second.sourcePostTime) > GMAIL_COMPANION_POST_WINDOW_MS) return false
        return first.contentFingerprint != null && first.contentFingerprint == second.contentFingerprint
    }

    private fun primaryComparator(currentPrimaryKey: String?): Comparator<MessagePresentationSource> {
        return compareBy<MessagePresentationSource> { it.quality.rank }
            .thenBy { if (it.notificationType == NotificationType.MESSAGE) 1 else 0 }
            .thenBy { if (it.sourceKey == currentPrimaryKey) 1 else 0 }
    }

    private companion object {
        val GMAIL_PACKAGES = setOf("com.google.android.gm")
        const val GMAIL_COMPANION_POST_WINDOW_MS = 250L
    }
}

/** Evidence-backed coalescing for Gmail's summary-first, child-immediately-after topology. */
object MessagePresentationTimingPolicy {
    const val GMAIL_SUMMARY_GRACE_MS = 200L

    fun initialSummaryDelayMs(
        packageName: String,
        isGroupSummary: Boolean,
        quality: MessageSourceQuality,
        isEmailCategory: Boolean
    ): Long = if (
        packageName == "com.google.android.gm" &&
        isGroupSummary &&
        isEmailCategory &&
        quality == MessageSourceQuality.AGGREGATE_SUMMARY
    ) {
        GMAIL_SUMMARY_GRACE_MS
    } else {
        0L
    }
}
