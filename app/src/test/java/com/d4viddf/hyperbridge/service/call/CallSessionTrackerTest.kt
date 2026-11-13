package com.d4viddf.hyperbridge.service.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CallSessionTrackerTest {
    @Test
    fun ongoingPresentationImmediatelyKeepsOutgoingCallPreConnected() {
        val session = CallSessionTracker().resolve(
            input(classification(CallState.OUTGOING_CALLING, presentation = CallPresentationType.ONGOING), 10_000L)
        )

        assertEquals(CallState.OUTGOING_CALLING, session.state)
        assertNull(session.connectedAt)
        assertNull(CallTimerPolicy.connectedAtForTimer(session))
    }

    @Test
    fun initialOngoingChronometerDoesNotStartTimer() {
        val session = CallSessionTracker().resolve(
            input(
                classification(
                    CallState.OUTGOING_CALLING,
                    CallActiveEvidence.CHRONOMETER_PRESENT,
                    presentation = CallPresentationType.ONGOING
                ),
                now = 10_000L,
                showsChronometer = true,
                base = 9_000L
            )
        )

        assertEquals(CallState.OUTGOING_CALLING, session.state)
        assertNull(session.connectedAt)
        assertNull(CallTimerPolicy.connectedAtForTimer(session))
    }

    @Test
    fun repeatedIdenticalDialingChronometerDoesNotBecomeActive() {
        val tracker = CallSessionTracker()
        tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, CallActiveEvidence.CHRONOMETER_PRESENT),
                10_000L,
                showsChronometer = true,
                base = 9_000L
            )
        )
        val repeated = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, CallActiveEvidence.CHRONOMETER_PRESENT),
                12_000L,
                showsChronometer = true,
                base = 9_000L
            )
        )

        assertEquals(CallState.OUTGOING_CALLING, repeated.state)
        assertNull(repeated.connectedAt)
    }

    @Test
    fun chronometerAppearanceStartsActiveCallAtSourceBase() {
        val tracker = CallSessionTracker()
        val calling = tracker.resolve(input(classification(CallState.OUTGOING_CALLING), 10_000L))
        val active = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, CallActiveEvidence.CHRONOMETER_PRESENT),
                12_000L,
                showsChronometer = true,
                base = 11_500L
            )
        )

        assertEquals(calling.logicalCallId, active.logicalCallId)
        assertEquals(CallState.ACTIVE, active.state)
        assertEquals(11_500L, active.connectedAt)
        assertEquals(ConnectedAtSource.SOURCE_CHRONOMETER, active.connectedAtSource)
        assertEquals(CallActiveEvidence.CHRONOMETER_STARTED, active.activeEvidence)
        assertEquals(11_500L, CallTimerPolicy.connectedAtForTimer(active))
    }

    @Test
    fun materialChronometerBaseResetStartsActiveCallAtNewBase() {
        val tracker = CallSessionTracker()
        tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, CallActiveEvidence.CHRONOMETER_PRESENT),
                10_000L,
                showsChronometer = true,
                base = 9_000L
            )
        )
        val active = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, CallActiveEvidence.CHRONOMETER_PRESENT),
                15_000L,
                showsChronometer = true,
                base = 14_000L
            )
        )

        assertEquals(CallState.ACTIVE, active.state)
        assertEquals(14_000L, active.connectedAt)
        assertEquals(CallActiveEvidence.CHRONOMETER_BASE_RESET, active.activeEvidence)
    }

    @Test
    fun smallChronometerBaseNoiseDoesNotStartTimer() {
        val tracker = CallSessionTracker()
        tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, CallActiveEvidence.CHRONOMETER_PRESENT),
                10_000L,
                showsChronometer = true,
                base = 9_000L
            )
        )
        val noisy = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, CallActiveEvidence.CHRONOMETER_PRESENT),
                11_000L,
                showsChronometer = true,
                base = 9_500L
            )
        )

        assertEquals(CallState.OUTGOING_CALLING, noisy.state)
        assertNull(noisy.connectedAt)
    }

    @Test
    fun incomingAnswerDisappearanceStartsActiveAtObservation() {
        val tracker = CallSessionTracker()
        tracker.resolve(
            input(classification(CallState.INCOMING_RINGING, hasAnswer = true, hasHangUp = true), 20_000L)
        )
        val answered = tracker.resolve(
            input(classification(CallState.OUTGOING_CALLING, hasHangUp = true), 23_000L)
        )

        assertEquals(CallState.ACTIVE, answered.state)
        assertEquals(23_000L, answered.connectedAt)
        assertEquals(ConnectedAtSource.OBSERVED_CONNECTION_TRANSITION, answered.connectedAtSource)
        assertEquals(CallActiveEvidence.INCOMING_ANSWERED, answered.activeEvidence)
    }

    @Test
    fun connectedControlAppearanceIsSupportingActiveEvidence() {
        val tracker = CallSessionTracker()
        tracker.resolve(input(classification(CallState.OUTGOING_CALLING), 20_000L))
        val connected = tracker.resolve(
            input(classification(CallState.OUTGOING_CALLING, hasConnectedControl = true), 21_000L)
        )

        assertEquals(CallState.ACTIVE, connected.state)
        assertEquals(21_000L, connected.connectedAt)
        assertEquals(CallActiveEvidence.CONNECTED_ACTIONS_APPEARED, connected.activeEvidence)
    }

    @Test
    fun connectedControlPresentFromFirstCallbackDoesNotStartTimer() {
        val tracker = CallSessionTracker()
        tracker.resolve(input(classification(CallState.OUTGOING_CALLING, hasConnectedControl = true), 20_000L))
        val repeated = tracker.resolve(
            input(classification(CallState.OUTGOING_CALLING, hasConnectedControl = true), 21_000L)
        )

        assertEquals(CallState.OUTGOING_CALLING, repeated.state)
        assertNull(repeated.connectedAt)
    }

    @Test
    fun activeNoisyCallbacksNeverRegressOrResetConnectedAt() {
        val tracker = CallSessionTracker()
        tracker.resolve(input(classification(CallState.OUTGOING_CALLING), 10_000L))
        val active = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, CallActiveEvidence.CHRONOMETER_PRESENT),
                12_000L,
                showsChronometer = true,
                base = 11_500L
            )
        )
        val noisy = tracker.resolve(
            input(classification(CallState.OUTGOING_CALLING, presentation = CallPresentationType.ONGOING), 15_000L)
        )

        assertEquals(CallState.ACTIVE, noisy.state)
        assertEquals(active.connectedAt, noisy.connectedAt)
        assertEquals(active.connectedAtSource, noisy.connectedAtSource)
    }

    @Test
    fun lockScreenParticipantRewriteDoesNotRegressActiveCall() {
        val tracker = CallSessionTracker()
        tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING),
                10_000L,
                participant = "visible-person"
            )
        )
        val active = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, CallActiveEvidence.CHRONOMETER_PRESENT),
                12_000L,
                participant = "visible-person",
                showsChronometer = true,
                base = 11_500L
            )
        )

        val locked = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING),
                15_000L,
                participant = "lock-screen-redacted"
            )
        )

        assertEquals(active.logicalCallId, locked.logicalCallId)
        assertEquals(CallState.ACTIVE, locked.state)
        assertEquals(active.connectedAt, locked.connectedAt)
        assertEquals(active.connectedAtSource, locked.connectedAtSource)
    }

    @Test
    fun ongoingPresentationDoesNotRegressConnectingState() {
        val tracker = CallSessionTracker()
        tracker.resolve(input(classification(CallState.CONNECTING), 10_000L))
        val repeated = tracker.resolve(
            input(classification(CallState.OUTGOING_CALLING, presentation = CallPresentationType.ONGOING), 11_000L)
        )

        assertEquals(CallState.CONNECTING, repeated.state)
        assertNull(repeated.connectedAt)
    }

    @Test
    fun activeCandidateWithoutBoundaryEvidenceRemainsPreConnected() {
        val first = CallSessionTracker().resolve(input(classification(CallState.ACTIVE), 16_000L))
        assertEquals(CallState.CONNECTING, first.state)
        assertNull(first.connectedAt)

        val tracker = CallSessionTracker()
        val previous = tracker.resolve(input(classification(CallState.OUTGOING_RINGING), 17_000L))
        val candidate = tracker.resolve(input(classification(CallState.ACTIVE), 18_000L))
        assertEquals(CallState.OUTGOING_RINGING, previous.state)
        assertEquals(CallState.OUTGOING_RINGING, candidate.state)
        assertNull(candidate.connectedAt)
    }

    @Test
    fun sourceReplacementWhileCallingKeepsLogicalCallAndBridgeIdentity() {
        val tracker = CallSessionTracker()
        val first = tracker.resolve(input(classification(CallState.OUTGOING_CALLING), 30_000L, sourceKey = "old"))
        tracker.markSourceRemoved("old", 30_100L)
        val replacement = tracker.resolve(
            input(classification(CallState.OUTGOING_CALLING), 30_600L, sourceKey = "new", notificationId = 99)
        )

        assertEquals(first.logicalCallId, replacement.logicalCallId)
        assertEquals(first.logicalCallId.hashCode(), replacement.logicalCallId.hashCode())
        assertTrue(replacement.sourceReplacement)
        assertEquals(CallState.OUTGOING_CALLING, replacement.state)
    }

    @Test
    fun replacementPostedBeforeRemovalRebindsOnUniqueParticipantIdentity() {
        val tracker = CallSessionTracker()
        val first = tracker.resolve(input(classification(CallState.OUTGOING_CALLING), 35_000L, sourceKey = "old"))
        val replacement = tracker.resolve(
            input(classification(CallState.OUTGOING_CALLING), 35_500L, sourceKey = "new", notificationId = 99)
        )

        assertEquals(first.logicalCallId, replacement.logicalCallId)
        assertTrue(replacement.sourceReplacement)
        assertNull(tracker.markSourceRemoved("old", 35_600L))
    }

    @Test
    fun outgoingReplacementPostedBeforeRemovalRebindsWithoutParticipantMetadata() {
        val tracker = CallSessionTracker()
        val first = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING),
                36_000L,
                sourceKey = "dialing",
                participant = null
            )
        )
        val replacement = tracker.resolve(
            input(
                classification(CallState.OUTGOING_RINGING),
                36_400L,
                sourceKey = "ringing",
                participant = null,
                notificationId = 99
            )
        )

        assertEquals(first.logicalCallId, replacement.logicalCallId)
        assertTrue(replacement.sourceReplacement)
        assertEquals(CallState.OUTGOING_RINGING, replacement.state)
        assertNull(tracker.markSourceRemoved("dialing", 36_500L))
    }

    @Test
    fun incomingCallDoesNotMergeWithRecentOutgoingCallWhenParticipantIsMissing() {
        val tracker = CallSessionTracker()
        val outgoing = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING),
                37_000L,
                sourceKey = "outgoing",
                participant = null
            )
        )
        val incoming = tracker.resolve(
            input(
                classification(CallState.INCOMING_RINGING, hasAnswer = true, hasHangUp = true),
                37_400L,
                sourceKey = "incoming",
                participant = null,
                notificationId = 100
            )
        )

        assertNotEquals(outgoing.logicalCallId, incoming.logicalCallId)
    }

    @Test
    fun missingParticipantDoesNotMergeWhenOutgoingCandidateIsAmbiguous() {
        val tracker = CallSessionTracker()
        val first = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING),
                38_000L,
                sourceKey = "first",
                participant = "person-a"
            )
        )
        val second = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING),
                38_100L,
                sourceKey = "second",
                participant = "person-b",
                notificationId = 101
            )
        )
        val unknown = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING),
                38_200L,
                sourceKey = "unknown",
                participant = null,
                notificationId = 102
            )
        )

        assertNotEquals(first.logicalCallId, unknown.logicalCallId)
        assertNotEquals(second.logicalCallId, unknown.logicalCallId)
    }

    @Test
    fun replacementsNearOldOneSecondBoundaryKeepLogicalCall() {
        listOf(900L, 1_000L, 1_100L, 1_500L).forEach { elapsed ->
            val tracker = CallSessionTracker()
            val first = tracker.resolve(input(classification(CallState.OUTGOING_CALLING), 40_000L, sourceKey = "old"))
            tracker.markSourceRemoved("old", 40_000L)
            val replacement = tracker.resolve(
                input(
                    classification(CallState.OUTGOING_CALLING),
                    40_000L + elapsed,
                    sourceKey = "new",
                    notificationId = elapsed.toInt()
                )
            )

            assertEquals("elapsed=$elapsed", first.logicalCallId, replacement.logicalCallId)
        }
        assertTrue(CallReplacementPolicy.REMOVAL_DELAY_MS > CallReplacementPolicy.MATCH_GRACE_MS)
    }

    @Test
    fun answerEvidenceAfterSourceReplacementUpdatesSameLogicalCall() {
        val tracker = CallSessionTracker()
        val first = tracker.resolve(input(classification(CallState.OUTGOING_CALLING), 50_000L, sourceKey = "old"))
        tracker.markSourceRemoved("old", 50_100L)
        tracker.resolve(
            input(classification(CallState.OUTGOING_CALLING), 50_600L, sourceKey = "new", notificationId = 99)
        )
        val active = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, CallActiveEvidence.CHRONOMETER_PRESENT),
                52_000L,
                sourceKey = "new",
                notificationId = 99,
                showsChronometer = true,
                base = 51_900L
            )
        )

        assertEquals(first.logicalCallId, active.logicalCallId)
        assertEquals(CallState.ACTIVE, active.state)
        assertEquals(51_900L, active.connectedAt)
    }

    @Test
    fun videoSourceReplacementChronometerAloneStaysPreConnected() {
        val tracker = CallSessionTracker()
        val first = tracker.resolve(
            input(classification(CallState.OUTGOING_CALLING), 55_000L, sourceKey = "dialing", isVideo = true)
        )
        tracker.markSourceRemoved("dialing", 55_100L)
        val ringing = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, CallActiveEvidence.CHRONOMETER_PRESENT),
                55_500L,
                sourceKey = "ringing",
                notificationId = 99,
                showsChronometer = true,
                base = 55_000L,
                isVideo = true
            )
        )

        assertEquals(first.logicalCallId, ringing.logicalCallId)
        assertEquals(CallState.OUTGOING_CALLING, ringing.state)
        assertNull(ringing.connectedAt)
        assertEquals("source-replacement-signals-not-connection", ringing.transitionReason)
    }

    @Test
    fun videoSourceReplacementConnectedControlAloneStaysPreConnected() {
        val tracker = CallSessionTracker()
        val first = tracker.resolve(
            input(classification(CallState.OUTGOING_CALLING), 56_000L, sourceKey = "dialing", isVideo = true)
        )
        tracker.markSourceRemoved("dialing", 56_100L)
        val ringing = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, hasConnectedControl = true),
                56_500L,
                sourceKey = "ringing",
                notificationId = 99,
                isVideo = true
            )
        )

        assertEquals(first.logicalCallId, ringing.logicalCallId)
        assertEquals(CallState.OUTGOING_CALLING, ringing.state)
        assertNull(ringing.connectedAt)
    }

    @Test
    fun videoSourceReplacementCompoundBoundaryBecomesActiveOnce() {
        val tracker = CallSessionTracker()
        val first = tracker.resolve(
            input(classification(CallState.OUTGOING_CALLING), 57_000L, sourceKey = "dialing", isVideo = true)
        )
        tracker.markSourceRemoved("dialing", 57_100L)
        val active = tracker.resolve(
            input(
                classification(
                    CallState.OUTGOING_CALLING,
                    CallActiveEvidence.CHRONOMETER_PRESENT,
                    hasConnectedControl = true
                ),
                57_500L,
                sourceKey = "connected",
                notificationId = 99,
                showsChronometer = true,
                base = 57_400L,
                isVideo = true
            )
        )

        assertEquals(first.logicalCallId, active.logicalCallId)
        assertEquals(CallState.ACTIVE, active.state)
        assertEquals(57_400L, active.connectedAt)
        assertEquals(CallActiveEvidence.COMPOUND_SOURCE_REPLACEMENT, active.activeEvidence)
    }

    @Test
    fun audioVideoModeChangesKeepActiveSessionAndConnectedAt() {
        val tracker = CallSessionTracker()
        tracker.resolve(input(classification(CallState.OUTGOING_CALLING), 58_000L, isVideo = true))
        val connected = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, CallActiveEvidence.CHRONOMETER_PRESENT),
                59_000L,
                showsChronometer = true,
                base = 58_900L,
                isVideo = true
            )
        )
        val audio = tracker.resolve(
            input(classification(CallState.OUTGOING_CALLING), 60_000L, isVideo = false)
        )
        val videoAgain = tracker.resolve(
            input(classification(CallState.OUTGOING_CALLING), 61_000L, isVideo = true)
        )

        assertEquals(connected.logicalCallId, audio.logicalCallId)
        assertEquals(connected.logicalCallId, videoAgain.logicalCallId)
        assertEquals(connected.connectedAt, audio.connectedAt)
        assertEquals(connected.connectedAt, videoAgain.connectedAt)
        assertEquals(CallState.ACTIVE, videoAgain.state)
        assertTrue(videoAgain.isVideoCall)
    }

    @Test
    fun audioToVideoSourceReplacementWithRewrittenParticipantKeepsActiveState() {
        val tracker = CallSessionTracker()
        tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING),
                62_000L,
                sourceKey = "audio-source",
                participant = "audio-person"
            )
        )
        val active = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, CallActiveEvidence.CHRONOMETER_PRESENT),
                63_000L,
                sourceKey = "audio-source",
                participant = "audio-person",
                showsChronometer = true,
                base = 62_900L
            )
        )

        val video = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING),
                63_500L,
                sourceKey = "video-source",
                participant = "video-person",
                notificationId = 99,
                isVideo = true
            )
        )

        assertEquals(active.logicalCallId, video.logicalCallId)
        assertEquals(CallState.ACTIVE, video.state)
        assertEquals(active.connectedAt, video.connectedAt)
        assertEquals(active.connectedAtSource, video.connectedAtSource)
        assertTrue(video.sourceReplacement)
        assertTrue(video.isVideoCall)
    }

    @Test
    fun activeVideoSourceReplacementKeepsTimerAndLogicalSession() {
        val tracker = CallSessionTracker()
        tracker.resolve(
            input(classification(CallState.OUTGOING_CALLING), 62_000L, sourceKey = "connected-a", isVideo = true)
        )
        val active = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, CallActiveEvidence.CHRONOMETER_PRESENT),
                63_000L,
                sourceKey = "connected-a",
                showsChronometer = true,
                base = 62_900L,
                isVideo = true
            )
        )
        tracker.markSourceRemoved("connected-a", 63_100L)
        val replacement = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, hasConnectedControl = true),
                63_500L,
                sourceKey = "connected-b",
                notificationId = 99,
                isVideo = true
            )
        )

        assertEquals(active.logicalCallId, replacement.logicalCallId)
        assertEquals(CallState.ACTIVE, replacement.state)
        assertEquals(active.connectedAt, replacement.connectedAt)
        assertEquals(active.connectedAtSource, replacement.connectedAtSource)
    }

    @Test
    fun unansweredOutgoingCallNeverGainsTimerAsTimePasses() {
        val tracker = CallSessionTracker()
        var session = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING, CallActiveEvidence.CHRONOMETER_PRESENT),
                60_000L,
                showsChronometer = true,
                base = 59_000L
            )
        )
        repeat(20) { second ->
            session = tracker.resolve(
                input(
                    classification(CallState.OUTGOING_CALLING, CallActiveEvidence.CHRONOMETER_PRESENT),
                    61_000L + second * 1_000L,
                    showsChronometer = true,
                    base = 59_000L
                )
            )
            assertEquals(CallState.OUTGOING_CALLING, session.state)
            assertNull(session.connectedAt)
            assertNull(CallTimerPolicy.connectedAtForTimer(session))
        }

        tracker.end(session.logicalCallId)
        assertEquals(0, tracker.size())
    }

    @Test
    fun differentParticipantDoesNotMergeDuringReplacementWindow() {
        val tracker = CallSessionTracker()
        val first = tracker.resolve(input(classification(CallState.OUTGOING_CALLING), 70_000L, sourceKey = "one"))
        tracker.markSourceRemoved("one", 70_100L)
        val second = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING),
                70_200L,
                sourceKey = "two",
                participant = "person-b",
                notificationId = 8
            )
        )

        assertNotEquals(first.logicalCallId, second.logicalCallId)
    }

    @Test
    fun replacementAfterGraceDoesNotMerge() {
        val tracker = CallSessionTracker()
        val first = tracker.resolve(input(classification(CallState.OUTGOING_CALLING), 80_000L, sourceKey = "one"))
        tracker.markSourceRemoved("one", 80_000L)
        val second = tracker.resolve(
            input(
                classification(CallState.OUTGOING_CALLING),
                80_000L + CallReplacementPolicy.MATCH_GRACE_MS + 1L,
                sourceKey = "two",
                notificationId = 8
            )
        )

        assertNotEquals(first.logicalCallId, second.logicalCallId)
    }

    @Test
    fun preConnectedStatesCannotExposeTimerAndInvalidSessionFailsFast() {
        val tracker = CallSessionTracker()
        listOf(
            CallState.INCOMING_RINGING,
            CallState.OUTGOING_CALLING,
            CallState.OUTGOING_RINGING,
            CallState.CONNECTING
        ).forEachIndexed { index, state ->
            val session = tracker.resolve(
                input(
                    classification(state, hasAnswer = state == CallState.INCOMING_RINGING),
                    90_000L + index,
                    sourceKey = "source-$index",
                    notificationId = index,
                    participant = "person-$index"
                )
            )
            assertNull(CallTimerPolicy.connectedAtForTimer(session))
        }

        val invalid = tracker.resolve(input(classification(CallState.OUTGOING_CALLING), 100_000L))
            .copy(connectedAt = 99_000L)
        assertThrows(IllegalStateException::class.java) {
            CallTimerPolicy.connectedAtForTimer(invalid)
        }
    }

    @Test
    fun endingAndPruningSessionsCleanSourceAliases() {
        val tracker = CallSessionTracker(staleSessionMs = 100L)
        val session = tracker.resolve(input(classification(CallState.OUTGOING_CALLING), 1_000L))
        tracker.end(session.logicalCallId)
        assertEquals(0, tracker.size())
        assertNull(tracker.logicalIdForSource("source-1"))

        tracker.resolve(input(classification(CallState.OUTGOING_CALLING), 2_000L))
        tracker.pruneStale(2_101L)
        assertEquals(0, tracker.size())
        assertNull(tracker.logicalIdForSource("source-1"))
    }

    private fun classification(
        state: CallState,
        evidence: CallActiveEvidence = CallActiveEvidence.NONE,
        hasAnswer: Boolean = false,
        hasHangUp: Boolean = state == CallState.OUTGOING_CALLING,
        hasConnectedControl: Boolean = false,
        presentation: CallPresentationType = CallPresentationType.UNKNOWN
    ) = CallClassification(
        isCall = true,
        state = state,
        reason = "test",
        activeEvidence = evidence,
        presentationType = presentation,
        hasAnswer = hasAnswer,
        hasDeclineOrHangUp = hasHangUp,
        hasConnectedControl = hasConnectedControl
    )

    private fun input(
        classification: CallClassification,
        now: Long,
        sourceKey: String = "source-1",
        participant: String? = "person-a",
        notificationId: Int = 7,
        showsChronometer: Boolean = false,
        base: Long = 0L,
        isVideo: Boolean = false
    ) = CallSessionInput(
        sourceKey = sourceKey,
        packageName = "example.calls",
        notificationId = notificationId,
        notificationTag = null,
        groupKey = "calls",
        participantId = participant,
        classification = classification,
        showsChronometer = showsChronometer,
        chronometerBase = base,
        observedAt = now,
        isVideoCall = isVideo
    )
}
