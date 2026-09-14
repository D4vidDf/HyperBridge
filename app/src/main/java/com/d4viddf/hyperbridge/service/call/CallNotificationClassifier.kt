package com.d4viddf.hyperbridge.service.call

enum class CallState {
    INCOMING_RINGING,
    OUTGOING_CALLING,
    OUTGOING_RINGING,
    CONNECTING,
    ACTIVE,
    ENDED
}

/**
 * Android's CallStyle presentation classification. This deliberately does not model whether the
 * remote party has answered: the public API has no outgoing-dialing or outgoing-ringing type.
 */
enum class CallPresentationType {
    UNKNOWN,
    INCOMING,
    ONGOING,
    SCREENING
}

enum class CallActionRole {
    ANSWER,
    DECLINE_OR_HANG_UP,
    MICROPHONE,
    SPEAKER,
    OTHER
}

/**
 * State reported by the source application's notification action. An action named "Mute" means
 * the microphone is currently live; an action named "Unmute" means it is currently muted.
 */
enum class CallMicrophoneState {
    UNKNOWN,
    UNMUTED,
    MUTED
}

enum class CallActiveEvidence {
    NONE,
    CHRONOMETER_PRESENT,
    CHRONOMETER_STARTED,
    CHRONOMETER_BASE_RESET,
    CONNECTED_ACTIONS_APPEARED,
    INCOMING_ANSWERED,
    COMPOUND_SOURCE_REPLACEMENT
}

data class CallActionSignal(
    val title: String,
    val semanticAction: Int,
    val hasPendingIntent: Boolean,
    val hasRemoteInput: Boolean = false
)

data class CallNotificationSignals(
    val category: String?,
    val template: String?,
    val callType: Int?,
    val showsChronometer: Boolean,
    val whenTime: Long,
    val actions: List<CallActionSignal>,
    val isOngoingEvent: Boolean = false,
    val isForegroundService: Boolean = false,
    val isVideoCall: Boolean = false
)

data class CallClassification(
    val isCall: Boolean,
    val state: CallState,
    val reason: String,
    val activeEvidence: CallActiveEvidence = CallActiveEvidence.NONE,
    val presentationType: CallPresentationType = CallPresentationType.UNKNOWN,
    val hasAnswer: Boolean = false,
    val hasDeclineOrHangUp: Boolean = false,
    val hasConnectedControl: Boolean = false,
    val microphoneState: CallMicrophoneState = CallMicrophoneState.UNKNOWN,
    val actionRoles: Set<CallActionRole> = emptySet()
)

class CallNotificationClassifier(
    private val answerKeywords: List<String>,
    private val declineKeywords: List<String>,
    private val hangUpKeywords: List<String>,
    private val muteKeywords: List<String>,
    private val unmuteKeywords: List<String>,
    private val speakerKeywords: List<String>
) {
    fun classify(signals: CallNotificationSignals): CallClassification {
        val template = signals.template.orEmpty()
        val roles = signals.actions.map { roleForAction(it) }.toSet()
        val hasAnswer = roles.any { it == CallActionRole.ANSWER }
        val hasDeclineOrHangUp = roles.any { it == CallActionRole.DECLINE_OR_HANG_UP }
        val hasConnectedControl = signals.actions.any { action ->
            action.hasPendingIntent && when (roleForAction(action)) {
                CallActionRole.MICROPHONE,
                CallActionRole.SPEAKER -> true
                else -> false
            }
        }
        val microphoneState = signals.actions.asSequence()
            .filter { it.hasPendingIntent }
            .map(::microphoneStateForAction)
            .firstOrNull { it != CallMicrophoneState.UNKNOWN }
            ?: CallMicrophoneState.UNKNOWN
        val hasSemanticCallAction = signals.actions.any { it.semanticAction == SEMANTIC_ACTION_CALL }
        val callType = signals.callType ?: CALL_TYPE_UNKNOWN
        val presentationType = when (callType) {
            CALL_TYPE_INCOMING -> CallPresentationType.INCOMING
            CALL_TYPE_ONGOING -> CallPresentationType.ONGOING
            CALL_TYPE_SCREENING -> CallPresentationType.SCREENING
            else -> CallPresentationType.UNKNOWN
        }

        val hasValidChronometer = signals.showsChronometer && signals.whenTime > 0L
        val hasLiveCallEvidence = template == CALL_STYLE_TEMPLATE ||
                callType != CALL_TYPE_UNKNOWN ||
                hasAnswer ||
                hasDeclineOrHangUp ||
                hasValidChronometer ||
                (hasSemanticCallAction && signals.isOngoingEvent)
        val isMetadataCall = template == CALL_STYLE_TEMPLATE ||
                callType != CALL_TYPE_UNKNOWN ||
                (signals.category == CATEGORY_CALL && hasLiveCallEvidence)
        val isActionCall = (hasSemanticCallAction && signals.isOngoingEvent) ||
                (hasAnswer && hasDeclineOrHangUp) ||
                (hasValidChronometer && hasDeclineOrHangUp)

        if (!isMetadataCall && !isActionCall) {
            val reason = if (signals.category == CATEGORY_CALL) {
                // Apps such as Instagram keep CATEGORY_CALL on missed-call follow-ups whose
                // actions are Message and Call back. SEMANTIC_ACTION_CALL starts a future call;
                // without ongoing/live controls it is not evidence of a current call session.
                "passive-call-category"
            } else {
                "no-call-signals"
            }
            return CallClassification(false, CallState.ENDED, reason)
        }

        val activeEvidence = when {
            hasValidChronometer -> CallActiveEvidence.CHRONOMETER_PRESENT
            else -> CallActiveEvidence.NONE
        }

        val state = when {
            callType == CALL_TYPE_INCOMING || callType == CALL_TYPE_SCREENING -> CallState.INCOMING_RINGING
            hasAnswer && hasDeclineOrHangUp -> CallState.INCOMING_RINGING
            // CALL_TYPE_ONGOING is only an Android CallStyle presentation type. VoIP apps may use
            // it from the first outgoing callback, so it is never proof that the call was answered.
            callType == CALL_TYPE_ONGOING -> CallState.OUTGOING_CALLING
            hasDeclineOrHangUp -> CallState.OUTGOING_CALLING
            else -> CallState.CONNECTING
        }

        val reason = when {
            callType == CALL_TYPE_ONGOING -> "ongoing-presentation-not-connection"
            callType != CALL_TYPE_UNKNOWN -> "call-type-"
            signals.category == CATEGORY_CALL -> "category-call"
            template == CALL_STYLE_TEMPLATE -> "call-style"
            hasSemanticCallAction -> "semantic-call-action"
            else -> "call-action-fallback"
        }
        return CallClassification(
            isCall = true,
            state = state,
            reason = reason,
            activeEvidence = activeEvidence,
            presentationType = presentationType,
            hasAnswer = hasAnswer,
            hasDeclineOrHangUp = hasDeclineOrHangUp,
            hasConnectedControl = hasConnectedControl,
            microphoneState = microphoneState,
            actionRoles = roles
        )
    }

    fun roleForAction(action: CallActionSignal): CallActionRole {
        val title = action.title.lowercase()
        return when {
            answerKeywords.any { title.contains(it.lowercase()) } -> CallActionRole.ANSWER
            declineKeywords.any { title.contains(it.lowercase()) } -> CallActionRole.DECLINE_OR_HANG_UP
            hangUpKeywords.any { title.contains(it.lowercase()) } -> CallActionRole.DECLINE_OR_HANG_UP
            action.semanticAction == SEMANTIC_ACTION_MUTE ||
                    action.semanticAction == SEMANTIC_ACTION_UNMUTE -> CallActionRole.MICROPHONE
            // Check unmute first because labels such as "Unmute" also contain "mute".
            unmuteKeywords.any { matchesActionKeyword(title, it) } -> CallActionRole.MICROPHONE
            muteKeywords.any { matchesActionKeyword(title, it) } -> CallActionRole.MICROPHONE
            speakerKeywords.any { title.contains(it.lowercase()) } -> CallActionRole.SPEAKER
            else -> CallActionRole.OTHER
        }
    }

    fun microphoneStateForAction(action: CallActionSignal): CallMicrophoneState {
        val title = action.title.lowercase()
        return when {
            action.semanticAction == SEMANTIC_ACTION_UNMUTE -> CallMicrophoneState.MUTED
            action.semanticAction == SEMANTIC_ACTION_MUTE -> CallMicrophoneState.UNMUTED
            unmuteKeywords.any { matchesActionKeyword(title, it) } -> CallMicrophoneState.MUTED
            muteKeywords.any { matchesActionKeyword(title, it) } -> CallMicrophoneState.UNMUTED
            else -> CallMicrophoneState.UNKNOWN
        }
    }

    private fun matchesActionKeyword(normalizedTitle: String, keyword: String): Boolean {
        val normalizedKeyword = keyword.trim().lowercase()
        if (normalizedKeyword.isEmpty()) return false
        return normalizedTitle == normalizedKeyword ||
                normalizedTitle.startsWith(" ") ||
                normalizedTitle.endsWith(" ")
    }

    companion object {
        const val CATEGORY_CALL = "call"
        const val CALL_STYLE_TEMPLATE = "android.app.Notification\$CallStyle"
        const val CALL_TYPE_UNKNOWN = 0
        const val CALL_TYPE_INCOMING = 1
        const val CALL_TYPE_ONGOING = 2
        const val CALL_TYPE_SCREENING = 3
        const val SEMANTIC_ACTION_MUTE = 6
        const val SEMANTIC_ACTION_UNMUTE = 7
        const val SEMANTIC_ACTION_CALL = 10
    }
}
