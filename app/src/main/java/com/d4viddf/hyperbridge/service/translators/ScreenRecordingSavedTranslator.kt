package com.d4viddf.hyperbridge.service.translators

import android.content.Context
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import io.github.d4viddf.hyperisland_kit.models.BigIslandArea
import io.github.d4viddf.hyperisland_kit.models.HyperIslandPayload
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.ParamIsland
import io.github.d4viddf.hyperisland_kit.models.ParamV2
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.SmallIslandArea
import io.github.d4viddf.hyperisland_kit.models.TextInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ScreenRecordingSavedTranslator(
    context: Context,
    repository: ThemeRepository
) : BaseTranslator(context, repository) {

    fun translate(
        sbn: StatusBarNotification,
        picKey: String,
        config: IslandConfig,
        theme: HyperTheme?
    ): HyperIslandData {
        val checkKey = "${picKey}_recording_saved"
        val highlightColor = resolveColor(theme, sbn.packageName, "#FFFFFF")
        val checkPicture = getColoredPicture(checkKey, R.drawable.ic_screen_recording_saved, SAVED_GREEN)
        val sourcePicture = resolveIcon(sbn, picKey)
        val resources = Bundle().apply {
            putBundle("miui.focus.actions", Bundle())
            putBundle("miui.focus.pics", Bundle().apply {
                putParcelable("miui.focus.pic_${checkPicture.key}", checkPicture.icon)
                putParcelable("miui.focus.pic_${sourcePicture.key}", sourcePicture.icon)
            })
        }
        val payload = ScreenRecordingSavedPayloadFactory.build(
            business = stableBusinessId(picKey),
            compactTitle = context.getString(R.string.screen_recording_done),
            checkKey = "miui.focus.pic_$checkKey",
            sourceIconKey = "miui.focus.pic_$picKey",
            showNotification = config.isShowShade ?: false,
            timeout = config.timeout,
            highlightColor = highlightColor
        )
        return HyperIslandData(resources, payload)
    }

    private companion object {
        const val SAVED_GREEN = "#34C759"
    }
}

internal object ScreenRecordingSavedPayloadFactory {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
    }

    fun build(
        business: String,
        compactTitle: String,
        checkKey: String,
        sourceIconKey: String,
        showNotification: Boolean,
        timeout: Int?,
        highlightColor: String
    ): String = json.encodeToString(
        HyperIslandPayload(
            paramV2 = ParamV2(
                protocol = 3,
                business = business,
                ticker = compactTitle,
                enableFloat = false,
                isShowNotification = showNotification,
                islandFirstFloat = false,
                reopen = false,
                paramIsland = ParamIsland(
                    islandProperty = 1,
                    islandPriority = 2,
                    islandTimeout = timeout,
                    dismissIsland = true,
                    expandedTime = 0,
                    highlightColor = highlightColor,
                    // Keep the proven Permanent Island big-area anchor, but populate its two
                    // visual slots. With no content intent and no expanded content this remains
                    // non-touchable while HyperOS still renders it around the camera cutout.
                    bigIslandArea = BigIslandArea(
                        imageTextInfoLeft = ImageTextInfoLeft(
                            type = 1,
                            picInfo = PicInfo(type = 1, pic = sourceIconKey),
                            textInfo = TextInfo(title = compactTitle, content = "")
                        ),
                        imageTextInfoRight = ImageTextInfoRight(
                            type = 2,
                            picInfo = PicInfo(type = 1, pic = checkKey)
                        )
                    ),
                    smallIslandArea = SmallIslandArea(
                        picInfo = PicInfo(type = 1, pic = sourceIconKey)
                    )
                )
            )
        )
    )
}
