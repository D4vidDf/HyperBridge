package com.d4viddf.hyperbridge.service.message

import com.d4viddf.hyperbridge.models.MessageEventFingerprintSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageEventFingerprintResolverTest {
    @Test
    fun latestMessageTimestampAndCountArePreferred() {
        val fingerprint = MessageEventFingerprintResolver.resolve(
            MessageEventSignals(
                latestMessageTimestamp = 100L,
                messageCount = 2,
                notificationWhen = 200L,
                sourcePostTime = 300L
            )
        )

        assertEquals(MessageEventFingerprintSource.MESSAGING_STYLE, fingerprint?.source)
        assertEquals(100L, fingerprint?.primaryValue)
        assertEquals(2, fingerprint?.messageCount)
    }

    @Test
    fun notificationWhenThenSourcePostTimeAreFallbacks() {
        val notificationWhen = MessageEventFingerprintResolver.resolve(
            MessageEventSignals(notificationWhen = 200L, sourcePostTime = 300L)
        )
        val postTime = MessageEventFingerprintResolver.resolve(
            MessageEventSignals(
                notificationWhen = 200L,
                notificationWhenIsReliable = false,
                sourcePostTime = 300L
            )
        )

        assertEquals(MessageEventFingerprintSource.NOTIFICATION_WHEN, notificationWhen?.source)
        assertEquals(200L, notificationWhen?.primaryValue)
        assertEquals(300L, notificationWhen?.secondaryValue)
        assertEquals(MessageEventFingerprintSource.SOURCE_POST_TIME, postTime?.source)
        assertEquals(300L, postTime?.primaryValue)
    }

    @Test
    fun constantNotificationWhenDoesNotMaskChangedSourcePostTime() {
        val first = MessageEventFingerprintResolver.resolve(
            MessageEventSignals(notificationWhen = 200L, sourcePostTime = 300L)
        )
        val second = MessageEventFingerprintResolver.resolve(
            MessageEventSignals(notificationWhen = 200L, sourcePostTime = 400L)
        )

        assertNotEquals(first, second)
    }

    @Test
    fun duplicateCallbackBurstReusesOneFallbackFingerprint() {
        val tracker = MessageEventFallbackTracker(duplicateWindowMs = 120L)
        val noMetadata = MessageEventSignals()

        val first = tracker.resolve("source", 7, noMetadata, 1L, observedAt = 1_000L, recovery = false)
        val duplicate = tracker.resolve("source", 7, noMetadata, 2L, observedAt = 1_080L, recovery = false)
        val burstTail = tracker.resolve("source", 7, noMetadata, 3L, observedAt = 1_160L, recovery = false)
        val laterEvent = tracker.resolve("source", 7, noMetadata, 4L, observedAt = 1_281L, recovery = false)

        assertEquals(first, duplicate)
        assertEquals(first, burstTail)
        assertNotEquals(first, laterEvent)
    }

    @Test
    fun unchangedPostTimeUsesCallbackFallbackOnlyAfterDuplicateWindow() {
        val tracker = MessageEventFallbackTracker(duplicateWindowMs = 120L)
        val postTimeOnly = MessageEventSignals(sourcePostTime = 300L)

        val first = tracker.resolve("source", 7, postTimeOnly, 1L, 1_000L, recovery = false)
        val duplicate = tracker.resolve("source", 7, postTimeOnly, 2L, 1_080L, recovery = false)
        val laterCallback = tracker.resolve("source", 7, postTimeOnly, 3L, 1_201L, recovery = false)

        assertEquals(first, duplicate)
        assertNotEquals(first, laterCallback)
        assertEquals(MessageEventFingerprintSource.CALLBACK_GENERATION, laterCallback?.source)
    }

    @Test
    fun identicalReliableEventMetadataRemainsStableOutsideCallbackWindow() {
        val tracker = MessageEventFallbackTracker(duplicateWindowMs = 120L)
        val metadata = MessageEventSignals(notificationWhen = 200L, sourcePostTime = 300L)

        val first = tracker.resolve("source", 7, metadata, 1L, 1_000L, recovery = false)
        val repost = tracker.resolve("source", 7, metadata, 2L, 5_000L, recovery = false)

        assertEquals(first, repost)
        assertEquals(MessageEventFingerprintSource.NOTIFICATION_WHEN, repost?.source)
    }

    @Test
    fun sameContentPostTimeRefreshWithSameWhenRemainsOneEvent() {
        val tracker = MessageEventFallbackTracker(duplicateWindowMs = 120L)

        val first = tracker.resolve(
            "source", 7, MessageEventSignals(notificationWhen = 200L, sourcePostTime = 300L),
            1L, 1_000L, recovery = false
        )
        val immediateRepost = tracker.resolve(
            "source", 7, MessageEventSignals(notificationWhen = 200L, sourcePostTime = 400L),
            2L, 1_080L, recovery = false
        )
        val burstTail = tracker.resolve(
            "source", 7, MessageEventSignals(notificationWhen = 200L, sourcePostTime = 400L),
            3L, 1_160L, recovery = false
        )
        val laterEvent = tracker.resolve(
            "source", 7, MessageEventSignals(notificationWhen = 200L, sourcePostTime = 500L),
            4L, 1_281L, recovery = false
        )

        assertEquals(first, immediateRepost)
        assertEquals(first, burstTail)
        assertEquals(first, laterEvent)
    }

    @Test
    fun changedContentWithReusedNotificationWhenIsANewEvent() {
        val tracker = MessageEventFallbackTracker(duplicateWindowMs = 120L)

        val first = tracker.resolve(
            "conversation", 7, MessageEventSignals(notificationWhen = 200L, sourcePostTime = 300L),
            1L, 1_000L, recovery = false
        )
        val changed = tracker.resolve(
            "conversation", 8, MessageEventSignals(notificationWhen = 200L, sourcePostTime = 400L),
            2L, 5_000L, recovery = false
        )

        assertNotEquals(first, changed)
    }

    @Test
    fun coupledChildRepostWithRefreshedWhenRemainsTheOriginalEvent() {
        val tracker = MessageEventFallbackTracker(duplicateWindowMs = 120L)

        val originalUserX = tracker.resolve(
            "conversation-x", 7,
            MessageEventSignals(notificationWhen = 200L, sourcePostTime = 300L),
            1L, 1_000L, recovery = false
        )
        val userXRepostedWhileUserYArrives = tracker.resolve(
            "conversation-x", 7,
            MessageEventSignals(notificationWhen = 400L, sourcePostTime = 500L),
            2L, 5_000L, recovery = false
        )

        assertEquals(originalUserX, userXRepostedWhileUserYArrives)
    }

    @Test
    fun coupledChildRepostWithOnlyRefreshedPostTimeRemainsTheOriginalEvent() {
        val tracker = MessageEventFallbackTracker(duplicateWindowMs = 120L)

        val originalUserX = tracker.resolve(
            "conversation-x", 7, MessageEventSignals(sourcePostTime = 300L),
            1L, 1_000L, recovery = false
        )
        val userXRepostedWhileUserYArrives = tracker.resolve(
            "conversation-x", 7, MessageEventSignals(sourcePostTime = 500L),
            2L, 5_000L, recovery = false
        )

        assertEquals(originalUserX, userXRepostedWhileUserYArrives)
    }

    @Test
    fun changedContentWithOnlyRefreshedPostTimeIsANewEvent() {
        val tracker = MessageEventFallbackTracker(duplicateWindowMs = 120L)

        val original = tracker.resolve(
            "conversation", 7, MessageEventSignals(sourcePostTime = 300L),
            1L, 1_000L, recovery = false
        )
        val changed = tracker.resolve(
            "conversation", 8, MessageEventSignals(sourcePostTime = 500L),
            2L, 5_000L, recovery = false
        )

        assertNotEquals(original, changed)
    }

    @Test
    fun rapidMessagesWithRealMetadataNeverEnterCallbackDebounce() {
        val tracker = MessageEventFallbackTracker(duplicateWindowMs = 120L)
        val first = tracker.resolve(
            "source",
            7,
            MessageEventSignals(latestMessageTimestamp = 100L, messageCount = 1),
            callbackGeneration = 1L,
            observedAt = 1_000L,
            recovery = false
        )
        val second = tracker.resolve(
            "source",
            7,
            MessageEventSignals(latestMessageTimestamp = 101L, messageCount = 2),
            callbackGeneration = 2L,
            observedAt = 1_001L,
            recovery = false
        )

        assertNotEquals(first, second)
    }

    @Test
    fun recoveryDoesNotInventCallbackIdentity() {
        val tracker = MessageEventFallbackTracker()

        assertNull(
            tracker.resolve(
                "source",
                7,
                MessageEventSignals(),
                callbackGeneration = 1L,
                observedAt = 1_000L,
                recovery = true
            )
        )
    }

    @Test
    fun messagingSignalsStayNarrowForStandardWhatsappFallback() {
        assertTrue(
            isMessagingEvent(
                MessagingEventSignals(
                    packageName = "com.whatsapp",
                    isStandardNotificationType = true,
                    hasUsefulContent = true
                )
            )
        )
        assertFalse(
            isMessagingEvent(
                MessagingEventSignals(
                    packageName = "com.whatsapp",
                    isStandardNotificationType = false,
                    hasUsefulContent = true
                )
            )
        )
        assertFalse(
            isMessagingEvent(
                MessagingEventSignals(
                    packageName = "com.example",
                    isStandardNotificationType = true,
                    hasUsefulContent = true,
                    hasRemoteInputReply = true
                )
            )
        )
        assertTrue(
            isMessagingEvent(
                MessagingEventSignals(
                    packageName = "com.example",
                    isStandardNotificationType = true,
                    hasUsefulContent = true,
                    hasMessagePersonMetadata = true,
                    hasRemoteInputReply = true
                )
            )
        )
        assertFalse(
            isMessagingEvent(
                MessagingEventSignals(
                    packageName = "com.example.calls",
                    isStandardNotificationType = false,
                    hasConversationShortcut = true,
                    hasUsefulContent = true
                )
            )
        )
        assertTrue(
            isMessagingEvent(
                MessagingEventSignals(
                    packageName = "com.google.android.gm",
                    isStandardNotificationType = true,
                    hasEmailCategory = true,
                    hasUsefulContent = true
                )
            )
        )
        assertFalse(
            isMessagingEvent(
                MessagingEventSignals(
                    packageName = "com.google.android.gm",
                    isStandardNotificationType = true,
                    hasEmailCategory = false,
                    hasUsefulContent = true
                )
            )
        )
    }
}
