package com.d4viddf.hyperbridge.service

import com.d4viddf.hyperbridge.models.MessageEventFingerprint
import com.d4viddf.hyperbridge.models.MessageEventFingerprintSource
import com.d4viddf.hyperbridge.models.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandUpdateResolverTest {
    @Test
    fun newLogicalIdAlwaysCreatesNewIsland() {
        val decision = IslandUpdateResolver.decide(
            logicalId = "conversation-a",
            candidateBridgeId = 42,
            contentHash = 123,
            previous = null
        )

        assertEquals(IslandPresentationKind.NEW, decision.kind)
        assertEquals(42, decision.bridgeId)
        assertFalse(decision.onlyAlertOnce)
        assertEquals(IslandPresentationReason.NEW_EVENT, decision.presentationReason)
        assertFalse(decision.cancelBeforeNotify)
    }

    @Test
    fun changedContentUpdatesExistingIslandSilently() {
        val decision = IslandUpdateResolver.decide(
            logicalId = "conversation-a",
            candidateBridgeId = 99,
            contentHash = 456,
            previous = PreviousIslandPresentation("conversation-a", 42, 123),
            presentationReason = IslandPresentationReason.CONTENT_UPDATE
        )

        assertEquals(IslandPresentationKind.UPDATE, decision.kind)
        assertEquals(42, decision.bridgeId)
        assertTrue(decision.onlyAlertOnce)
        assertEquals(IslandPresentationReason.CONTENT_UPDATE, decision.presentationReason)
        assertFalse(decision.cancelBeforeNotify)
    }

    @Test
    fun unchangedContentDoesNotNotify() {
        val decision = IslandUpdateResolver.decide(
            logicalId = "conversation-a",
            candidateBridgeId = 99,
            contentHash = 123,
            previous = PreviousIslandPresentation("conversation-a", 42, 123)
        )

        assertEquals(IslandPresentationKind.UNCHANGED, decision.kind)
        assertEquals(42, decision.bridgeId)
        assertTrue(decision.onlyAlertOnce)
        assertFalse(decision.cancelBeforeNotify)
    }

    @Test
    fun sourcePromotionAllowsAutoExpand() {
        val decision = IslandUpdateResolver.decide(
            logicalId = "conversation-a",
            candidateBridgeId = 42,
            contentHash = 123,
            previous = null,
            presentationReason = IslandPresentationReason.SOURCE_PROMOTION
        )

        assertEquals(IslandPresentationKind.NEW, decision.kind)
        assertFalse(decision.onlyAlertOnce)
        assertEquals(IslandPresentationReason.SOURCE_PROMOTION, decision.presentationReason)
        assertFalse(decision.cancelBeforeNotify)
    }

    @Test
    fun restorePreservesOriginalBridgeIdSilently() {
        val decision = IslandUpdateResolver.decide(
            logicalId = "conversation-a",
            candidateBridgeId = 42,
            contentHash = 123,
            previous = null,
            presentationReason = IslandPresentationReason.RESTORE
        )

        assertEquals(IslandPresentationKind.NEW, decision.kind)
        assertEquals(42, decision.bridgeId)
        assertTrue(decision.onlyAlertOnce)
        assertEquals(IslandPresentationReason.RESTORE, decision.presentationReason)
        assertFalse(decision.cancelBeforeNotify)
    }

    @Test
    fun messageBurstsUseNewBridgeIdsToForceReopen() {
        val first = IslandUpdateResolver.decide(
            logicalId = "conversation-a",
            candidateBridgeId = 42,
            contentHash = "hello".hashCode(),
            previous = null,
            notificationType = NotificationType.MESSAGE
        )
        val second = IslandUpdateResolver.decide(
            logicalId = "conversation-a",
            candidateBridgeId = 999,
            contentHash = "where are you?".hashCode(),
            previous = PreviousIslandPresentation("conversation-a", first.bridgeId, "hello".hashCode()),
            notificationType = NotificationType.MESSAGE
        )
        val third = IslandUpdateResolver.decide(
            logicalId = "conversation-a",
            candidateBridgeId = 1000,
            contentHash = "HELLO".hashCode(),
            previous = PreviousIslandPresentation("conversation-a", second.bridgeId, "where are you?".hashCode()),
            notificationType = NotificationType.MESSAGE
        )

        assertEquals(999, second.bridgeId)
        assertEquals(1000, third.bridgeId)
        assertEquals(IslandPresentationKind.NEW, second.kind)
        assertEquals(IslandPresentationKind.NEW, third.kind)
        assertTrue(second.presentationReason.mayAutoExpand)
        assertTrue(third.presentationReason.mayAutoExpand)
        assertTrue(second.cancelBeforeNotify)
        assertTrue(third.cancelBeforeNotify)
    }

    @Test
    fun identicalMessageRepostRemainsUnchanged() {
        val event = messageEvent(timestamp = 200L, messageCount = 2)
        val decision = IslandUpdateResolver.decide(
            logicalId = "conversation-a",
            candidateBridgeId = 999,
            contentHash = "hello".hashCode(),
            previous = PreviousIslandPresentation(
                "conversation-a",
                42,
                "hello".hashCode(),
                messageEventFingerprint = event
            ),
            notificationType = NotificationType.MESSAGE,
            messageEventFingerprint = event
        )

        assertEquals(IslandPresentationKind.UNCHANGED, decision.kind)
        assertEquals(42, decision.bridgeId)
        assertFalse(decision.cancelBeforeNotify)
    }

    @Test
    fun sameMessageRenderingDriftIsASilentInPlaceUpdate() {
        val event = messageEvent(timestamp = 200L, messageCount = 2)
        val decision = IslandUpdateResolver.decide(
            logicalId = "conversation-a",
            candidateBridgeId = 999,
            contentHash = "same message, refreshed artwork".hashCode(),
            previous = PreviousIslandPresentation(
                "conversation-a",
                42,
                "same message".hashCode(),
                messageEventFingerprint = event
            ),
            notificationType = NotificationType.MESSAGE,
            messageEventFingerprint = event
        )

        assertEquals(IslandPresentationKind.UPDATE, decision.kind)
        assertEquals(42, decision.bridgeId)
        assertTrue(decision.onlyAlertOnce)
        assertFalse(decision.cancelBeforeNotify)
    }

    @Test
    fun sameContentAndNotificationWhenIgnoresRefreshedPostTime() {
        val previousEvent = MessageEventFingerprint(
            source = MessageEventFingerprintSource.NOTIFICATION_WHEN,
            primaryValue = 200L,
            secondaryValue = 300L
        )
        val repostEvent = previousEvent.copy(secondaryValue = 400L)
        val decision = IslandUpdateResolver.decide(
            logicalId = "conversation-a",
            candidateBridgeId = 999,
            contentHash = "hello".hashCode(),
            previous = PreviousIslandPresentation(
                "conversation-a",
                42,
                "hello".hashCode(),
                messageEventFingerprint = previousEvent
            ),
            notificationType = NotificationType.MESSAGE,
            messageEventFingerprint = repostEvent
        )

        assertEquals(IslandPresentationKind.UNCHANGED, decision.kind)
        assertEquals(42, decision.bridgeId)
        assertFalse(decision.cancelBeforeNotify)
    }

    @Test
    fun identicalTextWithDifferentMessageEventCreatesFreshGeneration() {
        val firstEvent = messageEvent(timestamp = 100L, messageCount = 1)
        val secondEvent = messageEvent(timestamp = 200L, messageCount = 2)
        val decision = IslandUpdateResolver.decide(
            logicalId = "conversation-a",
            candidateBridgeId = 999,
            contentHash = "hello".hashCode(),
            previous = PreviousIslandPresentation(
                "conversation-a",
                42,
                "hello".hashCode(),
                messageEventFingerprint = firstEvent
            ),
            notificationType = NotificationType.MESSAGE,
            messageEventFingerprint = secondEvent
        )

        assertEquals(IslandPresentationKind.NEW, decision.kind)
        assertEquals(999, decision.bridgeId)
        assertEquals(IslandPresentationReason.NEW_EVENT, decision.presentationReason)
        assertFalse(decision.onlyAlertOnce)
        assertTrue(decision.cancelBeforeNotify)
    }

    @Test
    fun messageLikeStandardNotificationUsesMessageReplacementLifecycle() {
        val decision = IslandUpdateResolver.decide(
            logicalId = "conversation-a",
            candidateBridgeId = 999,
            contentHash = "hello".hashCode(),
            previous = PreviousIslandPresentation(
                "conversation-a",
                42,
                "hello".hashCode(),
                messageEventFingerprint = messageEvent(100L, 1)
            ),
            notificationType = NotificationType.STANDARD,
            isMessagingEvent = true,
            messageEventFingerprint = messageEvent(200L, 2)
        )

        assertEquals(IslandPresentationKind.NEW, decision.kind)
        assertTrue(decision.cancelBeforeNotify)
    }

    @Test
    fun messageBridgeIdsChangeByGenerationAndAvoidReservedRanges() {
        val first = MessageBridgeIdPolicy.candidate("conversation-a", 2L, 123)
        val second = MessageBridgeIdPolicy.candidate("conversation-a", 3L, 456)

        assertTrue(first < -1_000_000_000)
        assertTrue(second < -1_000_000_000)
        assertTrue(first != second)
        assertTrue(first != PermanentIslandManager.PERMANENT_BRIDGE_ID)
    }

    @Test
    fun messageBridgeIdIncludesEventIdentityEvenForSameGenerationAndContent() {
        val first = MessageBridgeIdPolicy.candidate(
            logicalId = "conversation-a",
            generation = 2L,
            contentHash = 123,
            messageEventFingerprint = messageEvent(100L, 1)
        )
        val second = MessageBridgeIdPolicy.candidate(
            logicalId = "conversation-a",
            generation = 2L,
            contentHash = 123,
            messageEventFingerprint = messageEvent(200L, 2)
        )

        assertTrue(first != second)
    }

    @Test
    fun internalReplacementMarkerIsConsumedOnceAndExpires() {
        val registry = InternalBridgeReplacementRegistry(ttlMs = 100L, maxEntries = 2)
        registry.mark(42, "conversation-a", generation = 2L, now = 1_000L)

        assertEquals("conversation-a", registry.consume(42, now = 1_050L)?.logicalId)
        assertEquals(null, registry.consume(42, now = 1_050L))

        registry.mark(43, "conversation-a", generation = 3L, now = 2_000L)
        assertEquals(null, registry.consume(43, now = 2_101L))
    }

    @Test
    fun permanentIslandIsOnlyDesiredWhenNoRealOrNativeIslandExists() {
        assertTrue(PermanentIslandVisibilityPolicy.desiredActive(true, 0, false, false, false))
        assertFalse(PermanentIslandVisibilityPolicy.desiredActive(true, 1, false, false, false))
        assertFalse(PermanentIslandVisibilityPolicy.desiredActive(true, 0, true, false, false))
        assertFalse(PermanentIslandVisibilityPolicy.desiredActive(true, 0, false, true, true))
        assertFalse(PermanentIslandVisibilityPolicy.desiredActive(false, 0, false, false, false))
    }

    private fun messageEvent(timestamp: Long, messageCount: Int): MessageEventFingerprint {
        return MessageEventFingerprint(
            source = MessageEventFingerprintSource.MESSAGING_STYLE,
            primaryValue = timestamp,
            messageCount = messageCount
        )
    }
}
