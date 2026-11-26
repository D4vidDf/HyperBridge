package com.d4viddf.hyperbridge.service

import android.app.Notification
import androidx.core.app.NotificationCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTemplatesTest {
    @Test
    fun constantsMatchWhatThePlatformAndCompatActuallyWrite() {
        // EXTRA_TEMPLATE is filled with Class.getName(), which uses '$' for nested classes.
        assertEquals(Notification.MessagingStyle::class.java.name, NotificationTemplates.PLATFORM_MESSAGING_STYLE)
        assertEquals(NotificationCompat.MessagingStyle::class.java.name, NotificationTemplates.COMPAT_MESSAGING_STYLE)
    }

    @Test
    fun dottedSpellingIsNotWhatThePlatformSends() {
        // Regression guard for the old equality check in detectNotificationType (#331).
        assertFalse("android.app.Notification.MessagingStyle" == Notification.MessagingStyle::class.java.name)
    }

    @Test
    fun recognisesPlatformAndCompatMessagingStyle() {
        assertTrue(NotificationTemplates.isMessagingStyle(NotificationTemplates.PLATFORM_MESSAGING_STYLE))
        assertTrue(NotificationTemplates.isMessagingStyle(NotificationTemplates.COMPAT_MESSAGING_STYLE))
    }

    @Test
    fun rejectsOtherStylesAndMissingTemplate() {
        assertFalse(NotificationTemplates.isMessagingStyle(Notification.BigTextStyle::class.java.name))
        assertFalse(NotificationTemplates.isMessagingStyle(Notification.InboxStyle::class.java.name))
        assertFalse(NotificationTemplates.isMessagingStyle(Notification.MediaStyle::class.java.name))
        assertFalse(NotificationTemplates.isMessagingStyle(""))
        assertFalse(NotificationTemplates.isMessagingStyle(null))
    }
}
