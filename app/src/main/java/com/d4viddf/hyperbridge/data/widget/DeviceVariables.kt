package com.d4viddf.hyperbridge.data.widget

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Device-level values for the `{device.*}` / `{time.*}` micro-widget tokens, shared by every
 * place that builds a [VariableContext] (notification translator and permanent island) so a
 * widget resolves the same values wherever it is rendered.
 */
object DeviceVariables {

    fun batteryPercent(context: Context): Int? {
        return try {
            val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
            val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level < 0 || scale <= 0) null else level * 100 / scale
        } catch (_: Exception) {
            null
        }
    }

    fun timeNow(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}
