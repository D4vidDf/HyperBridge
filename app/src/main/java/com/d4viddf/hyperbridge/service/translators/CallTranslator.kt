package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.BridgeAction
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

class CallTranslator(context: Context) : BaseTranslator(context) {

    // Keywords (Loaded from XML)
    private val hangUpKeywords by lazy {
        context.resources.getStringArray(R.array.call_keywords_hangup).toList()
    }
    private val answerKeywords by lazy {
        context.resources.getStringArray(R.array.call_keywords_answer).toList()
    }
    private val speakerKeywords by lazy {
        context.resources.getStringArray(R.array.call_keywords_speaker).toList()
    }

    fun translate(sbn: StatusBarNotification, picKey: String, config: IslandConfig): HyperIslandData {
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: "Call"

        val isChronometerShown = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)

        val actions = sbn.notification.actions ?: emptyArray()
        val hasAnswerAction = actions.any { action ->
            val txt = action.title.toString().lowercase()
            answerKeywords.any { k -> txt.contains(k) }
        }

        val isIncoming = !isChronometerShown && hasAnswerAction

        val builder = HyperIslandNotification.Builder(context, "bridge_${sbn.packageName}", title)

        val finalTimeout = config.timeout ?: 5000L
        // Keep calls persistent/floating if active
        val shouldFloat = if (finalTimeout == 0L) false else (config.isFloat ?: true)
        builder.setEnableFloat(shouldFloat)
        builder.setTimeout(finalTimeout)
        builder.setShowNotification(config.isShowShade ?: true)

        val hiddenKey = "hidden_pixel"
        builder.addPicture(resolveIcon(sbn, picKey))
        builder.addPicture(getTransparentPicture(hiddenKey))

        // --- ACTIONS ---
        val bridgeActions = getFilteredCallActions(sbn, isIncoming)

        bridgeActions.forEach {
            builder.addAction(it.action)
            it.actionImage?.let { pic -> builder.addPicture(pic) }
        }
        val actionKeys = bridgeActions.map { it.action.key }

        // --- TEXT ---
        val rightText: String

        if (isIncoming) {
            rightText = context.getString(R.string.call_incoming)
        } else {
            val subText = extras.getString(Notification.EXTRA_TEXT)
            rightText = if (!subText.isNullOrEmpty() && subText.contains(":")) subText else context.getString(R.string.call_ongoing)
        }

        builder.setBaseInfo(
            title = title,
            content = rightText,
            pictureKey = picKey,
            actionKeys = actionKeys
        )

        builder.setBigIslandInfo(
            left = ImageTextInfoLeft(1, PicInfo(1, picKey), TextInfo(title, "")),
            right = ImageTextInfoRight(2, PicInfo(1, hiddenKey), TextInfo(rightText, ""))
        )

        builder.setSmallIslandIcon(picKey)

        return HyperIslandData(builder.buildResourceBundle(), builder.buildJsonParam())
    }

    private fun getFilteredCallActions(sbn: StatusBarNotification, isIncoming: Boolean): List<BridgeAction> {
        val rawActions = sbn.notification.actions ?: return emptyList()
        val results = mutableListOf<BridgeAction>()

        var answerIndex = -1
        var hangUpIndex = -1
        var speakerIndex = -1

        rawActions.forEachIndexed { index, action ->
            val txt = action.title.toString().lowercase()
            if (answerKeywords.any { txt.contains(it) }) answerIndex = index
            else if (hangUpKeywords.any { txt.contains(it) }) hangUpIndex = index
            else if (speakerKeywords.any { txt.contains(it) }) speakerIndex = index
        }

        val indicesToShow = mutableListOf<Int>()

        if (isIncoming) {
            // 1. Decline (Red), 2. Answer (Green)
            if (hangUpIndex != -1) indicesToShow.add(hangUpIndex)
            if (answerIndex != -1) indicesToShow.add(answerIndex)
        } else {
            // 1. Speaker (Grey), 2. Hang Up (Red)
            if (speakerIndex != -1) indicesToShow.add(speakerIndex)
            if (hangUpIndex != -1) indicesToShow.add(hangUpIndex)
        }

        // Fallback
        if (indicesToShow.isEmpty()) {
            if (rawActions.isNotEmpty()) indicesToShow.add(0)
            if (rawActions.size > 1) indicesToShow.add(1)
        }

        indicesToShow.take(2).forEach { index ->
            val action = rawActions[index]
            val title = action.title?.toString() ?: ""
            val uniqueKey = "act_${sbn.key.hashCode()}_$index"

            var hyperPic: HyperPicture? = null
            var actionIcon: Icon? = null

            val isHangUp = index == hangUpIndex
            val isAnswer = index == answerIndex

            // DETERMINE BACKGROUND COLOR
            val bgColor = when {
                isHangUp -> "#FF3B30" // Red
                isAnswer -> "#34C759" // Green
                else -> null
            }

            // LOAD & TINT ICON
            val originalIcon = action.getIcon()
            val originalBitmap = if (originalIcon != null) loadIconBitmap(originalIcon, sbn.packageName) else null

            if (originalBitmap != null) {
                // If we have a background color, make the icon WHITE for contrast.
                // Otherwise, leave it original (usually grey/black).
                val finalBitmap = if (bgColor != null) {
                    tintBitmap(originalBitmap, Color.WHITE)
                } else {
                    originalBitmap
                }

                val picKey = "${uniqueKey}_icon"
                actionIcon = Icon.createWithBitmap(finalBitmap)
                hyperPic = HyperPicture(picKey, finalBitmap)
            }

            val hyperAction = HyperAction(
                key = uniqueKey,
                title = null, // Title usually hidden if icon is prominent in island style
                icon = actionIcon,
                pendingIntent = action.actionIntent,
                actionIntentType = 1,
                false,
                0,
                null,
                bgColor
            )

            results.add(BridgeAction(hyperAction, hyperPic))
        }

        return results
    }

    private fun tintBitmap(source: Bitmap, color: Int): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}