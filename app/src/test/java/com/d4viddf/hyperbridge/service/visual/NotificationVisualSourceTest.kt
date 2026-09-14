package com.d4viddf.hyperbridge.service.visual

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationVisualSourceTest {
    @Test
    fun meaningfulNotificationImagesUseAppBadge() {
        assertTrue(NotificationVisualSource.PERSON.shouldShowAppBadge)
        assertTrue(NotificationVisualSource.PICTURE.shouldShowAppBadge)
        assertTrue(NotificationVisualSource.LARGE_ICON.shouldShowAppBadge)
    }

    @Test
    fun appIconFallbackDoesNotDuplicateBadge() {
        assertFalse(NotificationVisualSource.APP_ICON.shouldShowAppBadge)
        assertFalse(NotificationVisualSource.SMALL_ICON.shouldShowAppBadge)
        assertFalse(NotificationVisualSource.FALLBACK.shouldShowAppBadge)
    }
}
