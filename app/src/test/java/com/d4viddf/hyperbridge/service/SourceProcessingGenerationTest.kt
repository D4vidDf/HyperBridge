package com.d4viddf.hyperbridge.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceProcessingGenerationTest {
    @Test
    fun staleGenerationCannotPostAfterNewerCallback() {
        val generations = SourceProcessingGeneration()
        val old = generations.next("source", SourceCandidateQuality.USABLE)
        val latest = generations.next("source", SourceCandidateQuality.USABLE)

        assertFalse(generations.isCurrent("source", old))
        assertTrue(generations.isCurrent("source", latest))
    }

    @Test
    fun usefulCallbackIsNotInvalidatedByLaterSparseCallback() {
        val generations = SourceProcessingGeneration()
        val initiallyUseful = generations.next("source", SourceCandidateQuality.USABLE)
        val initiallySparse = generations.next("source", SourceCandidateQuality.SPARSE)

        assertTrue(initiallySparse > initiallyUseful)
        assertTrue(generations.isCurrent("source", initiallyUseful))
        assertFalse(generations.isCurrent("source", initiallySparse))
    }

    @Test
    fun sparseCallbackCanPromoteAfterRefresh() {
        val generations = SourceProcessingGeneration()
        val useful = generations.next("source", SourceCandidateQuality.USABLE)
        val sparse = generations.next("source", SourceCandidateQuality.SPARSE)

        generations.consider("source", sparse, SourceCandidateQuality.USABLE)

        assertFalse(generations.isCurrent("source", useful))
        assertTrue(generations.isCurrent("source", sparse))
    }

    @Test
    fun olderRefreshedCandidateCannotDisplaceNewerEquallyUsefulCandidate() {
        val generations = SourceProcessingGeneration()
        val old = generations.next("source", SourceCandidateQuality.SPARSE)
        val latest = generations.next("source", SourceCandidateQuality.USABLE)

        generations.consider("source", old, SourceCandidateQuality.USABLE)

        assertFalse(generations.isCurrent("source", old))
        assertTrue(generations.isCurrent("source", latest))
    }

    @Test
    fun generationsAreTrackedIndependentlyPerSource() {
        val generations = SourceProcessingGeneration()
        val first = generations.next("first", SourceCandidateQuality.USABLE)
        val second = generations.next("second", SourceCandidateQuality.USABLE)

        assertTrue(generations.isCurrent("first", first))
        assertTrue(generations.isCurrent("second", second))
    }

    @Test
    fun removalDoesNotBlindlyInvalidateQueuedWork() {
        val generations = SourceProcessingGeneration()
        val posted = generations.next("source", SourceCandidateQuality.USABLE)

        // Removal is recorded by the lifecycle layer; it does not call remove on this owner.
        assertTrue(generations.isCurrent("source", posted))
    }

    @Test
    fun fiveCallbackStormRetainsNewestUsableCandidate() {
        val generations = SourceProcessingGeneration()
        val claims = listOf(
            generations.next("slot", SourceCandidateQuality.USABLE),
            generations.next("slot", SourceCandidateQuality.SPARSE),
            generations.next("slot", SourceCandidateQuality.SPARSE),
            generations.next("slot", SourceCandidateQuality.USABLE),
            generations.next("slot", SourceCandidateQuality.SPARSE)
        )

        assertTrue(generations.isCurrent("slot", claims[3]))
        assertFalse(generations.isCurrent("slot", claims[4]))
    }

    @Test
    fun usableOwnerCannotBeStarvedBySparseCallbacksWhileWaitingForLifecycleLock() {
        val generations = SourceProcessingGeneration()
        val waitingUsable = generations.next("slot", SourceCandidateQuality.USABLE)

        repeat(20) {
            generations.next("slot", SourceCandidateQuality.SPARSE)
        }

        assertTrue(generations.isCurrent("slot", waitingUsable))
    }

    @Test
    fun finishingStaleWorkCannotClearNewerOwner() {
        val generations = SourceProcessingGeneration()
        val old = generations.next("slot", SourceCandidateQuality.USABLE)
        val latest = generations.next("slot", SourceCandidateQuality.USABLE)

        generations.finish("slot", old)

        assertTrue(generations.isCurrent("slot", latest))
    }
}
