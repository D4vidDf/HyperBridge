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
}
