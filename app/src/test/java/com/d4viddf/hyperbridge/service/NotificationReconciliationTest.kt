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
}
