package com.d4viddf.hyperbridge.service.vpn

import kotlin.math.abs

class VpnSessionReducer {
    private var nextGeneration = 0L

    fun reduce(previous: VpnSession?, event: VpnSessionEvent): VpnSession? = when (event) {
        is VpnSessionEvent.NetworkAvailable -> if (previous == null) {
            val generation = ++nextGeneration
            VpnSession(
                logicalId = "vpn:$generation", generation = generation,
                state = VpnConnectionState.CONNECTED,
                networkHandles = setOf(event.networkHandle),
                connectedAtMillis = event.observedAtMillis.takeIf { event.connectionStartObserved },
                timestampSource = if (event.connectionStartObserved) {
                    VpnTimestampSource.NETWORK_OBSERVED
                } else {
                    VpnTimestampSource.UNKNOWN
                },
                lastUpdatedAtMillis = event.observedAtMillis
            )
        } else previous.copy(
            state = VpnConnectionState.CONNECTED,
            networkHandles = previous.networkHandles + event.networkHandle,
            lastUpdatedAtMillis = event.observedAtMillis
        )
        is VpnSessionEvent.NetworkLost -> previous?.let {
            val remaining = it.networkHandles - event.networkHandle
            it.copy(
                state = if (remaining.isEmpty()) VpnConnectionState.RECONNECTING else it.state,
                networkHandles = remaining,
                networkMetadata = it.networkMetadata - event.networkHandle,
                lastUpdatedAtMillis = event.observedAtMillis
            )
        }
        is VpnSessionEvent.NetworkCapabilitiesChanged -> previous?.takeIf {
            event.metadata.networkHandle in it.networkHandles
        }?.copy(
            networkMetadata = previous.networkMetadata + (event.metadata.networkHandle to event.metadata),
            lastUpdatedAtMillis = event.observedAtMillis
        ) ?: previous
        is VpnSessionEvent.Enriched -> previous?.let {
            if (
                it.networkHandles.isEmpty() &&
                event.providerState == null &&
                (event.provider == null || it.providerState in persistentNoNetworkStates)
            ) return null
            val provider = chooseProvider(it.provider, event.provider)
            val providerChanged = it.provider != null && provider?.packageName != it.provider.packageName
            val destination = chooseDestination(if (providerChanged) null else it.destination, event.destination)
            val timestamp = chooseTimestamp(it, event)
            val state = authoritativeState(it, event.providerState)
            it.copy(
                state = state,
                providerState = event.providerState,
                provider = provider,
                destination = destination,
                traffic = if (providerChanged) event.traffic else event.traffic ?: it.traffic,
                connectedAtMillis = timestamp.first,
                timestampSource = timestamp.second,
                controlProvenance = event.controlProvenance ?: it.controlProvenance,
                nativeProviderIslandPresent = event.nativeProviderIslandPresent ?: it.nativeProviderIslandPresent,
                lastUpdatedAtMillis = event.observedAtMillis
            )
        }
        is VpnSessionEvent.DisconnectRequested -> previous?.takeIf { it.networkHandles.isNotEmpty() }?.copy(
            state = VpnConnectionState.DISCONNECTING, lastUpdatedAtMillis = event.observedAtMillis
        )
        is VpnSessionEvent.DisconnectFailed -> previous?.takeIf { it.networkHandles.isNotEmpty() }?.copy(
            state = VpnConnectionState.CONNECTED, lastUpdatedAtMillis = event.observedAtMillis
        )
        is VpnSessionEvent.HandoffGraceExpired -> previous?.let {
            if (
                it.generation == event.generation &&
                it.networkHandles.isEmpty() &&
                it.providerState !in persistentNoNetworkStates
            ) null else it
        }
    }

    private fun chooseProvider(current: VpnProviderIdentity?, candidate: VpnProviderIdentity?) = when {
        candidate == null -> current
        current == null -> candidate
        candidate.packageName == current.packageName && candidate.confidence >= current.confidence -> candidate
        candidate.confidence > current.confidence -> candidate
        candidate.confidence == current.confidence && candidate.packageName != current.packageName -> candidate
        else -> current
    }

    private fun authoritativeState(current: VpnSession, providerState: VpnConnectionState?): VpnConnectionState {
        if (current.networkHandles.isEmpty()) {
            return when (providerState) {
                VpnConnectionState.RECONNECTING,
                VpnConnectionState.WAITING_FOR_NETWORK,
                VpnConnectionState.BLOCKING,
                VpnConnectionState.ERROR -> providerState
                else -> current.state
            }
        }
        return when (providerState) {
            null -> when {
                current.state == VpnConnectionState.DISCONNECTING -> current.state
                current.providerState != null -> VpnConnectionState.CONNECTED
                else -> current.state
            }
            VpnConnectionState.DISCONNECTED -> current.state
            else -> providerState
        }
    }

    private fun chooseDestination(current: VpnDestination?, candidate: VpnDestination?) = when {
        candidate == null -> current
        current == null -> candidate
        candidate.confidence >= current.confidence -> candidate
        else -> current
    }

    private fun chooseTimestamp(current: VpnSession, event: VpnSessionEvent.Enriched): Pair<Long?, VpnTimestampSource> {
        val candidate = event.connectedAtMillis
        if (candidate == null || event.timestampSource == VpnTimestampSource.UNKNOWN) return current.connectedAtMillis to current.timestampSource
        if (
            event.timestampSource == VpnTimestampSource.PROVIDER_NOTIFICATION_ELAPSED &&
            current.timestampSource == VpnTimestampSource.PROVIDER_NOTIFICATION_ELAPSED &&
            current.connectedAtMillis != null && abs(candidate - current.connectedAtMillis) <= ELAPSED_TIMESTAMP_JITTER_MS
        ) {
            return current.connectedAtMillis to current.timestampSource
        }
        return if (timestampRank(event.timestampSource) >= timestampRank(current.timestampSource)) {
            candidate to event.timestampSource
        } else current.connectedAtMillis to current.timestampSource
    }

    private fun timestampRank(source: VpnTimestampSource) = when (source) {
        VpnTimestampSource.UNKNOWN -> 0
        VpnTimestampSource.NETWORK_OBSERVED -> 1
        VpnTimestampSource.PROVIDER_NOTIFICATION_TIMESTAMP -> 2
        VpnTimestampSource.PROVIDER_NOTIFICATION_ELAPSED -> 2
        VpnTimestampSource.NOTIFICATION_CHRONOMETER -> 2
        VpnTimestampSource.PROVIDER_API -> 3
    }

    private val persistentNoNetworkStates = setOf(
        VpnConnectionState.CONNECTING,
        VpnConnectionState.RECONNECTING,
        VpnConnectionState.WAITING_FOR_NETWORK,
        VpnConnectionState.BLOCKING,
        VpnConnectionState.ERROR
    )

    companion object {
        private const val ELAPSED_TIMESTAMP_JITTER_MS = 2_500L
    }
}

