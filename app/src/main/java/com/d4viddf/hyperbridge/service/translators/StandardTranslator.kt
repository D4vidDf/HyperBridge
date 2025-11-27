package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

class StandardTranslator(context: Context) : BaseTranslator(context) {

    fun translate(sbn: StatusBarNotification, picKey: String, config: IslandConfig): HyperIslandData {
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: sbn.packageName
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
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
        val finalTimeout = config.timeout ?: 5000L
        // If timeout is 0, we force float to false to prevent stuck heads-up
        val shouldFloat = if (finalTimeout == 0L) false else (config.isFloat ?: true)

        builder.setEnableFloat(shouldFloat)
        builder.setTimeout(finalTimeout)
        builder.setShowNotification(config.isShowShade ?: true)
        // ---------------------

        val hiddenKey = "hidden_pixel"
        builder.addPicture(resolveIcon(sbn, picKey))
        builder.addPicture(getTransparentPicture(hiddenKey))

        val actions = extractBridgeActions(sbn)
        val actionKeys = actions.map { it.action.key }

        // Action Logic: Move to Hint if > 1 (Optional, keeping standard behavior for now)
        builder.setBaseInfo(
            title = displayTitle,
            content = displayContent,
            pictureKey = picKey,
            actionKeys = actionKeys
        )

        if (isMedia) {
            builder.setBigIslandInfo(left = ImageTextInfoLeft(1, PicInfo(1, picKey), TextInfo("", "")))
        } else {
            builder.setBigIslandInfo(
                left = ImageTextInfoLeft(1, PicInfo(1, picKey), TextInfo("", "")),
                right = ImageTextInfoRight(1, PicInfo(1, hiddenKey), TextInfo(displayTitle, displayContent))
            )
        }

        builder.setSmallIslandIcon(picKey)

        actions.forEach {
            builder.addAction(it.action)
            it.actionImage?.let { iconPic -> builder.addPicture(iconPic) }
        }

        return HyperIslandData(builder.buildResourceBundle(), builder.buildJsonParam())
    }
}