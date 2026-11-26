package com.d4viddf.hyperbridge.service.translators

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnTranslatorConfigurationTest {
    @Test
    fun `persistent VPN island timeout is one day in milliseconds`() {
        assertEquals(86_400_000, VpnTranslator.PERSISTENT_ISLAND_TIMEOUT_MILLIS)
    }
}

