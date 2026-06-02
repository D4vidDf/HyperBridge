package com.d4viddf.hyperbridge.integration.shizuku

import android.content.Context
import android.os.IBinder
import android.util.Log
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * Minimal "hook mode" helper adapted from InstallerX Revived.
 *
 * Instead of hopping through our own Shizuku user service, we wrap the original
 * system Connectivity binder with [ShizukuBinderWrapper] and invoke the hidden
 * firewall APIs directly through that hooked binder.
 */
object ShizukuHook {
    private const val TAG = "ShizukuHook"
    private const val OEM_DENY_CHAIN = 9

    private data class ServiceBackend(
        val serviceName: String,
        val stubClassName: String,
        val label: String
    )

    private val serviceBackends = listOf(
        ServiceBackend(
            serviceName = Context.CONNECTIVITY_SERVICE,
            stubClassName = "android.net.IConnectivityManager\$Stub",
            label = "ConnectivityManager"
        ),
        ServiceBackend(
            serviceName = "network_management",
            stubClassName = "android.os.INetworkManagementService\$Stub",
            label = "NetworkManagementService"
        )
    )

    private val hookedServiceCache = ConcurrentHashMap<String, Any>()

    fun setPackageNetworkingEnabled(uid: Int, enabled: Boolean) {
        val rule = if (enabled) 0 else 2
        val failures = mutableListOf<String>()

        try {
            val cm = getHookedConnectivityManager()
            if (!enabled) {
                cm.javaClass.getMethod("setFirewallChainEnabled", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType).invoke(cm, OEM_DENY_CHAIN, true)
                Log.d(TAG, "Enabled firewall chain $OEM_DENY_CHAIN via typed IConnectivityManager before blocking uid=$uid")
            }
            cm.javaClass.getMethod("setUidFirewallRule", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType).invoke(cm, OEM_DENY_CHAIN, uid, rule)
            Log.d(TAG, "Network ${if (enabled) "restored" else "blocked"} for uid=$uid via typed IConnectivityManager")
            return
        } catch (t: Throwable) {
            val detail = "Typed IConnectivityManager failed: ${t.message}"
            failures += detail
            Log.w(TAG, detail, t)
        }

        try {
            val nm = getHookedNetworkManagementService()
            if (!enabled) {
                nm.javaClass.getMethod("setFirewallChainEnabled", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType).invoke(nm, OEM_DENY_CHAIN, true)
                Log.d(TAG, "Enabled firewall chain $OEM_DENY_CHAIN via typed INetworkManagementService before blocking uid=$uid")
            }
            try {
                nm.javaClass.getMethod("setUidFirewallRule", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType).invoke(nm, OEM_DENY_CHAIN, uid, rule)
                Log.d(TAG, "Network ${if (enabled) "restored" else "blocked"} for uid=$uid via typed INetworkManagementService.setUidFirewallRule")
            } catch (_: Throwable) {
                nm.javaClass.getMethod("setFirewallUidRule", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType).invoke(nm, OEM_DENY_CHAIN, uid, rule)
                Log.d(TAG, "Network ${if (enabled) "restored" else "blocked"} for uid=$uid via typed INetworkManagementService.setFirewallUidRule")
            }
            return
        } catch (t: Throwable) {
            val detail = "Typed INetworkManagementService failed: ${t.message}"
            failures += detail
            Log.w(TAG, detail, t)
        }

        for (backend in serviceBackends) {
            try {
                val service = getHookedService(backend)
                Log.d(TAG, "Trying ${backend.label} backend for uid=$uid, enabled=$enabled")

                if (!enabled) {
                    callMethodResilient(
                        service,
                        listOf("setFirewallChainEnabled"),
                        OEM_DENY_CHAIN,
                        true
                    )
                    Log.d(TAG, "Enabled firewall chain $OEM_DENY_CHAIN via ${backend.label} before blocking uid=$uid")
                }

                val methodUsed = callMethodResilient(
                    service,
                    listOf("setUidFirewallRule", "setFirewallUidRule"),
                    OEM_DENY_CHAIN,
                    uid,
                    rule
                )
                Log.d(
                    TAG,
                    "Network ${if (enabled) "restored" else "blocked"} for uid=$uid via ${backend.label}.$methodUsed"
                )
                return
            } catch (t: Throwable) {
                val available = summarizeFirewallMethods(backend)
                val detail = buildString {
                    append("${backend.label} failed: ${t.message}")
                    if (available.isNotBlank()) {
                        append(" | available=$available")
                    }
                }
                failures += detail
                Log.w(TAG, detail, t)
            }
        }

        throw IllegalStateException(
            "No compatible firewall backend found for uid=$uid. ${failures.joinToString(" || ")}"
        )
    }

    private fun getHookedConnectivityManager(): Any {
        return getHookedService(
            ServiceBackend(
                serviceName = Context.CONNECTIVITY_SERVICE,
                stubClassName = "android.net.IConnectivityManager\$Stub",
                label = "ConnectivityManager"
            )
        )
    }

    private fun getHookedNetworkManagementService(): Any {
        return getHookedService(
            ServiceBackend(
                serviceName = "network_management",
                stubClassName = "android.os.INetworkManagementService\$Stub",
                label = "NetworkManagementService"
            )
        )
    }

    private fun getHookedService(backend: ServiceBackend): Any {
        hookedServiceCache[backend.stubClassName]?.let { return it }

        return synchronized(this) {
            hookedServiceCache[backend.stubClassName]?.let { return@synchronized it }

            val originalBinder = SystemServiceHelper.getSystemService(backend.serviceName)
                ?: throw IllegalStateException("${backend.label} binder is null")

            val wrapper: IBinder = ShizukuBinderWrapper(originalBinder)
            val stubClass = Class.forName(backend.stubClassName)
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            val hooked = asInterface.invoke(null, wrapper) ?: throw IllegalStateException(
                "${backend.label}.Stub.asInterface returned null"
            )

            hookedServiceCache[backend.stubClassName] = hooked
            Log.i(TAG, "Created hooked ${backend.label} binder wrapper")
            hooked
        }
    }

    private fun callMethodResilient(target: Any, methodNames: List<String>, vararg args: Any): String {
        val methods = target.javaClass.methods.filter {
            it.name in methodNames && it.parameterCount == args.size
        }
        if (methods.isEmpty()) {
            throw NoSuchMethodException(
                "Could not find any of ${methodNames.joinToString()} with ${args.size} args on ${target.javaClass.name}"
            )
        }

        var lastError: Throwable? = null
        for (method in methods) {
            try {
                invokeMethod(target, method, args)
                return method.name
            } catch (t: Throwable) {
                lastError = t
            }
        }

        throw lastError ?: NoSuchMethodException(
            "Methods ${methodNames.joinToString()} exist on ${target.javaClass.name} but none accepted the arguments"
        )
    }

    private fun invokeMethod(target: Any, method: java.lang.reflect.Method, args: Array<out Any>) {
        method.isAccessible = true
        val adaptedArgs = Array(args.size) { index ->
            val arg = args[index]
            when (val expected = method.parameterTypes[index]) {
                Int::class.javaPrimitiveType -> when (arg) {
                    is Int -> arg
                    is Boolean -> if (arg) 1 else 0
                    is Number -> arg.toInt()
                    else -> throw IllegalArgumentException("Unsupported arg $arg for int parameter")
                }

                Boolean::class.javaPrimitiveType -> when (arg) {
                    is Boolean -> arg
                    is Number -> arg.toInt() != 0
                    else -> throw IllegalArgumentException("Unsupported arg $arg for boolean parameter")
                }

                else -> if (expected.isInstance(arg)) arg
                else throw IllegalArgumentException("Arg $arg does not match ${expected.name}")
            }
        }

        try {
            method.invoke(target, *adaptedArgs)
        } catch (e: InvocationTargetException) {
            throw e.targetException ?: e
        }
    }

    private fun summarizeFirewallMethods(backend: ServiceBackend): String {
        return runCatching {
            val service = getHookedService(backend)
            service.javaClass.methods
                .filter { method ->
                    method.name.contains("Firewall", ignoreCase = true) ||
                        method.name.contains("firewall", ignoreCase = true)
                }
                .sortedWith(compareBy({ it.name }, { it.parameterCount }))
                .joinToString("; ") { method ->
                    val params = method.parameterTypes.joinToString(",") { it.simpleName }
                    "${method.name}($params)"
                }
        }.getOrElse { "" }
    }
}


