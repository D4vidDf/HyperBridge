package com.d4viddf.hyperbridge.service.widget

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomWidgetRendererTest {

    @Test
    fun baseCanvasWidthDpIs350() {
        assertEquals(350, CustomWidgetRenderer.BASE_CANVAS_WIDTH_DP)
    }

    @Test
    fun getIslandWidthDpCalculatesCorrectlyWithinBounds() {
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            density = 3f // screenWidthDp = 360f
        }

        val fakeContext = object : android.content.ContextWrapper(null) {
            private val res = object : Resources(null, displayMetrics, null) {
                override fun getDisplayMetrics(): DisplayMetrics = displayMetrics
            }
            override fun getResources(): Resources = res
        }

        // screenWidthDp = 360, (360 - 24) = 336f, coerced to [320, 600] -> 336f
        val width = CustomWidgetRenderer.getIslandWidthDp(fakeContext)
        assertEquals(336f, width, 0.01f)
    }

    @Test
    fun getIslandWidthDpClampsToBounds() {
        val smallMetrics = DisplayMetrics().apply {
            widthPixels = 300
            density = 1f // screenWidthDp = 300f -> 300 - 24 = 276f -> clamped to 320f
        }
        val smallContext = object : android.content.ContextWrapper(null) {
            private val res = object : Resources(null, smallMetrics, null) {
                override fun getDisplayMetrics(): DisplayMetrics = smallMetrics
            }
            override fun getResources(): Resources = res
        }
        assertEquals(320f, CustomWidgetRenderer.getIslandWidthDp(smallContext), 0.01f)

        val largeMetrics = DisplayMetrics().apply {
            widthPixels = 2000
            density = 2f // screenWidthDp = 1000f -> 1000 - 24 = 976f -> clamped to 600f
        }
        val largeContext = object : android.content.ContextWrapper(null) {
            private val res = object : Resources(null, largeMetrics, null) {
                override fun getDisplayMetrics(): DisplayMetrics = largeMetrics
            }
            override fun getResources(): Resources = res
        }
        assertEquals(600f, CustomWidgetRenderer.getIslandWidthDp(largeContext), 0.01f)
    }

    @Test
    fun computeImageBoundsFitCenterGlyph() {
        val bounds = CustomWidgetRenderer.computeImageBounds(
            srcW = 96f,
            srcH = 96f,
            widthPx = 100f,
            heightPx = 100f,
            scaleType = com.d4viddf.hyperbridge.models.widget.ImageScaleType.FIT_CENTER,
            isGlyph = true,
            minGlyphSize = 12f
        )
        assertEquals(17.5f, bounds.left, 0.01f)
        assertEquals(17.5f, bounds.top, 0.01f)
        assertEquals(65f, bounds.width, 0.01f)
        assertEquals(65f, bounds.height, 0.01f)
    }

    @Test
    fun computeImageBoundsFitCenterBitmapLandscape() {
        val bounds = CustomWidgetRenderer.computeImageBounds(
            srcW = 200f,
            srcH = 100f,
            widthPx = 100f,
            heightPx = 100f,
            scaleType = com.d4viddf.hyperbridge.models.widget.ImageScaleType.FIT_CENTER,
            isGlyph = false
        )
        assertEquals(0f, bounds.left, 0.01f)
        assertEquals(25f, bounds.top, 0.01f)
        assertEquals(100f, bounds.width, 0.01f)
        assertEquals(50f, bounds.height, 0.01f)
    }

    @Test
    fun computeImageBoundsFitCenterBitmapPortrait() {
        val bounds = CustomWidgetRenderer.computeImageBounds(
            srcW = 100f,
            srcH = 200f,
            widthPx = 100f,
            heightPx = 100f,
            scaleType = com.d4viddf.hyperbridge.models.widget.ImageScaleType.FIT_CENTER,
            isGlyph = false
        )
        assertEquals(25f, bounds.left, 0.01f)
        assertEquals(0f, bounds.top, 0.01f)
        assertEquals(50f, bounds.width, 0.01f)
        assertEquals(100f, bounds.height, 0.01f)
    }

    @Test
    fun computeImageBoundsCenterCropBitmap() {
        val bounds = CustomWidgetRenderer.computeImageBounds(
            srcW = 200f,
            srcH = 100f,
            widthPx = 100f,
            heightPx = 100f,
            scaleType = com.d4viddf.hyperbridge.models.widget.ImageScaleType.CENTER_CROP,
            isGlyph = false
        )
        assertEquals(-50f, bounds.left, 0.01f)
        assertEquals(0f, bounds.top, 0.01f)
        assertEquals(200f, bounds.width, 0.01f)
        assertEquals(100f, bounds.height, 0.01f)
    }

    @Test
    fun computeImageBoundsCenterCropGlyph() {
        val bounds = CustomWidgetRenderer.computeImageBounds(
            srcW = 96f,
            srcH = 96f,
            widthPx = 80f,
            heightPx = 100f,
            scaleType = com.d4viddf.hyperbridge.models.widget.ImageScaleType.CENTER_CROP,
            isGlyph = true
        )
        assertEquals(-10f, bounds.left, 0.01f)
        assertEquals(0f, bounds.top, 0.01f)
        assertEquals(100f, bounds.width, 0.01f)
        assertEquals(100f, bounds.height, 0.01f)
    }

    @Test
    fun computeImageBoundsFitWidth() {
        val bounds = CustomWidgetRenderer.computeImageBounds(
            srcW = 100f,
            srcH = 50f,
            widthPx = 200f,
            heightPx = 200f,
            scaleType = com.d4viddf.hyperbridge.models.widget.ImageScaleType.FIT_WIDTH,
            isGlyph = false
        )
        assertEquals(0f, bounds.left, 0.01f)
        assertEquals(50f, bounds.top, 0.01f)
        assertEquals(200f, bounds.width, 0.01f)
        assertEquals(100f, bounds.height, 0.01f)
    }

    @Test
    fun computeImageBoundsFitHeight() {
        val bounds = CustomWidgetRenderer.computeImageBounds(
            srcW = 50f,
            srcH = 100f,
            widthPx = 200f,
            heightPx = 200f,
            scaleType = com.d4viddf.hyperbridge.models.widget.ImageScaleType.FIT_HEIGHT,
            isGlyph = false
        )
        assertEquals(50f, bounds.left, 0.01f)
        assertEquals(0f, bounds.top, 0.01f)
        assertEquals(100f, bounds.width, 0.01f)
        assertEquals(200f, bounds.height, 0.01f)
    }
}
