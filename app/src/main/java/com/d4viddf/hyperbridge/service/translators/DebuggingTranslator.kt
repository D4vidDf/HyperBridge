package com.d4viddf.hyperbridge.service.translators

import android.content.Context
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.integration.xiaomi.HyperIslandProtocolOptions
import com.d4viddf.hyperbridge.integration.xiaomi.buildJsonParam
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.service.debugging.DebuggingState
import com.d4viddf.hyperbridge.service.debugging.DebuggingTransport
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

class DebuggingTranslator(
    context: Context,
    repository: ThemeRepository
) : BaseTranslator(context, repository) {

    fun title(state: DebuggingState): String = context.getString(
        when {
            !state.connected -> R.string.debugging_island_wireless_on
            state.transport == DebuggingTransport.BOTH -> R.string.debugging_island_both_connected
            state.transport == DebuggingTransport.WIRELESS -> R.string.debugging_island_wireless_connected
            else -> R.string.debugging_island_usb_connected
        }
    )

    fun content(state: DebuggingState): String =
        state.wifiAddress ?: context.getString(R.string.debugging_island_tap_to_manage)

    internal fun shortLabel(state: DebuggingState): String = context.getString(
        when (state.transport) {
            DebuggingTransport.USB -> R.string.debugging_island_short_usb
            DebuggingTransport.WIRELESS -> R.string.debugging_island_short_wireless
            DebuggingTransport.BOTH -> R.string.debugging_island_short_both
        }
    )

    fun translate(
        state: DebuggingState,
        config: IslandConfig,
        theme: HyperTheme?
    ): HyperIslandData {
        val title = title(state)
        // Connected reads as "in use"; wireless debugging waiting on the network is a heads-up.
        val color = if (state.connected) {
            theme?.defaultProgress?.activeColor ?: CONNECTED_COLOR
        } else {
            LISTENING_COLOR
        }
        val builder = HyperIslandNotification.Builder(context, BUSINESS, title)

        // Long-lived state like the VPN and charging islands: never take over the expanded
        // island, keep the pill until debugging stops.
        builder.setEnableFloat(false)
        builder.setIslandFirstFloat(false)
        builder.setShowNotification(config.isShowShade == true)
        builder.setIslandConfig(
            priority = VpnTranslator.ISLAND_PRIORITY,
            timeout = VpnTranslator.PERSISTENT_ISLAND_TIMEOUT_MILLIS,
            dismissible = false,
            highlightColor = color,
            expandedTimeMs = config.floatTimeout
        )

        builder.addPicture(getColoredPicture(PIC_ADB, R.drawable.ic_debugging_adb, "#FFFFFF"))
        builder.addPicture(getColoredPicture(PIC_BADGE_BLANK, R.drawable.ic_vpn_app_badge_blank, "#FFFFFF"))

        builder.setChatInfo(
            title = title,
            content = content(state),
            pictureKey = PIC_ADB,
            appPkg = PIC_BADGE_BLANK
        )
        builder.setBigIslandInfo(
            left = ImageTextInfoLeft(
                type = 1,
                picInfo = PicInfo(type = 1, pic = PIC_ADB),
                textInfo = TextInfo(title = shortLabel(state), content = "")
            )
        )
        builder.setSmallIsland(PIC_ADB)

        return HyperIslandData(
            builder.buildResourceBundle(),
            builder.buildJsonParam(
                HyperIslandProtocolOptions(islandProperty = VpnTranslator.ACTION_ORIENTED_ISLAND_PROPERTY)
            )
        )
    }

    companion object {
        const val BUSINESS = "hyperbridge_debugging"
        const val PIC_ADB = "debugging_adb"
        const val PIC_BADGE_BLANK = "debugging_badge_blank"
        private const val CONNECTED_COLOR = "#34C759"
        private const val LISTENING_COLOR = "#FF9F0A"
    }
}
