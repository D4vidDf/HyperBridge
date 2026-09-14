package com.d4viddf.hyperbridge.service.vpn

import org.junit.Assert.*
import org.junit.Test

class VpnNotificationAnalyzerTest {
    private val analyzer = VpnNotificationAnalyzer()
    @Test fun extractsConservativeDestinationTrafficAndTimerWithoutGuessingGenericActions() {
        val evidence = analyzer.analyze(signals(
            title = "Connected to Netherlands", text = "↓ 8.2 MB/s · ↑ 1.3 MB/s",
            whenMillis = 10_000L, usesChronometer = true,
            actions = listOf(VpnNotificationActionSignal(0, "Disconnect", 0, true))
        ), setOf(PACKAGE), 20_000L)!!
        assertEquals("Netherlands", evidence.destination?.label)
        assertEquals("NL", evidence.destination?.countryCode)
        assertEquals(8_200_000L, evidence.traffic?.downloadBytesPerSecond)
        assertEquals(1_300_000L, evidence.traffic?.uploadBytesPerSecond)
        assertEquals(10_000L, evidence.connectedAtMillis); assertNull(evidence.disconnectActionIndex)
    }
    @Test fun protonStatusActionIsAcceptedByVerifiedStructuralContract() {
        val evidence = analyzer.analyze(
            signals(
                packageName = "ch.protonvpn.android",
                actions = listOf(VpnNotificationActionSignal(0, "Bağlantıyı kes", 0, true)),
                category = android.app.Notification.CATEGORY_SERVICE,
                whenMillis = 10_000L
            ),
            setOf("ch.protonvpn.android"),
            20_000L
        )!!
        assertEquals(0, evidence.disconnectActionIndex)
        assertEquals(10_000L, evidence.connectedAtMillis)
        assertEquals(VpnTimestampSource.PROVIDER_NOTIFICATION_TIMESTAMP, evidence.timestampSource)
    }
    @Test fun protonCurrentLayoutExtractsLiveRatesInsteadOfSessionTotals() {
        val evidence = analyzer.analyze(
            signals(
                packageName = "ch.protonvpn.android",
                text = "↓ 15 MB | 8.2 MB/s ↑ 3 MB | 1.3 MB/s",
                actions = listOf(VpnNotificationActionSignal(0, "Disconnect", 0, true)),
                category = android.app.Notification.CATEGORY_SERVICE
            ),
            setOf("ch.protonvpn.android"),
            20_000L
        )!!
        assertEquals(8_200_000L, evidence.traffic?.downloadBytesPerSecond)
        assertEquals(1_300_000L, evidence.traffic?.uploadBytesPerSecond)
    }
    @Test fun rejectsOpaqueHostAndUnlabelledText() {
        assertNull(analyzer.analyze(signals(title = "Connected to nl-42.internal.example.com"), setOf(PACKAGE), 20_000L)!!.destination)
        assertNull(analyzer.analyze(signals(title = "Protected", text = "Premium account Netherlands"), setOf(PACKAGE), 20_000L)!!.destination)
    }
    @Test fun ivpnPausedStopActionUsesVerifiedIconInsteadOfLocalizedText() {
        val evidence = analyzer.analyze(
            signals(
                packageName = "net.ivpn.client",
                title = "Duraklatıldı",
                actions = listOf(
                    VpnNotificationActionSignal(0, "Sürdür", 0, true, "ic_play"),
                    VpnNotificationActionSignal(1, "Durdur", 0, true, "ic_stop")
                )
            ),
            setOf("net.ivpn.client"),
            20_000L
        )!!
        assertEquals(VpnConnectionState.PAUSED, evidence.providerState)
        assertEquals(1, evidence.disconnectActionIndex)
    }
    @Test fun localizedProviderHintsResolveStateAndDestinationWithoutEnglishParsing() {
        val evidence = analyzer.analyze(
            signals(
                packageName = "ch.protonvpn.android",
                title = "Niederlande ile verbunden",
                actions = listOf(VpnNotificationActionSignal(0, "Trennen", 0, true)),
                category = android.app.Notification.CATEGORY_SERVICE,
                localizedStateLabels = mapOf("Neu verbinden" to VpnConnectionState.RECONNECTING),
                localizedDestinationTemplates = listOf("Mit %1\$s verbunden")
            ).copy(title = "Mit Niederlande verbunden"),
            setOf("ch.protonvpn.android"),
            20_000L
        )!!
        assertEquals("Niederlande", evidence.destination?.label)
    }

    @Test fun mullvadUsesCurrentActionIconOrderingForLocalizedDisconnect() {
        val evidence = analyzer.analyze(
            signals(
                packageName = "net.mullvad.mullvadvpn",
                title = "Bağlı",
                text = "İsveç, Göteborg, se-got-wg-001",
                actions = listOf(
                    VpnNotificationActionSignal(0, "Yeniden bağlan", 0, true, "icon_notification_disconnect"),
                    VpnNotificationActionSignal(1, "Bağlantıyı kes", 0, true, "icon_notification_disconnect")
                ),
                localizedStateLabels = mapOf("Bağlı" to VpnConnectionState.CONNECTED)
            ),
            setOf("net.mullvad.mullvadvpn"),
            20_000L
        )!!
        assertEquals(VpnConnectionState.CONNECTED, evidence.providerState)
        assertEquals(1, evidence.disconnectActionIndex)
        assertEquals("İsveç, Göteborg, se-got-wg-001", evidence.destination?.label)
        assertEquals("SE", evidence.destination?.countryCode)
    }
    @Test fun explicitCountryFlagIsPreservedAsStructuredDestinationMetadata() {
        val evidence = analyzer.analyze(
            signals(title = "Connected to 🇯🇵 Tokyo"),
            setOf(PACKAGE),
            20_000L
        )!!
        assertEquals("JP", evidence.destination?.countryCode)
    }
    @Test fun acceptsNotificationLargeIconOnlyAfterCountryAndVpnSemanticsAreVerified() {
        val evidence = analyzer.analyze(
            signals(title = "Connected to Netherlands").copy(
                hasLargeIcon = true,
                largeIconResourceName = "ic_country_flag_nl"
            ),
            setOf(PACKAGE),
            20_000L
        )!!
        assertTrue(evidence.useLargeIconAsCountryFlag)
    }
    @Test fun rejectsProviderLogoAsCountryFlag() {
        val evidence = analyzer.analyze(
            signals(title = "Connected to Netherlands").copy(
                hasLargeIcon = true,
                largeIconResourceName = "ic_app_logo"
            ),
            setOf(PACKAGE),
            20_000L
        )!!
        assertFalse(evidence.useLargeIconAsCountryFlag)
    }
    @Test fun vpnServicePackageRemainsSufficientForOngoingNotificationEvidence() {
        val evidence = analyzer.analyze(
            signals(title = "Background sync", text = "Account updated").copy(hasLargeIcon = true),
            setOf(PACKAGE),
            20_000L
        )
        assertNotNull(evidence)
        assertFalse(evidence!!.useLargeIconAsCountryFlag)
    }
    @Test fun ignoresNonProviderAndNonOngoing() {
        assertNull(analyzer.analyze(signals(), emptySet(), 20_000L))
        assertNull(analyzer.analyze(signals(isOngoing = false, isForegroundService = false), setOf(PACKAGE), 20_000L))
    }
    @Test fun genericNotificationWhenIsNotTrustedWithoutAChronometer() {
        val evidence = analyzer.analyze(signals(whenMillis = 10_000L), setOf(PACKAGE), 20_000L)!!
        assertNull(evidence.connectedAtMillis)
        assertEquals(VpnTimestampSource.UNKNOWN, evidence.timestampSource)
    }
    @Test fun providerElapsedTitleRecoversColdStartChronometerWithoutTrustingNotificationWhen() {
        val evidence = analyzer.analyze(
            signals(
                title = "Netherlands is connected 03:31",
                whenMillis = 499_900L
            ),
            setOf(PACKAGE),
            500_000L
        )!!
        assertEquals("Netherlands", evidence.destination?.label)
        assertEquals("NL", evidence.destination?.countryCode)
        assertEquals(289_000L, evidence.connectedAtMillis)
        assertEquals(VpnTimestampSource.PROVIDER_NOTIFICATION_ELAPSED, evidence.timestampSource)
    }
    @Test fun preservesNativeXiaomiCapabilitySignal() {
        val evidence = analyzer.analyze(signals().copy(hasNativeXiaomiPayload = true), setOf(PACKAGE), 20_000L)!!
        assertTrue(evidence.hasNativeXiaomiPayload)
    }
    @Test fun formatsTrafficWithoutFabricatingMissingDirection() {
        val value = VpnTrafficFormatter.format(VpnTrafficSnapshot(640_000L, null, 1L, VpnEvidenceSource.PROVIDER_ADAPTER))
        assertEquals("↓ 640 KB/s", value)
        assertNull(VpnTrafficFormatter.format(VpnTrafficSnapshot(0L, 0L, 1L, VpnEvidenceSource.PROVIDER_ADAPTER)))
    }
    private fun signals(
        packageName: String = PACKAGE, title: String = "VPN active", text: String = "", isOngoing: Boolean = true,
        isForegroundService: Boolean = true, usesChronometer: Boolean = false, whenMillis: Long = 0,
        actions: List<VpnNotificationActionSignal> = emptyList(), category: String? = null,
        localizedStateLabels: Map<String, VpnConnectionState> = emptyMap(),
        localizedDestinationTemplates: List<String> = emptyList()
    ) = VpnNotificationSignals(packageName, "Example VPN", title, text, "", isOngoing, isForegroundService,
        usesChronometer, whenMillis, actions, category, false, localizedStateLabels, localizedDestinationTemplates)
    companion object { private const val PACKAGE = "net.example.vpn" }
}

