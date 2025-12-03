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

class ProgressTranslator(context: Context) : BaseTranslator(context) {

    private val finishKeywords by lazy {
        context.resources.getStringArray(R.array.progress_finish_keywords).toList()
    }

    fun translate(sbn: StatusBarNotification, title: String, picKey: String, config: IslandConfig): HyperIslandData {
        val builder = HyperIslandNotification.Builder(context, "bridge_${sbn.packageName}", title)

        // --- CONFIGURATION ---
        val finalTimeout = config.timeout ?: 5000L
        val shouldFloat = if (finalTimeout == 0L) false else (config.isFloat ?: true)
        builder.setEnableFloat(shouldFloat)
        builder.setTimeout(finalTimeout)
        builder.setShowNotification(config.isShowShade ?: true)
        // ---------------------

        val extras = sbn.notification.extras

        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val current = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE)
        val textContent = (extras.getString(Notification.EXTRA_TEXT) ?: "")

        val percent = if (max > 0) ((current.toFloat() / max.toFloat()) * 100).toInt() else 0

        val isTextFinished = finishKeywords.any { textContent.contains(it, ignoreCase = true) }
        val isFinished = percent >= 100 || isTextFinished

        val tickKey = "${picKey}_tick"
        val hiddenKey = "hidden_pixel"
        val greenColor = "#34C759"
        val blueColor = "#007AFF"

        // Resources
        builder.addPicture(resolveIcon(sbn, picKey))
        builder.addPicture(getTransparentPicture(hiddenKey))

        if (isFinished) {
            builder.addPicture(getColoredPicture(tickKey, R.drawable.rounded_check_circle_24, greenColor))
        }

        val actions = extractBridgeActions(sbn)
        val actionKeys = actions.map { it.action.key }

        // Expanded Info
        builder.setChatInfo(
            title = title,
            content = if (isFinished) "Download Complete" else textContent,
            pictureKey = picKey,
            actionKeys = actionKeys
        )

        // *** FIX: Correct setProgressBar Signature ***
        if (!isFinished && !indeterminate) {
            builder.setProgressBar(
                progress = percent, // Must be 0-100 Int
                color = blueColor,
            )
        }

        // Big Island
        if (isFinished) {
            builder.setBigIslandInfo(
                left = ImageTextInfoLeft(1, PicInfo(1, hiddenKey), TextInfo("", "")),
                right = ImageTextInfoRight(1, PicInfo(1, tickKey), TextInfo("Finished", title))
            )
            builder.setSmallIslandIcon(tickKey)
        } else {
            builder.setBigIslandInfo(
                left = ImageTextInfoLeft(1, PicInfo(1, picKey), TextInfo("", "")),
                right = ImageTextInfoRight(1, PicInfo(1, hiddenKey), TextInfo(title, "$percent%"))
            )

            if (!indeterminate) {
                // *** FIX: Added Title String Argument ***
                builder.setBigIslandProgressCircle(
                    picKey,
                    "", // Title inside circle (Empty)
                    percent,
                    blueColor,
                    true,
                )
                builder.setSmallIslandCircularProgress(picKey, percent, blueColor, true)
            } else {
                builder.setSmallIslandIcon(picKey)
            }
        }

        actions.forEach {
            builder.addAction(it.action)
            it.actionImage?.let { pic -> builder.addPicture(pic) }
        }

        return HyperIslandData(builder.buildResourceBundle(), builder.buildJsonParam())
    }
}