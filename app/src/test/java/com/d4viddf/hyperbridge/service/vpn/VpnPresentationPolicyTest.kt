package com.d4viddf.hyperbridge.service.vpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnPresentationPolicyTest {
    @Test fun richIdentifiedProviderShowsEveryVerifiedField() {
        val plan = VpnPresentationPolicy.plan(
            session(
                provider = provider(),
                destination = VpnDestination("Netherlands", 90, VpnEvidenceSource.PROVIDER_ADAPTER, "NL"),
                traffic = VpnTrafficSnapshot(8_200_000, 1_300_000, 2_000, VpnEvidenceSource.PROVIDER_ADAPTER)
            ),
            hasVerifiedDisconnect = true
        )
        assertEquals(PACKAGE, plan.providerPackageName)
        assertEquals("Netherlands", plan.destinationLabel)
        assertEquals("NL", plan.countryCode)
        assertTrue(plan.showDuration)
        assertTrue(plan.showTraffic)
        assertEquals(VpnControlKind.DISCONNECT, plan.controlKind)
    }

    @Test fun identifiedProviderWithoutTrafficKeepsIdentityDurationAndControl() {
        val plan = VpnPresentationPolicy.plan(session(provider = provider()), hasVerifiedDisconnect = true)
        assertEquals(PACKAGE, plan.providerPackageName)
        assertTrue(plan.showDuration)
        assertFalse(plan.showTraffic)
        assertEquals(VpnControlKind.DISCONNECT, plan.controlKind)
    }

    @Test fun identifiedProviderWithoutNotificationUsesOnlyKnownFields() {
        val plan = VpnPresentationPolicy.plan(
            session(provider = provider(VpnEvidenceSource.PRIVILEGED_INSPECTION)),
            hasVerifiedDisconnect = false
        )
        assertEquals(PACKAGE, plan.providerPackageName)
        assertNull(plan.destinationLabel)
        assertNull(plan.countryCode)
        assertEquals(VpnControlKind.MANAGE, plan.controlKind)
    }

    @Test fun unknownProviderNeverGetsFakeBadgeOrPowerControl() {
        val plan = VpnPresentationPolicy.plan(session(), hasVerifiedDisconnect = false)
        assertNull(plan.providerPackageName)
        assertNull(plan.destinationLabel)
        assertEquals(VpnControlKind.MANAGE, plan.controlKind)
    }

    @Test fun coldStartDoesNotFabricateZeroDuration() {
        val plan = VpnPresentationPolicy.plan(session(connectedAtMillis = null), hasVerifiedDisconnect = false)
        assertFalse(plan.showDuration)
    }

    private fun session(
        provider: VpnProviderIdentity? = null,
        destination: VpnDestination? = null,
        traffic: VpnTrafficSnapshot? = null,
        connectedAtMillis: Long? = 1_000
    ) = VpnSession(
        logicalId = "vpn:1",
        generation = 1,
        state = VpnConnectionState.CONNECTED,
        networkHandles = setOf(10),
        connectedAtMillis = connectedAtMillis,
        timestampSource = if (connectedAtMillis == null) VpnTimestampSource.UNKNOWN else VpnTimestampSource.NETWORK_OBSERVED,
        provider = provider,
        destination = destination,
        traffic = traffic,
        lastUpdatedAtMillis = 2_000
    )

    private fun provider(source: VpnEvidenceSource = VpnEvidenceSource.PROVIDER_ADAPTER) =
        VpnProviderIdentity(PACKAGE, "Example VPN", 95, source)

    companion object { private const val PACKAGE = "net.example.vpn" }
}

