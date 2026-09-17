package com.d4viddf.hyperbridge.service

import com.google.gson.JsonParser

/**
 * Decides for how long the permanent island must yield to one native island.
 *
 * HyperOS 3 draws two islands side by side: the newest big, the other collapsed into a round
 * mini island that shows nothing but the app icon. So while a native island is on screen the
 * permanent island cannot just sit "underneath" it — it shows up as a green HyperBridge bubble
 * next to the stopwatch, timer, navigation… (#335). The yield therefore has to last as long as
 * the native island is actually visible, and no longer (#255).
 *
 * For focus notifications that visibility is declared in the payload: Xiaomi's protocol puts
 * `param_v2.param_island.islandTimeout` (seconds) on every island and documents a default of
 * `60 * 60` when it is omitted. A focus notification without `param_island` has no island at
 * all. MediaStyle players carry no focus param — HyperOS builds their island itself and drops
 * it shortly after playback pauses while the notification lives on — so they keep the short
 * window that fixed #255.
 */
object NativeIslandYieldPolicy {
    /** Xiaomi's documented default when a focus island omits `islandTimeout`. */
    const val DEFAULT_ISLAND_TIMEOUT_SECONDS = 3_600L

    /** A MediaStyle player: no payload to read, HyperOS hides its island soon after a pause. */
    const val MEDIA_YIELD_MS = 30_000L

    /** A focus param this policy cannot read: keep the historical flat window. */
    const val FALLBACK_YIELD_MS = 30_000L

    private const val MAX_YIELD_MS = Long.MAX_VALUE / 2

    /**
     * @param hasFocusParam the notification carries `miui.focus.param` / `miui.system.focus.param`
     * @param focusParam that extra as a string, when it is one
     * @param isMediaStyle the notification uses MediaStyle
     * @return the yield window in ms, `0` when the notification has no island, or `null` when it is
     *   not a native island at all
     */
    fun yieldMsFor(hasFocusParam: Boolean, focusParam: String?, isMediaStyle: Boolean): Long? {
        if (hasFocusParam) return focusYieldMs(focusParam)
        if (isMediaStyle) return MEDIA_YIELD_MS
        return null
    }

    /** Yield window for a focus notification, from its declared island timeout. */
    fun focusYieldMs(focusParam: String?): Long {
        if (focusParam.isNullOrBlank()) return FALLBACK_YIELD_MS
        val root = try {
            JsonParser.parseString(focusParam).takeIf { it.isJsonObject }?.asJsonObject
        } catch (_: Exception) {
            null
        } ?: return FALLBACK_YIELD_MS
        val paramV2 = root.get("param_v2")?.takeIf { it.isJsonObject }?.asJsonObject
            ?: return FALLBACK_YIELD_MS
        val island = paramV2.get("param_island")?.takeIf { it.isJsonObject }?.asJsonObject
            ?: return 0L
        val seconds = try {
            island.get("islandTimeout")?.takeIf { it.isJsonPrimitive }?.asLong
        } catch (_: Exception) {
            null
        }
        val effective = if (seconds != null && seconds > 0) seconds else DEFAULT_ISLAND_TIMEOUT_SECONDS
        return effective.coerceAtMost(MAX_YIELD_MS / 1_000L) * 1_000L
    }
}
