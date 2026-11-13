package com.d4viddf.hyperbridge.service

import org.junit.Assert.assertEquals
import org.junit.Test

class RenderedJsonNormalizerTest {
    @Test
    fun newAndUpdatePresentationFlagsDoNotChangeSemanticPayload() {
        val newJson = """{"title":"Alice","text":"hello","islandFirstFloat":true,"reopen":true}"""
        val updateJson = """{"title":"Alice","text":"hello","islandFirstFloat":false}"""
        val newHash = RenderedJsonNormalizer.normalize(newJson).hashCode()
        val updateHash = RenderedJsonNormalizer.normalize(updateJson).hashCode()

        assertEquals(newHash, updateHash)
        val decision = IslandUpdateResolver.decide(
            logicalId = "conversation",
            candidateBridgeId = 99,
            contentHash = updateHash,
            previous = PreviousIslandPresentation("conversation", 42, newHash),
            presentationReason = IslandPresentationReason.CONTENT_UPDATE
        )
        assertEquals(IslandPresentationKind.UNCHANGED, decision.kind)
        assertEquals(42, decision.bridgeId)
    }

    @Test
    fun generatedBridgeAndPictureIdsDoNotChangeSemanticPayload() {
        val first = """{"business":"bridge_42","picture":"pic_42","action":"act_42_0","text":"hello"}"""
        val replacement = """{"business":"bridge_-1500000000","picture":"pic_-1500000000","action":"act_-1500000000_0","text":"hello"}"""

        assertEquals(RenderedJsonNormalizer.normalize(first), RenderedJsonNormalizer.normalize(replacement))
    }
}
