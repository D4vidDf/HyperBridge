package com.d4viddf.hyperbridge.service.vpn

/** Keeps a count-up origin stable when Android reports an already-running VPN without a start time. */
class VpnTimerOriginTracker {
    private var generation: Long? = null
    private var startedAtMillis: Long? = null

    @Synchronized
    fun resolve(
        sessionGeneration: Long,
        authoritativeStartMillis: Long?,
        observedAtMillis: Long
    ): Long {
        if (generation != sessionGeneration) {
            generation = sessionGeneration
            startedAtMillis = authoritativeStartMillis ?: observedAtMillis
        } else if (authoritativeStartMillis != null) {
            startedAtMillis = authoritativeStartMillis
        }
        return checkNotNull(startedAtMillis)
    }

    @Synchronized
    fun clear() {
        generation = null
        startedAtMillis = null
    }
}

