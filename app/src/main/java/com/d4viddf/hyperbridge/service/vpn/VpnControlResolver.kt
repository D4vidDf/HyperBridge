package com.d4viddf.hyperbridge.service.vpn

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.d4viddf.hyperbridge.service.vpn.providers.KnownVpnProviderAdapters

enum class VpnControlKind { DISCONNECT, MANAGE }

data class VpnResolvedControl(
    val kind: VpnControlKind,
    val provenance: VpnControlProvenance,
    val pendingIntent: PendingIntent
)

class VpnControlResolver(private val context: Context) {
    fun resolveDisconnect(session: VpnSession, sourcePendingIntent: PendingIntent?): VpnResolvedControl? {
        sourcePendingIntent?.let {
            return VpnResolvedControl(VpnControlKind.DISCONNECT, VpnControlProvenance.SOURCE_PENDING_INTENT, it)
        }
        val provider = session.provider?.takeIf { it.confidence >= PROVIDER_CONTROL_CONFIDENCE } ?: return null
        val adapter = KnownVpnProviderAdapters.forPackage(provider.packageName) ?: return null
        val pendingIntent = adapter.createDisconnectPendingIntent(context, provider.packageName) ?: return null
        return VpnResolvedControl(
            VpnControlKind.DISCONNECT,
            adapter.capabilities.directDisconnect ?: VpnControlProvenance.UNSUPPORTED,
            pendingIntent
        ).takeIf { it.provenance != VpnControlProvenance.UNSUPPORTED }
    }

    fun manage(session: VpnSession, sourceContentIntent: PendingIntent?): VpnResolvedControl {
        sourceContentIntent?.let {
            return VpnResolvedControl(VpnControlKind.MANAGE, VpnControlProvenance.SOURCE_PENDING_INTENT, it)
        }
        // Provider confidence gates destructive controls, but it must not gate opening the
        // provider app. The identity already comes from an installed VPN service/notification;
        // applying the disconnect threshold here made the manage icon fall back to HyperBridge.
        val providerPackage = session.provider?.packageName
        val intent = providerPackage
            ?.let(context.packageManager::getLaunchIntentForPackage)
            ?: Intent(Settings.ACTION_VPN_SETTINGS)
        return VpnResolvedControl(
            VpnControlKind.MANAGE,
            VpnControlProvenance.PUBLIC_SYSTEM_API,
            PendingIntent.getActivity(
                context,
                providerPackage?.hashCode() ?: MANAGE_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    companion object {
        private const val PROVIDER_CONTROL_CONFIDENCE = 80
        private const val MANAGE_REQUEST_CODE = 0x56504E
    }
}

