package com.d4viddf.hyperbridge.service.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyIndicatorResolverTest {

    @Test
    fun `nothing in use shows nothing`() {
        assertNull(PrivacyIndicatorResolver.resolve(microphoneActive = false, cameraActive = false))
    }

    @Test
    fun `microphone only is orange`() {
        val state = PrivacyIndicatorResolver.resolve(microphoneActive = true, cameraActive = false)!!
        assertEquals(PrivacyIndicatorKind.MICROPHONE, state.kind)
        assertEquals("#FF9500", state.kind.colorHex)
    }

    @Test
    fun `camera only is green`() {
        val state = PrivacyIndicatorResolver.resolve(microphoneActive = false, cameraActive = true)!!
        assertEquals(PrivacyIndicatorKind.CAMERA, state.kind)
        assertEquals("#34C759", state.kind.colorHex)
    }

    @Test
    fun `both sensors show the combined state`() {
        val state = PrivacyIndicatorResolver.resolve(microphoneActive = true, cameraActive = true)!!
        assertEquals(PrivacyIndicatorKind.MICROPHONE_AND_CAMERA, state.kind)
        assertTrue(state.microphone && state.camera)
    }

    @Test
    fun `equal inputs give equal states so the island is not reposted`() {
        assertEquals(
            PrivacyIndicatorResolver.resolve(true, false),
            PrivacyIndicatorResolver.resolve(true, false)
        )
    }

    @Test
    fun `camera tracker is idle until a camera is unavailable`() {
        val tracker = CameraUsageTracker()
        tracker.onAvailable("0")
        tracker.onAvailable("1")
        assertFalse(tracker.inUse)
    }

    @Test
    fun `camera tracker stays in use while any camera is unavailable`() {
        val tracker = CameraUsageTracker()
        tracker.onUnavailable("0")
        tracker.onUnavailable("1")
        tracker.onAvailable("0")
        assertTrue(tracker.inUse)
        tracker.onAvailable("1")
        assertFalse(tracker.inUse)
    }

    @Test
    fun `appearing waits longer than updating or hiding`() {
        assertEquals(
            PrivacyIndicatorResolver.SHOW_DELAY_MILLIS,
            PrivacyIndicatorResolver.settleDelayMillis(isShowing = false, wantsToShow = true)
        )
        assertEquals(
            PrivacyIndicatorResolver.DEBOUNCE_MILLIS,
            PrivacyIndicatorResolver.settleDelayMillis(isShowing = true, wantsToShow = true)
        )
        assertEquals(
            PrivacyIndicatorResolver.DEBOUNCE_MILLIS,
            PrivacyIndicatorResolver.settleDelayMillis(isShowing = true, wantsToShow = false)
        )
        assertTrue(PrivacyIndicatorResolver.SHOW_DELAY_MILLIS > PrivacyIndicatorResolver.DEBOUNCE_MILLIS)
    }

    @Test
    fun `camera tracker ignores duplicate reports and clears`() {
        val tracker = CameraUsageTracker()
        tracker.onUnavailable("0")
        tracker.onUnavailable("0")
        tracker.onAvailable("0")
        assertFalse(tracker.inUse)
        tracker.onUnavailable("2")
        tracker.clear()
        assertFalse(tracker.inUse)
    }
}
