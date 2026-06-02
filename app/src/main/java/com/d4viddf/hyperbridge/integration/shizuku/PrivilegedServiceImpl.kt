package com.d4viddf.hyperbridge.integration.shizuku

import androidx.annotation.Keep
import com.d4viddf.hyperbridge.IPrivilegedService
import com.d4viddf.hyperbridge.IPrivilegedLogCallback
import android.util.Log
import android.os.IBinder
import android.os.IInterface
import java.lang.reflect.InvocationTargetException
import android.os.Handler
import android.os.HandlerThread
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@Keep
class PrivilegedServiceImpl : IPrivilegedService.Stub() {

    companion object {
        private const val TAG = "PrivilegedServiceImpl"
        private const val OP_TIMEOUT_MS = 3000L
        private val workerThread: HandlerThread by lazy {
            HandlerThread("PrivilegedServiceWorker").apply { start() }
        }
        private val workerHandler: Handler by lazy { Handler(workerThread.looper) }
        
        init {
            try {
                Log.d(TAG, "⚡ PrivilegedServiceImpl class loaded")
            } catch (ignored: Exception) {}
        }
    }
    
    init {
        try {
            logD("⚡ PrivilegedServiceImpl instance created")
        } catch (ignored: Exception) {}
    }

    @Volatile private var logCallback: IPrivilegedLogCallback? = null

    override fun setLogCallback(callback: IPrivilegedLogCallback?) {
        logCallback = callback
        logD("Log callback set: ${callback != null}")
    }

    override fun setPackageNetworkingEnabled(uid: Int, enabled: Boolean): Boolean {
        logD("🚀 ENTRY: setPackageNetworkingEnabled(uid=$uid, enabled=$enabled)")
        
        try {
            val resultRef = AtomicReference<Result<Boolean>?>(null)
            val latch = CountDownLatch(1)

            workerHandler.post {
                val result = runCatching {
                    logD("Step 1: Getting ConnectivityManager...")
                    val realCm = getConnectivityManagerInstance()
                    logD("Step 2: Got ConnectivityManager: ${realCm.javaClass.name}")
                    
                    // Chain IDs: 9 = FILTER_CHAIN_NAME_STANDBY_ALLOWLIST or similar on some ROMs
                    // On some vendors, 2 or 1 might be used. 9 is most common for firewall.
                    val chain = 9
                    
                    logD("Step 3: Calling setFirewallChainEnabled($chain, true)...")
                    // Pass Boolean instead of Integer to match expected 'boolean' type
                    callMethodResilient(realCm, "setFirewallChainEnabled", chain, true)
                    logD("Step 4: setFirewallChainEnabled succeeded")
                    
                    val rule = if (enabled) 0 else 2 // 0 = ALLOW, 2 = DENY
                    logD("Step 5: Calling setUidFirewallRule($chain, $uid, $rule)...")
                    callMethodResilient(realCm, "setUidFirewallRule", chain, uid, rule)
                    
                    logD("✅ SUCCESS: Firewall rules updated for $uid")
                    true
                }

                resultRef.set(result)
                latch.countDown()
            }

            val completed = latch.await(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            if (!completed) {
                logE("❌ FAILURE in setPackageNetworkingEnabled: timeout after ${OP_TIMEOUT_MS}ms")
                return false
            }

            val result = resultRef.get() ?: return false
            
            if (result.isFailure) {
                val e = result.exceptionOrNull()
                logE("❌ FAILURE in setPackageNetworkingEnabled: ${e?.javaClass?.name}: ${e?.message}")
                e?.cause?.let { cause ->
                    logE("❌ Root cause: ${cause.javaClass.name}: ${cause.message}")
                }
                e?.printStackTrace()
                return false
            }
            
            return result.getOrDefault(false)
            
        } catch (e: Throwable) {
            // ABSOLUTE guard against any crash in the privileged process
            logE("🔥 CRITICAL ERROR in PrivilegedServiceImpl: ${e.message}")
            return false
        } finally {
            logD("🏁 EXIT: setPackageNetworkingEnabled")
        }
    }
    
    private fun getConnectivityManagerInstance(): Any {
        val smClass = Class.forName("android.os.ServiceManager")
        val getService = smClass.getMethod("getService", String::class.java)
        val binder = getService.invoke(null, "connectivity") as? IBinder
            ?: throw RuntimeException("connectivity service not found")
            
        val stubClass = Class.forName("android.net.IConnectivityManager\$Stub")
        val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
        return asInterface.invoke(null, binder) ?: throw RuntimeException("asInterface returned null")
    }
    
    /**
     * More resilient method call that searches for the best matching method
     */
    private fun callMethodResilient(obj: Any, methodName: String, vararg args: Any) {
        val clazz = obj.javaClass
        val methods = clazz.methods
        
        // Find a method that matches by name and parameter count
        val targetMethod = methods.find { it.name == methodName && it.parameterCount == args.size }
            ?: throw NoSuchMethodException("Could not find method $methodName with ${args.size} params on ${clazz.name}")
            
        targetMethod.isAccessible = true
        
        // Ensure arguments match primitive types if needed
        val finalArgs = Array(args.size) { i ->
            val paramType = targetMethod.parameterTypes[i]
            val arg = args[i]
            
            when {
                paramType == Int::class.javaPrimitiveType && arg is Int -> arg
                paramType == Boolean::class.javaPrimitiveType && arg is Boolean -> arg
                // Force conversion if there's a mismatch (common in reflection)
                paramType == Boolean::class.javaPrimitiveType && arg is Number -> arg.toInt() != 0
                paramType == Int::class.javaPrimitiveType && arg is Boolean -> if (arg) 1 else 0
                else -> arg
            }
        }
        
        try {
            targetMethod.invoke(obj, *finalArgs)
        } catch (e: InvocationTargetException) {
            val cause = e.targetException ?: e.cause
            if (cause != null) {
                logW("InvocationTargetException cause: ${cause.javaClass.name}: ${cause.message}")
            } else {
                logW("InvocationTargetException with null cause")
            }
            throw e
        }
    }

    private fun logD(message: String) {
        Log.d(TAG, message)
        logCallback?.let { runCatching { it.log(0, TAG, message) } }
    }

    private fun logW(message: String) {
        Log.w(TAG, message)
        logCallback?.let { runCatching { it.log(2, TAG, message) } }
    }

    private fun logE(message: String) {
        Log.e(TAG, message)
        logCallback?.let { runCatching { it.log(3, TAG, message) } }
    }
}


