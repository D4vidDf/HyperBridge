package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import androidx.core.graphics.toColorInt
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.BridgeAction
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo
import io.github.d4viddf.hyperisland_kit.models.TimerInfo

class CallTranslator(
    context: Context,
    repo: ThemeRepository
) : BaseTranslator(context, repo) {

    private val hangUpKeywords by lazy { context.resources.getStringArray(R.array.call_keywords_hangup).toList() }
    private val answerKeywords by lazy { context.resources.getStringArray(R.array.call_keywords_answer).toList() }
    private val speakerKeywords by lazy { context.resources.getStringArray(R.array.call_keywords_speaker).toList() }

    fun translate(
        sbn: StatusBarNotification,
        picKey: String,
        config: IslandConfig,
        theme: HyperTheme?
    ): HyperIslandData {
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: "Call"

        val isChronometerShown = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)
        val baseTime = sbn.notification.`when`
        val now = System.currentTimeMillis()

        val actions = sbn.notification.actions ?: emptyArray()
        val hasAnswerAction = actions.any { action ->
            val txt = action.title.toString().lowercase()
            answerKeywords.any { k -> txt.contains(k) }
        }

        val isIncoming = !isChronometerShown && hasAnswerAction

        val builder = HyperIslandNotification.Builder(context, "bridge_${sbn.packageName}", title)
        builder.setEnableFloat(config.isFloat ?: false)
        builder.setShowNotification(config.isShowShade ?: true)
        builder.setIslandFirstFloat(config.isFloat ?: false)

        val hiddenKey = "hidden_pixel"
        builder.addPicture(resolveIcon(sbn, picKey))
        builder.addPicture(getTransparentPicture(hiddenKey))

        val bridgeActions = getFilteredCallActions(sbn, isIncoming, theme)
        val actionKeys = bridgeActions.map { it.action.key }

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

        bridgeActions.forEach {
            builder.addAction(it.action)
            it.actionImage?.let { pic -> builder.addPicture(pic) }
        }

        builder.setChatInfo(
            title = title,
            content = rightText,
            pictureKey = picKey,
            actionKeys = actionKeys,
            appPkg = sbn.packageName,
            timer = timerInfo
        )

        builder.setSmallIsland(picKey)

        val highlight = resolveColor(theme, sbn.packageName, "#FFFFFF")
        builder.setIslandConfig(highlightColor = highlight)

        if (isIncoming) {
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
        } else {
            if (baseTime > 0) {
                builder.setBigIslandCountUp(baseTime, picKey)
            } else {
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

    private fun getFilteredCallActions(
        sbn: StatusBarNotification,
        isIncoming: Boolean,
        theme: HyperTheme?
    ): List<BridgeAction> {
        val rawActions = sbn.notification.actions ?: return emptyList()
        val results = mutableListOf<BridgeAction>()

        val hangUpColor = theme?.callConfig?.declineColor ?: "#FF3B30"
        val answerColor = theme?.callConfig?.answerColor ?: "#34C759"
        val neutralColor = "#8E8E93"

        // Load custom icons from theme resources
        val customAnswerBitmap = getThemeBitmap(theme, "call_answer")
        val customDeclineBitmap = getThemeBitmap(theme, "call_decline")

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

        // Fallback detection logic if keywords failed
        if (indicesToShow.isEmpty()) {
            if (rawActions.isNotEmpty()) {
                indicesToShow.add(0)
                hangUpIndex = 0
            }
            if (rawActions.size > 1) {
                indicesToShow.add(1)
                answerIndex = 1
            }
        }

        indicesToShow.take(2).forEach { index ->
            val action = rawActions[index]
            val uniqueKey = "act_${sbn.key.hashCode()}_$index"
            val isHangUp = index == hangUpIndex
            val isAnswer = index == answerIndex

            // Determine Background Color
            val bgColorHex = when {
                isHangUp -> hangUpColor
                isAnswer -> answerColor
                else -> neutralColor
            }
            val bgColorInt = try { bgColorHex.toColorInt() } catch(e: Exception) { 0xFF8E8E93.toInt() }

            // Determine Icon Source (Custom vs System)
            var originalBitmap: Bitmap? = null
            if (isAnswer && customAnswerBitmap != null) {
                originalBitmap = customAnswerBitmap
            } else if (isHangUp && customDeclineBitmap != null) {
                originalBitmap = customDeclineBitmap
            } else {
                val originalIcon = action.getIcon()
                if (originalIcon != null) originalBitmap = loadIconBitmap(originalIcon, sbn.packageName)
            }

            var actionIcon: Icon? = null
            var hyperPic: HyperPicture? = null

            // [NEW] Determine Shape
            val actionShapeId = if (theme != null) {
                when {
                    isAnswer -> theme.callConfig.answerShapeId
                    isHangUp -> theme.callConfig.declineShapeId
                    else -> theme.global.iconShapeId
                }
            } else "circle"

            val padding = theme?.global?.iconPaddingPercent ?: 15

            if (originalBitmap != null) {
                // Apply Shape & Padding to ACTION ICON
                val processedBitmap = if (theme != null) {
                    // [FIX] Use specific shape for this action
                    applyThemeToActionIcon(originalBitmap, actionShapeId, padding, bgColorInt)
                } else {
                    createRoundedIconWithBackground(originalBitmap, bgColorInt, 12)
                }

                val picKey = "${uniqueKey}_icon"
                actionIcon = Icon.createWithBitmap(processedBitmap)
                hyperPic = HyperPicture(picKey, processedBitmap)
            }

            // Create Action with Color String
            val hyperAction = io.github.d4viddf.hyperisland_kit.HyperAction(
                key = uniqueKey,
                title = action.title?.toString() ?: "",
                icon = actionIcon,
                pendingIntent = action.actionIntent,
                actionIntentType = 1,
                actionBgColor = bgColorHex, // [FIX] Hex String
                titleColor = "#FFFFFF"
            )

            results.add(BridgeAction(hyperAction, hyperPic))
        }
        return results
    }
}