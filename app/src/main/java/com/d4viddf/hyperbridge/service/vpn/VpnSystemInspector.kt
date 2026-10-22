package com.d4viddf.hyperbridge.service.vpn

import android.content.Context
import android.os.IBinder
import android.os.Process
import android.provider.Settings
import com.d4viddf.hyperbridge.util.ShizukuManager
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * Resolves the active VPN package directly from Android system state:
 * 1. Privileged inspection via Shizuku (`IVpnManager.getVpnConfig(userId)` / `IConnectivityManager.getVpnConfig(userId)`),
 *    which returns the exact `VpnConfig.user` package name that owns the active tunnel.
 * 2. System Settings (`Settings.Secure` / `Settings.Global` `always_on_vpn_app`), which reflects the
 *    Always-On VPN package configured in Android's VPN settings.
 */
class VpnSystemInspector(
    private val context: Context,
    private val providerRegistry: VpnProviderRegistry
) {
    fun resolveSystemProvider(allowedProviders: Set<String>): VpnProviderIdentity? {
        resolveShizukuVpnPackage(allowedProviders)?.let { pkg ->
            return VpnProviderIdentity(
                packageName = pkg,
                displayName = providerRegistry.appLabel(pkg),
                confidence = 100,
                source = VpnEvidenceSource.PRIVILEGED_INSPECTION
            )
        }
        resolveAlwaysOnVpnPackage(allowedProviders)?.let { pkg ->
            return VpnProviderIdentity(
                packageName = pkg,
                displayName = providerRegistry.appLabel(pkg),
                confidence = 92,
                source = VpnEvidenceSource.NETWORK_CALLBACK
            )
        }
        return null
    }

    private fun resolveShizukuVpnPackage(allowedProviders: Set<String>): String? {
        if (!ShizukuManager.isPermissionGranted.value) return null
        val userId = Process.myUid() / 100_000
        for ((serviceName, stubClassName) in VPN_SERVICE_BACKENDS) {
            val pkg = runCatching {
                val originalBinder = SystemServiceHelper.getSystemService(serviceName) ?: return@runCatching null
                val wrapper: IBinder = ShizukuBinderWrapper(originalBinder)
                val stubClass = Class.forName(stubClassName)
                val service = stubClass.getMethod("asInterface", IBinder::class.java).invoke(null, wrapper)
                    ?: return@runCatching null
                val method = service.javaClass.methods.firstOrNull {
                    it.name == "getVpnConfig" && it.parameterCount == 1
                } ?: return@runCatching null
                val vpnConfig = method.invoke(service, userId) ?: return@runCatching null
                val userField = vpnConfig.javaClass.getField("user")
                (userField.get(vpnConfig) as? String)?.trim()?.takeIf { it.isNotEmpty() }
            }.getOrNull()
            if (pkg != null && pkg in allowedProviders) {
                return pkg
            }
        }
        return null
    }

    private fun resolveAlwaysOnVpnPackage(allowedProviders: Set<String>): String? {
        val resolver = context.contentResolver
        val securePkg = runCatching {
            Settings.Secure.getString(resolver, ALWAYS_ON_VPN_APP)
        }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        if (securePkg != null && securePkg in allowedProviders) return securePkg

        val globalPkg = runCatching {
            Settings.Global.getString(resolver, ALWAYS_ON_VPN_APP)
        }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        if (globalPkg != null && globalPkg in allowedProviders) return globalPkg

        return null
    }

    companion object {
        private const val ALWAYS_ON_VPN_APP = "always_on_vpn_app"
        private val VPN_SERVICE_BACKENDS = listOf(
            "vpn_management" to "android.net.IVpnManager\$Stub",
            Context.CONNECTIVITY_SERVICE to "android.net.IConnectivityManager\$Stub"
        )
    }
}
