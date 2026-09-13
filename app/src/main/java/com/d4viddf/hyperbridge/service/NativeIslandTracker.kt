package com.d4viddf.hyperbridge.service

import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks other apps' native islands (focus notifications, MediaStyle players) and decides for how
 * long the permanent island should yield to them.
 *
 * The permanent island must step aside when a native island appears, but only briefly: HyperOS
 * always shows the newest focus island on top, so a posted pill never blocks a fresh island. Gating
 * on the mere existence of a native notification instead suppressed the pill for as long as that
 * notification lived — a paused media player or a system roaming banner can sit for hours (#255).
 *
 * The first-seen timestamp is kept across updates on purpose: a progress-updating media
 * notification would otherwise renew its own yield window forever.
 */
class NativeIslandTracker(
    private val yieldMs: Long = DEFAULT_YIELD_MS,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val firstSeen = ConcurrentHashMap<String, Long>()

    /** @return true when [key] was not tracked yet (first sighting). */
    fun note(key: String): Boolean = firstSeen.putIfAbsent(key, clock()) == null

    /** @return true when [key] was tracked. */
    fun remove(key: String): Boolean = firstSeen.remove(key) != null

    fun keys(): List<String> = firstSeen.keys.toList()

    fun isEmpty(): Boolean = firstSeen.isEmpty()

    /** True while at least one native island is inside its yield window. */
    fun hasFresh(): Boolean {
        val now = clock()
        return firstSeen.values.any { now - it < yieldMs }
    }

    /** Milliseconds until the newest yield window closes, or 0 when none is open. */
    fun remainingYieldMs(): Long {
        val now = clock()
        val newest = firstSeen.values.maxOrNull() ?: return 0L
        return (newest + yieldMs - now).coerceAtLeast(0L)
    }

    companion object {
        const val DEFAULT_YIELD_MS = 30_000L
    }
}
