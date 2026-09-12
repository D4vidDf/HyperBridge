package com.d4viddf.hyperbridge.service.visual

import kotlin.math.min
import kotlin.math.roundToInt

data class PixelBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left + 1
    val height: Int get() = bottom - top + 1
}

data class FloatBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

object IconGeometry {
    const val DEFAULT_ALPHA_THRESHOLD = 8

    fun findVisibleBounds(
        pixels: IntArray,
        width: Int,
        height: Int,
        alphaThreshold: Int = DEFAULT_ALPHA_THRESHOLD
    ): PixelBounds? {
        if (width <= 0 || height <= 0 || pixels.size < width * height) return null

        var left = width
        var top = height
        var right = -1
        var bottom = -1

        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                val alpha = pixels[row + x] ushr 24
                if (alpha > alphaThreshold) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }

        return if (right >= left && bottom >= top) PixelBounds(left, top, right, bottom) else null
    }

    fun fitCenterInside(
        sourceWidth: Int,
        sourceHeight: Int,
        destLeft: Float,
        destTop: Float,
        destRight: Float,
        destBottom: Float
    ): FloatBounds {
        val destWidth = destRight - destLeft
        val destHeight = destBottom - destTop
        if (sourceWidth <= 0 || sourceHeight <= 0 || destWidth <= 0f || destHeight <= 0f) {
            return FloatBounds(destLeft, destTop, destLeft, destTop)
        }

        val scale = min(destWidth / sourceWidth.toFloat(), destHeight / sourceHeight.toFloat())
        val fittedWidth = sourceWidth * scale
        val fittedHeight = sourceHeight * scale
        val left = destLeft + (destWidth - fittedWidth) / 2f
        val top = destTop + (destHeight - fittedHeight) / 2f
        return FloatBounds(
            left = left,
            top = top,
            right = left + fittedWidth,
            bottom = top + fittedHeight
        )
    }
}

fun FloatBounds.roundToPixelRect(): android.graphics.Rect {
    return android.graphics.Rect(
        left.roundToInt(),
        top.roundToInt(),
        right.roundToInt(),
        bottom.roundToInt()
    )
}
