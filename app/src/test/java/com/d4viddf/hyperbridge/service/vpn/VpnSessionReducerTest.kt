package com.d4viddf.hyperbridge.service.vpn

import org.junit.Assert.*
import org.junit.Test

class VpnSessionReducerTest {
    private val reducer = VpnSessionReducer()

    @Test fun firstNetworkCreatesStableObservedSession() {
        val session = reducer.reduce(null, VpnSessionEvent.NetworkAvailable(10L, 1_000L))
        assertNotNull(session); assertEquals(VpnConnectionState.CONNECTED, session?.state)
        assertEquals(setOf(10L), session?.networkHandles); assertEquals(1_000L, session?.connectedAtMillis)
    }

    @Test fun providerClaimsCannotCreateAConnectedSessionWithoutNetworkTruth() {
        assertNull(reducer.reduce(null, VpnSessionEvent.Enriched(
            provider = VpnProviderIdentity("net.example.vpn", "Example", 95, VpnEvidenceSource.PROVIDER_ADAPTER),
            providerState = VpnConnectionState.CONNECTED,
            observedAtMillis = 1_000L
        )))
    }

    @Test fun removingNotificationEvidenceWhileNetworkRemainsKeepsGenericSession() {
        val connected = reducer.reduce(null, VpnSessionEvent.NetworkAvailable(10L, 1_000L))!!
        val enriched = reducer.reduce(connected, VpnSessionEvent.Enriched(
            provider = VpnProviderIdentity("net.example.vpn", "Example", 95, VpnEvidenceSource.PROVIDER_ADAPTER),
            providerState = VpnConnectionState.CONNECTED,
            observedAtMillis = 2_000L
        ))!!
        val notificationRemoved = reducer.reduce(enriched, VpnSessionEvent.Enriched(observedAtMillis = 3_000L))!!
        assertEquals(VpnConnectionState.CONNECTED, notificationRemoved.state)
        assertEquals(setOf(10L), notificationRemoved.networkHandles)
    }

    @Test fun preexistingNetworkDoesNotFabricateStartTime() {
        val session = reducer.reduce(null, VpnSessionEvent.NetworkAvailable(10L, 1_000L, connectionStartObserved = false))!!
        assertNull(session.connectedAtMillis)
        assertEquals(VpnTimestampSource.UNKNOWN, session.timestampSource)
    }

    @Test fun overlappingNetworkHandoffKeepsLogicalIdAndTimer() {
        val first = reducer.reduce(null, VpnSessionEvent.NetworkAvailable(10L, 1_000L))!!
        val overlap = reducer.reduce(first, VpnSessionEvent.NetworkAvailable(11L, 2_000L))!!
        val handedOff = reducer.reduce(overlap, VpnSessionEvent.NetworkLost(10L, 2_100L))!!
        assertEquals(first.logicalId, handedOff.logicalId); assertEquals(1_000L, handedOff.connectedAtMillis)
        assertEquals(setOf(11L), handedOff.networkHandles); assertEquals(VpnConnectionState.CONNECTED, handedOff.state)
    }

    @Test fun finalLossWaitsForMatchingGraceExpiry() {
        val first = reducer.reduce(null, VpnSessionEvent.NetworkAvailable(10L, 1_000L))!!
        val lost = reducer.reduce(first, VpnSessionEvent.NetworkLost(10L, 2_000L))!!
        assertNotNull(reducer.reduce(lost, VpnSessionEvent.HandoffGraceExpired(first.generation + 1, 3_000L)))
        assertNull(reducer.reduce(lost, VpnSessionEvent.HandoffGraceExpired(first.generation, 3_000L)))
    }

    @Test fun providerConfirmedReconnectSurvivesGraceAndReturnsAsSameActivity() {
        val first = reducer.reduce(null, VpnSessionEvent.NetworkAvailable(10L, 1_000L))!!
        val reconnecting = reducer.reduce(first, VpnSessionEvent.Enriched(
            providerState = VpnConnectionState.RECONNECTING,
            observedAtMillis = 1_500L
        ))!!
        val lost = reducer.reduce(reconnecting, VpnSessionEvent.NetworkLost(10L, 2_000L))!!
        val afterGrace = reducer.reduce(lost, VpnSessionEvent.HandoffGraceExpired(first.generation, 3_500L))!!
        val returned = reducer.reduce(afterGrace, VpnSessionEvent.NetworkAvailable(11L, 4_000L))!!
        assertEquals(VpnConnectionState.RECONNECTING, lost.providerState)
        assertEquals(first.logicalId, returned.logicalId)
        assertEquals(1_000L, returned.connectedAtMillis)
    }

    @Test fun removingStrongProviderEvidenceEndsNetworklessSession() {
        val first = reducer.reduce(null, VpnSessionEvent.NetworkAvailable(10L, 1_000L))!!
        val reconnecting = reducer.reduce(first, VpnSessionEvent.Enriched(
            providerState = VpnConnectionState.RECONNECTING,
            observedAtMillis = 1_500L
        ))!!
        val lost = reducer.reduce(reconnecting, VpnSessionEvent.NetworkLost(10L, 2_000L))!!
        assertNull(reducer.reduce(lost, VpnSessionEvent.Enriched(observedAtMillis = 3_000L)))
    }

    @Test fun strongerProviderAndTimestampEvidenceWins() {
        val first = reducer.reduce(null, VpnSessionEvent.NetworkAvailable(10L, 1_000L))!!
        val enriched = reducer.reduce(first, VpnSessionEvent.Enriched(
            provider = VpnProviderIdentity("net.example.vpn", "Example VPN", 95, VpnEvidenceSource.PROVIDER_ADAPTER),
            connectedAtMillis = 500L, timestampSource = VpnTimestampSource.NOTIFICATION_CHRONOMETER,
            observedAtMillis = 2_000L
        ))!!
        assertEquals("net.example.vpn", enriched.provider?.packageName); assertEquals(500L, enriched.connectedAtMillis)
    }

    @Test fun elapsedNotificationUpdatesDoNotJitterCompactTimerOrigin() {
        val cold = reducer.reduce(
            null,
            VpnSessionEvent.NetworkAvailable(10L, 500_000L, connectionStartObserved = false)
        )!!
        val first = reducer.reduce(
            cold,
            VpnSessionEvent.Enriched(
                connectedAtMillis = 289_000L,
                timestampSource = VpnTimestampSource.PROVIDER_NOTIFICATION_ELAPSED,
                observedAtMillis = 500_000L
            )
        )!!
        val update = reducer.reduce(
            first,
            VpnSessionEvent.Enriched(
                connectedAtMillis = 289_850L,
                timestampSource = VpnTimestampSource.PROVIDER_NOTIFICATION_ELAPSED,
                observedAtMillis = 501_850L
            )
        )!!
        assertEquals(289_000L, update.connectedAtMillis)
    }

    @Test fun disconnectFailureDoesNotLieWhileNetworkStillExists() {
        val first = reducer.reduce(null, VpnSessionEvent.NetworkAvailable(10L, 1_000L))!!
        val disconnecting = reducer.reduce(first, VpnSessionEvent.DisconnectRequested(2_000L))!!
        val restored = reducer.reduce(disconnecting, VpnSessionEvent.DisconnectFailed(12_000L))!!
        assertEquals(VpnConnectionState.DISCONNECTING, disconnecting.state)
        assertEquals(VpnConnectionState.CONNECTED, restored.state)
    }

    @Test fun staleProviderDisconnectedStateCannotOverrideExistingNetwork() {
        val connected = reducer.reduce(null, VpnSessionEvent.NetworkAvailable(10L, 1_000L))!!
        val enriched = reducer.reduce(connected, VpnSessionEvent.Enriched(
            providerState = VpnConnectionState.DISCONNECTED,
            observedAtMillis = 2_000L
        ))!!
        assertEquals(VpnConnectionState.CONNECTED, enriched.state)
    }

    @Test fun providerCanRefineBlockingAndWaitingStatesWithoutClaimingAFalseTunnel() {
        val connected = reducer.reduce(null, VpnSessionEvent.NetworkAvailable(10L, 1_000L))!!
        val blocking = reducer.reduce(connected, VpnSessionEvent.Enriched(
            providerState = VpnConnectionState.BLOCKING,
            observedAtMillis = 2_000L
        ))!!
        val lost = reducer.reduce(blocking, VpnSessionEvent.NetworkLost(10L, 3_000L))!!
        val waiting = reducer.reduce(lost, VpnSessionEvent.Enriched(
            providerState = VpnConnectionState.WAITING_FOR_NETWORK,
            observedAtMillis = 3_100L
        ))!!
        assertEquals(VpnConnectionState.BLOCKING, blocking.state)
        assertEquals(VpnConnectionState.WAITING_FOR_NETWORK, waiting.state)
        assertTrue(waiting.networkHandles.isEmpty())
    }

    @Test fun equalConfidenceProviderHandoffClearsOldProviderMetadata() {
        val connected = reducer.reduce(null, VpnSessionEvent.NetworkAvailable(10L, 1_000L))!!
        val providerA = reducer.reduce(connected, VpnSessionEvent.Enriched(
            provider = VpnProviderIdentity("provider.a", "A", 95, VpnEvidenceSource.PROVIDER_ADAPTER),
            destination = VpnDestination("Old server", 90, VpnEvidenceSource.PROVIDER_ADAPTER),
            traffic = VpnTrafficSnapshot(1_000, 1_000, 2_000, VpnEvidenceSource.PROVIDER_ADAPTER),
            observedAtMillis = 2_000L
        ))!!
        val providerB = reducer.reduce(providerA, VpnSessionEvent.Enriched(
            provider = VpnProviderIdentity("provider.b", "B", 95, VpnEvidenceSource.PROVIDER_ADAPTER),
            observedAtMillis = 3_000L
        ))!!
        assertEquals("provider.b", providerB.provider?.packageName)
        assertNull(providerB.destination)
        assertNull(providerB.traffic)
    }

    @Test fun capabilitiesAreRecordedButValidationDoesNotControlExistence() {
        val connected = reducer.reduce(null, VpnSessionEvent.NetworkAvailable(10L, 1_000L))!!
        val metadata = VpnNetworkMetadata(10L, true, false, false, true, setOf(VpnUnderlyingTransport.WIFI))
        val updated = reducer.reduce(connected, VpnSessionEvent.NetworkCapabilitiesChanged(metadata, 2_000L))!!
        assertEquals(VpnConnectionState.CONNECTED, updated.state)
        assertFalse(updated.networkMetadata.getValue(10L).isValidated)
    }
}

