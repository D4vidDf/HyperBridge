package com.d4viddf.hyperbridge.service.vpn

data class VpnPresentationPlan(
    val providerPackageName: String?,
    val destinationLabel: String?,
    val countryCode: String?,
    val showDuration: Boolean,
    val showTraffic: Boolean,
    val controlKind: VpnControlKind
)

object VpnPresentationPolicy {
    fun plan(session: VpnSession, hasVerifiedDisconnect: Boolean): VpnPresentationPlan =
        VpnPresentationPlan(
            providerPackageName = session.provider
                ?.takeIf { it.confidence >= PROVIDER_ICON_CONFIDENCE }
                ?.packageName,
            destinationLabel = session.destination
                ?.takeIf { it.confidence >= DESTINATION_CONFIDENCE }
                ?.label,
            countryCode = session.destination
                ?.takeIf { it.confidence >= DESTINATION_CONFIDENCE }
                ?.countryCode,
            showDuration = session.connectedAtMillis != null,
            showTraffic = session.traffic?.hasUsefulData == true,
            controlKind = if (hasVerifiedDisconnect) VpnControlKind.DISCONNECT else VpnControlKind.MANAGE
        )

    const val PROVIDER_ICON_CONFIDENCE = 80
    const val DESTINATION_CONFIDENCE = 75
}

