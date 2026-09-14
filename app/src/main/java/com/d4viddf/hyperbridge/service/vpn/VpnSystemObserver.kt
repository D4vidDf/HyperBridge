package com.d4viddf.hyperbridge.service.vpn

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.LinkProperties
import android.net.NetworkRequest
import androidx.annotation.RequiresPermission

class VpnSystemObserver(context: Context, private val listener: Listener) {
    interface Listener {
        fun onVpnNetworkAvailable(networkHandle: Long, observedAtMillis: Long, wasAlreadyPresent: Boolean)
        fun onVpnNetworkCapabilitiesChanged(metadata: VpnNetworkMetadata, observedAtMillis: Long)
        fun onVpnLinkPropertiesChanged(networkHandle: Long, observedAtMillis: Long)
        fun onVpnNetworkLost(networkHandle: Long, observedAtMillis: Long)
    }
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private var started = false
    private val preexistingHandles = mutableSetOf<Long>()
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val wasAlreadyPresent = synchronized(preexistingHandles) { preexistingHandles.remove(network.networkHandle) }
            listener.onVpnNetworkAvailable(network.networkHandle, System.currentTimeMillis(), wasAlreadyPresent)
        }
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            listener.onVpnNetworkCapabilitiesChanged(capabilities.toMetadata(network.networkHandle), System.currentTimeMillis())
        }
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            listener.onVpnLinkPropertiesChanged(network.networkHandle, System.currentTimeMillis())
        }
        override fun onLost(network: Network) = listener.onVpnNetworkLost(network.networkHandle, System.currentTimeMillis())
    }

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun start() {
        if (started) return
        val existing = connectivityManager.allNetworks.filter { network ->
            connectivityManager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        }.mapTo(mutableSetOf()) { it.networkHandle }
        synchronized(preexistingHandles) { preexistingHandles.clear(); preexistingHandles.addAll(existing) }
        val request = NetworkRequest.Builder().clearCapabilities()
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN).setIncludeOtherUidNetworks(true).build()
        connectivityManager.registerNetworkCallback(request, callback); started = true
    }
    fun stop() {
        if (started) runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        synchronized(preexistingHandles) { preexistingHandles.clear() }
        started = false
    }

    private fun NetworkCapabilities.toMetadata(networkHandle: Long): VpnNetworkMetadata {
        val transports = buildSet {
            if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add(VpnUnderlyingTransport.WIFI)
            if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add(VpnUnderlyingTransport.CELLULAR)
            if (hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add(VpnUnderlyingTransport.ETHERNET)
            if (hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) add(VpnUnderlyingTransport.BLUETOOTH)
        }
        return VpnNetworkMetadata(
            networkHandle = networkHandle,
            hasInternet = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            isValidated = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            isNotMetered = hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            isNotSuspended = hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED),
            underlyingTransports = transports
        )
    }

}

