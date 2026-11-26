package com.d4viddf.hyperbridge.service.vpn

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.annotation.SuppressLint
import com.d4viddf.hyperbridge.service.vpn.providers.KnownVpnProviderAdapters
import com.d4viddf.hyperbridge.service.vpn.providers.VpnProviderAdapter

class VpnProviderRegistry(private val context: Context) {
    data class NotificationHints(
        val stateLabels: Map<String, VpnConnectionState>,
        val destinationTemplates: List<String>
    )
    @Volatile private var cached: Set<String>? = null
    fun providerPackages(refresh: Boolean = false): Set<String> {
        if (!refresh) cached?.let { return it }
        val packages = context.packageManager.queryIntentServices(
            Intent(VpnService.SERVICE_INTERFACE), PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
        ).mapNotNull { it.serviceInfo?.packageName }.toSet()
        cached = packages; return packages
    }
    fun appLabel(packageName: String): String = try {
        val info = context.packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        context.packageManager.getApplicationLabel(info).toString()
    } catch (_: PackageManager.NameNotFoundException) { packageName }

    fun adapterFor(packageName: String): VpnProviderAdapter? = KnownVpnProviderAdapters.forPackage(packageName)

    fun resourceEntryName(packageName: String, resourceId: Int): String? = runCatching {
        context.packageManager.getResourcesForApplication(packageName).getResourceEntryName(resourceId)
    }.getOrNull()

    @SuppressLint("DiscouragedApi")
    fun notificationHints(packageName: String): NotificationHints {
        val adapter = adapterFor(packageName) ?: return NotificationHints(emptyMap(), emptyList())
        val resources = runCatching { context.packageManager.getResourcesForApplication(packageName) }.getOrNull()
            ?: return NotificationHints(emptyMap(), emptyList())
        fun stringFor(name: String): String? = runCatching {
            val id = resources.getIdentifier(name, "string", packageName)
            id.takeIf { it != 0 }?.let(resources::getString)
        }.getOrNull()
        val states = buildMap {
            adapter.stateStringResources.forEach { (state, names) ->
                names.mapNotNull(::stringFor).filterNot { '%' in it }.forEach { put(it, state) }
            }
        }
        return NotificationHints(states, adapter.destinationStringResources.mapNotNull(::stringFor))
    }
}

