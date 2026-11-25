package com.d4viddf.hyperbridge.integration.xiaomi

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HyperIslandProtocolExtensionsTest {
    @Test fun exposesActionPropertyAndCorrectsToolkitCountUpClock() {
        val patched = patchHyperIslandJson(
            """{"param_v2":{"param_island":{"islandProperty":1,"bigIslandArea":{"sameWidthDigitInfo":{"timerInfo":{"timerType":1,"timerWhen":1000,"timerTotal":1000,"timerSystemCurrent":1000}}}}}}""",
            HyperIslandProtocolOptions(islandProperty = 2, timerSystemCurrentMillis = 9_000)
        )
        val island = JsonParser.parseString(patched).asJsonObject
            .getAsJsonObject("param_v2").getAsJsonObject("param_island")
        val timer = island.getAsJsonObject("bigIslandArea")
            .getAsJsonObject("sameWidthDigitInfo").getAsJsonObject("timerInfo")
        assertEquals(2, island["islandProperty"].asInt)
        assertEquals(1_000L, timer["timerWhen"].asLong)
        assertFalse(timer.has("timerTotal"))
        assertEquals(9_000L, timer["timerSystemCurrent"].asLong)
    }

    @Test fun acceptsKeyguardIslandProperty() {
        val patched = patchHyperIslandJson(
            """{"param_v2":{"param_island":{"islandProperty":1}}}""",
            HyperIslandProtocolOptions(islandProperty = KEYGUARD_ISLAND_PROPERTY)
        )
        val island = JsonParser.parseString(patched).asJsonObject
            .getAsJsonObject("param_v2").getAsJsonObject("param_island")
        assertEquals(0, island["islandProperty"].asInt)
    }

    @Test fun patchesCustomRemoteViewPayloadIslandProperty() {
        val patched = patchHyperIslandCustomJson(
            """{"ticker":"Song","param_island":{"islandProperty":1,"islandPriority":2}}""",
            HyperIslandProtocolOptions(islandProperty = KEYGUARD_ISLAND_PROPERTY)
        )
        val island = JsonParser.parseString(patched).asJsonObject
            .getAsJsonObject("param_island")
        assertEquals(0, island["islandProperty"].asInt)
    }

    @Test fun restoresUnlockedIslandPropertyForCustomPayload() {
        val patched = patchHyperIslandCustomJson(
            """{"ticker":"Song","param_island":{"islandProperty":0,"islandPriority":2}}""",
            HyperIslandProtocolOptions(islandProperty = 1)
        )
        val island = JsonParser.parseString(patched).asJsonObject
            .getAsJsonObject("param_island")
        assertEquals(1, island["islandProperty"].asInt)
    }

    @Test fun restoresUnlockedIslandPropertyForStandardJson() {
        val patched = patchHyperIslandJson(
            """{"param_v2":{"param_island":{"islandProperty":0}}}""",
            HyperIslandProtocolOptions(islandProperty = 1)
        )
        val island = JsonParser.parseString(patched).asJsonObject
            .getAsJsonObject("param_v2").getAsJsonObject("param_island")
        assertEquals(1, island["islandProperty"].asInt)
    }

    @Test fun patchesSameWidthDigitInfoWithTimerAndRemovesImageTextInfoRight() {
        val original = """
            {
              "param_v2": {
                "param_island": {
                  "islandProperty": 1,
                  "bigIslandArea": {
                    "imageTextInfoLeft": {
                      "type": 1,
                      "textInfo": { "title": "Song Title", "content": "" }
                    },
                    "imageTextInfoRight": {
                      "type": 2,
                      "textInfo": { "title": "02:45", "content": "" }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val options = HyperIslandProtocolOptions(
            islandProperty = 1,
            sameWidthDigitInfo = SameWidthDigitOptions(
                timerType = 1,
                timerWhen = 50_000L,
                timerSystemCurrentMillis = 65_000L,
                showHighlightColor = true
            )
        )

        val patched = patchHyperIslandJson(original, options)
        val bigIsland = JsonParser.parseString(patched).asJsonObject
            .getAsJsonObject("param_v2").getAsJsonObject("param_island")
            .getAsJsonObject("bigIslandArea")

        assertFalse("imageTextInfoRight should be removed when sameWidthDigitInfo is present", bigIsland.has("imageTextInfoRight"))
        assertEquals("Song Title", bigIsland.getAsJsonObject("imageTextInfoLeft").getAsJsonObject("textInfo")["title"].asString)

        val digitInfo = bigIsland.getAsJsonObject("sameWidthDigitInfo")
        val timer = digitInfo.getAsJsonObject("timerInfo")
        assertEquals(1, timer["timerType"].asInt)
        assertEquals(50_000L, timer["timerWhen"].asLong)
        assertEquals(65_000L, timer["timerSystemCurrent"].asLong)
        assertEquals(true, digitInfo["showHighlightColor"].asBoolean)
    }

    @Test fun patchesSameWidthDigitInfoWithStaticDigit() {
        val original = """
            {
              "param_v2": {
                "param_island": {
                  "islandProperty": 1,
                  "bigIslandArea": {
                    "imageTextInfoLeft": {
                      "type": 1,
                      "textInfo": { "title": "Song Title", "content": "" }
                    },
                    "imageTextInfoRight": {
                      "type": 2,
                      "textInfo": { "title": "02:45", "content": "" }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val options = HyperIslandProtocolOptions(
            islandProperty = 1,
            sameWidthDigitInfo = SameWidthDigitOptions(
                digit = "-01:15",
                showHighlightColor = true
            )
        )

        val patched = patchHyperIslandJson(original, options)
        val bigIsland = JsonParser.parseString(patched).asJsonObject
            .getAsJsonObject("param_v2").getAsJsonObject("param_island")
            .getAsJsonObject("bigIslandArea")

        assertFalse("imageTextInfoRight should be removed when sameWidthDigitInfo is present", bigIsland.has("imageTextInfoRight"))
        val digitInfo = bigIsland.getAsJsonObject("sameWidthDigitInfo")
        assertEquals("-01:15", digitInfo["digit"].asString)
        assertFalse("timerInfo should not exist for static digit", digitInfo.has("timerInfo"))
        assertEquals(true, digitInfo["showHighlightColor"].asBoolean)
    }

    @Test fun patchesSameWidthDigitInfoWithBothDigitAndTimer() {
        val original = """
            {
              "param_v2": {
                "param_island": {
                  "islandProperty": 1,
                  "bigIslandArea": {
                    "imageTextInfoLeft": {
                      "type": 1,
                      "textInfo": { "title": "Song Title", "content": "" }
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val options = HyperIslandProtocolOptions(
            islandProperty = 1,
            sameWidthDigitInfo = SameWidthDigitOptions(
                digit = "02:30",
                timerType = 1,
                timerWhen = 50_000L,
                timerSystemCurrentMillis = 65_000L,
                showHighlightColor = true
            )
        )

        val patched = patchHyperIslandJson(original, options)
        val bigIsland = JsonParser.parseString(patched).asJsonObject
            .getAsJsonObject("param_v2").getAsJsonObject("param_island")
            .getAsJsonObject("bigIslandArea")

        val digitInfo = bigIsland.getAsJsonObject("sameWidthDigitInfo")
        assertEquals("02:30", digitInfo["digit"].asString)
        val timer = digitInfo.getAsJsonObject("timerInfo")
        assertEquals(1, timer["timerType"].asInt)
        assertEquals(50_000L, timer["timerWhen"].asLong)
        assertEquals(65_000L, timer["timerSystemCurrent"].asLong)
        assertEquals(true, digitInfo["showHighlightColor"].asBoolean)
    }
}
