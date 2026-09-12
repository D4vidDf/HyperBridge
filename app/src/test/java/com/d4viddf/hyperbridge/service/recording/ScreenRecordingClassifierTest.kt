package com.d4viddf.hyperbridge.service.recording

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenRecordingClassifierTest {
    @Test
    fun confirmedXiaomiRecorderNotificationIsAccepted() {
        assertTrue(ScreenRecordingClassifier.isScreenRecording(activeSignals()))
    }

    @Test
    fun savedRecorderNotificationIsRejected() {
        val saved = activeSignals().copy(
            notificationId = ScreenRecordingClassifier.SAVED_NOTIFICATION_ID,
            channelId = ScreenRecordingClassifier.SAVED_CHANNEL_ID,
            isOngoing = false,
            isForegroundService = false
        )

        assertFalse(ScreenRecordingClassifier.isScreenRecording(saved))
        assertTrue(ScreenRecordingClassifier.isSavedScreenRecording(saved))
        assertFalse(ScreenRecordingClassifier.isSavedScreenRecording(activeSignals()))
    }

    @Test
    fun unrelatedForegroundRecorderLikeNotificationIsRejected() {
        assertFalse(
            ScreenRecordingClassifier.isScreenRecording(
                activeSignals().copy(packageName = "com.example.streaming")
            )
        )
        assertFalse(
            ScreenRecordingClassifier.isScreenRecording(
                activeSignals().copy(channelId = "other-channel")
            )
        )
        assertFalse(
            ScreenRecordingClassifier.isScreenRecording(
                activeSignals().copy(isGroupSummary = true)
            )
        )
    }

    @Test
    fun timeoutPolicyNeverAppliesToActiveRecordingAndUsesDedicatedTimeoutForSaved() {
        assertEquals(null, ScreenRecordingTimeoutPolicy.resolve(1800, 4, true, false))
        assertEquals(4, ScreenRecordingTimeoutPolicy.resolve(1800, 4, false, true))
        assertEquals(1800, ScreenRecordingTimeoutPolicy.resolve(1800, 4, false, false))
    }

    private fun activeSignals() = ScreenRecordingSignals(
        packageName = ScreenRecordingClassifier.PACKAGE_NAME,
        notificationId = ScreenRecordingClassifier.ACTIVE_NOTIFICATION_ID,
        channelId = ScreenRecordingClassifier.ACTIVE_CHANNEL_ID,
        isOngoing = true,
        isForegroundService = true,
        isGroupSummary = false
    )
}
