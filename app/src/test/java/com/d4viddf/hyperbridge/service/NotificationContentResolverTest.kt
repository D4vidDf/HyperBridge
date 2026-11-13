package com.d4viddf.hyperbridge.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationContentResolverTest {
    @Test
    fun blankTopLevelMessagingStyleResolvesLatestMessage() {
        val content = NotificationContentResolver.resolve(
            title = "",
            text = "",
            bigTitle = null,
            bigText = null,
            messages = listOf(MessageContentCandidate("Alice", "hello")),
            isMessageStyle = true
        )

        assertEquals("Alice", content.title)
        assertEquals("hello", content.text)
        assertTrue(content.hasMessageContent)
    }

    @Test
    fun messagingContentPreventsEmptyClassification() {
        val content = NotificationContentResolver.resolve(
            title = null,
            text = null,
            bigTitle = null,
            bigText = null,
            messages = listOf(MessageContentCandidate("Alice", "hello")),
            isMessageStyle = true
        )

        assertFalse(content.title.isEmpty() && content.text.isEmpty() && !content.hasMessageContent)
    }

    @Test
    fun validTopLevelContentTakesPrecedence() {
        val content = NotificationContentResolver.resolve(
            title = "Conversation",
            text = "Top-level text",
            bigTitle = "Big title",
            bigText = "Big text",
            messages = listOf(MessageContentCandidate("Alice", "message text")),
            isMessageStyle = true
        )

        assertEquals("Conversation", content.title)
        assertEquals("Top-level text", content.text)
    }

    @Test
    fun lastMeaningfulTextLinePrecedesMessagingStyleFallback() {
        val content = NotificationContentResolver.resolve(
            title = "Conversation",
            text = null,
            bigTitle = null,
            bigText = null,
            messages = listOf(MessageContentCandidate("Alice", "message fallback", timestamp = 100L)),
            isMessageStyle = true,
            textLines = listOf("older", "  ", "latest line")
        )

        assertEquals("latest line", content.text)
        assertEquals(100L, content.latestMessageTimestamp)
        assertEquals(1, content.messageCount)
    }

    @Test
    fun bigTextPrecedesTextLines() {
        val content = NotificationContentResolver.resolve(
            title = "Conversation",
            text = null,
            bigTitle = null,
            bigText = "expanded text",
            messages = emptyList(),
            isMessageStyle = false,
            textLines = listOf("line fallback")
        )

        assertEquals("expanded text", content.text)
    }

    @Test
    fun usefulStandardContentDoesNotRequireMessagingStyle() {
        val content = NotificationContentResolver.resolve(
            title = "Alice",
            text = "hello",
            bigTitle = null,
            bigText = null,
            messages = emptyList(),
            isMessageStyle = false
        )

        assertEquals("Alice", content.title)
        assertEquals("hello", content.text)
        assertFalse(content.hasMessageContent)
    }
}
