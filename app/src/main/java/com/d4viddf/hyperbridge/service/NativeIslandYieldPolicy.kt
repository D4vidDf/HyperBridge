package com.d4viddf.hyperbridge.service

import com.google.gson.JsonParser

/**
 * Decides for how long the permanent island must yield to one native island.
 *
 * HyperOS 3 draws two islands side by side: the newest big, the other collapsed into a round
 * mini island that shows nothing but the app icon. So while a native island is on screen the
 * permanent island cannot just sit "underneath" it — it shows up as a green HyperBridge bubble
 * next to the stopwatch, timer, navigation… (#335). The permanent island therefore hides for as
 * long as the native island is there.
 *
 * The one exception is media players. HyperOS turns every notification that carries a
 * MediaSession into an island player itself, and drops that island shortly after playback
 * pauses while the notification lives on for hours (#255). Only they get a time limit: the short
 * window that fixed #255. Any other native island keeps the permanent island hidden until its
 * notification is removed, bounded only by the island lifetime HyperOS itself applies —
 * `param_v2.param_island.islandTimeout` (seconds, documented default `60 * 60`) — since the
 * island is gone from the screen after that no matter how long the notification stays.
 */
object NativeIslandYieldPolicy {
    /** Xiaomi's documented default when a focus island omits `islandTimeout`. */
    const val DEFAULT_ISLAND_TIMEOUT_SECONDS = 3_600L

    /** A media player: HyperOS hides its island soon after a pause, the notification stays. */
    const val MEDIA_YIELD_MS = 30_000L

    /** No time limit: the permanent island stays hidden until the notification is removed. */
    const val UNBOUNDED_YIELD_MS = Long.MAX_VALUE

    private const val MAX_YIELD_MS = Long.MAX_VALUE / 2

    /**
     * @param hasFocusParam the notification carries `miui.focus.param` / `miui.system.focus.param`
     * @param focusParam that extra as a string, when it is one
     * @param isMediaPlayer the notification is a media player (MediaStyle, or it carries a
     *   MediaSession, which HyperOS always renders as an island player)
     * @return the yield window in ms, `0` when the notification has no island, or `null` when it is
     *   not a native island at all
     */
    fun yieldMsFor(hasFocusParam: Boolean, focusParam: String?, isMediaPlayer: Boolean): Long? {
        if (isMediaPlayer) return MEDIA_YIELD_MS
        if (hasFocusParam) return focusYieldMs(focusParam)
        return null
    }

    /**
     * Yield window for a non-media focus notification: no time limit of our own, only the
     * island lifetime it declares to HyperOS. A payload this policy cannot read is treated as an
     * island that stays as long as its notification.
     */
    fun focusYieldMs(focusParam: String?): Long {
        if (focusParam.isNullOrBlank()) return UNBOUNDED_YIELD_MS
        val root = try {
            JsonParser.parseString(focusParam).takeIf { it.isJsonObject }?.asJsonObject
        } catch (_: Exception) {
            null
        } ?: return UNBOUNDED_YIELD_MS
        val paramV2 = root.get("param_v2")?.takeIf { it.isJsonObject }?.asJsonObject
            ?: return UNBOUNDED_YIELD_MS
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
