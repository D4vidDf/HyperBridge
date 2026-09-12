package com.d4viddf.hyperbridge.service.vpn

object VpnControlRegistry {
    @Volatile private var handler: ((String) -> Unit)? = null

    fun register(value: (String) -> Unit) { handler = value }
    fun unregister(value: (String) -> Unit) { if (handler === value) handler = null }
    fun requestDisconnect(logicalId: String) { handler?.invoke(logicalId) }
}

