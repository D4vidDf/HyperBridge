package com.d4viddf.hyperbridge.service

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

enum class SourceCandidateQuality(val rank: Int) {
    SPARSE(0),
    USABLE(1)
}

/**
 * Latest-best generation gate.
 *
 * Sparse callbacks are allowed to refresh, but they cannot invalidate queued usable work. A sparse
 * candidate that becomes usable after refresh can promote itself; ties are resolved latest-wins.
 */
class SourceProcessingGeneration {
    private data class Owner(val generation: Long, val quality: SourceCandidateQuality)

    private val sequence = AtomicLong()
    private val latestBySource = ConcurrentHashMap<String, Owner>()

    fun next(sourceKey: String, quality: SourceCandidateQuality): Long =
        sequence.incrementAndGet().also { generation ->
            consider(sourceKey, generation, quality)
        }

    fun consider(sourceKey: String, generation: Long, quality: SourceCandidateQuality) {
        latestBySource.compute(sourceKey) { _, current ->
            when {
                current == null -> Owner(generation, quality)
                quality.rank > current.quality.rank -> Owner(generation, quality)
                quality == current.quality && generation > current.generation -> Owner(generation, quality)
                else -> current
            }
        }
    }

    fun isCurrent(sourceKey: String, generation: Long): Boolean =
        latestBySource[sourceKey]?.generation == generation

    fun current(sourceKey: String): Long? = latestBySource[sourceKey]?.generation

    fun finish(sourceKey: String, generation: Long) {
        latestBySource.computeIfPresent(sourceKey) { _, current ->
            if (current.generation == generation) null else current
        }
    }

    fun remove(sourceKey: String) {
        latestBySource.remove(sourceKey)
    }

    fun clear() = latestBySource.clear()
}
