package com.d4viddf.hyperbridge.service.recording

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenRecordingToolProtocolTest {
    @Test
    fun statusRequestUsesVerifiedJsonRpcContract() {
        val request = JsonParser.parseString(
            ScreenRecordingToolProtocol.request(ScreenRecordingToolProtocol.TOOL_STATUS, requestId = 7)
        ).asJsonObject

        assertEquals("2.0", request["jsonrpc"].asString)
        assertEquals(7, request["id"].asInt)
        assertEquals("tools/call", request["method"].asString)
        assertEquals(
            "screenrecorder_get_status",
            request.getAsJsonObject("params")["name"].asString
        )
        assertTrue(request.getAsJsonObject("params").getAsJsonObject("arguments").entrySet().isEmpty())
    }

    @Test
    fun responseParsingUsesStructuralErrorFlagNotLocalizedText() {
        assertTrue(
            ScreenRecordingToolProtocol.isSuccessfulResponse(
                """{"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"正在录屏"}],"isError":false}}"""
            )
        )
        assertFalse(
            ScreenRecordingToolProtocol.isSuccessfulResponse(
                """{"jsonrpc":"2.0","id":1,"result":{"content":[],"isError":true}}"""
            )
        )
        assertFalse(ScreenRecordingToolProtocol.isSuccessfulResponse("not-json"))
        assertFalse(
            ScreenRecordingToolProtocol.isSuccessfulResponse(
                """{"jsonrpc":"2.0","id":1,"error":{"code":-32603}}"""
            )
        )
        assertFalse(
            ScreenRecordingToolProtocol.isSuccessfulResponse(
                """{"jsonrpc":"2.0","id":1,"result":{"isError":"false"}}"""
            )
        )
    }
}
