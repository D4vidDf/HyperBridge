package com.d4viddf.hyperbridge.util

import android.os.Build
import java.io.BufferedReader
import java.io.InputStreamReader

object DeviceUtils {

    val isXiaomi: Boolean
        get() = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
                Build.MANUFACTURER.equals("Poco", ignoreCase = true) ||
                Build.MANUFACTURER.equals("Redmi", ignoreCase = true)

    fun getHyperOSVersion(): String {
        val version = getSystemProperty("ro.mi.os.version.name")
        if (version.isNotEmpty()) return "HyperOS $version"

        val miuiVer = getSystemProperty("ro.miui.ui.version.name")
        if (miuiVer.isNotEmpty()) return "MIUI $miuiVer"

        return "Android ${Build.VERSION.RELEASE}"
    }

    // NEW: Get Marketing Name (e.g. "Redmi Note 13 Pro+")
    fun getDeviceMarketName(): String {
        // 1. Try Xiaomi specific property
        val marketName = getSystemProperty("ro.product.marketname")
        if (marketName.isNotEmpty()) return marketName

        // 3. Fallback to Model (e.g. "23127PN0CC")
        return Build.MODEL
    }

    fun isCompatibleOS(): Boolean {
        // 1. Future Proof: Android 16 (API 36) is definitely HyperOS 3+ base
        if (Build.VERSION.SDK_INT >= 36) return true

        // 2. HyperOS 3 on Android 15 (API 35)
        val version = getSystemProperty("ro.mi.os.version.name")
        return version.startsWith("OS3", ignoreCase = true)
    }

    val isCNRom: Boolean
        get() {
            val region = getSystemProperty("ro.miui.region")
            if (region.equals("CN", ignoreCase = true)) return true
            return Build.DISPLAY.contains("CNXM", ignoreCase = true)
        }

    private fun getSystemProperty(key: String): String {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine()?.trim() ?: ""
        } catch (_: Exception) {
            ""
        }
    }
}