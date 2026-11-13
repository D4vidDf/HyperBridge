package com.d4viddf.hyperbridge.service.translators

import com.google.gson.JsonParser
import com.d4viddf.hyperbridge.service.recording.ScreenRecordingCapabilities
import com.d4viddf.hyperbridge.service.recording.ScreenRecordingSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenRecordingPayloadFactoryTest {
    @Test
    fun nativeRecorderPayloadContainsAvatarTimerLayoutAndStop() {
        val root = JsonParser.parseString(payload(canStop = true)).asJsonObject
        val param = root.getAsJsonObject("param_v2")
        val island = param.getAsJsonObject("param_island")
        val timer = island.getAsJsonObject("bigIslandArea")
            .getAsJsonObject("sameWidthDigitInfo")
            .getAsJsonObject("timerInfo")
        val expanded = param.getAsJsonObject("chatInfo")
        val expandedLabel = island.getAsJsonObject("bigIslandArea")
            .getAsJsonObject("sameWidthDigitInfo")
        val expandedPulse = island.getAsJsonObject("bigIslandArea")
            .getAsJsonObject("imageTextInfoLeft")
            .getAsJsonObject("picInfo")
        val minimizedLabel = island.getAsJsonObject("bigIslandArea")
            .getAsJsonObject("imageTextInfoLeft")
            .getAsJsonObject("textInfo")
        val action = param.getAsJsonArray("actions")[0].asJsonObject

        assertEquals(1, param["protocol"].asInt)
        assertEquals("screen_recording", param["business"].asString)
        assertEquals("recorder", param["scene"].asString)
        assertEquals("", param["content"].asString)
        assertEquals("", param["ticker"].asString)
        assertFalse(param["enableFloat"].asBoolean)
        assertFalse(param["showSmallIcon"].asBoolean)
        assertTrue(param["hideDeco"].asBoolean)
        assertFalse(param["islandFirstFloat"].asBoolean)
        assertEquals(1, island["islandPriority"].asInt)
        assertEquals(2, island["islandProperty"].asInt)
        assertEquals(43_200, island["islandTimeout"].asInt)
        assertEquals("#FB382F", island["highlightColor"].asString)
        assertEquals(1_000L, timer["timerWhen"].asLong)
        assertEquals(1, timer["timerType"].asInt)
        assertEquals(9_000L, timer["timerSystemCurrent"].asLong)
        assertFalse(expandedLabel.has("content"))
        assertEquals("Recording..", minimizedLabel["title"].asString)
        assertEquals(1, expandedPulse["type"].asInt)
        assertEquals("miui.focus.pic_ticker", expandedPulse["pic"].asString)
        assertFalse(param.has("animTextInfo"))
        assertEquals("miui.focus.pic_ticker", expanded["picProfile"].asString)
        assertEquals("miui.focus.pic_ticker", expanded["picProfileDark"].asString)
        assertEquals("miui.focus.pic_recorder_app_badge", expanded["appIconPkg"].asString)
        assertEquals("Recording screen..", expanded["title"].asString)
        assertEquals(1_000L, expanded.getAsJsonObject("timerInfo")["timerWhen"].asLong)
        assertEquals(1_000L, expanded.getAsJsonObject("timerInfo")["timerTotal"].asLong)
        assertEquals(9_000L, expanded.getAsJsonObject("timerInfo")["timerSystemCurrent"].asLong)
        assertEquals("miui.focus.action_1", action["action"].asString)
        assertEquals("miui.focus.pic_stop", action["actionIcon"].asString)
        assertEquals("miui.focus.pic_stop_dark", action["actionIconDark"].asString)
    }

    @Test
    fun failedCapabilityProbeRemovesOnlyActions() {
        val param = JsonParser.parseString(payload(canStop = false))
            .asJsonObject
            .getAsJsonObject("param_v2")

        assertFalse(param.has("actions"))
        assertTrue(param.has("param_island"))
        assertTrue(param.has("chatInfo"))
    }

    @Test
    fun savedPayloadIsCompactOnlyWithoutTextOrExpandableContent() {
        val param = JsonParser.parseString(
            ScreenRecordingSavedPayloadFactory.build(
                business = "screen_recording_saved_42",
                compactTitle = "Done recording.",
                checkKey = "miui.focus.pic_saved_check",
                sourceIconKey = "miui.focus.pic_source",
                showNotification = false,
                timeout = 30,
                highlightColor = "#34C759"
            )
        ).asJsonObject.getAsJsonObject("param_v2")
        val island = param.getAsJsonObject("param_island")
        val anchor = island.getAsJsonObject("bigIslandArea")
            .getAsJsonObject("imageTextInfoLeft")
        val check = island.getAsJsonObject("bigIslandArea")
            .getAsJsonObject("imageTextInfoRight")
        val compact = island.getAsJsonObject("smallIslandArea")

        assertEquals("Done recording.", param["ticker"].asString)
        assertFalse(param["enableFloat"].asBoolean)
        assertFalse(param["islandFirstFloat"].asBoolean)
        assertFalse(param["reopen"].asBoolean)
        assertTrue(island["dismissIsland"].asBoolean)
        assertEquals(0, island["expandedTime"].asInt)
        assertEquals(1, anchor["type"].asInt)
        assertEquals("miui.focus.pic_source", anchor.getAsJsonObject("picInfo")["pic"].asString)
        assertEquals("Done recording.", anchor.getAsJsonObject("textInfo")["title"].asString)
        assertEquals("", anchor.getAsJsonObject("textInfo")["content"].asString)
        assertEquals(2, check["type"].asInt)
        assertEquals("miui.focus.pic_saved_check", check.getAsJsonObject("picInfo")["pic"].asString)
        assertFalse(check.has("textInfo"))
        assertFalse(param.has("iconTextInfo"))
        assertFalse(param.has("animTextInfo"))
        assertEquals("miui.focus.pic_source", compact.getAsJsonObject("picInfo")["pic"].asString)
    }

    @Test
    fun customDesignLeftIconOnlyOmitText() {
        val root = JsonParser.parseString(
            payload(
                canStop = true,
                design = com.d4viddf.hyperbridge.models.ScreenRecordingDesignConfig(
                    left = com.d4viddf.hyperbridge.models.ScreenRecordingLeftDesign.ICON_ONLY
                )
            )
        ).asJsonObject
        val left = root.getAsJsonObject("param_v2")
            .getAsJsonObject("param_island")
            .getAsJsonObject("bigIslandArea")
            .getAsJsonObject("imageTextInfoLeft")

        assertEquals("miui.focus.pic_ticker", left.getAsJsonObject("picInfo")["pic"].asString)
        assertFalse(left.has("textInfo"))
    }

    @Test
    fun customDesignLeftTextOnlyUsesBlankBadge() {
        val root = JsonParser.parseString(
            payload(
                canStop = true,
                design = com.d4viddf.hyperbridge.models.ScreenRecordingDesignConfig(
                    left = com.d4viddf.hyperbridge.models.ScreenRecordingLeftDesign.TEXT_ONLY
                )
            )
        ).asJsonObject
        val left = root.getAsJsonObject("param_v2")
            .getAsJsonObject("param_island")
            .getAsJsonObject("bigIslandArea")
            .getAsJsonObject("imageTextInfoLeft")

        assertEquals("miui.focus.pic_recorder_app_badge", left.getAsJsonObject("picInfo")["pic"].asString)
        assertEquals("Recording..", left.getAsJsonObject("textInfo")["title"].asString)
    }

    @Test
    fun customDesignRightNoneOmitsTimerDigitInfo() {
        val root = JsonParser.parseString(
            payload(
                canStop = true,
                design = com.d4viddf.hyperbridge.models.ScreenRecordingDesignConfig(
                    right = com.d4viddf.hyperbridge.models.ScreenRecordingRightDesign.NONE
                )
            )
        ).asJsonObject
        val bigIsland = root.getAsJsonObject("param_v2")
            .getAsJsonObject("param_island")
            .getAsJsonObject("bigIslandArea")

        assertFalse(bigIsland.has("sameWidthDigitInfo"))
    }

    private fun payload(
        canStop: Boolean,
        design: com.d4viddf.hyperbridge.models.ScreenRecordingDesignConfig = com.d4viddf.hyperbridge.models.ScreenRecordingDesignConfig()
    ) = ScreenRecordingPayloadFactory.build(
        session = ScreenRecordingSession(
            logicalId = "screen-recording:key:1000",
            sourceKey = "key",
            packageName = "com.miui.screenrecorder",
            startedAt = 1_000L,
            capabilities = ScreenRecordingCapabilities(canStop = canStop)
        ),
        now = 9_000L,
        compactText = "Recording..",
        expandedText = "Recording screen..",
        notifyId = "com.d4viddf.hyperbridge:42",
        design = design
    )
}
