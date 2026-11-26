package com.d4viddf.hyperbridge.service.visual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IconGeometryTest {
    @Test
    fun findsVisibleBoundsIgnoringTransparentPadding() {
        val pixels = IntArray(25)
        pixels[1 * 5 + 2] = 0xFF000000.toInt()
        pixels[2 * 5 + 1] = 0xFF000000.toInt()
        pixels[3 * 5 + 3] = 0xFF000000.toInt()

        val bounds = IconGeometry.findVisibleBounds(pixels, 5, 5)

        assertEquals(PixelBounds(left = 1, top = 1, right = 3, bottom = 3), bounds)
    }

    @Test
    fun transparentIconReturnsNullBounds() {
        val pixels = IntArray(16)

        assertNull(IconGeometry.findVisibleBounds(pixels, 4, 4))
    }

    @Test
    fun preservesAspectRatioWhenFittingWideIcon() {
        val fitted = IconGeometry.fitCenterInside(
            sourceWidth = 20,
            sourceHeight = 10,
            destLeft = 0f,
            destTop = 0f,
            destRight = 100f,
            destBottom = 100f
        )

        assertEquals(100f, fitted.width, 0.001f)
        assertEquals(50f, fitted.height, 0.001f)
        assertEquals(25f, fitted.top, 0.001f)
        assertEquals(75f, fitted.bottom, 0.001f)
    }
}
