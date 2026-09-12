package com.d4viddf.hyperbridge.service.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DiagnosticEvent(
    val timestamp: Long,
    val packageName: String?,
    val classification: String,
    val action: String,
    val reason: String? = null
)

data class DiagnosticsState(
    val serviceConnected: Boolean = false,
    val activeIslands: Int = 0,
    val lastClassification: String? = null,
    val lastCallState: String? = null,
    val events: List<DiagnosticEvent> = emptyList()
)

/** Process-local, metadata-only diagnostics. Notification text and people are never accepted. */
object DiagnosticsStore {
    const val MAX_EVENTS = 50

    private val mutableState by lazy { MutableStateFlow(DiagnosticsState()) }
    val state: StateFlow<DiagnosticsState>
        get() = mutableState.asStateFlow()

    fun setServiceConnected(connected: Boolean) {
        mutableState.update { it.copy(serviceConnected = connected) }
    }

    fun setActiveIslands(count: Int) {
        mutableState.update { it.copy(activeIslands = count.coerceAtLeast(0)) }
    }

    fun record(
        classification: String,
        action: String,
        packageName: String? = null,
        reason: String? = null,
        callState: String? = null,
        timestamp: Long? = null
    ) {
        val event = DiagnosticEvent(timestamp ?: System.currentTimeMillis(), packageName, classification, action, reason)
        mutableState.update { current ->
            current.copy(
                lastClassification = classification,
                lastCallState = callState ?: current.lastCallState,
                events = (current.events + event).takeLast(MAX_EVENTS)
            )
        }
    }

    internal fun resetForTest() {
        mutableState.value = DiagnosticsState()
    }
}
