package com.d4viddf.hyperbridge.service

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationReconciliationTest {
    @Test
    fun identifiesStaleOrphanAndMissingStateIndependently() {
        val plan = NotificationReconciliation.plan(
            ReconciliationInput(
                activeLogicalSources = mapOf("live" to "source-live", "stale" to "source-gone"),
                currentSourceKeys = setOf("source-live", "source-new"),
                trackedBridgeIds = setOf(10),
                postedBridgeIds = setOf(10, 11),
                recoverableSourceKeys = setOf("source-live", "source-new"),
                mappedSourceKeys = setOf("source-live")
            )
        )

        assertEquals(setOf("stale"), plan.staleLogicalIds)
        assertEquals(setOf(11), plan.orphanBridgeIds)
        assertEquals(setOf("source-new"), plan.missingSourceKeys)
    }

    @Test
    fun expiredEphemeralSourceIsNotAReconciliationCandidate() {
        val plan = NotificationReconciliation.plan(
            ReconciliationInput(
                activeLogicalSources = emptyMap(),
                currentSourceKeys = setOf("message-in-shade"),
                trackedBridgeIds = emptySet(),
                postedBridgeIds = emptySet(),
                recoverableSourceKeys = emptySet(),
                mappedSourceKeys = emptySet()
            )
        )

        assertEquals(emptySet<String>(), plan.missingSourceKeys)
    }

    @Test
    fun ongoingSourceCanBeAReconciliationCandidate() {
        val plan = NotificationReconciliation.plan(
            ReconciliationInput(
                activeLogicalSources = emptyMap(),
                currentSourceKeys = setOf("active-call"),
                trackedBridgeIds = emptySet(),
                postedBridgeIds = emptySet(),
                recoverableSourceKeys = setOf("active-call"),
                mappedSourceKeys = emptySet()
            )
        )

        assertEquals(setOf("active-call"), plan.missingSourceKeys)
    }

    /** Regression for #323 / #278: a message island is keyed by a logical id, not by its shade key. */
    @Test
    fun logicalIdThatIsNotAShadeKeyIsNotStaleWhileItsSourceIsPosted() {
        val plan = NotificationReconciliation.plan(
            ReconciliationInput(
                activeLogicalSources = mapOf(
                    "message:com.whatsapp:shortcut:abc" to "0|com.whatsapp|1|null|10123",
                    "call:com.whatsapp:1" to "0|com.whatsapp|2|call|10123"
                ),
                currentSourceKeys = setOf("0|com.whatsapp|1|null|10123", "0|com.whatsapp|2|call|10123"),
                trackedBridgeIds = emptySet(),
                postedBridgeIds = emptySet(),
                recoverableSourceKeys = emptySet(),
                mappedSourceKeys = emptySet()
            )
        )

        assertEquals(emptySet<String>(), plan.staleLogicalIds)
    }

    @Test
    fun aliasSourceKeepsLogicalIslandAliveAfterPrimarySourceIsGone() {
        val plan = NotificationReconciliation.plan(
            ReconciliationInput(
                activeLogicalSources = mapOf("message:pkg:slot:1" to "source-old"),
                currentSourceKeys = setOf("source-replacement"),
                trackedBridgeIds = emptySet(),
                postedBridgeIds = emptySet(),
                recoverableSourceKeys = emptySet(),
                mappedSourceKeys = emptySet(),
                activeLogicalSourceAliases = mapOf("message:pkg:slot:1" to setOf("source-old", "source-replacement"))
            )
        )

        assertEquals(emptySet<String>(), plan.staleLogicalIds)
    }

    @Test
    fun logicalIslandIsStaleOnlyWhenEveryAliasLeftTheShade() {
        val plan = NotificationReconciliation.plan(
            ReconciliationInput(
                activeLogicalSources = mapOf("message:pkg:slot:1" to "source-old"),
                currentSourceKeys = setOf("unrelated"),
                trackedBridgeIds = emptySet(),
                postedBridgeIds = emptySet(),
                recoverableSourceKeys = emptySet(),
                mappedSourceKeys = emptySet(),
                activeLogicalSourceAliases = mapOf("message:pkg:slot:1" to setOf("source-old", "source-replacement"))
            )
        )

        assertEquals(setOf("message:pkg:slot:1"), plan.staleLogicalIds)
    }
}
