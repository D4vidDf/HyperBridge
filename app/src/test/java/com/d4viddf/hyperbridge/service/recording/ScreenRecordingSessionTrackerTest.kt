package com.d4viddf.hyperbridge.service.recording

import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.service.IslandPresentationKind
import com.d4viddf.hyperbridge.service.IslandUpdateResolver
import com.d4viddf.hyperbridge.service.NotificationLifecyclePolicy
import com.d4viddf.hyperbridge.service.PreviousIslandPresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenRecordingSessionTrackerTest {
    @Test
    fun duplicateCallbacksPreserveSessionAndDoNotRepostForClockPassage() {
        val tracker = ScreenRecordingSessionTracker()
        val first = tracker.resolve(input(postTime = 1_000L))
        val duplicate = tracker.resolve(input(postTime = 1_000L))
        val firstHash = ScreenRecordingSemanticFingerprint.compute(first)
        val duplicateHash = ScreenRecordingSemanticFingerprint.compute(duplicate)

        assertEquals(first.logicalId, duplicate.logicalId)
        assertEquals(firstHash, duplicateHash)
        val decision = IslandUpdateResolver.decide(
            logicalId = duplicate.logicalId,
            candidateBridgeId = 99,
            contentHash = duplicateHash,
            previous = PreviousIslandPresentation(first.logicalId, 42, firstHash),
            notificationType = NotificationType.SCREEN_RECORDING
        )
        assertEquals(IslandPresentationKind.UNCHANGED, decision.kind)
        assertEquals(42, decision.bridgeId)
    }

    @Test
    fun removalClearsSessionAndReusedKeyWithNewPostTimeCreatesNewSession() {
        val tracker = ScreenRecordingSessionTracker()
        val first = tracker.resolve(input(postTime = 1_000L))
        tracker.end(first.logicalId)

        assertNull(tracker.logicalIdForSource(SOURCE_KEY))
        val second = tracker.resolve(input(postTime = 2_000L))
        assertNotEquals(first.logicalId, second.logicalId)
        assertEquals(2_000L, second.startedAt)
    }

    @Test
    fun staleRemovalCannotClearReusedSourceKey() {
        val tracker = ScreenRecordingSessionTracker()
        tracker.resolve(input(postTime = 1_000L))
        val current = tracker.resolve(input(postTime = 2_000L))

        assertNull(tracker.endSource(SOURCE_KEY, sourcePostTime = 1_000L))
        assertEquals(current.logicalId, tracker.logicalIdForSource(SOURCE_KEY))
        assertEquals(current.logicalId, tracker.endSource(SOURCE_KEY, sourcePostTime = 2_000L))
        assertNull(tracker.logicalIdForSource(SOURCE_KEY))
    }

    @Test
    fun capabilityChangeUpdatesSameSessionSemantics() {
        val tracker = ScreenRecordingSessionTracker()
        val visualOnly = tracker.resolve(input(postTime = 1_000L, canStop = false))
        val actionable = tracker.resolve(input(postTime = 1_000L, canStop = true))

        assertEquals(visualOnly.logicalId, actionable.logicalId)
        assertNotEquals(
            ScreenRecordingSemanticFingerprint.compute(visualOnly),
            ScreenRecordingSemanticFingerprint.compute(actionable)
        )
        assertFalse(actionable.capabilities.canPause)
        assertFalse(actionable.capabilities.canResume)
    }

    @Test
    fun recordingUsesImmediateSourceLifecycleAndCannotMirrorSource() {
        assertTrue(NotificationLifecyclePolicy.dismissesWithSource(NotificationType.SCREEN_RECORDING))
        assertFalse(NotificationLifecyclePolicy.canIntentionallyMirrorSource(NotificationType.SCREEN_RECORDING))
        assertFalse(NotificationLifecyclePolicy.dismissesWithSource(NotificationType.TIMER))
        assertTrue(NotificationLifecyclePolicy.canIntentionallyMirrorSource(NotificationType.STANDARD))
    }

    @Test
    fun savedRecordingSourceIsDismissedOnlyWhenItsBridgeIsOpened() {
        assertTrue(
            NotificationLifecyclePolicy.shouldDismissSourceAfterBridgeRemoval(
                dismissSourceOnContentClick = true,
                wasContentClick = true
            )
        )
        assertFalse(
            NotificationLifecyclePolicy.shouldDismissSourceAfterBridgeRemoval(
                dismissSourceOnContentClick = true,
                wasContentClick = false
            )
        )
        assertFalse(
            NotificationLifecyclePolicy.shouldDismissSourceAfterBridgeRemoval(
                dismissSourceOnContentClick = false,
                wasContentClick = true
            )
        )
    }

    @Test
    fun savedRecordingReusesIdentityOnlyWithinTheSameSourceGeneration() {
        val first = ScreenRecordingSavedIdentity.logicalId(SAVED_SOURCE_KEY, 1_000L)
        val duplicate = ScreenRecordingSavedIdentity.logicalId(SAVED_SOURCE_KEY, 1_000L)
        val nextRecording = ScreenRecordingSavedIdentity.logicalId(SAVED_SOURCE_KEY, 2_000L)

        assertEquals(first, duplicate)
        assertNotEquals(first, nextRecording)
    }

    private fun input(postTime: Long, canStop: Boolean = true) = ScreenRecordingSessionInput(
        sourceKey = SOURCE_KEY,
        packageName = ScreenRecordingClassifier.PACKAGE_NAME,
        sourcePostTime = postTime,
        capabilities = ScreenRecordingCapabilities(canStop = canStop)
    )

    private companion object {
        const val SOURCE_KEY = "0|com.miui.screenrecorder|110|null|10331"
        const val SAVED_SOURCE_KEY = "0|com.miui.screenrecorder|111|null|10331"
    }
}
