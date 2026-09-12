package com.d4viddf.hyperbridge.service.vpn

import android.app.Notification

object VpnNativeCapabilityGuard {
    private const val XIAOMI_FOCUS_PARAM = "miui.focus.param"
    private const val XIAOMI_SYSTEM_FOCUS_PARAM = "miui.system.focus.param"

    fun hasNativeIsland(notification: Notification): Boolean =
        notification.extras.containsKey(XIAOMI_FOCUS_PARAM) ||
            notification.extras.containsKey(XIAOMI_SYSTEM_FOCUS_PARAM)
}

