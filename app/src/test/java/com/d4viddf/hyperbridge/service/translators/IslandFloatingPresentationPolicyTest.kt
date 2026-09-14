package com.d4viddf.hyperbridge.service.translators

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
}
