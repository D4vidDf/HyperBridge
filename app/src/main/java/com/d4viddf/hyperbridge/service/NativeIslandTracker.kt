package com.d4viddf.hyperbridge.service

import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks other apps' native islands (focus notifications, MediaStyle players) and decides for how
 * long the permanent island should yield to them.
 *
 * Each sighting carries its own yield window (see [NativeIslandYieldPolicy]): a focus island yields
 * for the lifetime it declares, a media player for a short fixed window. The window is bounded by
 * the notification's life — callers [remove] a key as soon as the notification is gone — but never
 * by it alone: a paused media player or a system roaming banner can sit for hours after its island
 * is long gone (#255), and HyperOS 3 draws a permanent island posted next to a live native island
 * as a bare app-icon bubble (#335), so both edges matter.
 *
 * The first-seen timestamp is kept across updates on purpose: a progress-updating media
 * notification would otherwise renew its own yield window forever.
 */
class NativeIslandTracker(
    private val defaultYieldMs: Long = DEFAULT_YIELD_MS,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private class Sighting(val firstSeen: Long, yieldMs: Long) {
        val until: Long = if (yieldMs >= Long.MAX_VALUE - firstSeen) Long.MAX_VALUE else firstSeen + yieldMs
    }

    private val sightings = ConcurrentHashMap<String, Sighting>()

    /** @return true when [key] was not tracked yet (first sighting). */
    fun note(key: String, yieldMs: Long = defaultYieldMs): Boolean =
        sightings.putIfAbsent(key, Sighting(clock(), yieldMs.coerceAtLeast(0L))) == null

    /** @return true when [key] was tracked. */
    fun remove(key: String): Boolean = sightings.remove(key) != null

    fun keys(): List<String> = sightings.keys.toList()

    fun isEmpty(): Boolean = sightings.isEmpty()

    /** True while at least one native island is inside its yield window. */
    fun hasFresh(): Boolean {
        val now = clock()
        return sightings.values.any { now < it.until }
    }

    /** Milliseconds until the last open yield window closes, or 0 when none is open. */
    fun remainingYieldMs(): Long {
        val now = clock()
        val latest = sightings.values.maxOfOrNull { it.until } ?: return 0L
        return if (latest == Long.MAX_VALUE) Long.MAX_VALUE else (latest - now).coerceAtLeast(0L)
    }

    companion object {
        const val DEFAULT_YIELD_MS = 30_000L
    }
}
