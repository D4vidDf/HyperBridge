package com.d4viddf.hyperbridge.service

import com.d4viddf.hyperbridge.models.MessageEventFingerprint
import com.d4viddf.hyperbridge.models.MessageEventFingerprintSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpiredIslandRegistryTest {
    @Test
    fun identicalExpiredGenerationRemainsSuppressed() {
        val registry = ExpiredIslandRegistry(retentionMs = 1_000)
        registry.record(ExpiredIslandRecord("conversation", "source", 7, expiredAt = 100))

        assertEquals(ExpiredSourceDecision.SUPPRESS_IDENTICAL, registry.evaluate("source", 7, now = 200))
    }

    @Test
    fun changedGenerationBecomesEligibleAndClearsTombstone() {
        val registry = ExpiredIslandRegistry(retentionMs = 1_000)
        registry.record(ExpiredIslandRecord("conversation", "source", 7, expiredAt = 100))

        assertEquals(ExpiredSourceDecision.NEW_GENERATION, registry.evaluate("source", 8, now = 200))
        assertEquals(ExpiredSourceDecision.SUPPRESS_IDENTICAL, registry.evaluate("source", 7, now = 201))
        registry.acceptNewGeneration("source", 8)
        assertEquals(ExpiredSourceDecision.NOT_EXPIRED, registry.evaluate("source", 8, now = 201))
    }

    @Test
    fun sameTextNewMessageEventBypassesExpiredTombstone() {
        val registry = ExpiredIslandRegistry(retentionMs = 1_000)
        val firstEvent = messageEvent(100L)
        val secondEvent = messageEvent(200L)
        registry.record(
            ExpiredIslandRecord(
                "conversation",
                "source",
                sourceFingerprint = 7,
                expiredAt = 100,
                messageEventFingerprint = firstEvent
            )
        )

        assertEquals(
            ExpiredSourceDecision.NEW_GENERATION,
            registry.evaluate(
                "source",
                sourceFingerprint = 7,
                now = 200,
                messageEventFingerprint = secondEvent
            )
        )
        registry.acceptNewGeneration(
            "source",
            sourceFingerprint = 7,
            messageEventFingerprint = secondEvent
        )
        assertEquals(ExpiredSourceDecision.NOT_EXPIRED, registry.evaluate("source", 7, now = 201))
    }

    @Test
    fun identicalMessageEventStaysExpiredDespiteRenderingDrift() {
        val registry = ExpiredIslandRegistry(retentionMs = 1_000)
        val event = messageEvent(100L)
        registry.record(
            ExpiredIslandRecord(
                "conversation",
                "source",
                sourceFingerprint = 7,
                expiredAt = 100,
                messageEventFingerprint = event
            )
        )

        assertEquals(
            ExpiredSourceDecision.SUPPRESS_IDENTICAL,
            registry.evaluate(
                "source",
                sourceFingerprint = 8,
                now = 200,
                messageEventFingerprint = event
            )
        )
    }

    @Test
    fun identicalExpiredEventStaysSuppressedAcrossSourceKeyReplacement() {
        val registry = ExpiredIslandRegistry(retentionMs = 1_000)
        val event = messageEvent(100L)
        registry.record(ExpiredIslandRecord("conversation", "old-source", 7, 100, event))

        assertEquals(
            ExpiredSourceDecision.SUPPRESS_IDENTICAL,
            registry.evaluate("new-source", 8, 200, event, logicalId = "conversation")
        )
    }

    @Test
    fun genuinelyNewEventClearsLogicalTombstoneAfterReplacementPost() {
        val registry = ExpiredIslandRegistry(retentionMs = 1_000)
        registry.record(ExpiredIslandRecord("conversation", "old-source", 7, 100, messageEvent(100L)))
        val newEvent = messageEvent(200L)

        assertEquals(
            ExpiredSourceDecision.NEW_GENERATION,
            registry.evaluate("new-source", 8, 200, newEvent, logicalId = "conversation")
        )
        registry.acceptNewGeneration("new-source", 8, newEvent, logicalId = "conversation")
        assertEquals(
            ExpiredSourceDecision.NOT_EXPIRED,
            registry.evaluate("new-source", 8, 201, newEvent, logicalId = "conversation")
        )
    }

    @Test
    fun tombstonesExpireAndRemainBounded() {
        val registry = ExpiredIslandRegistry(maxEntries = 1, retentionMs = 100)
        registry.record(ExpiredIslandRecord("one", "source-one", 1, expiredAt = 0))
        registry.record(ExpiredIslandRecord("two", "source-two", 2, expiredAt = 10))
        assertEquals(1, registry.size())
        registry.prune(now = 111)
        assertEquals(0, registry.size())
    }

    @Test
    fun staleTimeoutCannotDeleteNewerUpdate() {
        assertFalse(IslandTimeoutPolicy.isCurrent(2, 42, scheduledGeneration = 1, scheduledBridgeId = 42))
        assertTrue(IslandTimeoutPolicy.isCurrent(2, 42, scheduledGeneration = 2, scheduledBridgeId = 42))
    }

    @Test
    fun timeoutDurationUsesConfiguredSecondsWithoutOneMinuteCap() {
        assertEquals(5_000L, IslandTimeoutPolicy.durationMillis(5))
        assertEquals(120_000L, IslandTimeoutPolicy.durationMillis(120))
    }

    @Test
    fun disabledTimeoutDoesNotScheduleAutoHide() {
        assertEquals(null, IslandTimeoutPolicy.durationMillis(null))
        assertEquals(null, IslandTimeoutPolicy.durationMillis(0))
    }

    private fun messageEvent(timestamp: Long): MessageEventFingerprint {
        return MessageEventFingerprint(
            source = MessageEventFingerprintSource.MESSAGING_STYLE,
            primaryValue = timestamp,
            messageCount = 1
        )
    }
}
