package com.d4viddf.hyperbridge.service.translators

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicTranslatorRawParamTest {

    @Test
    fun testFallbackCascadingInterpolation() {
        val contextMap = mapOf(
            "notif.title" to "",
            "notif.text" to "Hello World",
            "var.empty" to "",
            "var.exists" to "Active Value"
        )

        // Empty title falls back to text
        val res1 = DynamicTranslator.interpolateStatic("{notif.title | notif.text}", contextMap)
        assertEquals("Hello World", res1)

        // Non-empty exists wins over default
        val res2 = DynamicTranslator.interpolateStatic("{var.exists ?: Default Value}", contextMap)
        assertEquals("Active Value", res2)

        // Empty var falls back to default literal
        val res3 = DynamicTranslator.interpolateStatic("{var.empty ?: Fallback Literal}", contextMap)
        assertEquals("Fallback Literal", res3)

        // Missing variable cascades to literal
        val res4 = DynamicTranslator.interpolateStatic("{var.nonexistent | var.also_missing ?: Final Fallback}", contextMap)
        assertEquals("Final Fallback", res4)
    }

    @Test
    fun testValidateOrNormalizeParamV2() {
        val validJson = """
            {
              "param_v2": {
                "bigIslandArea": {
                  "title": "Order Update",
                  "info": "Driver on the way"
                }
              }
            }
        """.trimIndent()

        val normalized = DynamicTranslator.validateOrNormalizeParamV2(validJson)
        assertNotNull(normalized)
        assertTrue(normalized!!.contains("\"bigIslandArea\""))

        val bareObjectJson = """
            {
              "bigIslandArea": {
                "title": "Order Update",
                "info": "Driver on the way"
              }
            }
        """.trimIndent()

        val wrapped = DynamicTranslator.validateOrNormalizeParamV2(bareObjectJson)
        assertNotNull(wrapped)
        assertTrue(wrapped!!.contains("\"param_v2\""))
        assertTrue(wrapped.contains("\"bigIslandArea\""))

        val invalidJson = "{ not valid json }"
        val failed = DynamicTranslator.validateOrNormalizeParamV2(invalidJson)
        assertNull(failed)
    }

    @Test
    fun testFullParamV2JsonTemplateInterpolation() {
        val template = "{\"param_v2\":{\"protocol\":1,\"business\":\"media\",\"enableFloat\":true,\"updatable\":true,\"ticker\":\"{notif.title}\",\"tickerPic\":\"miui.focus.pic_imageText\",\"aodTitle\":\"{notif.title}\",\"aodPic\":\"miui.focus.pic_imageText\",\"param_island\":{\"islandProperty\":1,\"bigIslandArea\":{\"imageTextInfoLeft\":{\"type\":1,\"picInfo\":{\"type\":1,\"pic\":\"miui.focus.pic_imageText\"},\"textInfo\":{\"title\":\"{notif.title}\",\"content\":\"{media.artist | notif.text}\"}},\"picInfo\":{\"type\":1,\"pic\":\"miui.focus.pic_imageText\"}},\"smallIslandArea\":{\"picInfo\":{\"type\":1,\"pic\":\"miui.focus.pic_imageText\"}}},\"baseInfo\":{\"type\":2,\"title\":\"{notif.title}\",\"subTitle\":\"{notif.subtext | 'YouTube'}\",\"content\":\"Playing\",\"subContent\":\"{media.artist | notif.text}\",\"pic\":\"miui.focus.pic_imageText\"},\"bgInfo\":{\"type\":2,\"color\":\"#1C1C1E\",\"pic\":\"miui.focus.pic_imageText\"}}}"

        val variables = mapOf(
            "notif.title" to "Hero",
            "media.artist" to "David Kushner",
            "notif.text" to "David Kushner"
        )

        val interpolated = DynamicTranslator.interpolateStatic(template, variables)
        val validated = DynamicTranslator.validateOrNormalizeParamV2(interpolated)
        assertNotNull("Validated JSON should not be null", validated)
        assertTrue(validated!!.contains("\"Hero\""))
        assertTrue(validated.contains("\"David Kushner\""))
        assertTrue(validated.contains("\"YouTube\""))
        assertTrue(validated.contains("\"param_island\""))
        assertTrue(validated.contains("\"imageTextInfoLeft\""))
    }
}
