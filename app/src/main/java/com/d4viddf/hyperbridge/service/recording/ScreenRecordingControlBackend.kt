package com.d4viddf.hyperbridge.service.recording

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.concurrent.atomic.AtomicBoolean

interface ScreenRecordingControlBackend {
    suspend fun probeCapabilities(): ScreenRecordingCapabilities
    suspend fun stop(): Result<Unit>
}

internal object ScreenRecordingToolProtocol {
    const val SERVICE_CLASS = "com.miui.screenrecorder.service.ScreenRecorderToolService"
    const val SERVICE_ACTION = "com.aios.osbot.action.APP_TOOL_PROVIDER"
    const val DESCRIPTOR = "com.aios.apptoolsdk.aidl.IAppToolProvider"
    const val TRANSACTION_CALL = 1
    const val TOOL_STATUS = "screenrecorder_get_status"
    const val TOOL_STOP = "screenrecorder_stop_recording"

    fun request(toolName: String, requestId: Int = 1): String = buildJsonObject {
        put("jsonrpc", "2.0")
        put("id", requestId)
        put("method", "tools/call")
        put("params", buildJsonObject {
            put("name", toolName)
            put("arguments", buildJsonObject {})
        })
    }.toString()

    fun isSuccessfulResponse(response: String?): Boolean {
        if (response.isNullOrBlank()) return false
        return try {
            val root = Json.parseToJsonElement(response).jsonObject
            val isError = root["result"]?.jsonObject?.get("isError")?.jsonPrimitive
            !root.containsKey("error") && isError?.isString == false && isError.booleanOrNull == false
        } catch (_: Exception) {
            false
        }
    }
}

class XiaomiScreenRecordingControlBackend(context: Context) : ScreenRecordingControlBackend {
    private val appContext = context.applicationContext

    @Volatile
    private var verified = false

    override suspend fun probeCapabilities(): ScreenRecordingCapabilities {
        if (verified) return ScreenRecordingCapabilities(canStop = true)
        if (!hasSafeExportedService()) return ScreenRecordingCapabilities(canStop = false)

        val success = callTool(ScreenRecordingToolProtocol.TOOL_STATUS).isSuccess
        verified = success
        return ScreenRecordingCapabilities(canStop = success)
    }

    override suspend fun stop(): Result<Unit> {
        if (!verified) {
            val capabilities = probeCapabilities()
            if (!capabilities.canStop) {
                return Result.failure(IllegalStateException("Xiaomi recorder Stop tool is unavailable"))
            }
        }
        return callTool(ScreenRecordingToolProtocol.TOOL_STOP).map { Unit }
    }

    private fun hasSafeExportedService(): Boolean = try {
        val component = recorderComponent()
        val info = appContext.packageManager.getServiceInfo(
            component,
            PackageManager.ComponentInfoFlags.of(0)
        )
        info.exported && info.permission == null
    } catch (_: Exception) {
        false
    }

    private suspend fun callTool(toolName: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            withTimeout(BIND_TIMEOUT_MS) {
                val binder = bindRecorderService()
                try {
                    check(binder.interfaceDescriptor == ScreenRecordingToolProtocol.DESCRIPTOR) {
                        "Unexpected Xiaomi recorder Binder descriptor"
                    }
                    val response = transact(
                        binder,
                        ScreenRecordingToolProtocol.request(toolName)
                    )
                    check(ScreenRecordingToolProtocol.isSuccessfulResponse(response)) {
                        "Xiaomi recorder tool returned an error"
                    }
                    response
                } finally {
                    binder.close()
                }
            }
        }.onFailure {
            Log.w(TAG, "Recorder tool call failed: $toolName", it)
        }
    }

    private suspend fun bindRecorderService(): BoundRecorderService {
        val deferred = CompletableDeferred<IBinder>()
        val closed = AtomicBoolean(false)
        lateinit var connection: ServiceConnection
        connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                deferred.complete(service)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                if (!deferred.isCompleted) {
                    deferred.completeExceptionally(IllegalStateException("Recorder service disconnected"))
                }
            }

            override fun onNullBinding(name: ComponentName) {
                deferred.completeExceptionally(IllegalStateException("Recorder service returned a null binding"))
            }

            override fun onBindingDied(name: ComponentName) {
                deferred.completeExceptionally(IllegalStateException("Recorder service binding died"))
            }
        }

        val intent = Intent(ScreenRecordingToolProtocol.SERVICE_ACTION).apply {
            component = recorderComponent()
        }
        val didBind = appContext.bindService(
            intent,
            Context.BIND_AUTO_CREATE,
            appContext.mainExecutor,
            connection
        )
        check(didBind) { "Unable to bind Xiaomi recorder tool service" }

        try {
            return BoundRecorderService(deferred.await()) {
                if (closed.compareAndSet(false, true)) {
                    runCatching { appContext.unbindService(connection) }
                }
            }
        } catch (error: Throwable) {
            if (closed.compareAndSet(false, true)) {
                runCatching { appContext.unbindService(connection) }
            }
            throw error
        }
    }

    private fun transact(binder: IBinder, request: String): String {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(ScreenRecordingToolProtocol.DESCRIPTOR)
            data.writeString(request)
            check(binder.transact(ScreenRecordingToolProtocol.TRANSACTION_CALL, data, reply, 0)) {
                "Xiaomi recorder Binder rejected the transaction"
            }
            reply.readException()
            reply.readString() ?: error("Xiaomi recorder returned an empty response")
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    private fun recorderComponent() = ComponentName(
        ScreenRecordingClassifier.PACKAGE_NAME,
        ScreenRecordingToolProtocol.SERVICE_CLASS
    )

    private class BoundRecorderService(
        val binder: IBinder,
        private val onClose: () -> Unit
    ) : IBinder by binder {
        fun close() = onClose()
    }

    private companion object {
        const val TAG = "ScreenRecordingControl"
        const val BIND_TIMEOUT_MS = 1_500L
    }
}
