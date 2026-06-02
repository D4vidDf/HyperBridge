package com.d4viddf.hyperbridge.integration.shizuku

import android.content.pm.PackageManager
import com.d4viddf.hyperbridge.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import rikka.shizuku.Shizuku


class ShizukuNotWorkException(cause: Throwable? = null) : Exception("Shizuku is not working or permission denied.", cause)

suspend fun <T> requireShizukuPermissionGranted(action: suspend () -> T): T {
    callbackFlow {
        try {
            rikka.sui.Sui.init(BuildConfig.APPLICATION_ID)
        } catch (e: Exception) {}
        
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            if (Shizuku.pingBinder()) {
                send(Unit)
            } else {
                close(ShizukuNotWorkException())
            }
            awaitClose()
        } else {
            val requestCode = (Int.MIN_VALUE..Int.MAX_VALUE).random()
            val listener =
                Shizuku.OnRequestPermissionResultListener { _requestCode, grantResult ->
                    if (_requestCode != requestCode) return@OnRequestPermissionResultListener
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED)
                        trySend(Unit)
                    else close(Exception("sui/shizuku permission denied"))
                }
            Shizuku.addRequestPermissionResultListener(listener)
            Shizuku.requestPermission(requestCode)
            awaitClose { Shizuku.removeRequestPermissionResultListener(listener) }
        }
    }.catch {
        throw ShizukuNotWorkException(it)
    }.first()

    return action()
}

