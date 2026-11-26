package com.d4viddf.hyperbridge.service.call

import com.d4viddf.hyperbridge.models.CallStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallStageVisibilityPolicyTest {
    @Test
    fun mapsEveryCallStateToItsUserFacingStage() {
        assertEquals(CallStage.INCOMING, CallStageVisibilityPolicy.stageFor(CallState.INCOMING_RINGING))
        assertEquals(CallStage.OUTGOING, CallStageVisibilityPolicy.stageFor(CallState.OUTGOING_CALLING))
        assertEquals(CallStage.OUTGOING, CallStageVisibilityPolicy.stageFor(CallState.OUTGOING_RINGING))
        assertEquals(CallStage.OUTGOING, CallStageVisibilityPolicy.stageFor(CallState.CONNECTING))
        assertEquals(CallStage.ACTIVE, CallStageVisibilityPolicy.stageFor(CallState.ACTIVE))
        assertEquals(null, CallStageVisibilityPolicy.stageFor(CallState.ENDED))
    }

    @Test
    fun incomingCanBeDisabledIndependently() {
        val enabled = setOf(CallStage.OUTGOING, CallStage.ACTIVE)

        assertFalse(CallStageVisibilityPolicy.isVisible(enabled, CallState.INCOMING_RINGING))
        assertTrue(CallStageVisibilityPolicy.isVisible(enabled, CallState.OUTGOING_CALLING))
        assertTrue(CallStageVisibilityPolicy.isVisible(enabled, CallState.ACTIVE))
    }

    @Test
    fun outgoingCoversCallingRingingAndConnectingOnly() {
        val enabled = setOf(CallStage.OUTGOING)

        assertTrue(CallStageVisibilityPolicy.isVisible(enabled, CallState.OUTGOING_CALLING))
        assertTrue(CallStageVisibilityPolicy.isVisible(enabled, CallState.OUTGOING_RINGING))
        assertTrue(CallStageVisibilityPolicy.isVisible(enabled, CallState.CONNECTING))
        assertFalse(CallStageVisibilityPolicy.isVisible(enabled, CallState.INCOMING_RINGING))
        assertFalse(CallStageVisibilityPolicy.isVisible(enabled, CallState.ACTIVE))
    }

    @Test
    fun activeCanBeEnabledWithoutPreConnectedStages() {
        val enabled = setOf(CallStage.ACTIVE)

        assertFalse(CallStageVisibilityPolicy.isVisible(enabled, CallState.OUTGOING_CALLING))
        assertTrue(CallStageVisibilityPolicy.isVisible(enabled, CallState.ACTIVE))
    }

    @Test
    fun endedAndEmptySelectionNeverPresent() {
        assertFalse(CallStageVisibilityPolicy.isVisible(CallStage.entries.toSet(), CallState.ENDED))
        CallState.entries.forEach { state ->
            assertFalse(CallStageVisibilityPolicy.isVisible(emptySet(), state))
        }
    }
}
