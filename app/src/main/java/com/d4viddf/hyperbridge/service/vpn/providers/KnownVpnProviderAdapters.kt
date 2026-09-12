package com.d4viddf.hyperbridge.service.vpn.providers

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.d4viddf.hyperbridge.service.vpn.VpnControlProvenance
import com.d4viddf.hyperbridge.service.vpn.VpnConnectionState
import com.d4viddf.hyperbridge.service.vpn.VpnNotificationSignals

object KnownVpnProviderAdapters {
    val all: List<VpnProviderAdapter> = listOf(
        ProtonVpnAdapter,
        MullvadAdapter,
        IvpnAdapter,
        OpenVpnForAndroidAdapter,
        StrongSwanAdapter,
        WireGuardAdapter,
        TailscaleAdapter
    )

    fun forPackage(packageName: String): VpnProviderAdapter? = all.firstOrNull { it.matches(packageName) }
}

private object ProtonVpnAdapter : VpnProviderAdapter {
    override val id = "proton_vpn"
    override val packageNames = setOf("ch.protonvpn.android")
    override val capabilities = VpnAdapterCapabilities(
        notificationMetadata = true,
        notificationState = true,
        notificationTraffic = true,
        notificationTimestamp = true,
        directDisconnect = VpnControlProvenance.SOURCE_PENDING_INTENT
    )
    override val stateStringResources = mapOf(
        VpnConnectionState.DISCONNECTING to setOf("state_disconnecting"),
        VpnConnectionState.RECONNECTING to setOf("loaderReconnecting"),
        VpnConnectionState.WAITING_FOR_NETWORK to setOf("loaderReconnectNoNetwork"),
        VpnConnectionState.ERROR to setOf("state_error")
    )
    override val destinationStringResources = setOf("loaderConnectedTo")

    override fun analyzeNotification(signals: VpnNotificationSignals): VpnAdapterNotificationResult {
        // Proton's current ongoing CATEGORY_SERVICE status notification exposes exactly one
        // action for Connecting/Connected, backed by its NotificationActionReceiver.
        val action = signals.actions.singleOrNull()?.takeIf {
            signals.isOngoing && signals.category == Notification.CATEGORY_SERVICE && it.hasPendingIntent
        }
        return VpnAdapterNotificationResult(disconnectActionIndex = action?.index)
    }
}

private object MullvadAdapter : VpnProviderAdapter {
    override val id = "mullvad"
    override val packageNames = setOf("net.mullvad.mullvadvpn")
    override val capabilities = VpnAdapterCapabilities(true, true, false, false)
    override val stateStringResources = mapOf(
        VpnConnectionState.CONNECTED to setOf("connected"),
        VpnConnectionState.CONNECTING to setOf("connecting"),
        VpnConnectionState.DISCONNECTING to setOf("disconnecting"),
        VpnConnectionState.BLOCKING to setOf("blocking", "blocking_internet"),
        VpnConnectionState.WAITING_FOR_NETWORK to setOf("blocking_internet_device_offline"),
        VpnConnectionState.ERROR to setOf("critical_error", "vpn_permission_error_notification_title")
    )
    override val trustsContentAsConnectedDestination = true

    override fun analyzeNotification(signals: VpnNotificationSignals): VpnAdapterNotificationResult {
        val disconnectIcons = signals.actions.filter {
            it.hasPendingIntent && it.iconResourceName == "icon_notification_disconnect"
        }
        val disconnect = if (disconnectIcons.size >= 2) disconnectIcons.last() else disconnectIcons.singleOrNull()
        return VpnAdapterNotificationResult(disconnectActionIndex = disconnect?.index)
    }
}

private object IvpnAdapter : VpnProviderAdapter {
    override val id = "ivpn"
    override val packageNames = setOf("net.ivpn.client")
    override val capabilities = VpnAdapterCapabilities(
        notificationMetadata = true,
        notificationState = true,
        notificationTraffic = false,
        notificationTimestamp = false,
        directDisconnect = VpnControlProvenance.SOURCE_PENDING_INTENT
    )
    override val stateStringResources = mapOf(
        VpnConnectionState.CONNECTED to setOf("notification_title_connected"),
        VpnConnectionState.CONNECTING to setOf("notification_title_connecting"),
        VpnConnectionState.PAUSED to setOf("notification_title_paused"),
        VpnConnectionState.DISCONNECTING to setOf("notification_title_disconnecting")
    )
    override val trustsContentAsConnectedDestination = true

    override fun analyzeNotification(signals: VpnNotificationSignals): VpnAdapterNotificationResult {
        val paused = signals.actions.any { it.iconResourceName == "ic_play" }
        val disconnect = signals.actions.firstOrNull {
            it.hasPendingIntent && it.iconResourceName in setOf("ic_notifications_disconnect", "ic_stop")
        }
        return VpnAdapterNotificationResult(
            state = if (paused) com.d4viddf.hyperbridge.service.vpn.VpnConnectionState.PAUSED else null,
            disconnectActionIndex = disconnect?.index
        )
    }
}

private object OpenVpnForAndroidAdapter : VpnProviderAdapter {
    override val id = "openvpn_for_android"
    override val packageNames = setOf("de.blinkt.openvpn")
    override val capabilities = VpnAdapterCapabilities(
        notificationMetadata = true,
        notificationState = true,
        notificationTraffic = false,
        notificationTimestamp = false,
        directDisconnect = VpnControlProvenance.PROVIDER_API
    )

    override fun createDisconnectPendingIntent(context: Context, packageName: String): PendingIntent? {
        val intent = Intent(Intent.ACTION_MAIN).setComponent(
            ComponentName(packageName, "de.blinkt.openvpn.api.DisconnectVPN")
        )
        val resolved = context.packageManager.resolveActivity(
            intent,
            PackageManager.ResolveInfoFlags.of(0)
        )?.activityInfo?.takeIf { it.exported } ?: return null
        return PendingIntent.getActivity(
            context,
            id.hashCode(),
            intent.setComponent(ComponentName(resolved.packageName, resolved.name)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

private object StrongSwanAdapter : VpnProviderAdapter {
    override val id = "strongswan"
    override val packageNames = setOf("org.strongswan.android")
    override val capabilities = VpnAdapterCapabilities(true, true, false, false)
    // Current automation needs the active VPN_PROFILE_UUID. Generic notification evidence
    // does not prove that UUID, so no direct command is exposed here.
}

private object WireGuardAdapter : VpnProviderAdapter {
    override val id = "wireguard"
    override val packageNames = setOf("com.wireguard.android")
    override val capabilities = VpnAdapterCapabilities(
        notificationMetadata = true,
        notificationState = false,
        notificationTraffic = false,
        notificationTimestamp = false,
        requiredPermission = "com.wireguard.android.permission.CONTROL_TUNNELS"
    )
    // Current official source returns before SET_TUNNEL_UP/DOWN handling. Observation only.
}

private object TailscaleAdapter : VpnProviderAdapter {
    override val id = "tailscale"
    override val packageNames = setOf("com.tailscale.ipn")
    override val capabilities = VpnAdapterCapabilities(
        notificationMetadata = true,
        notificationState = true,
        notificationTraffic = false,
        notificationTimestamp = false,
        directDisconnect = VpnControlProvenance.PROVIDER_API
    )

    override fun createDisconnectPendingIntent(context: Context, packageName: String): PendingIntent? {
        val intent = Intent("com.tailscale.ipn.DISCONNECT_VPN").setPackage(packageName)
        val exportedReceiver = context.packageManager.queryBroadcastReceivers(
            intent,
            PackageManager.ResolveInfoFlags.of(0)
        ).any { it.activityInfo?.exported == true }
        if (!exportedReceiver) return null
        return PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

