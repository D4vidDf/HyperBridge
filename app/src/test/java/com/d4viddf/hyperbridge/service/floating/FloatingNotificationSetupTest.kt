package com.d4viddf.hyperbridge.service.floating

import org.junit.Assert.assertEquals
import org.junit.Test

class FloatingNotificationSetupTest {
    @Test
    fun selectedAppNeedsReviewUntilUserConfirms() {
        assertEquals(
            FloatingSetupStatus.NEEDS_REVIEW,
            FloatingNotificationSetup.status(true, false, true)
        )
        assertEquals(
            FloatingSetupStatus.USER_CONFIRMED,
            FloatingNotificationSetup.status(true, true, true)
        )
    }

    @Test
    fun unsupportedOrUnselectedAppDoesNotRequireSetup() {
        assertEquals(FloatingSetupStatus.NOT_REQUIRED, FloatingNotificationSetup.status(true, false, false))
        assertEquals(FloatingSetupStatus.NOT_REQUIRED, FloatingNotificationSetup.status(false, false, true))
    }
}
