package com.d4viddf.hyperbridge.service.privacy

import com.d4viddf.hyperbridge.models.PrivacyIndicatorDesignConfig
import com.d4viddf.hyperbridge.models.PrivacyIndicatorStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyIndicatorDesignTest {

    @Test
    fun `default design config uses sensor icon and shows text`() {
        val config = PrivacyIndicatorDesignConfig()
        assertEquals(PrivacyIndicatorStyle.ICON, config.style)
        assertTrue(config.showText)
    }

    @Test
    fun `custom design config can select dot and hide text`() {
        val config = PrivacyIndicatorDesignConfig(
            style = PrivacyIndicatorStyle.DOT,
            showText = false
        )
        assertEquals(PrivacyIndicatorStyle.DOT, config.style)
        assertFalse(config.showText)
    }

    @Test
    fun `privacy indicator style entries exist`() {
        assertEquals(listOf(PrivacyIndicatorStyle.ICON, PrivacyIndicatorStyle.DOT), PrivacyIndicatorStyle.entries)
    }
}
