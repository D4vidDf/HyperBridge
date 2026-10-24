package com.d4viddf.hyperbridge.integration.xiaomi

import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.JsonParser
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification

/**
 * `islandProperty` value that SystemUI exempts from hiding islands on the keyguard, so the pill
 * keeps drawing at the cutout while locked (found in #267, verified on HyperOS 4). It only
 * takes effect when the island is added while the screen is on, and such islands get no row in
 * the notification shade.
 */
const val KEYGUARD_ISLAND_PROPERTY = 0

/** Capabilities supported by Xiaomi's protocol but not exposed by toolkit 0.4.4. */
data class HyperIslandProtocolOptions(
    val islandProperty: Int,
    val timerSystemCurrentMillis: Long? = null
) {
    init {
        require(islandProperty >= 0)
        require(timerSystemCurrentMillis == null || timerSystemCurrentMillis >= 0)
    }
}

fun HyperIslandNotification.buildJsonParam(options: HyperIslandProtocolOptions): String =
    patchHyperIslandJson(buildJsonParam(), options)

fun HyperIslandNotification.buildCustomExtras(options: HyperIslandProtocolOptions): Bundle =
    buildCustomExtras().apply {
        getString("miui.focus.param.custom")?.let { customJson ->
            putString("miui.focus.param.custom", patchHyperIslandCustomJson(customJson, options))
        }
    }

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

internal fun patchHyperIslandCustomJson(json: String, options: HyperIslandProtocolOptions): String {
    val root = JsonParser.parseString(json).asJsonObject
    val island = checkNotNull(root.getAsJsonObject("param_island")) { "Missing param_island" }
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
