package com.d4viddf.hyperbridge.service

data class MessageContentCandidate(
    val sender: String?,
    val text: String?,
    val timestamp: Long? = null
)

data class ResolvedNotificationContent(
    val title: String,
    val text: String,
    val hasMessageContent: Boolean,
    val latestMessageTimestamp: Long?,
    val messageCount: Int
)

/** Resolves display content without depending on any app-specific notification shape. */
object NotificationContentResolver {
    fun resolve(
        title: CharSequence?,
        text: CharSequence?,
        bigTitle: CharSequence?,
        bigText: CharSequence?,
        messages: List<MessageContentCandidate>,
        isMessageStyle: Boolean,
        textLines: List<CharSequence?> = emptyList()
    ): ResolvedNotificationContent {
        val latestMessage = messages.lastOrNull { it.text.clean().isNotEmpty() }
        val latestTextLine = textLines.asReversed().firstOrNull { it.clean().isNotEmpty() }
        val resolvedTitle = title.clean()
            .ifEmpty { bigTitle.clean() }
            .ifEmpty { latestMessage?.sender.clean() }
        val resolvedText = text.clean()
            .ifEmpty { bigText.clean() }
            .ifEmpty { latestTextLine.clean() }
            .ifEmpty { latestMessage?.text.clean() }
        return ResolvedNotificationContent(
            title = resolvedTitle,
            text = resolvedText,
            hasMessageContent = isMessageStyle && latestMessage != null,
            latestMessageTimestamp = latestMessage?.timestamp?.takeIf { it > 0L },
            messageCount = messages.size
        )
    }

    private fun Any?.clean(): String = this?.toString()?.trim().orEmpty()
}
