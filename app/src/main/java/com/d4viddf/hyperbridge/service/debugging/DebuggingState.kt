package com.d4viddf.hyperbridge.service.debugging

enum class DebuggingTransport { USB, WIRELESS, BOTH }

data class DebuggingState(
    val transport: DebuggingTransport,
    /** False while wireless debugging is on but no computer is attached yet. */
    val connected: Boolean,
    /** Wi-Fi address of the phone, shown while wireless debugging is on. */
    val wifiAddress: String? = null
)

/**
 * Pure mapping from what the system exposes to what the island shows, so it can be unit tested.
 *
 * The toggles live in Settings.Global (adb_enabled, adb_wifi_enabled) and are readable by any app.
 * Whether a computer is actually attached is only visible through the ongoing notifications
 * system_server posts for it ("USB debugging connected" / "Wireless debugging connected").
 */
object DebuggingStateResolver {
    /** SystemMessage.NOTE_ADB_ACTIVE, posted by UsbDeviceManager. */
    const val USB_NOTIFICATION_ID = 26
    /** SystemMessage.NOTE_ADB_WIFI_ACTIVE, posted by AdbDebuggingManager. */
    const val WIRELESS_NOTIFICATION_ID = 62
    const val SYSTEM_PACKAGE = "android"

    fun isUsbNotification(packageName: String, id: Int, tag: String?): Boolean =
        packageName == SYSTEM_PACKAGE && id == USB_NOTIFICATION_ID && tag == null

    fun isWirelessNotification(packageName: String, id: Int, tag: String?): Boolean =
        packageName == SYSTEM_PACKAGE && id == WIRELESS_NOTIFICATION_ID && tag == null

    fun resolve(
        usbEnabled: Boolean,
        wirelessEnabled: Boolean,
        usbConnected: Boolean,
        wirelessConnected: Boolean,
        wifiAddress: String? = null
    ): DebuggingState? {
        // The notifications can outlive the toggle by a moment; the setting is the source of truth.
        val usb = usbEnabled && usbConnected
        val wireless = wirelessEnabled && wirelessConnected
        val address = wifiAddress?.takeIf { wirelessEnabled && it.isNotBlank() }
        return when {
            usb && wireless -> DebuggingState(DebuggingTransport.BOTH, connected = true, wifiAddress = address)
            usb -> DebuggingState(DebuggingTransport.USB, connected = true)
            wireless -> DebuggingState(DebuggingTransport.WIRELESS, connected = true, wifiAddress = address)
            // A phone listening for adb on the network is worth showing even with nobody attached.
            // USB debugging left on without a cable is the normal state for developers: not shown.
            wirelessEnabled -> DebuggingState(DebuggingTransport.WIRELESS, connected = false, wifiAddress = address)
            else -> null
        }
    }
}
