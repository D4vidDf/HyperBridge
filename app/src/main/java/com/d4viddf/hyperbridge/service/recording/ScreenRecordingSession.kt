package com.d4viddf.hyperbridge.service.recording

import com.d4viddf.hyperbridge.models.ScreenRecordingDesignConfig

data class ScreenRecordingCapabilities(
    val canStop: Boolean,
    val canPause: Boolean = false,
    val canResume: Boolean = false
)

data class ScreenRecordingSessionInput(
    val sourceKey: String,
    val packageName: String,
    val sourcePostTime: Long,
    val capabilities: ScreenRecordingCapabilities
)

data class ScreenRecordingSession(
    val logicalId: String,
    val sourceKey: String,
    val packageName: String,
    val startedAt: Long,
    val capabilities: ScreenRecordingCapabilities
)

object ScreenRecordingSavedIdentity {
    fun logicalId(sourceKey: String, sourcePostTime: Long): String =
        "screen-recording-saved:$sourceKey:$sourcePostTime"
}

class ScreenRecordingSessionTracker {
    private val sessionsBySource = mutableMapOf<String, ScreenRecordingSession>()

    @Synchronized
    fun resolve(input: ScreenRecordingSessionInput): ScreenRecordingSession {
        val current = sessionsBySource[input.sourceKey]
        if (current != null && current.startedAt == input.sourcePostTime) {
            val updated = current.copy(capabilities = input.capabilities)
            sessionsBySource[input.sourceKey] = updated
            return updated
        }

        val session = ScreenRecordingSession(
            logicalId = "screen-recording:${input.sourceKey}:${input.sourcePostTime}",
            sourceKey = input.sourceKey,
            packageName = input.packageName,
            startedAt = input.sourcePostTime,
            capabilities = input.capabilities
        )
        sessionsBySource[input.sourceKey] = session
        return session
    }

    @Synchronized
    fun logicalIdForSource(sourceKey: String): String? = sessionsBySource[sourceKey]?.logicalId

    @Synchronized
    fun endSource(sourceKey: String, sourcePostTime: Long): String? {
        val current = sessionsBySource[sourceKey] ?: return null
        if (current.startedAt != sourcePostTime) return null
        sessionsBySource.remove(sourceKey)
        return current.logicalId
    }

    @Synchronized
    fun end(logicalId: String) {
        sessionsBySource.entries.removeIf { it.value.logicalId == logicalId }
    }

    @Synchronized
    fun clear() = sessionsBySource.clear()
}

object ScreenRecordingSemanticFingerprint {
    fun compute(
        session: ScreenRecordingSession,
        design: ScreenRecordingDesignConfig = ScreenRecordingDesignConfig()
    ): Int = listOf(
        session.logicalId,
        session.startedAt,
        session.capabilities.canStop,
        session.capabilities.canPause,
        session.capabilities.canResume,
        design.left.name,
        design.right.name,
        "screen_recording_avatar_timer_v14_blank_app_badge",
        "recorder",
        1,
        2,
        43_200,
        "#FB382F"
    ).hashCode()
}
