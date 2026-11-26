package com.d4viddf.hyperbridge.service

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LockscreenVisibilityPolicyTest {

    private fun visibility(
        source: Int = Notification.VISIBILITY_PRIVATE,
        override: Int = LockscreenVisibilityPolicy.NO_OVERRIDE,
        privateAllowed: Boolean = true,
        explicit: Boolean = false
    ) = LockscreenVisibilityPolicy.bridgeVisibility(source, override, privateAllowed, explicit)

    @Test
    fun privateSourceIsLeftAloneWhenTheLockScreenShowsContent() {
        assertNull(visibility())
    }

    @Test
    fun privateSourceIsKeptOffTheLockScreenWhenContentIsHiddenSystemWide() {
        assertEquals(Notification.VISIBILITY_SECRET, visibility(privateAllowed = false))
    }

    @Test
    fun publicSourceStaysVisibleEvenWhenContentIsHiddenSystemWide() {
        assertNull(visibility(source = Notification.VISIBILITY_PUBLIC, privateAllowed = false))
    }

    @Test
    fun secretSourceIsNeverMirroredOnTheLockScreen() {
        assertEquals(Notification.VISIBILITY_SECRET, visibility(source = Notification.VISIBILITY_SECRET))
    }

    @Test
    fun perAppOverrideWinsOverThePublicFlagOfTheSource() {
        assertEquals(
            Notification.VISIBILITY_SECRET,
            visibility(source = Notification.VISIBILITY_PUBLIC, override = Notification.VISIBILITY_PRIVATE)
        )
        assertEquals(
            Notification.VISIBILITY_SECRET,
            visibility(source = Notification.VISIBILITY_PUBLIC, override = Notification.VISIBILITY_SECRET)
        )
    }

    @Test
    fun explicitLockscreenIslandIsNotHidden() {
        assertNull(visibility(source = Notification.VISIBILITY_SECRET, privateAllowed = false, explicit = true))
    }
}
