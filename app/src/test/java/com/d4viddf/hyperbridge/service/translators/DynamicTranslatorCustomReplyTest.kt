package com.d4viddf.hyperbridge.service.translators

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DynamicTranslatorCustomReplyTest {

    private val chatA = "0|com.whatsapp|1|chat_a@s.whatsapp.net|10123"
    private val chatB = "0|com.whatsapp|1|chat_b@s.whatsapp.net|10123"

    @Test
    fun `two islands from the same translator get different reply request codes`() {
        assertNotEquals(
            DynamicTranslator.customReplyRequestCode(chatA, "reply"),
            DynamicTranslator.customReplyRequestCode(chatB, "reply")
        )
    }

    @Test
    fun `request code is stable for the same notification and action`() {
        assertEquals(
            DynamicTranslator.customReplyRequestCode(chatA, "reply"),
            DynamicTranslator.customReplyRequestCode(chatA, "reply")
        )
    }

    @Test
    fun `different custom actions on one island do not share a request code`() {
        assertNotEquals(
            DynamicTranslator.customReplyRequestCode(chatA, "reply"),
            DynamicTranslator.customReplyRequestCode(chatA, "reply_2")
        )
    }
}
