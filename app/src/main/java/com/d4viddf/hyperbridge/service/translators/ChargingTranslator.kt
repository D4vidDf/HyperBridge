package com.d4viddf.hyperbridge.service.translators

import android.content.Context
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.integration.xiaomi.HyperIslandProtocolOptions
import com.d4viddf.hyperbridge.integration.xiaomi.buildJsonParam
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.service.charging.ChargingSource
import com.d4viddf.hyperbridge.service.charging.ChargingState
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

class ChargingTranslator(
    context: Context,
    repository: ThemeRepository
) : BaseTranslator(context, repository) {

    fun title(state: ChargingState): String = context.getString(
        when {
            state.isFull -> R.string.charging_island_full
            !state.isCharging -> R.string.charging_island_plugged_in
            state.source == ChargingSource.WIRELESS -> R.string.charging_island_wireless
            else -> R.string.charging_island_charging
        }
    )

    fun content(state: ChargingState): String = listOfNotNull(
        "${state.percent}%",
        state.watts?.let { context.getString(R.string.charging_island_watts, it) },
        state.minutesToFull?.let { context.getString(R.string.charging_island_full_in, formatMinutes(it)) }
    ).joinToString(" · ")

    fun translate(state: ChargingState, config: IslandConfig, theme: HyperTheme?): HyperIslandData {
        val title = title(state)
        val color = theme?.defaultProgress?.let { if (state.isFull) it.finishedColor else it.activeColor }
            ?: DEFAULT_COLOR
        val builder = HyperIslandNotification.Builder(context, BUSINESS, title)

        // Like the VPN island this is long-lived state: HyperOS already animates the plug-in
        // itself, so never take over the expanded island, and keep the pill until unplugged.
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

        builder.addPicture(getColoredPicture(PIC_BOLT, R.drawable.ic_charging_bolt, "#FFFFFF"))
        builder.addPicture(getColoredPicture(PIC_BADGE_BLANK, R.drawable.ic_vpn_app_badge_blank, "#FFFFFF"))

        builder.setChatInfo(
            title = title,
            content = content(state),
            pictureKey = PIC_BOLT,
            appPkg = PIC_BADGE_BLANK
        )
        builder.setProgressBar(state.percent, color)

        // Collapsed: bolt + speed on the left (when the kernel reports a believable current),
        // percentage on the right; the small pill is a ring that fills with the battery.
        builder.setBigIslandInfo(
            left = ImageTextInfoLeft(
                type = 1,
                picInfo = PicInfo(type = 1, pic = PIC_BOLT),
                textInfo = TextInfo(
                    title = state.watts?.let { context.getString(R.string.charging_island_watts, it) }.orEmpty(),
                    content = ""
                )
            ),
            right = ImageTextInfoRight(
                type = 2,
                textInfo = TextInfo(title = "${state.percent}%", content = "")
            )
        )
        builder.setSmallIslandCircularProgress(PIC_BOLT, state.percent, color, isCCW = true)

        return HyperIslandData(
            builder.buildResourceBundle(),
            builder.buildJsonParam(
                HyperIslandProtocolOptions(islandProperty = VpnTranslator.ACTION_ORIENTED_ISLAND_PROPERTY)
            )
        )
    }

    private fun formatMinutes(minutes: Int): String = if (minutes < 60) {
        context.getString(R.string.charging_island_minutes, minutes)
    } else {
        context.getString(R.string.charging_island_hours_minutes, minutes / 60, minutes % 60)
    }

    companion object {
        const val BUSINESS = "hyperbridge_charging"
        const val PIC_BOLT = "charging_bolt"
        const val PIC_BADGE_BLANK = "charging_badge_blank"
        private const val DEFAULT_COLOR = "#34C759"
    }
}
