package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

class StandardTranslator(
    context: Context,
    repo: ThemeRepository
) : BaseTranslator(context, repo) {

    fun translate(
        sbn: StatusBarNotification,
        title: String,
        text: String,
        picKey: String,
        config: IslandConfig,
        theme: HyperTheme?
    ): HyperIslandData {
        val extras = sbn.notification.extras
        val template = extras.getString(Notification.EXTRA_TEMPLATE) ?: ""
        val subText = extras.getString(Notification.EXTRA_SUB_TEXT) ?: ""

        val isMedia = template.contains("MediaStyle")
        val isCall = sbn.notification.category == Notification.CATEGORY_CALL

        val displayTitle = title
        val displayContent = when {
            isMedia -> context.getString(R.string.status_now_playing)
            isCall && subText.isNotEmpty() -> "$text • $subText"
            subText.isNotEmpty() -> if (text.isNotEmpty()) "$text • $subText" else subText
            else -> text
        }

        val builder = HyperIslandNotification.Builder(context, "bridge_${sbn.packageName}", displayTitle)

        // --- CONFIGURATION ---
        builder.setEnableFloat(config.isFloat ?: false)
        builder.setIslandConfig(timeout = config.timeout)
        builder.setShowNotification(config.isShowShade ?: true)
        builder.setReopen(true)
        builder.setIslandFirstFloat(config.isFloat ?: false)

        val hiddenKey = "hidden_pixel"
        // [UPDATED] Just return bitmap, no shaping
        builder.addPicture(resolveIcon(sbn, picKey))
        builder.addPicture(getTransparentPicture(hiddenKey))

        // [FIX] Pass Theme to Extract Actions (enables shaped action icons)
        val bridgeActions = extractBridgeActions(
            sbn = sbn,
            theme = theme,
            hideReplies = false,
            useAppOpenForReplies = true
        )

        // Base Info (Shade)
        builder.setBaseInfo(
            type = 2,
            title = displayTitle,
            content = displayContent
        )
        builder.setIconTextInfo(
            picKey= picKey,
            title = displayTitle,
            content = displayContent
        )

        // Island Layout
        if (isMedia) {
            builder.setBigIslandInfo(left = ImageTextInfoLeft(1, PicInfo(1, picKey), TextInfo("", "")))
        } else {
            builder.setBigIslandInfo(
                left = ImageTextInfoLeft(1, PicInfo(1, picKey), TextInfo("", "")),
                right = ImageTextInfoRight(1, PicInfo(1, hiddenKey), TextInfo(displayTitle, displayContent))
            )
        }

        builder.setSmallIsland(picKey)

        // Add Actions
        if (bridgeActions.isNotEmpty()) {
            val hyperActions = bridgeActions.map { it.action }.toTypedArray()

            // Set actions visible in shade
            builder.setTextButtons(*hyperActions)

            // Register them internally (required for clicks to work)
            hyperActions.forEach {
                builder.addHiddenAction(it)
            }

            // Register any custom/shaped icons if we extracted them
            bridgeActions.forEach {
                it.actionImage?.let { pic -> builder.addPicture(pic) }
            }
        }

        // [FIX] Apply Theme Highlight Color (with App Override logic via helper)
        val highlight = resolveColor(theme, sbn.packageName, "#FFFFFF")
        builder.setIslandConfig(highlightColor = highlight)

        return HyperIslandData(builder.buildResourceBundle(), builder.buildJsonParam())
    }
}