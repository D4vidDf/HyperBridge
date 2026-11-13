package com.d4viddf.hyperbridge.service.call

/** Keeps call-control glyphs legible inside HyperOS's already-compact action buttons. */
object CallActionIconSizingPolicy {
    const val MAX_CLASSIC_PADDING_PERCENT = 10
    const val NATIVE_PADDING_PERCENT = 7

    fun classicPaddingPercent(configuredPaddingPercent: Int): Int =
        configuredPaddingPercent.coerceIn(0, MAX_CLASSIC_PADDING_PERCENT)
}
