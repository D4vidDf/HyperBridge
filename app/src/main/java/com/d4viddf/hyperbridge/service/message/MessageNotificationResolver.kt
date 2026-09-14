package com.d4viddf.hyperbridge.service.message

data class MessageNotificationSignals(
    val packageName: String,
    val notificationId: Int,
    val notificationTag: String?,
    val shortcutId: String?,
    val locusId: String?,
    val conversationTitle: String?,
    val isGroupSummary: Boolean
)

data class MessageIdentity(
    val logicalId: String,
    val source: String
)

class MessageNotificationResolver {
    fun resolve(signals: MessageNotificationSignals): MessageIdentity {
        val candidates = if (signals.isGroupSummary) emptyList() else listOf(
            "shortcut" to signals.shortcutId,
            "locus" to signals.locusId,
            "conversation" to signals.conversationTitle
        )
        val selected = candidates.firstOrNull { !it.second.isNullOrBlank() }
        if (selected != null) {
            return MessageIdentity(
                logicalId = "message:${signals.packageName}:${selected.first}:${opaque(selected.second!!)}",
                source = selected.first
            )
        }

        // Android's package + id/tag notification slot is deliberately the fallback identity.
        // It stays stable for normal notify(id, updatedNotification) message updates without
        // guessing from mutable titles, participants, or group-summary relationships.
        val slot = "${signals.notificationId}:${signals.notificationTag.orEmpty()}"
        return MessageIdentity(
            logicalId = "message:${signals.packageName}:slot:${opaque(slot)}",
            source = "notification-slot"
        )
    }

    private fun opaque(value: String): String = value.hashCode().toUInt().toString(16)
}
