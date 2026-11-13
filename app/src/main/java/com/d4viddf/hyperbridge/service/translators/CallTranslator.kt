package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toColorInt
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.BridgeAction
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.service.call.CallActionIconSizingPolicy
import com.d4viddf.hyperbridge.service.call.CallActionRole
import com.d4viddf.hyperbridge.service.call.CallActionSelectionPolicy
import com.d4viddf.hyperbridge.service.call.CallActionSignal
import com.d4viddf.hyperbridge.service.call.CallMicrophoneState
import com.d4viddf.hyperbridge.service.call.CallNotificationClassifier
import com.d4viddf.hyperbridge.service.call.CallSession
import com.d4viddf.hyperbridge.service.call.CallState
import com.d4viddf.hyperbridge.service.call.CallTimerPolicy
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
    private val classifier by lazy {
        CallNotificationClassifier(
            answerKeywords = answerKeywords,
            declineKeywords = hangUpKeywords,
            hangUpKeywords = hangUpKeywords,
            muteKeywords = context.resources.getStringArray(R.array.call_keywords_mute).toList(),
            unmuteKeywords = context.resources.getStringArray(R.array.call_keywords_unmute).toList(),
            speakerKeywords = speakerKeywords
        )
    }

    fun translate(
        sbn: StatusBarNotification,
        picKey: String,
        config: IslandConfig,
        theme: HyperTheme?,
        session: CallSession,
        isUpdate: Boolean,
        resolvedTitle: String? = null
    ): HyperIslandData {
        val extras = sbn.notification.extras
        val title = resolvedTitle?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: "Call"
        val now = System.currentTimeMillis()

        val isIncoming = session.state == CallState.INCOMING_RINGING

        val builder = HyperIslandNotification.Builder(context, stableBusinessId(picKey), title)
        builder.applyFloatingPresentation(config.isFloat ?: false, isUpdate)
        builder.setShowNotification(config.isShowShade ?: true)

        val hiddenKey = "hidden_pixel"
        builder.addPicture(resolveIcon(sbn, picKey, preferNativeAppBadge = true))
        builder.addPicture(getTransparentPicture(hiddenKey))

        val bridgeActions = getFilteredCallActions(sbn, picKey, isIncoming, theme)
        val actionKeys = bridgeActions.map { it.action.key }

        val rightText: String
        var timerInfo: TimerInfo? = null
        val connectedAtForTimer = CallTimerPolicy.connectedAtForTimer(session)

        rightText = when (session.state) {
            CallState.INCOMING_RINGING -> context.getString(R.string.call_incoming)
            CallState.OUTGOING_CALLING -> context.getString(R.string.call_calling)
            CallState.OUTGOING_RINGING -> context.getString(R.string.call_ringing)
            CallState.CONNECTING -> context.getString(R.string.call_connecting)
            CallState.ACTIVE -> context.getString(R.string.call_ongoing)
            CallState.ENDED -> context.getString(R.string.call_ended)
        }
        if (connectedAtForTimer != null) {
            val duration = (now - connectedAtForTimer).coerceAtLeast(0L)
            timerInfo = TimerInfo(1, connectedAtForTimer, duration, now)
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
        builder.setIslandConfig(highlightColor = highlight, expandedTimeMs = config.floatTimeout)

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
            if (connectedAtForTimer != null) {
                builder.setBigIslandCountUp(connectedAtForTimer, picKey)
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
        picKey: String,
        isIncoming: Boolean,
        theme: HyperTheme?
    ): List<BridgeAction> {
        val rawActions = sbn.notification.actions ?: return emptyList()
        val results = mutableListOf<BridgeAction>()

        // [FIX] Resolve Colors from Override -> Global
        val appOverride = theme?.apps?.get(sbn.packageName)
        val callOverride = appOverride?.callConfig

        val hangUpColor = callOverride?.declineColor ?: theme?.callConfig?.declineColor ?: "#FF3B30"
        val answerColor = callOverride?.answerColor ?: theme?.callConfig?.answerColor ?: "#34C759"
        val neutralColor = "#8E8E93"

        // Load custom icons from theme resources
        val customAnswerBitmap = getThemeBitmap(theme, "call_answer")
        val customDeclineBitmap = getThemeBitmap(theme, "call_decline")

        val actionSignals = rawActions.map { action ->
            CallActionSignal(
                title = action.title?.toString().orEmpty(),
                semanticAction = action.semanticAction,
                hasPendingIntent = action.actionIntent != null
            )
        }
        val selectedActions = CallActionSelectionPolicy.select(
            actions = actionSignals,
            isIncoming = isIncoming,
            classifier = classifier
        )

        selectedActions.forEach { selected ->
            val index = selected.index
            val action = rawActions[index]
            val role = selected.role
            val microphoneState = selected.microphoneState
            val stateKey = if (role == CallActionRole.MICROPHONE) {
                "_${microphoneState.name.lowercase()}"
            } else {
                ""
            }
            val uniqueKey = "act_${picKey.removePrefix("pic_")}_${index}$stateKey"
            val isHangUp = role == CallActionRole.DECLINE_OR_HANG_UP
            val isAnswer = role == CallActionRole.ANSWER
            val isMicrophone = role == CallActionRole.MICROPHONE

            val bgColorHex = when {
                isHangUp -> hangUpColor
                isAnswer -> answerColor
                isMicrophone && microphoneState == CallMicrophoneState.MUTED -> "#FF9500"
                else -> neutralColor
            }
            val bgColorInt = try { bgColorHex.toColorInt() } catch(e: Exception) { 0xFF8E8E93.toInt() }

            var originalBitmap: Bitmap? = null
            if (isAnswer && customAnswerBitmap != null) {
                originalBitmap = customAnswerBitmap
            } else if (isHangUp && customDeclineBitmap != null) {
                originalBitmap = customDeclineBitmap
            } else if (isMicrophone && microphoneState != CallMicrophoneState.UNKNOWN) {
                val microphoneIcon = if (microphoneState == CallMicrophoneState.MUTED) {
                    R.drawable.ic_call_microphone_muted
                } else {
                    R.drawable.ic_call_microphone_live
                }
                originalBitmap = ContextCompat.getDrawable(context, microphoneIcon)
                    ?.mutate()
                    ?.toBitmap(width = 96, height = 96)
            } else {
                val originalIcon = action.getIcon()
                if (originalIcon != null) {
                    originalBitmap = loadIconBitmap(
                        originalIcon,
                        sbn.packageName,
                        width = 96,
                        height = 96
                    )
                }
            }

            var actionIcon: Icon? = null
            var hyperPic: HyperPicture? = null

            // [FIX] Resolve shape from Call Override -> Global Call -> Global Icon
            val actionShapeId = if (theme != null) {
                when {
                    isAnswer -> callOverride?.answerShapeId ?: theme.callConfig.answerShapeId
                    isHangUp -> callOverride?.declineShapeId ?: theme.callConfig.declineShapeId
                    else -> resolveShape(theme, sbn.packageName) // Fallback for other buttons
                }
            } else "circle"

            val padding = CallActionIconSizingPolicy.classicPaddingPercent(
                resolvePadding(theme, sbn.packageName)
            )

            if (originalBitmap != null) {
                // This bitmap has a fixed 96 px canvas, so use percentage padding. The old
                // no-theme path converted 12 dp using screen density and could shrink the actual
                // phone glyph to roughly 24 px on a xxhdpi device.
                val processedBitmap = applyThemeToActionIcon(
                    originalBitmap,
                    actionShapeId,
                    padding,
                    bgColorInt
                )

                val actionPictureKey = "${uniqueKey}_icon"
                actionIcon = Icon.createWithBitmap(processedBitmap)
                hyperPic = HyperPicture(actionPictureKey, processedBitmap)
            }

            val hyperAction = io.github.d4viddf.hyperisland_kit.HyperAction(
                key = uniqueKey,
                title = action.title?.toString() ?: "",
                icon = actionIcon,
                pendingIntent = action.actionIntent,
                actionIntentType = 1,
                actionBgColor = bgColorHex,
                titleColor = "#FFFFFF"
            )

            results.add(BridgeAction(hyperAction, hyperPic))
        }
        return results
    }
}