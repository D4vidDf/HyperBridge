package com.d4viddf.hyperbridge.service.debugging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebuggingStateResolverTest {

    private fun resolve(
        usbEnabled: Boolean = false,
        wirelessEnabled: Boolean = false,
        usbConnected: Boolean = false,
        wirelessConnected: Boolean = false,
        wifiAddress: String? = null
    ) = DebuggingStateResolver.resolve(usbEnabled, wirelessEnabled, usbConnected, wirelessConnected, wifiAddress)

    @Test
    fun `nothing enabled shows nothing`() {
        assertNull(resolve())
    }

    @Test
    fun `usb debugging left on without a computer is not shown`() {
        assertNull(resolve(usbEnabled = true))
    }

    @Test
    fun `usb computer attached`() {
        val state = resolve(usbEnabled = true, usbConnected = true)
        assertEquals(DebuggingState(DebuggingTransport.USB, connected = true), state)
    }

    @Test
    fun `wireless on with nobody attached is a heads up with the address`() {
        val state = resolve(wirelessEnabled = true, wifiAddress = "192.168.1.50")
        assertEquals(DebuggingTransport.WIRELESS, state?.transport)
        assertFalse(state!!.connected)
        assertEquals("192.168.1.50", state.wifiAddress)
    }

    @Test
    fun `wireless computer attached`() {
        val state = resolve(wirelessEnabled = true, wirelessConnected = true, wifiAddress = "10.0.0.7")
        assertEquals(DebuggingState(DebuggingTransport.WIRELESS, connected = true, wifiAddress = "10.0.0.7"), state)
    }

    @Test
    fun `both attached`() {
        val state = resolve(
            usbEnabled = true, wirelessEnabled = true,
            usbConnected = true, wirelessConnected = true,
            wifiAddress = "10.0.0.7"
        )
        assertEquals(DebuggingTransport.BOTH, state?.transport)
        assertTrue(state!!.connected)
        assertEquals("10.0.0.7", state.wifiAddress)
    }

    @Test
    fun `usb attached while wireless only listens counts as usb and keeps the address`() {
        val state = resolve(usbEnabled = true, wirelessEnabled = true, usbConnected = true, wifiAddress = "10.0.0.7")
        assertEquals(DebuggingTransport.USB, state?.transport)
        assertTrue(state!!.connected)
        assertEquals("10.0.0.7", state.wifiAddress)
    }

    @Test
    fun `usb alone never carries an address`() {
        assertNull(resolve(usbEnabled = true, usbConnected = true, wifiAddress = "10.0.0.7")!!.wifiAddress)
    }

    @Test
    fun `a stale connected notification after the toggle went off is ignored`() {
        assertNull(resolve(usbEnabled = false, usbConnected = true))
        assertNull(resolve(wirelessEnabled = false, wirelessConnected = true))
    }

    @Test
    fun `blank address is dropped`() {
        assertNull(resolve(wirelessEnabled = true, wifiAddress = " ")!!.wifiAddress)
    }

    @Test
    fun `only the untagged system notifications count`() {
        assertTrue(DebuggingStateResolver.isUsbNotification("android", 26, null))
        assertTrue(DebuggingStateResolver.isWirelessNotification("android", 62, null))
        assertFalse(DebuggingStateResolver.isUsbNotification("com.example", 26, null))
        assertFalse(DebuggingStateResolver.isUsbNotification("android", 26, "tag"))
        assertFalse(DebuggingStateResolver.isWirelessNotification("android", 26, null))
    }
}
