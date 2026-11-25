package com.d4viddf.hyperbridge.service.translators

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.integration.xiaomi.HyperIslandProtocolOptions
import com.d4viddf.hyperbridge.integration.xiaomi.buildJsonParam
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.service.systemtoggles.SystemToggle
import com.d4viddf.hyperbridge.service.systemtoggles.SystemToggleEvent
import com.d4viddf.hyperbridge.service.systemtoggles.SystemToggleTracker
import com.d4viddf.hyperbridge.service.systemtoggles.ToggleValue
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

/** The short island shown when the user flips a system switch ("Battery saver on"). */
class SystemToggleTranslator(
    context: Context,
    repository: ThemeRepository
) : BaseTranslator(context, repository) {

    fun title(): String = context.getString(R.string.system_toggles_title)

    fun content(event: SystemToggleEvent): String = context.getString(textRes(event))

    fun translate(event: SystemToggleEvent, config: IslandConfig): HyperIslandData {
        val title = title()
        val text = content(event)
        val builder = HyperIslandNotification.Builder(context, BUSINESS, title)

        // A passing confirmation, not a state to keep: it never takes over the expanded island and
        // the controller removes it after a few seconds.
        builder.setEnableFloat(false)
        builder.setIslandFirstFloat(false)
        builder.setShowNotification(config.isShowShade == true)
        builder.setIslandConfig(
            priority = VpnTranslator.ISLAND_PRIORITY,
            timeout = SystemToggleTracker.ISLAND_TIMEOUT_SECONDS,
            dismissible = true,
            highlightColor = event.value.colorHex,
            expandedTimeMs = config.floatTimeout
        )

        builder.addPicture(getColoredPicture(PIC_ICON, iconRes(event), event.value.colorHex))
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
                textInfo = TextInfo(title = text, content = "")
            )
        )
        builder.setSmallIsland(PIC_ICON)

        return HyperIslandData(
            builder.buildResourceBundle(),
            builder.buildJsonParam(
                HyperIslandProtocolOptions(islandProperty = VpnTranslator.ACTION_ORIENTED_ISLAND_PROPERTY)
            )
        )
    }

    companion object {
        const val BUSINESS = "hyperbridge_system_toggle"
        const val PIC_ICON = "system_toggle_icon"
        const val PIC_BADGE_BLANK = "system_toggle_badge_blank"

        @StringRes
        fun nameRes(toggle: SystemToggle): Int = when (toggle) {
            SystemToggle.BATTERY_SAVER -> R.string.system_toggle_battery_saver
            SystemToggle.AIRPLANE_MODE -> R.string.system_toggle_airplane_mode
            SystemToggle.DO_NOT_DISTURB -> R.string.system_toggle_do_not_disturb
            SystemToggle.RINGER -> R.string.system_toggle_ringer
            SystemToggle.BLUETOOTH -> R.string.system_toggle_bluetooth
            SystemToggle.WIFI -> R.string.system_toggle_wifi
            SystemToggle.MOBILE_DATA -> R.string.system_toggle_mobile_data
            SystemToggle.HOTSPOT -> R.string.system_toggle_hotspot
            SystemToggle.NFC -> R.string.system_toggle_nfc
            SystemToggle.LOCATION -> R.string.system_toggle_location
            SystemToggle.AUTO_BRIGHTNESS -> R.string.system_toggle_auto_brightness
        }

        /** Icon for the settings list. */
        @DrawableRes
        fun listIconRes(toggle: SystemToggle): Int {
            val value = if (toggle == SystemToggle.RINGER) ToggleValue.RING else ToggleValue.ON
            return iconRes(SystemToggleEvent(toggle, value))
        }

        @StringRes
        fun textRes(event: SystemToggleEvent): Int {
            val on = event.value == ToggleValue.ON
            return when (event.toggle) {
                SystemToggle.BATTERY_SAVER ->
                    if (on) R.string.system_toggle_battery_saver_on else R.string.system_toggle_battery_saver_off
                SystemToggle.AIRPLANE_MODE ->
                    if (on) R.string.system_toggle_airplane_mode_on else R.string.system_toggle_airplane_mode_off
                SystemToggle.DO_NOT_DISTURB ->
                    if (on) R.string.system_toggle_do_not_disturb_on else R.string.system_toggle_do_not_disturb_off
                SystemToggle.RINGER -> when (event.value) {
                    ToggleValue.SILENT -> R.string.system_toggle_ringer_silent
                    ToggleValue.VIBRATE -> R.string.system_toggle_ringer_vibrate
                    else -> R.string.system_toggle_ringer_ring
                }
                SystemToggle.BLUETOOTH ->
                    if (on) R.string.system_toggle_bluetooth_on else R.string.system_toggle_bluetooth_off
                SystemToggle.WIFI ->
                    if (on) R.string.system_toggle_wifi_on else R.string.system_toggle_wifi_off
                SystemToggle.MOBILE_DATA ->
                    if (on) R.string.system_toggle_mobile_data_on else R.string.system_toggle_mobile_data_off
                SystemToggle.HOTSPOT ->
                    if (on) R.string.system_toggle_hotspot_on else R.string.system_toggle_hotspot_off
                SystemToggle.NFC ->
                    if (on) R.string.system_toggle_nfc_on else R.string.system_toggle_nfc_off
                SystemToggle.LOCATION ->
                    if (on) R.string.system_toggle_location_on else R.string.system_toggle_location_off
                SystemToggle.AUTO_BRIGHTNESS ->
                    if (on) R.string.system_toggle_auto_brightness_on else R.string.system_toggle_auto_brightness_off
            }
        }

        @DrawableRes
        fun iconRes(event: SystemToggleEvent): Int = when (event.toggle) {
            SystemToggle.BATTERY_SAVER -> R.drawable.ic_toggle_battery_saver
            SystemToggle.AIRPLANE_MODE -> R.drawable.ic_toggle_airplane
            SystemToggle.DO_NOT_DISTURB -> R.drawable.ic_toggle_dnd
            SystemToggle.RINGER -> when (event.value) {
                ToggleValue.SILENT -> R.drawable.ic_toggle_ringer_silent
                ToggleValue.VIBRATE -> R.drawable.ic_toggle_ringer_vibrate
                else -> R.drawable.ic_toggle_ringer_ring
            }
            SystemToggle.BLUETOOTH -> R.drawable.ic_toggle_bluetooth
            SystemToggle.WIFI -> R.drawable.ic_toggle_wifi
            SystemToggle.MOBILE_DATA -> R.drawable.ic_toggle_mobile_data
            SystemToggle.HOTSPOT -> R.drawable.ic_toggle_hotspot
            SystemToggle.NFC -> R.drawable.ic_toggle_nfc
            SystemToggle.LOCATION -> R.drawable.ic_toggle_location
            SystemToggle.AUTO_BRIGHTNESS -> R.drawable.ic_toggle_auto_brightness
        }
    }
}
