package com.d4viddf.hyperbridge.service.translators

import com.d4viddf.hyperbridge.models.IslandConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandFloatingPresentationPolicyTest {
    @Test
    fun newConfiguredEventMayFloatAndExpand() {
        val presentation = IslandFloatingPresentationPolicy.resolve(
            configuredToFloat = true,
            isUpdate = false
        )

        assertTrue(presentation.enableFloat)
        assertTrue(presentation.islandFirstFloat)
    }

    @Test
    fun updateCannotFloatOrReExpand() {
        val presentation = IslandFloatingPresentationPolicy.resolve(
            configuredToFloat = true,
            isUpdate = true
        )

        assertFalse(presentation.enableFloat)
        assertFalse(presentation.islandFirstFloat)
    }

    @Test
    fun disabledFloatingRemainsDisabledForNewEvents() {
        val presentation = IslandFloatingPresentationPolicy.resolve(
            configuredToFloat = false,
            isUpdate = false
        )

        assertFalse(presentation.enableFloat)
        assertFalse(presentation.islandFirstFloat)
    }

    @Test
    fun updateAllowsFloatingWhenFloatOnlyOnFirstIsFalse() {
        val presentation = IslandFloatingPresentationPolicy.resolve(
            configuredToFloat = true,
            isUpdate = true,
            floatOnlyOnFirst = false
        )

        assertTrue(presentation.enableFloat)
        assertTrue(presentation.islandFirstFloat)
    }

    @Test
    fun updateDisallowsFloatingWhenFloatOnlyOnFirstIsTrue() {
        val presentation = IslandFloatingPresentationPolicy.resolve(
            configuredToFloat = true,
            isUpdate = true,
            floatOnlyOnFirst = true
        )

        assertFalse(presentation.enableFloat)
        assertFalse(presentation.islandFirstFloat)
    }

    @Test
    fun landscapeOptionSuppressesFloatOnlyWhileRotated() {
        assertEquals(false, IslandFloatingPresentationPolicy.configuredToFloat(
            isFloat = true, noFloatInLandscape = true, isLandscape = true, isCall = false))
        assertEquals(true, IslandFloatingPresentationPolicy.configuredToFloat(
            isFloat = true, noFloatInLandscape = true, isLandscape = false, isCall = false))
    }

    @Test
    fun landscapeOptionOffOrUnsetLeavesFloatUntouched() {
        assertEquals(true, IslandFloatingPresentationPolicy.configuredToFloat(
            isFloat = true, noFloatInLandscape = false, isLandscape = true, isCall = false))
        assertEquals(true, IslandFloatingPresentationPolicy.configuredToFloat(
            isFloat = true, noFloatInLandscape = null, isLandscape = true, isCall = false))
        assertEquals(false, IslandFloatingPresentationPolicy.configuredToFloat(
            isFloat = false, noFloatInLandscape = false, isLandscape = false, isCall = false))
    }

    @Test
    fun callsStillFloatInLandscape() {
        assertEquals(true, IslandFloatingPresentationPolicy.configuredToFloat(
            isFloat = true, noFloatInLandscape = true, isLandscape = true, isCall = true))
    }

    @Test
    fun landscapeOptionInheritsGlobalUnlessAppOverrides() {
        val global = IslandConfig(isFloat = true, noFloatInLandscape = true)

        assertEquals(true, IslandConfig().mergeWith(global).noFloatInLandscape)
        assertEquals(false, IslandConfig(noFloatInLandscape = false).mergeWith(global).noFloatInLandscape)
        assertEquals(false, IslandConfig().mergeWith(IslandConfig()).noFloatInLandscape)
    }
}
