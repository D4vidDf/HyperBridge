package com.d4viddf.hyperbridge.service.message

import com.d4viddf.hyperbridge.models.MessageEventFingerprint
import com.d4viddf.hyperbridge.models.MessageEventFingerprintSource
import com.d4viddf.hyperbridge.models.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessagePresentationFamilyTrackerTest {
    @Test
    fun messageChildAndStandardSummaryBecomeOneFamily() {
        val tracker = MessagePresentationFamilyTracker()
        val child = tracker.resolve(child(event = 100L))
        val summary = tracker.resolve(summary(type = NotificationType.STANDARD, event = 100L))

        assertEquals(child.logicalId, summary.logicalId)
        assertTrue(summary.isAlias)
        assertFalse(summary.shouldPresent)
        assertEquals(2, tracker.sourceCount(child.logicalId))
    }

    @Test
    fun capturedGmailBigTextSummaryAndChildWithoutMessageMetadataBecomeOneFamily() {
        val tracker = MessagePresentationFamilyTracker()
        val summary = tracker.resolve(capturedGmailSource("summary", summary = true, postTime = 1_000L))
        val child = tracker.resolve(capturedGmailSource("child", summary = false, postTime = 1_001L))

        assertEquals(summary.logicalId, child.logicalId)
        assertTrue(child.primaryChanged)
        assertEquals(MessageSourceQuality.RICH_NON_SUMMARY, child.primaryQuality)
        assertEquals(2, tracker.sourceCount(child.logicalId))
        assertTrue(tracker.isPrimarySource(child.logicalId, "child"))
        assertFalse(tracker.isPrimarySource(child.logicalId, "summary"))
    }

    @Test
    fun onlyGmailAggregateSummaryGetsCoalescingGrace() {
        assertEquals(
            MessagePresentationTimingPolicy.GMAIL_SUMMARY_GRACE_MS,
            MessagePresentationTimingPolicy.initialSummaryDelayMs(
                "com.google.android.gm",
                isGroupSummary = true,
                quality = MessageSourceQuality.AGGREGATE_SUMMARY,
                isEmailCategory = true
            )
        )
        assertEquals(
            0L,
            MessagePresentationTimingPolicy.initialSummaryDelayMs(
                "com.google.android.gm",
                isGroupSummary = false,
                quality = MessageSourceQuality.RICH_NON_SUMMARY,
                isEmailCategory = true
            )
        )
        assertEquals(
            0L,
            MessagePresentationTimingPolicy.initialSummaryDelayMs(
                "com.whatsapp",
                isGroupSummary = true,
                quality = MessageSourceQuality.AGGREGATE_SUMMARY,
                isEmailCategory = false
            )
        )
        assertEquals(
            0L,
            MessagePresentationTimingPolicy.initialSummaryDelayMs(
                "com.google.android.gm",
                isGroupSummary = true,
                quality = MessageSourceQuality.AGGREGATE_SUMMARY,
                isEmailCategory = false
            )
        )
    }

    @Test
    fun capturedGmailPairDoesNotMergeAcrossAccountsOrPostGenerations() {
        val tracker = MessagePresentationFamilyTracker()
        val first = tracker.resolve(capturedGmailSource("summary-a", summary = true, postTime = 1_000L))
        val otherAccount = tracker.resolve(
            capturedGmailSource("child-b", summary = false, postTime = 1_001L, tag = "account-b")
        )
        val laterEmail = tracker.resolve(
            capturedGmailSource("child-c", summary = false, postTime = 1_500L)
        )

        assertNotEquals(first.logicalId, otherAccount.logicalId)
        assertNotEquals(first.logicalId, laterEmail.logicalId)
    }

    @Test
    fun messageChildAndMessageSummaryBecomeOneFamily() {
        val tracker = MessagePresentationFamilyTracker()
        val child = tracker.resolve(child(event = 100L))
        val summary = tracker.resolve(summary(type = NotificationType.MESSAGE, event = 100L))

        assertEquals(child.logicalId, summary.logicalId)
        assertFalse(summary.shouldPresent)
    }

    @Test
    fun summaryFirstIsPromotedByDirectChildWithoutSecondFamily() {
        val tracker = MessagePresentationFamilyTracker()
        val summary = tracker.resolve(summary(event = 100L))
        val child = tracker.resolve(child(event = 100L))

        assertEquals(summary.logicalId, child.logicalId)
        assertTrue(child.primaryChanged)
        assertTrue(child.shouldPresent)
        assertEquals(MessageSourceQuality.DIRECT_MESSAGING_STYLE, child.quality)
    }

    @Test
    fun childFirstKeepsLaterSummaryAsAlias() {
        val tracker = MessagePresentationFamilyTracker()
        val child = tracker.resolve(child(event = 100L))
        val summary = tracker.resolve(summary(event = 100L))

        assertEquals(child.logicalId, summary.logicalId)
        assertEquals("child", summary.primarySourceKey)
        assertTrue(summary.isAlias)
    }

    @Test
    fun summaryRemovalKeepsChildPresentation() {
        val tracker = MessagePresentationFamilyTracker()
        val logicalId = tracker.resolve(child(event = 100L)).logicalId
        tracker.resolve(summary(event = 100L))
        val removal = requireNotNull(tracker.removeSource("summary"))

        assertFalse(removal.familyEnded)
        assertFalse(removal.removedPrimary)
        assertEquals("child", removal.fallbackSourceKey)
        assertEquals(1, tracker.sourceCount(logicalId))
    }

    @Test
    fun childRemovalFallsBackToSummary() {
        val tracker = MessagePresentationFamilyTracker()
        val logicalId = tracker.resolve(summary(event = 100L)).logicalId
        tracker.resolve(child(event = 100L))
        val removal = requireNotNull(tracker.removeSource("child"))

        assertFalse(removal.familyEnded)
        assertTrue(removal.removedPrimary)
        assertEquals("summary", removal.fallbackSourceKey)
        assertEquals(logicalId, tracker.logicalIdForSource("summary"))
    }

    @Test
    fun twoDifferentEmailsAreNotMerged() {
        val tracker = MessagePresentationFamilyTracker()
        val first = tracker.resolve(child(source = "first", logical = "conversation:first", event = 100L))
        val second = tracker.resolve(child(source = "second", logical = "conversation:second", event = 200L))

        assertNotEquals(first.logicalId, second.logicalId)
    }

    @Test
    fun twoAccountsAreNotMerged() {
        val tracker = MessagePresentationFamilyTracker()
        val first = tracker.resolve(child(source = "first", logical = "conversation:first", group = "account-a", event = 100L))
        val second = tracker.resolve(summary(source = "second", logical = "slot:second", group = "account-b", event = 100L))

        assertNotEquals(first.logicalId, second.logicalId)
    }

    @Test
    fun sameTextCannotMergeSourcesWhenEventIdentityDiffers() {
        val tracker = MessagePresentationFamilyTracker()
        val first = tracker.resolve(child(source = "first", logical = "conversation:first", event = 100L))
        val second = tracker.resolve(summary(source = "second", logical = "slot:second", event = 101L))

        assertNotEquals(first.logicalId, second.logicalId)
    }

    @Test
    fun reusedSummarySlotForNextEmailDoesNotMergeDistinctChildren() {
        val tracker = MessagePresentationFamilyTracker()
        val first = tracker.resolve(child(source = "child-a", logical = "conversation:a", event = 100L))
        tracker.resolve(summary(event = 100L))

        val advancedSummary = tracker.resolve(summary(event = 200L))
        val second = tracker.resolve(child(source = "child-b", logical = "conversation:b", event = 200L))

        assertNotEquals(first.logicalId, advancedSummary.logicalId)
        assertEquals(advancedSummary.logicalId, second.logicalId)
        assertEquals(1, tracker.sourceCount(first.logicalId))
        assertEquals(2, tracker.sourceCount(second.logicalId))
    }

    @Test
    fun unrelatedStandardNotificationStaysOutsideMessagingFamilies() {
        val tracker = MessagePresentationFamilyTracker()
        val child = tracker.resolve(child(event = 100L))

        assertEquals(1, tracker.sourceCount(child.logicalId))
        assertEquals(null, tracker.logicalIdForSource("unrelated-standard"))
    }

    private fun child(
        source: String = "child",
        logical: String = "conversation:one",
        group: String = "gmail:account:inbox",
        event: Long
    ) = MessagePresentationSource(
        sourceKey = source,
        packageName = "com.google.android.gm",
        proposedLogicalId = logical,
        identitySource = "shortcut",
        notificationType = NotificationType.MESSAGE,
        groupKey = group,
        isGroupSummary = false,
        hasMessagingStyle = true,
        eventFingerprint = fingerprint(event, MessageEventFingerprintSource.MESSAGING_STYLE)
    )

    private fun summary(
        source: String = "summary",
        logical: String = "slot:summary",
        group: String = "gmail:account:inbox",
        type: NotificationType = NotificationType.STANDARD,
        event: Long
    ) = MessagePresentationSource(
        sourceKey = source,
        packageName = "com.google.android.gm",
        proposedLogicalId = logical,
        identitySource = "notification-slot",
        notificationType = type,
        groupKey = group,
        isGroupSummary = true,
        hasMessagingStyle = false,
        eventFingerprint = fingerprint(event, MessageEventFingerprintSource.NOTIFICATION_WHEN)
    )

    private fun fingerprint(value: Long, source: MessageEventFingerprintSource) =
        MessageEventFingerprint(source, value)

    private fun capturedGmailSource(
        source: String,
        summary: Boolean,
        postTime: Long,
        tag: String = "account-a"
    ) = MessagePresentationSource(
        sourceKey = source,
        packageName = "com.google.android.gm",
        proposedLogicalId = "slot:$source",
        identitySource = "notification-slot",
        notificationType = NotificationType.STANDARD,
        groupKey = "gmail:account:inbox",
        isGroupSummary = summary,
        hasMessagingStyle = false,
        eventFingerprint = null,
        notificationTag = tag,
        sourcePostTime = postTime,
        contentFingerprint = 0x5a17,
        isEmailCategory = true
    )
}
