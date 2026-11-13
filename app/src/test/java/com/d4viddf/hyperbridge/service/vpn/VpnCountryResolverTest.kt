package com.d4viddf.hyperbridge.service.vpn

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VpnCountryResolverTest {
    @Test fun resolvesExplicitFlagEmoji() {
        assertEquals("JP", VpnCountryResolver.resolve("🇯🇵 Tokyo")?.isoCode)
    }

    @Test fun resolvesIsoCodeAndRelayPrefix() {
        assertEquals("US", VpnCountryResolver.resolve("US - New York")?.isoCode)
        assertEquals("SE", VpnCountryResolver.resolve("se-got-wg-001")?.isoCode)
    }

    @Test fun resolvesEnglishAndLocalizedCountryNames() {
        assertEquals("NL", VpnCountryResolver.resolve("Netherlands, Amsterdam")?.isoCode)
        assertEquals(
            "DE",
            VpnCountryResolver.resolve("Almanya, Frankfurt", listOf(Locale.forLanguageTag("tr")))?.isoCode
        )
    }

    @Test fun doesNotGuessFromArbitraryServerText() {
        assertNull(VpnCountryResolver.resolve("Premium secure relay 42"))
    }

    @Test fun createsRegionalIndicatorFlag() {
        assertEquals("🇳🇱", VpnCountryResolver.flagEmoji("NL"))
        assertEquals("", VpnCountryResolver.flagEmoji("XX"))
    }
}

