package com.d4viddf.hyperbridge.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartActionsConfigTest {

    // ------------------------------------------------------- AppSmartActionsOverride

    @Test
    fun emptyOverrideHasNoFieldsSet() {
        val override = AppSmartActionsOverride()
        assertTrue(override.isEmpty)
        assertNull(override.get(SmartActionType.OTP))
        assertNull(override.get(SmartActionType.URL))
        assertNull(override.get(SmartActionType.PHONE))
        assertNull(override.get(SmartActionType.TRACKING))
    }

    @Test
    fun withSetsOnlyTheTargetedType() {
        val override = AppSmartActionsOverride().with(SmartActionType.OTP, false)
        assertFalse(override.isEmpty)
        assertEquals(false, override.get(SmartActionType.OTP))
        assertNull(override.get(SmartActionType.URL))
        assertNull(override.get(SmartActionType.PHONE))
        assertNull(override.get(SmartActionType.TRACKING))
    }

    @Test
    fun withNullClearsBackToInherit() {
        val set = AppSmartActionsOverride().with(SmartActionType.URL, true)
        val cleared = set.with(SmartActionType.URL, null)
        assertTrue(cleared.isEmpty)
        assertNull(cleared.get(SmartActionType.URL))
    }

    @Test
    fun getRoutesToTheCorrectField() {
        val override = AppSmartActionsOverride(otp = true, url = false, phone = true, tracking = false)
        assertEquals(true, override.get(SmartActionType.OTP))
        assertEquals(false, override.get(SmartActionType.URL))
        assertEquals(true, override.get(SmartActionType.PHONE))
        assertEquals(false, override.get(SmartActionType.TRACKING))
    }

    // ------------------------------------------------------------- applyOverride

    @Test
    fun applyNullOverrideReturnsSameConfig() {
        val config = SmartActionsConfig(enabled = true, otp = true, url = false)
        assertSame(config, config.applyOverride(null))
    }

    @Test
    fun applyEmptyOverrideChangesNothing() {
        val config = SmartActionsConfig(enabled = true, otp = true, url = false, phone = true, tracking = false)
        val result = config.applyOverride(AppSmartActionsOverride())
        assertEquals(config, result)
    }

    @Test
    fun overrideCanTurnAGloballyOnTypeOff() {
        val config = SmartActionsConfig(enabled = true, otp = true)
        val result = config.applyOverride(AppSmartActionsOverride(otp = false))
        assertFalse(result.otp)
    }

    @Test
    fun overrideCanTurnAGloballyOffTypeOn() {
        val config = SmartActionsConfig(enabled = true, url = false)
        val result = config.applyOverride(AppSmartActionsOverride(url = true))
        assertTrue(result.url)
    }

    @Test
    fun nullFieldsInOverrideInheritTheGlobalValue() {
        val config = SmartActionsConfig(enabled = true, otp = true, url = false, phone = true, tracking = false)
        // Only phone is overridden; every other field must fall back to the global config.
        val result = config.applyOverride(AppSmartActionsOverride(phone = false))
        assertTrue(result.otp)
        assertFalse(result.url)
        assertFalse(result.phone)
        assertFalse(result.tracking)
    }

    @Test
    fun applyOverrideLeavesGlobalOnlyFieldsUnchanged() {
        val config = SmartActionsConfig(
            enabled = true,
            excludedPackages = setOf("com.bank"),
            hideOtpCode = true
        )
        val result = config.applyOverride(AppSmartActionsOverride(otp = false))
        assertTrue(result.enabled)
        assertTrue(result.hideOtpCode)
        assertEquals(setOf("com.bank"), result.excludedPackages)
    }

    // ------------------------------------------------------------- IslandConfig.mergeWith

    @Test
    fun mergeWithNoAppSmartActionsFallsBackToGlobal() {
        val globalSmart = SmartActionsConfig(enabled = true, otp = true, url = false)
        val global = IslandConfig(smartActions = globalSmart)
        val app = IslandConfig()

        val merged = app.mergeWith(global)

        assertEquals(globalSmart, merged.smartActions)
        assertNull(merged.smartActionsOverride)
    }

    @Test
    fun mergeWithGlobalSmartActionsNullFallsBackToDisabled() {
        val global = IslandConfig()
        val app = IslandConfig()

        val merged = app.mergeWith(global)

        assertEquals(SmartActionsConfig.DISABLED, merged.smartActions)
    }

    @Test
    fun mergeWithAppOverrideAppliesOnTopOfGlobal() {
        val global = IslandConfig(smartActions = SmartActionsConfig(enabled = true, otp = true, tracking = true))
        val override = AppSmartActionsOverride(otp = false)
        val app = IslandConfig(smartActionsOverride = override)

        val merged = app.mergeWith(global)

        assertNotNull(merged.smartActions)
        assertFalse(merged.smartActions!!.otp)
        assertTrue(merged.smartActions!!.tracking)
        assertEquals(override, merged.smartActionsOverride)
    }

    @Test
    fun mergeWithAppOverrideOnDisabledGlobalStillAppliesToDefaults() {
        // Global has no smartActions configured at all (defaults to DISABLED before the override).
        val global = IslandConfig()
        val override = AppSmartActionsOverride(url = false)
        val app = IslandConfig(smartActionsOverride = override)

        val merged = app.mergeWith(global)

        assertNotNull(merged.smartActions)
        assertFalse(merged.smartActions!!.url)
        // Untouched fields keep SmartActionsConfig.DISABLED's own defaults.
        assertEquals(SmartActionsConfig.DISABLED.otp, merged.smartActions!!.otp)
    }
}
