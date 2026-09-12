package com.d4viddf.hyperbridge.integration.xiaomi

import com.google.gson.Gson
import com.google.gson.JsonParser
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification

/** Capabilities supported by Xiaomi's protocol but not exposed by toolkit 0.4.4. */
data class HyperIslandProtocolOptions(
    val islandProperty: Int,
    val timerSystemCurrentMillis: Long? = null
) {
    init {
        require(islandProperty > 0)
        require(timerSystemCurrentMillis == null || timerSystemCurrentMillis >= 0)
    }
}

fun HyperIslandNotification.buildJsonParam(options: HyperIslandProtocolOptions): String =
    patchHyperIslandJson(buildJsonParam(), options)

internal fun patchHyperIslandJson(json: String, options: HyperIslandProtocolOptions): String {
    val root = JsonParser.parseString(json).asJsonObject
    val paramV2 = checkNotNull(root.getAsJsonObject("param_v2")) { "Missing param_v2" }
    val island = checkNotNull(paramV2.getAsJsonObject("param_island")) { "Missing param_island" }
    island.addProperty("islandProperty", options.islandProperty)

    val timer = island.getAsJsonObject("bigIslandArea")
        ?.getAsJsonObject("sameWidthDigitInfo")
        ?.getAsJsonObject("timerInfo")
    options.timerSystemCurrentMillis?.let {
        timer?.remove("timerTotal")
        timer?.addProperty("timerSystemCurrent", it)
    }
    return Gson().toJson(root)
}
