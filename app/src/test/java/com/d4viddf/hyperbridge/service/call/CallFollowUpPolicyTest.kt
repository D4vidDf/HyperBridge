package com.d4viddf.hyperbridge.service.call

import com.d4viddf.hyperbridge.models.NotificationType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallFollowUpPolicyTest {

    @Test
    fun trackedCallSourceRewrittenAsFollowUpEndsTheCall() {
        val tracker = CallSessionTracker()
        val session = tracker.resolve(input(sourceKey = "0|com.instagram.android|42|null|10123"))

        val trackedId = tracker.logicalIdForSource("0|com.instagram.android|42|null|10123")

        assertTrue(trackedId == session.logicalCallId)
        assertTrue(CallFollowUpPolicy.shouldEndCall(trackedId, NotificationType.STANDARD))
        assertTrue(CallFollowUpPolicy.shouldEndCall(trackedId, NotificationType.MESSAGE))
    }

    @Test
    fun callUpdatesKeepTheCall() {
        assertFalse(CallFollowUpPolicy.shouldEndCall("call:com.instagram.android:1", NotificationType.CALL))
    }

    @Test
    fun untrackedSourcesAreIgnored() {
        assertFalse(CallFollowUpPolicy.shouldEndCall(null, NotificationType.STANDARD))
        assertFalse(CallFollowUpPolicy.shouldEndCall(null, NotificationType.CALL))
    }

    private fun input(sourceKey: String) = CallSessionInput(
        sourceKey = sourceKey,
        packageName = "com.instagram.android",
        notificationId = 42,
        notificationTag = null,
        groupKey = null,
        participantId = "alice",
        classification = CallClassification(
            isCall = true,
            state = CallState.OUTGOING_CALLING,
            reason = "call-style",
            hasDeclineOrHangUp = true
        ),
        showsChronometer = true,
        chronometerBase = 1_000L,
        observedAt = 2_000L
    )
}
