package com.d4viddf.hyperbridge.data.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetVariableEngineTest {

    private val engine = WidgetVariableEngine()

    @Test
    fun resolvesNotificationTokens() {
        val ctx = VariableContext(notifTitle = "Alice", notifText = "Hi there", notifProgress = 42, notifPackage = "com.example")
        assertEquals("Alice: Hi there (42%)", engine.resolve("{notif.title}: {notif.text} ({notif.progress}%)", ctx))
        assertEquals("com.example", engine.resolve("{notif.package}", ctx))
    }

    @Test
    fun resolvesDeviceAndTimeTokens() {
        val ctx = VariableContext(deviceBatteryPercent = 77, timeNowFormatted = "10:30")
        assertEquals("77% at 10:30", engine.resolve("{device.battery}% at {time.now}", ctx))
    }

    @Test
    fun resolvesSourceTokensViaInjectedLookup() {
        val ctx = VariableContext(sourceLookup = { id, field ->
            if (id == "weather" && field == "text") "Sunny, 21C" else null
        })
        assertEquals("Sunny, 21C", engine.resolve("{source.weather.text}", ctx))
    }

    @Test
    fun missingTokenResolvesToEmptyStringNotACrash() {
        val ctx = VariableContext()
        assertEquals("", engine.resolve("{notif.title}", ctx))
        assertEquals("", engine.resolve("{source.unknown.text}", ctx))
        assertEquals("", engine.resolve("{completely.unknown.token.extra}", ctx))
    }

    @Test
    fun mixedLiteralAndTokenTemplateIsPreserved() {
        val ctx = VariableContext(notifTitle = "Bob")
        assertEquals("From: Bob!", engine.resolve("From: {notif.title}!", ctx))
    }

    @Test
    fun unknownPackageTokenResolvesToEmptyString() {
        val ctx = VariableContext(notifPackage = null)
        assertEquals("pkg=", engine.resolve("pkg={notif.package}", ctx))
    }
}
