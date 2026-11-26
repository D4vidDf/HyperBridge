package com.d4viddf.hyperbridge.service.translators

import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.Bitmap
import androidx.core.graphics.toColorInt
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.integration.xiaomi.HyperIslandProtocolOptions
import com.d4viddf.hyperbridge.integration.xiaomi.buildJsonParam
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.service.vpn.VpnConnectionState
import com.d4viddf.hyperbridge.service.vpn.VpnControlKind
import com.d4viddf.hyperbridge.service.vpn.VpnCountryResolver
import com.d4viddf.hyperbridge.service.vpn.VpnSession
import com.d4viddf.hyperbridge.service.vpn.VpnPresentationPolicy
import com.d4viddf.hyperbridge.service.vpn.VpnTrafficFormatter
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo
import io.github.d4viddf.hyperisland_kit.models.TimerInfo

class VpnTranslator(
    context: Context,
    repository: ThemeRepository
) : BaseTranslator(context, repository) {

    fun translate(
        session: VpnSession,
        config: IslandConfig,
        theme: HyperTheme?,
        controlKind: VpnControlKind,
        controlIntent: PendingIntent?,
        notificationBacked: Boolean,
        timerStartedAtMillis: Long?,
        countryFlagBitmap: Bitmap? = null
    ): HyperIslandData {
        val presentation = VpnPresentationPolicy.plan(session, controlKind == VpnControlKind.DISCONNECT)
        val providerPackage = presentation.providerPackageName
        val title = titleFor(session, presentation.destinationLabel)
        val trafficText = session.traffic?.takeIf { presentation.showTraffic }?.let(VpnTrafficFormatter::format).orEmpty()
        val highlight = resolveColor(theme, providerPackage, DEFAULT_HIGHLIGHT)
        val builder = HyperIslandNotification.Builder(context, BUSINESS, title)

        // A VPN is long-lived state, not an arrival event. Never make it take over the expanded
        // Island automatically; notification-backed sessions can still be expanded manually.
        builder.setEnableFloat(false)
        builder.setIslandFirstFloat(false)
        builder.setShowNotification(config.isShowShade == true)
        builder.setIslandConfig(
            priority = ISLAND_PRIORITY,
            // A missing islandTimeout makes HyperOS apply its roughly 30-second default. The
            // controller owns this Island's lifecycle and cancels it when the VPN session ends,
            // so use the same effectively-permanent timeout strategy as PermanentIslandManager.
            timeout = PERSISTENT_ISLAND_TIMEOUT_MILLIS,
            dismissible = false,
            highlightColor = highlight,
            expandedTimeMs = config.floatTimeout
        )

        builder.addPicture(getColoredPicture(PIC_VPN, R.drawable.ic_vpn, "#FFFFFF"))
        if (notificationBacked) {
            builder.addPicture(
                getCountryFlagBadgedPicture(
                    key = PIC_IDENTITY,
                    resId = R.drawable.ic_vpn,
                    colorHex = "#FFFFFF",
                    countryFlagBitmap = countryFlagBitmap,
                    flagEmoji = presentation.countryCode?.let(VpnCountryResolver::flagEmoji)
                )
            )
            builder.addPicture(
                getColoredPicture(PIC_APP_BADGE_BLANK, R.drawable.ic_vpn_app_badge_blank, "#FFFFFF")
            )
        }

        val isDisconnect = controlKind == VpnControlKind.DISCONNECT
        val actionKey = if (isDisconnect) ACTION_DISCONNECT else ACTION_MANAGE
        val actionName = if (isDisconnect) "Disconnect" else "Manage"
        val actionConfig = providerPackage?.let { resolveActionConfig(theme, it, actionName) }
        val actionBackground = runCatching {
            (actionConfig?.backgroundColor ?: highlight).toColorInt()
        }.getOrDefault(Color.rgb(52, 199, 89))
        if (notificationBacked && controlIntent != null) {
            val actionPicture = getThemedActionPicture(
                key = if (isDisconnect) PIC_POWER else PIC_MANAGE,
                resId = if (isDisconnect) R.drawable.ic_vpn_power else R.drawable.ic_vpn_manage,
                theme = theme,
                packageName = providerPackage,
                backgroundColor = actionBackground
            )
            builder.addPicture(actionPicture)
            builder.addAction(
                HyperAction(
                    key = actionKey,
                    title = "",
                    icon = actionPicture.icon,
                    pendingIntent = controlIntent,
                    actionIntentType = 1,
                    actionBgColor = String.format("#%08X", actionBackground),
                    actionBgColorDark = String.format("#%08X", actionBackground),
                    titleColor = actionConfig?.tintColor ?: "#FFFFFF",
                    titleColorDark = actionConfig?.tintColor ?: "#FFFFFF"
                )
            )
        }

        val now = System.currentTimeMillis()
        val showDuration = timerStartedAtMillis != null && (!notificationBacked || presentation.showDuration)
        val timer = timerStartedAtMillis?.takeIf { showDuration }?.let { startedAt ->
            TimerInfo(
                timerType = TIMER_TYPE_COUNT_UP,
                timerWhen = startedAt,
                timerTotal = startedAt,
                timerSystemCurrent = now
            )
        }
        if (notificationBacked) {
            builder.setChatInfo(
                title = title,
                content = trafficText.ifBlank { null },
                pictureKey = PIC_IDENTITY,
                appPkg = providerPackage ?: PIC_APP_BADGE_BLANK,
                actionKeys = listOf(actionKey).takeIf { controlIntent != null }.orEmpty(),
                timer = timer
            )
        }

        if (showDuration) {
            builder.setBigIslandCountUp(checkNotNull(timerStartedAtMillis), PIC_VPN)
        } else {
            builder.setBigIslandInfo(
                left = ImageTextInfoLeft(
                    type = 1,
                    picInfo = PicInfo(type = 1, pic = PIC_VPN),
                    textInfo = TextInfo(title = "", content = "")
                )
            )
        }
        builder.setSmallIsland(PIC_VPN)

        return HyperIslandData(
            builder.buildResourceBundle(),
            builder.buildJsonParam(
                HyperIslandProtocolOptions(
                    islandProperty = ACTION_ORIENTED_ISLAND_PROPERTY,
                    timerSystemCurrentMillis = now.takeIf { showDuration }
                )
            )
        )
    }

    private fun titleFor(session: VpnSession, destinationLabel: String?): String = when (session.state) {
        VpnConnectionState.CONNECTING -> context.getString(R.string.vpn_connecting)
        VpnConnectionState.RECONNECTING -> context.getString(R.string.vpn_reconnecting)
        VpnConnectionState.WAITING_FOR_NETWORK -> context.getString(R.string.vpn_waiting_for_network)
        VpnConnectionState.PAUSED -> context.getString(R.string.vpn_paused)
        VpnConnectionState.DISCONNECTING -> context.getString(R.string.vpn_disconnecting)
        VpnConnectionState.BLOCKING -> context.getString(R.string.vpn_blocking)
        VpnConnectionState.ERROR -> context.getString(R.string.vpn_error)
        VpnConnectionState.DISCONNECTED -> context.getString(R.string.vpn_disconnected)
        VpnConnectionState.CONNECTED -> destinationLabel
            ?.let { context.getString(R.string.vpn_connected_to, it) }
            ?: context.getString(R.string.vpn_is_connected)
    }

    companion object {
        const val BUSINESS = "hyperbridge_vpn"
        const val ACTION_DISCONNECT = "vpn_disconnect"
        const val ACTION_MANAGE = "vpn_manage"
        const val PIC_VPN = "vpn_icon"
        const val PIC_IDENTITY = "vpn_identity"
        const val PIC_APP_BADGE_BLANK = "vpn_app_badge_blank"
        const val PIC_POWER = "vpn_power"
        const val PIC_MANAGE = "vpn_manage"
        const val TIMER_TYPE_COUNT_UP = 1
        // HyperOS interprets islandTimeout as milliseconds. Match PermanentIslandManager's
        // proven one-day value so ordinary controller refreshes cannot visibly expire the pill.
        const val PERSISTENT_ISLAND_TIMEOUT_MILLIS = 86_400_000
        const val ISLAND_PRIORITY = 1
        const val ACTION_ORIENTED_ISLAND_PROPERTY = 2
        private const val DEFAULT_HIGHLIGHT = "#34C759"
    }
}

