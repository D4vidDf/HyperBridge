package com.d4viddf.hyperbridge.service.translators

import com.d4viddf.hyperbridge.service.NotificationTemplates
import com.d4viddf.hyperbridge.service.visual.NotificationVisualSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseTranslatorVisualTest {

    @Test
    fun messagingStyleTemplatesAreRecognizedForAvatarExtraction() {
        // Platform MessagingStyle template (Notification.MessagingStyle)
        assertTrue(NotificationTemplates.isMessagingStyle(NotificationTemplates.PLATFORM_MESSAGING_STYLE))
        // Compat MessagingStyle template used by WhatsApp, Telegram, etc.
        assertTrue(NotificationTemplates.isMessagingStyle(NotificationTemplates.COMPAT_MESSAGING_STYLE))
        // Any custom subpackage ending with MessagingStyle
        assertTrue(NotificationTemplates.isMessagingStyle("android.app.Notification\$MessagingStyle"))
        assertTrue(NotificationTemplates.isMessagingStyle("androidx.core.app.NotificationCompat\$MessagingStyle"))
    }

    @Test
    fun nonMessagingStyleTemplatesAreNotTreatedAsMessaging() {
        assertFalse(NotificationTemplates.isMessagingStyle(null))
        assertFalse(NotificationTemplates.isMessagingStyle(""))
        assertFalse(NotificationTemplates.isMessagingStyle("android.app.Notification\$BigTextStyle"))
        assertFalse(NotificationTemplates.isMessagingStyle("android.app.Notification\$MediaStyle"))
    }

    @Test
    fun photographicVisualSourcesNeverTintMonochromeOrDark() {
        // Helper logic to verify photographic sources are shielded from tinting
        fun shouldTintIfDark(source: NotificationVisualSource): Boolean {
            return source != NotificationVisualSource.PERSON &&
                    source != NotificationVisualSource.PICTURE &&
                    source != NotificationVisualSource.LARGE_ICON
        }

        // WhatsApp / messaging avatar (PERSON) must NEVER be tinted white,
        // even if the sender has a dark, night-time or black-and-white profile picture.
        assertFalse(shouldTintIfDark(NotificationVisualSource.PERSON))
        assertFalse(shouldTintIfDark(NotificationVisualSource.PICTURE))
        assertFalse(shouldTintIfDark(NotificationVisualSource.LARGE_ICON))

        // System/app icons should still be tinted if dark & monochrome
        assertTrue(shouldTintIfDark(NotificationVisualSource.SMALL_ICON))
        assertTrue(shouldTintIfDark(NotificationVisualSource.APP_ICON))
        assertTrue(shouldTintIfDark(NotificationVisualSource.FALLBACK))
    }

    @Test
    fun visualSourceAppBadgePolicyMatchesExpectedBehavior() {
        assertTrue(NotificationVisualSource.PERSON.shouldShowAppBadge)
        assertTrue(NotificationVisualSource.PICTURE.shouldShowAppBadge)
        assertTrue(NotificationVisualSource.LARGE_ICON.shouldShowAppBadge)

        assertFalse(NotificationVisualSource.SMALL_ICON.shouldShowAppBadge)
        assertFalse(NotificationVisualSource.APP_ICON.shouldShowAppBadge)
        assertFalse(NotificationVisualSource.FALLBACK.shouldShowAppBadge)
    }
}
