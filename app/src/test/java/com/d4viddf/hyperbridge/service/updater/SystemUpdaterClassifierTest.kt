package com.d4viddf.hyperbridge.service.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemUpdaterClassifierTest {

    @Test
    fun packageMatchesSystemUpdater() {
        assertTrue(SystemUpdaterClassifier.isSystemUpdater("com.android.updater"))
        assertFalse(SystemUpdaterClassifier.isSystemUpdater("com.google.android.updater"))
        assertFalse(SystemUpdaterClassifier.isSystemUpdater("com.miui.screenrecorder"))
    }

    @Test
    fun timeoutPolicyResolvesAppropriately() {
        // Active system update: never times out (null)
        assertEquals(
            null,
            SystemUpdateTimeoutPolicy.resolve(
                configuredTimeout = 10,
                systemUpdateTimeout = 5,
                isSystemUpdate = true,
                isFinished = false
            )
        )

        // Finished system update: uses dedicated system update timeout
        assertEquals(
            5,
            SystemUpdateTimeoutPolicy.resolve(
                configuredTimeout = 10,
                systemUpdateTimeout = 5,
                isSystemUpdate = true,
                isFinished = true
            )
        )

        // System update without progress (e.g. update available / restart phone): uses system update timeout
        assertEquals(
            5,
            SystemUpdateTimeoutPolicy.resolve(
                configuredTimeout = 10,
                systemUpdateTimeout = 5,
                isSystemUpdate = true,
                isFinished = false,
                hasProgress = false
            )
        )

        // Non-system update: uses configured timeout
        assertEquals(
            10,
            SystemUpdateTimeoutPolicy.resolve(
                configuredTimeout = 10,
                systemUpdateTimeout = 5,
                isSystemUpdate = false,
                isFinished = false
            )
        )
    }
}
