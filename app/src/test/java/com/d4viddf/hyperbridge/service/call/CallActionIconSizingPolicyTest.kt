package com.d4viddf.hyperbridge.service.call

import org.junit.Assert.assertEquals
import org.junit.Test

class CallActionIconSizingPolicyTest {
    @Test
    fun defaultThemePaddingIsReducedForCallControls() {
        assertEquals(10, CallActionIconSizingPolicy.classicPaddingPercent(15))
    }

    @Test
    fun smallerCustomPaddingIsPreserved() {
        assertEquals(4, CallActionIconSizingPolicy.classicPaddingPercent(4))
    }

    @Test
    fun invalidNegativePaddingIsClamped() {
        assertEquals(0, CallActionIconSizingPolicy.classicPaddingPercent(-5))
    }
}
