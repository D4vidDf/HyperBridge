package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
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
import io.github.d4viddf.hyperisland_kit.models.TimerInfo

class CallTranslator(context: Context) : BaseTranslator(context) {

    private val TAG = "HyperBridgeCall"

    private val hangUpKeywords by lazy { context.resources.getStringArray(R.array.call_keywords_hangup).toList() }
    private val answerKeywords by lazy { context.resources.getStringArray(R.array.call_keywords_answer).toList() }
    private val speakerKeywords by lazy { context.resources.getStringArray(R.array.call_keywords_speaker).toList() }

    fun translate(sbn: StatusBarNotification, picKey: String, config: IslandConfig): HyperIslandData {
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: "Call"

        // --- Timer Data ---
        val isChronometerShown = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)
        val baseTime = sbn.notification.`when`
        val now = System.currentTimeMillis()

        // --- Action Detection ---
        val actions = sbn.notification.actions ?: emptyArray()
        val hasAnswerAction = actions.any { action ->
            val txt = action.title.toString().lowercase()
            answerKeywords.any { k -> txt.contains(k) }
        }

        // Incoming if: No timer running AND has "Answer" button
        val isIncoming = !isChronometerShown && hasAnswerAction

        val builder = HyperIslandNotification.Builder(context, "bridge_${sbn.packageName}", title)

        builder.setEnableFloat(config.isFloat ?: false)
        builder.setTimeout(config.timeout ?: 0)
        builder.setShowNotification(config.isShowShade ?: true)

        val hiddenKey = "hidden_pixel"
        builder.addPicture(resolveIcon(sbn, picKey))
        builder.addPicture(getTransparentPicture(hiddenKey))

        // --- ACTIONS ---
        val bridgeActions = getFilteredCallActions(sbn, isIncoming)
        val actionKeys = bridgeActions.map { it.action.key }

        // --- CONTENT SETUP ---
        val rightText: String
        var timerInfo: TimerInfo? = null

        if (isIncoming) {
            rightText = context.getString(R.string.call_incoming)
        } else {
            rightText = context.getString(R.string.call_ongoing)
            if (baseTime > 0) {
                val duration = if (now > baseTime) now - baseTime else 0L
                timerInfo = TimerInfo(1, baseTime, duration, now)
            }
        }

        // 1. Add Resources
        bridgeActions.forEach {
            builder.addAction(it.action)
            it.actionImage?.let { pic -> builder.addPicture(pic) }
        }

        // 2. ChatInfo
        builder.setChatInfo(
            title = title,
            content = rightText,
            pictureKey = picKey,
            actionKeys = actionKeys,
            appPkg = sbn.packageName,
            timer = timerInfo
        )

        // 3. Island Configuration
        builder.setSmallIsland(picKey)

        if (isIncoming) {
            // INCOMING: Name on Left, Status on Right
            builder.setBigIslandInfo(
                left = ImageTextInfoLeft(
                    type = 1,
                    picInfo = PicInfo(type = 1, pic = picKey),
                    // UPDATE: Show Name (Title) on the LEFT
                    textInfo = TextInfo(title = title, content = "")
                ),
                right = ImageTextInfoRight(
                    type = 2,
                    // UPDATE: Show Status ("Incoming Call") on the RIGHT
                    textInfo = TextInfo(title = rightText, content = "")
                )
            )
        } else {
            // ACTIVE: Timer on Right (using setBigIslandCountUp)
            if (baseTime > 0) {
                // This helper usually puts the picture on left and timer on right.
                // Note: Standard 'setBigIslandCountUp' might not support showing text on the left.
                // If you need Name + Timer, we might need a custom layout, but this is the Kit's timer method.
                builder.setBigIslandCountUp(baseTime, picKey)
            } else {
                // Fallback Layout
                builder.setBigIslandInfo(
                    left = ImageTextInfoLeft(
                        type = 1,
                        picInfo = PicInfo(type = 1, pic = picKey),
                        textInfo = TextInfo(title = title, content = "")
                    ),
                    right = ImageTextInfoRight(
                        type = 2,
                        textInfo = TextInfo(title = rightText, content = "")
                    )
                )
            }
        }

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
            if (hangUpIndex != -1) indicesToShow.add(hangUpIndex)
            if (answerIndex != -1) indicesToShow.add(answerIndex)
        } else {
            if (speakerIndex != -1) indicesToShow.add(speakerIndex)
            if (hangUpIndex != -1) indicesToShow.add(hangUpIndex)
        }

        if (indicesToShow.isEmpty()) {
            if (rawActions.isNotEmpty()) indicesToShow.add(0)
            if (rawActions.size > 1) indicesToShow.add(1)
        }

        indicesToShow.take(2).forEach { index ->
            val action = rawActions[index]
            val uniqueKey = "act_${sbn.key.hashCode()}_$index"
            val isHangUp = index == hangUpIndex
            val isAnswer = index == answerIndex

            val bgColorHex = when {
                isHangUp -> "#FF3B30"
                isAnswer -> "#34C759"
                else -> "#8E8E93"
            }
            val bgColorInt = bgColorHex.toColorInt()

            val originalIcon = action.getIcon()
            val originalBitmap = if (originalIcon != null) loadIconBitmap(originalIcon, sbn.packageName) else null

            var actionIcon: Icon? = null
            var hyperPic: HyperPicture? = null

            if (originalBitmap != null) {
                val roundedBitmap = createRoundedIconWithBackground(originalBitmap, bgColorInt, 12)
                val picKey = "${uniqueKey}_icon"
                actionIcon = Icon.createWithBitmap(roundedBitmap)
                hyperPic = HyperPicture(picKey, roundedBitmap)
            }

            val hyperAction = HyperAction(
                key = uniqueKey,
                title = action.title?.toString() ?: "",
                icon = actionIcon,
                pendingIntent = action.actionIntent,
                actionIntentType = 1,
                actionBgColor = null,
                titleColor = "#FFFFFF"
            )

            results.add(BridgeAction(hyperAction, hyperPic))
        }
        return results
    }

    private fun tintBitmap(source: Bitmap, color: Int): Bitmap {
        val result = createBitmap(source.width, source.height)
        val canvas = Canvas(result)
        val paint = Paint()
        paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}