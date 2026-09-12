package com.d4viddf.hyperbridge.service.vpn

enum class VpnConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    WAITING_FOR_NETWORK,
    PAUSED,
    DISCONNECTING,
    BLOCKING,
    ERROR
}
enum class VpnEvidenceSource { NETWORK_CALLBACK, PROVIDER_ADAPTER, STRUCTURED_NOTIFICATION, NOTIFICATION_TEXT, PRIVILEGED_INSPECTION }
enum class VpnTimestampSource {
    PROVIDER_API,
    PROVIDER_NOTIFICATION_TIMESTAMP,
    PROVIDER_NOTIFICATION_ELAPSED,
    NOTIFICATION_CHRONOMETER,
    NETWORK_OBSERVED,
    UNKNOWN
}
enum class VpnControlProvenance { SOURCE_PENDING_INTENT, PROVIDER_API, PUBLIC_SYSTEM_API, SHIZUKU_STOP_APP, SHIZUKU_FORCE_STOP, ROOT_FORCE_STOP, UNSUPPORTED }
enum class VpnUnderlyingTransport { WIFI, CELLULAR, ETHERNET, BLUETOOTH, OTHER }

data class VpnNetworkMetadata(
    val networkHandle: Long,
    val hasInternet: Boolean,
    val isValidated: Boolean,
    val isNotMetered: Boolean,
    val isNotSuspended: Boolean,
    val underlyingTransports: Set<VpnUnderlyingTransport>
)

data class VpnProviderIdentity(
    val packageName: String,
    val displayName: String,
    val confidence: Int,
    val source: VpnEvidenceSource
) { init { require(confidence in 0..100) } }

data class VpnDestination(
    val label: String,
    val confidence: Int,
    val source: VpnEvidenceSource,
    val countryCode: String? = null
) {
    init {
        require(label.isNotBlank())
        require(confidence in 0..100)
        require(countryCode == null || countryCode.matches(Regex("[A-Z]{2}")))
    }
}

data class VpnTrafficSnapshot(
    val downloadBytesPerSecond: Long?,
    val uploadBytesPerSecond: Long?,
    val observedAtMillis: Long,
    val source: VpnEvidenceSource
) {
    val hasUsefulData: Boolean get() = (downloadBytesPerSecond ?: 0L) > 0L || (uploadBytesPerSecond ?: 0L) > 0L
}

data class VpnSession(
    val logicalId: String,
    val generation: Long,
    val state: VpnConnectionState,
    val providerState: VpnConnectionState? = null,
    val networkHandles: Set<Long>,
    val networkMetadata: Map<Long, VpnNetworkMetadata> = emptyMap(),
    val connectedAtMillis: Long?,
    val timestampSource: VpnTimestampSource,
    val provider: VpnProviderIdentity? = null,
    val destination: VpnDestination? = null,
    val traffic: VpnTrafficSnapshot? = null,
    val controlProvenance: VpnControlProvenance = VpnControlProvenance.UNSUPPORTED,
    val nativeProviderIslandPresent: Boolean = false,
    val lastUpdatedAtMillis: Long
)

sealed interface VpnSessionEvent {
    val observedAtMillis: Long
    data class NetworkAvailable(
        val networkHandle: Long,
        override val observedAtMillis: Long,
        val connectionStartObserved: Boolean = true
    ) : VpnSessionEvent
    data class NetworkLost(val networkHandle: Long, override val observedAtMillis: Long) : VpnSessionEvent
    data class NetworkCapabilitiesChanged(
        val metadata: VpnNetworkMetadata,
        override val observedAtMillis: Long
    ) : VpnSessionEvent
    data class Enriched(
        val provider: VpnProviderIdentity? = null,
        val destination: VpnDestination? = null,
        val traffic: VpnTrafficSnapshot? = null,
        val connectedAtMillis: Long? = null,
        val timestampSource: VpnTimestampSource = VpnTimestampSource.UNKNOWN,
        val controlProvenance: VpnControlProvenance? = null,
        val providerState: VpnConnectionState? = null,
        val nativeProviderIslandPresent: Boolean? = null,
        override val observedAtMillis: Long
    ) : VpnSessionEvent
    data class DisconnectRequested(override val observedAtMillis: Long) : VpnSessionEvent
    data class DisconnectFailed(override val observedAtMillis: Long) : VpnSessionEvent
    data class HandoffGraceExpired(val generation: Long, override val observedAtMillis: Long) : VpnSessionEvent
}

