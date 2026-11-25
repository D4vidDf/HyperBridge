package com.d4viddf.hyperbridge.service.translators

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.integration.xiaomi.HyperIslandProtocolOptions
import com.d4viddf.hyperbridge.integration.xiaomi.KEYGUARD_ISLAND_PROPERTY
import com.d4viddf.hyperbridge.integration.xiaomi.buildJsonParam
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.PrivacyIndicatorStyle
import com.d4viddf.hyperbridge.service.privacy.PrivacyIndicatorKind
import com.d4viddf.hyperbridge.service.privacy.PrivacyIndicatorState
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

class PrivacyIndicatorTranslator(
    context: Context,
    repository: ThemeRepository
) : BaseTranslator(context, repository) {

    fun title(): String = context.getString(R.string.privacy_island_title)

    fun content(state: PrivacyIndicatorState): String = context.getString(
        when (state.kind) {
            PrivacyIndicatorKind.MICROPHONE -> R.string.privacy_island_mic_in_use
            PrivacyIndicatorKind.CAMERA -> R.string.privacy_island_camera_in_use
            PrivacyIndicatorKind.MICROPHONE_AND_CAMERA -> R.string.privacy_island_mic_and_camera
        }
    )

    fun translate(
        state: PrivacyIndicatorState,
        config: IslandConfig,
        style: PrivacyIndicatorStyle = PrivacyIndicatorStyle.ICON,
        showText: Boolean = true,
        alwaysShow: Boolean = false
    ): HyperIslandData {
        val title = title()
        val text = content(state)
        val builder = HyperIslandNotification.Builder(context, BUSINESS, title)

        // Quiet long-lived state like the VPN island: never take over the expanded island.
        builder.setEnableFloat(false)
        builder.setIslandFirstFloat(false)
        builder.setShowNotification(config.isShowShade == true)
        // setIslandConfig replaces every field on each call, so it is called exactly once. The
        // controller owns the lifecycle and cancels the island when nothing is in use.
        builder.setIslandConfig(
            priority = VpnTranslator.ISLAND_PRIORITY,
            timeout = VpnTranslator.PERSISTENT_ISLAND_TIMEOUT_MILLIS,
            dismissible = false,
            highlightColor = state.kind.colorHex,
            expandedTimeMs = config.floatTimeout
        )

        builder.addPicture(iconFor(state, style))
        builder.addPicture(getColoredPicture(PIC_BADGE_BLANK, R.drawable.ic_vpn_app_badge_blank, "#FFFFFF"))

        builder.setChatInfo(
            title = title,
            content = text,
            pictureKey = PIC_ICON,
            appPkg = PIC_BADGE_BLANK
        )
        builder.setBigIslandInfo(
            left = ImageTextInfoLeft(
                type = 1,
                picInfo = PicInfo(type = 1, pic = PIC_ICON),
                textInfo = TextInfo(title = if (showText) text else "", content = "")
            )
        )
        builder.setSmallIsland(PIC_ICON)

        return HyperIslandData(
            builder.buildResourceBundle(),
            builder.buildJsonParam(
                HyperIslandProtocolOptions(
                    // The keyguard property also keeps the pill over full-screen apps such as
                    // the camera, which hide the status bar and every regular island.
                    islandProperty = if (alwaysShow) KEYGUARD_ISLAND_PROPERTY
                    else VpnTranslator.ACTION_ORIENTED_ISLAND_PROPERTY
                )
            )
        )
    }

    private fun iconFor(state: PrivacyIndicatorState, style: PrivacyIndicatorStyle): HyperPicture = when (style) {
        PrivacyIndicatorStyle.DOT -> when (state.kind) {
            PrivacyIndicatorKind.MICROPHONE ->
                getColoredPicture(PIC_ICON, R.drawable.ic_privacy_dot, PrivacyIndicatorKind.MICROPHONE.colorHex)
            PrivacyIndicatorKind.CAMERA ->
                getColoredPicture(PIC_ICON, R.drawable.ic_privacy_dot, PrivacyIndicatorKind.CAMERA.colorHex)
            PrivacyIndicatorKind.MICROPHONE_AND_CAMERA -> HyperPicture(
                PIC_ICON,
                ContextCompat.getDrawable(context, R.drawable.ic_privacy_dot_mic_camera)!!.toBitmap(128, 128)
            )
        }
        PrivacyIndicatorStyle.ICON -> when (state.kind) {
            PrivacyIndicatorKind.MICROPHONE ->
                getColoredPicture(PIC_ICON, R.drawable.ic_privacy_mic, PrivacyIndicatorKind.MICROPHONE.colorHex)
            PrivacyIndicatorKind.CAMERA ->
                getColoredPicture(PIC_ICON, R.drawable.ic_privacy_camera, PrivacyIndicatorKind.CAMERA.colorHex)
            // Two-tone drawable with its own colours: loaded without tinting.
            PrivacyIndicatorKind.MICROPHONE_AND_CAMERA -> HyperPicture(
                PIC_ICON,
                ContextCompat.getDrawable(context, R.drawable.ic_privacy_mic_camera)!!.toBitmap(128, 128)
            )
        }
    }

    companion object {
        const val BUSINESS = "hyperbridge_privacy_indicator"
        const val PIC_ICON = "privacy_icon"
        const val PIC_BADGE_BLANK = "privacy_badge_blank"
    }
}
