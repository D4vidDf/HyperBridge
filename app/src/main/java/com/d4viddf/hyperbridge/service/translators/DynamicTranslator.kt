package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.graphics.BitmapFactory
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.graphics.toColorInt
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.SmartAction
import com.d4viddf.hyperbridge.models.SmartActionType
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.translator.ActionMatchBy
import com.d4viddf.hyperbridge.models.translator.ActionSlotConfig
import com.d4viddf.hyperbridge.models.translator.ActionSource
import com.d4viddf.hyperbridge.models.translator.CustomActionSource
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.CustomVariableDefinition
import com.d4viddf.hyperbridge.models.translator.PresentationMode
import com.d4viddf.hyperbridge.models.translator.ProgressSlotType
import com.d4viddf.hyperbridge.models.translator.SmartActionCategory
import com.d4viddf.hyperbridge.models.translator.VariableSource
import com.d4viddf.hyperbridge.models.translator.VariableType
import com.d4viddf.hyperbridge.service.smartactions.SmartActionIntents
import com.google.gson.JsonParser
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo
import java.io.File

class DynamicTranslator(
    context: Context,
    repository: ThemeRepository? = null
) : BaseTranslator(context, repository) {
    private val TAG = "HyperBridgeDebug"

    /**
     * Translates a StatusBarNotification using a declarative CustomTranslator rule.
     */
    fun translate(
        sbn: StatusBarNotification,
        customTranslator: CustomTranslator,
        picKey: String,
        config: IslandConfig,
        activeTheme: HyperTheme? = null,
        isUpdate: Boolean = false,
        resolvedTitle: String? = null,
        resolvedText: String? = null,
        extractedSenderName: String? = null,
        extractedMediaArtist: String? = null,
        extractedConversationTitle: String? = null,
        callSession: com.d4viddf.hyperbridge.service.call.CallSession? = null
    ): HyperIslandData {
        val notif = sbn.notification
        val extras = notif.extras

        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val rawSubtext = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val appLabel = try {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (_: Exception) {
            sbn.packageName
        }

        val notifTitle = if (!resolvedTitle.isNullOrBlank()) {
            resolvedTitle
        } else if (rawTitle.isNotBlank()) {
            rawTitle
        } else {
            appLabel
        }
        val notifText = if (!resolvedText.isNullOrBlank()) resolvedText else rawText

        val isCall = callSession != null ||
                notif.category == Notification.CATEGORY_CALL ||
                customTranslator.presentation.templateId == "tpl_call_kit"

        val callStateText = when (callSession?.state) {
            com.d4viddf.hyperbridge.service.call.CallState.INCOMING_RINGING -> context.getString(R.string.call_incoming)
            com.d4viddf.hyperbridge.service.call.CallState.OUTGOING_CALLING -> context.getString(R.string.call_calling)
            com.d4viddf.hyperbridge.service.call.CallState.OUTGOING_RINGING -> context.getString(R.string.call_ringing)
            com.d4viddf.hyperbridge.service.call.CallState.CONNECTING -> context.getString(R.string.call_connecting)
            com.d4viddf.hyperbridge.service.call.CallState.ACTIVE -> context.getString(R.string.call_ongoing)
            com.d4viddf.hyperbridge.service.call.CallState.ENDED -> context.getString(R.string.call_ended)
            null -> if (isCall) notifText.ifBlank { context.getString(R.string.call_incoming) } else ""
        }

        // 1. Resolve Effective Theme
        val effectiveTheme = resolveTheme(customTranslator, activeTheme)

        // 2. Extract Progress & Numeric Data
        val (progressValue, maxProgressValue) = extractProgress(customTranslator, notif, notifTitle, notifText)

        // 3. Resolve Dynamic Text Templates & Notification Catalog
        val variableMap = buildVariableMap(
            sbn = sbn,
            picKey = picKey,
            title = notifTitle,
            text = notifText,
            subtext = rawSubtext,
            sender = extractedSenderName,
            mediaArtist = extractedMediaArtist,
            conversationTitle = extractedConversationTitle,
            progress = progressValue?.toString(),
            callerName = extractedSenderName ?: notifTitle,
            callState = callStateText,
            theme = effectiveTheme
        )

        // 4. Build HyperIsland Base Config
        val isFloat = config.isFloat ?: customTranslator.behaviorOverride.isFloat ?: false
        val isShade = config.isShowShade ?: customTranslator.behaviorOverride.isShowShade ?: false
        val defaultHighlight = if (customTranslator.presentation.progressSlot.type == ProgressSlotType.TIMER) "#FF9500" else "#007AFF"
        val highlightColor = if (customTranslator.themeBinding.overrideHighlightColor != null) {
            customTranslator.themeBinding.overrideHighlightColor
        } else {
            resolveColor(effectiveTheme, sbn.packageName, defaultHighlight)
        }

        val interpolatedTitle = interpolate(customTranslator.presentation.textSlot.titleTemplate, variableMap).trim()
        val finalTitle = if (interpolatedTitle.isNotBlank()) interpolatedTitle else notifTitle.ifBlank { appLabel.ifBlank { "Notification" } }
        val finalText = interpolate(customTranslator.presentation.textSlot.subtitleTemplate, variableMap).trim()

        val builder = HyperIslandNotification.Builder(
            context,
            stableBusinessId(picKey),
            finalTitle
        )

        builder.applyFloatingPresentation(isFloat, isUpdate)
        builder.setIslandConfig(
            timeout = customTranslator.behaviorOverride.timeoutSeconds ?: config.timeout,
            dismissible = false, // Set to false for HyperOS 4 compatibility (prevents island from failing to show or being discarded)
            highlightColor = highlightColor,
            expandedTimeMs = customTranslator.behaviorOverride.floatTimeoutSeconds ?: config.floatTimeout
        )
        builder.setShowNotification(isShade)
        if (!isUpdate) builder.setReopen(true)

        val hiddenKey = "hidden_pixel"
        builder.addPicture(getTransparentPicture(hiddenKey))
        builder.addPicture(resolveIcon(sbn, picKey))

        val themeShape = resolveShape(effectiveTheme, sbn.packageName)
        val themePadding = resolvePadding(effectiveTheme, sbn.packageName)
        val defaultActionBg = try {
            resolveColor(effectiveTheme, sbn.packageName, "#007AFF").toColorInt()
        } catch (_: Exception) {
            "#007AFF".toColorInt()
        }

        // 1. Resolve pictures from Notification
        val notifLargeIcon = sbn.notification.getLargeIcon()
        val notifPictureBmp = try {
            extras.getParcelable<android.graphics.Bitmap>(Notification.EXTRA_PICTURE)
                ?: (extras.getParcelable<android.graphics.drawable.Icon>("android.pictureIcon")?.let { loadIconBitmap(it, sbn.packageName) })
                ?: (extras.getParcelable<android.graphics.drawable.Icon>(Notification.EXTRA_PICTURE_ICON)?.let { loadIconBitmap(it, sbn.packageName) })
        } catch (_: Exception) {
            null
        }

        val largeBmp = if (notifLargeIcon != null) {
            loadIconBitmap(notifLargeIcon, sbn.packageName)
        } else {
            try {
                extras.getParcelable<android.graphics.Bitmap>(Notification.EXTRA_LARGE_ICON)
                    ?: notifPictureBmp
            } catch (_: Exception) {
                null
            }
        } ?: notifPictureBmp ?: getNotificationBitmap(sbn)

        val posterBmp = notifPictureBmp ?: largeBmp

        if (largeBmp != null) {
            builder.addPicture(HyperPicture("miui.focus.pic_large_icon", largeBmp))
            builder.addPicture(HyperPicture("miui.focus.pic_sender_avatar", largeBmp))
            builder.addPicture(HyperPicture("album_art", largeBmp))
            builder.addPicture(HyperPicture("miui.focus.pic_imageText", largeBmp))
        }
        if (posterBmp != null) {
            builder.addPicture(HyperPicture("poster", posterBmp))
            builder.addPicture(HyperPicture("miui.focus.pic_poster", posterBmp))
            builder.addPicture(HyperPicture("miui.focus.pic_imageText", posterBmp))
        }
        val appIconPic = getAppIconBitmap(sbn.packageName)
        if (appIconPic != null) {
            builder.addPicture(HyperPicture("miui.focus.pic_app_icon", appIconPic))
        }
        builder.addPicture(resolveIcon(sbn, "default_icon"))

        // 5. Extract & Bundle Custom Dynamic Variables & Images
        val availablePictureKeys = mutableSetOf(
            picKey,
            "picKey",
            hiddenKey,
            "miui.focus.pic_app_icon",
            "miui.focus.pic_large_icon",
            "miui.focus.pic_sender_avatar",
            "miui.focus.pic_imageText",
            "miui.focus.pic_poster",
            "album_art",
            "poster",
            "default_icon"
        )
        extractCustomVariables(
            customTranslator = customTranslator,
            sbn = sbn,
            extras = extras,
            variableMap = variableMap,
            builder = builder,
            effectiveTheme = effectiveTheme,
            themeShape = themeShape,
            themePadding = themePadding,
            availablePictures = availablePictureKeys
        )

        // 6. Extract & Bundle Custom Actions
        extractCustomActions(
            customTranslator = customTranslator,
            sbn = sbn,
            extras = extras,
            rawText = rawText,
            variableMap = variableMap,
            builder = builder,
            effectiveTheme = effectiveTheme,
            defaultActionBg = defaultActionBg,
            themeShape = themeShape,
            themePadding = themePadding
        )

        // 7. Check for RAW_PARAM_V2 Mode
        if (customTranslator.presentation.mode == PresentationMode.RAW_PARAM_V2) {
            val rawConfig = customTranslator.presentation.rawParamV2
            val template = rawConfig?.jsonTemplate ?: ""
            if (template.isNotBlank()) {
                val interpolatedJson = interpolate(template, variableMap, availablePictureKeys)
                val validatedJson = validateOrNormalizeParamV2(interpolatedJson)
                val bundle = builder.buildResourceBundle()
                val picsBundle = bundle.getBundle("miui.focus.pics")
                if (picsBundle != null) {
                    val currentKeys = picsBundle.keySet().toList()
                    for (k in currentKeys) {
                        if (k.startsWith("miui.focus.pic_")) {
                            val rawKey = k.removePrefix("miui.focus.pic_")
                            val icon = picsBundle.getParcelable<android.graphics.drawable.Icon>(k)
                            if (icon != null && !picsBundle.containsKey(rawKey)) {
                                picsBundle.putParcelable(rawKey, icon)
                            }
                        } else {
                            val prefixed = "miui.focus.pic_$k"
                            val icon = picsBundle.getParcelable<android.graphics.drawable.Icon>(k)
                            if (icon != null && !picsBundle.containsKey(prefixed)) {
                                picsBundle.putParcelable(prefixed, icon)
                            }
                        }
                    }
                }

                val actionsBundle = bundle.getBundle("miui.focus.actions")
                if (actionsBundle != null) {
                    val currentActionKeys = actionsBundle.keySet().toList()
                    for (k in currentActionKeys) {
                        if (k.startsWith("miui.focus.action_")) {
                            val rawKey = k.removePrefix("miui.focus.action_")
                            val parcelableObj = actionsBundle.getParcelable<android.os.Parcelable>(k)
                            if (parcelableObj != null && !actionsBundle.containsKey(rawKey)) {
                                actionsBundle.putParcelable(rawKey, parcelableObj)
                            }
                        } else {
                            val prefixed = "miui.focus.action_$k"
                            val parcelableObj = actionsBundle.getParcelable<android.os.Parcelable>(k)
                            if (parcelableObj != null && !actionsBundle.containsKey(prefixed)) {
                                actionsBundle.putParcelable(prefixed, parcelableObj)
                            }
                        }
                    }
                }
                val picKeys = picsBundle?.keySet()?.joinToString(", ") ?: "none"
                val actionKeys = actionsBundle?.keySet()?.joinToString(", ") ?: "none"

                Log.i(TAG, " [DynamicTranslator RAW_PARAM_V2] Translator: '${customTranslator.meta.name}' (pkg=${sbn.packageName})")
                Log.i(TAG, " [DynamicTranslator RAW_PARAM_V2] Available Pics in Bundle: [$picKeys]")
                Log.i(TAG, " [DynamicTranslator RAW_PARAM_V2] Available Actions in Bundle: [$actionKeys]")

                if (validatedJson != null) {
                    Log.i(TAG, " [DynamicTranslator RAW_PARAM_V2] Output JSON:\n$validatedJson")
                    return HyperIslandData(bundle, validatedJson)
                } else if (rawConfig?.fallbackToStandardOnError == false) {
                    Log.w(TAG, " [DynamicTranslator RAW_PARAM_V2] Invalid JSON but fallback disabled, Output JSON:\n$interpolatedJson")
                    return HyperIslandData(bundle, interpolatedJson)
                }
                Log.w(TAG, " [DynamicTranslator RAW_PARAM_V2] Validation failed for JSON:\n$interpolatedJson\nFalling back to standard builder")
            }
        }

        val isMedia = notif.extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                notif.extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true ||
                customTranslator.presentation.templateId == "tpl_media_compact"

        // 5. Left Slot Graphic & Hidden Pixel
        val leftSlotConfig = customTranslator.presentation.leftSlot
        val isAvatarSource = leftSlotConfig.source.equals("AVATAR", ignoreCase = true) ||
                leftSlotConfig.source.equals("SENDER_AVATAR", ignoreCase = true) ||
                leftSlotConfig.source.equals("LARGE_ICON", ignoreCase = true)

        val leftPic = when {
            leftSlotConfig.source == "HIDDEN" -> getTransparentPicture(picKey)
            leftSlotConfig.customIconRes != null && repository != null -> {
                val customBmp = repository.getResourceBitmap(leftSlotConfig.customIconRes)
                if (customBmp != null) {
                    val shapeId = leftSlotConfig.shape ?: resolveShape(effectiveTheme, sbn.packageName)
                    val pad = resolvePadding(effectiveTheme, sbn.packageName)
                    val themedBmp = applyThemeToActionIcon(customBmp, shapeId, pad, android.graphics.Color.TRANSPARENT)
                    HyperPicture(picKey, themedBmp)
                } else {
                    resolveIcon(sbn, picKey)
                }
            }
            isAvatarSource -> {
                val largeIcon = notif.getLargeIcon()
                val avatarBmp: android.graphics.Bitmap? = if (largeIcon != null) {
                    loadIconBitmap(largeIcon, sbn.packageName)
                } else {
                    try {
                        extras.getParcelable<android.graphics.Bitmap>(Notification.EXTRA_LARGE_ICON)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (avatarBmp != null) {
                    val shapeId = leftSlotConfig.shape ?: resolveShape(effectiveTheme, sbn.packageName)
                    val themedBmp = applyThemeToActionIcon(avatarBmp, shapeId, 0, android.graphics.Color.TRANSPARENT)
                    HyperPicture(picKey, themedBmp)
                } else {
                    resolveIcon(sbn, picKey)
                }
            }
            isMedia -> {
                val largeIcon = notif.getLargeIcon()
                val albumArt: android.graphics.Bitmap? = if (largeIcon != null) {
                    loadIconBitmap(largeIcon, sbn.packageName)
                } else {
                    try {
                        extras.getParcelable<android.graphics.Bitmap>(Notification.EXTRA_LARGE_ICON)
                    } catch (_: Exception) {
                        null
                    }
                }
                if (albumArt != null) {
                    val shapeId = leftSlotConfig.shape ?: resolveShape(effectiveTheme, sbn.packageName)
                    val themedBmp = applyThemeToActionIcon(albumArt, shapeId, 0, android.graphics.Color.TRANSPARENT)
                    HyperPicture(picKey, themedBmp)
                } else {
                    resolveIcon(sbn, picKey)
                }
            }
            else -> resolveIcon(sbn, picKey)
        }

        builder.addPicture(leftPic)
        builder.addPicture(getTransparentPicture(hiddenKey))

        // 6. Template Selection & Layout Building
        val progressConfig = customTranslator.presentation.progressSlot
        val pillConfig = customTranslator.presentation.pill
        val leftDesign = pillConfig.leftDesign
        val rightDesign = pillConfig.rightDesign

        val isTimer = (progressConfig.type == ProgressSlotType.TIMER || rightDesign == com.d4viddf.hyperbridge.models.translator.PillRightDesign.TIMER) &&
                notif.`when` > System.currentTimeMillis()
        val hasProgress = (progressConfig.type == ProgressSlotType.PROGRESS_BAR || rightDesign == com.d4viddf.hyperbridge.models.translator.PillRightDesign.PROGRESS_PERCENT) && progressValue != null

        val progressPercent = if (hasProgress && progressValue != null) {
            if (maxProgressValue > 0) {
                ((progressValue.toFloat() / maxProgressValue.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else {
                progressValue.coerceIn(0, 100)
            }
        } else 0

        val progressColor = if (effectiveTheme?.defaultProgress?.activeColor != null) {
            effectiveTheme.defaultProgress.activeColor
        } else {
            highlightColor
        }

        val primaryPicKey = if (leftSlotConfig.source == "HIDDEN") hiddenKey else picKey
        val pillLeftPicKey = when (leftDesign) {
            com.d4viddf.hyperbridge.models.translator.PillLeftDesign.HIDDEN -> hiddenKey
            else -> primaryPicKey
        }

        val pillLeftText = when (leftDesign) {
            com.d4viddf.hyperbridge.models.translator.PillLeftDesign.ICON_AND_TEXT,
            com.d4viddf.hyperbridge.models.translator.PillLeftDesign.TEXT_ONLY -> finalTitle
            else -> ""
        }

        val pillRightText = when (rightDesign) {
            com.d4viddf.hyperbridge.models.translator.PillRightDesign.HIGHLIGHT_TEXT -> {
                val highlightTpl = customTranslator.presentation.textSlot.highlightTextTemplate
                if (!highlightTpl.isNullOrBlank()) interpolate(highlightTpl, variableMap) else finalText
            }
            com.d4viddf.hyperbridge.models.translator.PillRightDesign.NONE -> ""
            else -> finalText
        }

        val showBigIslandPic = leftDesign != com.d4viddf.hyperbridge.models.translator.PillLeftDesign.TEXT_ONLY &&
                leftDesign != com.d4viddf.hyperbridge.models.translator.PillLeftDesign.HIDDEN &&
                leftSlotConfig.source != "HIDDEN"

        val isIncomingCall = callSession?.state == com.d4viddf.hyperbridge.service.call.CallState.INCOMING_RINGING ||
                (callSession == null && isCall && (notif.extras.getString(Notification.EXTRA_TEMPLATE)?.contains("CallStyle") == true || notif.category == Notification.CATEGORY_CALL))

        val bridgeActions = resolveActionSlots(
            sbn = sbn,
            customTranslator = customTranslator,
            rawText = rawText,
            theme = effectiveTheme,
            config = config
        )
        val actionKeys = bridgeActions.map { it.action.key }

        when {
            // =========================================================================
            //  TEMPLATE 1: PROGRESS / DOWNLOAD TEMPLATE (IM chatInfo + progressInfo + actions)
            //  Used when progress bar or circle progress is active.
            // =========================================================================
            hasProgress -> {
                builder.setChatInfo(
                    title = finalTitle,
                    content = finalText,
                    pictureKey = primaryPicKey,
                    appPkg = sbn.packageName
                )
                builder.setProgressBar(progressPercent, progressColor)
                val progressPicKey = if (showBigIslandPic) pillLeftPicKey else ""
                builder.setBigIslandProgressCircle(progressPicKey, pillLeftText, progressPercent, progressColor, true)
                builder.setSmallIslandCircularProgress(pillLeftPicKey, progressPercent, progressColor, isCCW = true)

                if (bridgeActions.isNotEmpty()) {
                    bridgeActions.forEach {
                        builder.addAction(it.action)
                        builder.addHiddenAction(it.action)
                        it.actionImage?.let { pic -> builder.addPicture(pic) }
                    }
                }
            }

            // =========================================================================
            //  TEMPLATE 2: TIMER / COUNTDOWN TEMPLATE (IM chatInfo with timerInfo + actions)
            //  Used when countdown presentation is active.
            // =========================================================================
            isTimer -> {
                val baseTime = sbn.notification.`when`.let { if (it > 0) it else System.currentTimeMillis() }
                val now = System.currentTimeMillis()
                val isCountdown = baseTime > now
                val timerType = if (isCountdown) -1 else 1

                val chatTimerInfo = io.github.d4viddf.hyperisland_kit.models.TimerInfo(
                    timerType,
                    baseTime,
                    if (isCountdown) baseTime - now else now - baseTime,
                    now
                )

                builder.setChatInfo(
                    title = finalTitle,
                    content = finalText,
                    pictureKey = primaryPicKey,
                    appPkg = sbn.packageName,
                    timer = chatTimerInfo,
                    actionKeys = if (actionKeys.isNotEmpty()) actionKeys else null
                )

                val timerPicKey = if (showBigIslandPic) pillLeftPicKey else ""
                if (isCountdown) {
                    builder.setBigIslandCountdown(baseTime, timerPicKey)
                } else {
                    builder.setBigIslandCountUp(baseTime, timerPicKey)
                }
                builder.setSmallIsland(pillLeftPicKey)

                if (bridgeActions.isNotEmpty()) {
                    bridgeActions.forEach {
                        builder.addAction(it.action)
                        builder.addHiddenAction(it.action)
                        it.actionImage?.let { pic -> builder.addPicture(pic) }
                    }
                }
            }

            // =========================================================================
            //  TEMPLATE 3: MEDIA TEMPLATE (chatInfo + actions + background + island)
            //  Used when media playback notification is active.
            // =========================================================================
            isMedia -> {
                builder.setBackground(
                    picKey = null,
                    color = highlightColor,
                    type = 1
                )
                builder.setChatInfo(
                    title = finalTitle,
                    content = finalText,
                    pictureKey = primaryPicKey,
                    appPkg = sbn.packageName,
                    actionKeys = if (actionKeys.isNotEmpty()) actionKeys.take(3) else null
                )

                val leftBigIslandText = if (rightDesign == com.d4viddf.hyperbridge.models.translator.PillRightDesign.NONE) pillLeftText else ""
                val leftPicInfo = if (showBigIslandPic) PicInfo(1, pillLeftPicKey) else null
                builder.setBigIslandInfo(
                    left = ImageTextInfoLeft(1, leftPicInfo, TextInfo(leftBigIslandText, ""))
                )
                builder.setSmallIsland(pillLeftPicKey)
                builder.setHideDeco(true).setReopen(true).setShowSmallIcon(true)

                if (bridgeActions.isNotEmpty()) {
                    bridgeActions.take(3).forEach {
                        builder.addAction(it.action)
                        builder.addHiddenAction(it.action)
                        it.actionImage?.let { pic -> builder.addPicture(pic) }
                    }
                }
            }

            // =========================================================================
            //  TEMPLATE 4: CALL TEMPLATE (chatInfo + call actions + call timer / status)
            //  Used when incoming or ongoing call notification is active.
            // =========================================================================
            isCall -> {
                val connectedAtForTimer = callSession?.let { com.d4viddf.hyperbridge.service.call.CallTimerPolicy.connectedAtForTimer(it) }
                val now = System.currentTimeMillis()
                val callTimerInfo = if (connectedAtForTimer != null) {
                    val duration = (now - connectedAtForTimer).coerceAtLeast(0L)
                    io.github.d4viddf.hyperisland_kit.models.TimerInfo(1, connectedAtForTimer, duration, now)
                } else null

                builder.setChatInfo(
                    title = finalTitle,
                    content = if (finalText.isNotBlank()) finalText else callStateText,
                    pictureKey = primaryPicKey,
                    appPkg = sbn.packageName,
                    actionKeys = if (actionKeys.isNotEmpty()) actionKeys else null,
                    timer = callTimerInfo
                )

                val leftBigIslandText = if (rightDesign == com.d4viddf.hyperbridge.models.translator.PillRightDesign.NONE) pillLeftText else finalTitle
                val leftPicInfo = if (showBigIslandPic) PicInfo(1, pillLeftPicKey) else null

                if (connectedAtForTimer != null && !isIncomingCall) {
                    val timerPicKey = if (showBigIslandPic) pillLeftPicKey else ""
                    builder.setBigIslandCountUp(connectedAtForTimer, timerPicKey)
                } else {
                    builder.setBigIslandInfo(
                        left = ImageTextInfoLeft(1, leftPicInfo, TextInfo(leftBigIslandText, "")),
                        right = ImageTextInfoRight(2, null, TextInfo(callStateText, ""))
                    )
                }

                builder.setSmallIsland(pillLeftPicKey)

                if (bridgeActions.isNotEmpty()) {
                    bridgeActions.forEach {
                        builder.addAction(it.action)
                        builder.addHiddenAction(it.action)
                        it.actionImage?.let { pic -> builder.addPicture(pic) }
                    }
                }
            }

            // =========================================================================
            //  TEMPLATE 5: STANDARD / TEXT NOTICE / CHAT TEMPLATE
            //  Used for generic text notifications, messages, alerts.
            // =========================================================================
            else -> {
                if (isAvatarSource) {
                    builder.setChatInfo(
                        title = finalTitle,
                        content = finalText,
                        pictureKey = primaryPicKey,
                        appPkg = sbn.packageName,
                        actionKeys = if (actionKeys.isNotEmpty()) actionKeys else null
                    )
                } else {
                    builder.setBaseInfo(
                        type = 2,
                        title = finalTitle,
                        content = finalText
                    )
                    builder.setIconTextInfo(
                        picKey = primaryPicKey,
                        title = finalTitle,
                        content = finalText
                    )
                }

                val leftBigIslandText = if (rightDesign == com.d4viddf.hyperbridge.models.translator.PillRightDesign.NONE) pillLeftText else ""
                val leftPicInfo = if (showBigIslandPic) PicInfo(1, pillLeftPicKey) else null
                builder.setBigIslandInfo(
                    left = ImageTextInfoLeft(1, leftPicInfo, TextInfo(leftBigIslandText, "")),
                    right = if (rightDesign != com.d4viddf.hyperbridge.models.translator.PillRightDesign.NONE) {
                        ImageTextInfoRight(1, PicInfo(1, hiddenKey), TextInfo(finalTitle, pillRightText))
                    } else {
                        ImageTextInfoRight(1, PicInfo(1, hiddenKey), TextInfo("", ""))
                    }
                )
                builder.setSmallIsland(pillLeftPicKey)

                if (bridgeActions.isNotEmpty()) {
                    if (isAvatarSource) {
                        bridgeActions.forEach {
                            builder.addAction(it.action)
                            builder.addHiddenAction(it.action)
                            it.actionImage?.let { pic -> builder.addPicture(pic) }
                        }
                    } else {
                        val textActions = bridgeActions.map { it.action }.map { original ->
                            HyperAction(
                                key = original.key,
                                title = original.title,
                                icon = original.icon,
                                pendingIntent = original.pendingIntent,
                                actionIntentType = original.actionIntentType,
                                actionBgColor = null,
                                titleColor = "#FFFFFF"
                            )
                        }.toTypedArray()
                        builder.setTextButtons(*textActions)
                        textActions.forEach {
                            builder.addHiddenAction(it)
                        }
                        bridgeActions.forEach { it.actionImage?.let { pic -> builder.addPicture(pic) } }
                    }
                }
            }
        }

        val jsonParam = builder.buildJsonParam()
        Log.d(TAG, " [DynamicTranslator] '${customTranslator.meta.name}' (${sbn.packageName}): template=${customTranslator.presentation.templateId ?: customTranslator.presentation.mode.name}, float=$isFloat, shade=$isShade")

        return HyperIslandData(builder.buildResourceBundle(), jsonParam)
    }

    private fun resolveTheme(customTranslator: CustomTranslator, activeTheme: HyperTheme?): HyperTheme? {
        val binding = customTranslator.themeBinding
        if (binding.embeddedTheme != null) {
            return binding.embeddedTheme
        }
        if (binding.themeId.equals("active", ignoreCase = true)) {
            return activeTheme
        }
        val fetchedTheme = repository?.getThemeById(binding.themeId)
        if (fetchedTheme != null) {
            return fetchedTheme
        }
        return if (binding.fallbackToActiveIfMissing) activeTheme else null
    }

    private fun extractProgress(
        customTranslator: CustomTranslator,
        notif: Notification,
        title: String,
        text: String
    ): Pair<Int?, Int> {
        val dataExt = customTranslator.dataExtraction
        if (dataExt.extractProgressFromText && !dataExt.progressRegex.isNullOrEmpty()) {
            val regex = runCatching { Regex(dataExt.progressRegex) }.getOrNull()
            if (regex != null) {
                val match = regex.find(text) ?: regex.find(title)
                if (match != null) {
                    val groupVal = runCatching { match.groups["percent"]?.value }.getOrNull() ?: match.groupValues.getOrNull(1)
                    val numeric = groupVal?.toIntOrNull()
                    if (numeric != null) {
                        return Pair(numeric, dataExt.customMaxProgress)
                    }
                }
            }
        }

        val notifMax = notif.extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val notifCurrent = notif.extras.getInt(Notification.EXTRA_PROGRESS, 0)
        if (notifMax > 0) {
            return Pair(notifCurrent, notifMax)
        }

        return Pair(null, 100)
    }

    private fun buildVariableMap(
        sbn: StatusBarNotification,
        picKey: String = "pic_${sbn.key.hashCode()}",
        title: String,
        text: String,
        subtext: String?,
        sender: String?,
        mediaArtist: String?,
        conversationTitle: String?,
        progress: String?,
        callerName: String? = null,
        callState: String? = null,
        theme: HyperTheme? = null
    ): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        val extras = sbn.notification.extras

        // Base text & notification fields
        map["notif.title"] = title
        map["notif.text"] = text
        map["notif.subtext"] = subtext ?: ""
        map["notif.info_text"] = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString() ?: ""
        map["notif.big_text"] = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        map["notif.summary_text"] = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString() ?: ""

        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        textLines?.forEachIndexed { index, line ->
            map["notif.text_lines[$index]"] = line.toString()
        }

        // Messaging fields
        map["notif.sender"] = sender ?: ""
        map["msg.sender_name"] = sender ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        map["notif.conversation_title"] = conversationTitle ?: ""
        map["msg.conversation_title"] = conversationTitle ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString() ?: ""
        map["msg.is_group"] = (extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false) || !conversationTitle.isNullOrBlank()).toString()
        map["msg.latest_message"] = text

        // Progress fields
        map["notif.progress"] = progress ?: ""
        map["progress.percent"] = progress ?: ""
        val currentProgress = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val maxProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        map["progress.current"] = if (currentProgress > 0) currentProgress.toString() else ""
        map["progress.max"] = if (maxProgress > 0) maxProgress.toString() else "100"
        map["progress.is_indeterminate"] = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false).toString()

        // Call & Media fields
        map["notif.caller_name"] = callerName ?: sender ?: title
        map["call.caller_name"] = callerName ?: sender ?: title
        map["notif.call_state"] = callState ?: ""
        map["call.state"] = callState ?: ""
        map["notif.media_artist"] = mediaArtist ?: ""
        map["media.artist"] = mediaArtist ?: ""
        map["media.album"] = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        map["media.track"] = title

        // App & System metadata
        map["app.name"] = try {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (_: Exception) {
            sbn.packageName
        }
        map["app.package"] = sbn.packageName
        map["notif.channel"] = sbn.notification.channelId ?: ""
        map["notif.category"] = sbn.notification.category ?: ""
        map["notif.when_millis"] = sbn.notification.`when`.toString()
        map["system.time_millis"] = System.currentTimeMillis().toString()

        // Colors
        val highlightHex = resolveColor(theme, sbn.packageName, "#007AFF")
        map["color.highlight"] = highlightHex
        map["color.progress"] = theme?.defaultProgress?.activeColor ?: highlightHex

        // Smart Action extractions
        val otpCode = extractOtpFromText(text) ?: extractOtpFromText(title)
        if (otpCode != null) {
            map["smart_action.OTP.code"] = otpCode
            map["smart_action.OTP"] = "miui.focus.action_smart_otp"
            map["smart_actions.OTP"] = "miui.focus.action_smart_otp"
        }
        val url = extractUrlFromText(text) ?: extractUrlFromText(title)
        if (url != null) {
            map["smart_action.URL.link"] = url
            map["smart_action.URL"] = "miui.focus.action_smart_url"
            map["smart_actions.OPEN_URL"] = "miui.focus.action_smart_url"
        }
        val phone = extractPhoneFromText(text) ?: extractPhoneFromText(title)
        if (phone != null) {
            map["smart_action.PHONE.number"] = phone
            map["smart_action.PHONE"] = "miui.focus.action_smart_phone"
            map["smart_actions.DIAL_NUMBER"] = "miui.focus.action_smart_phone"
        }
        map["action.REPLY"] = "miui.focus.action_reply"

        // Default Picture keys
        map["pic.primary"] = picKey
        map["pic.poster"] = "poster"
        map["pic.album_art"] = "album_art"
        map["pic.hidden"] = "hidden_pixel"
        map["pic.app_icon"] = "miui.focus.pic_app_icon"
        map["pic.large_icon"] = "miui.focus.pic_large_icon"
        map["pic.sender_avatar"] = "miui.focus.pic_sender_avatar"
        map["pic.default_icon"] = "default_icon"

        return map
    }

    private fun extractCustomVariables(
        customTranslator: CustomTranslator,
        sbn: StatusBarNotification,
        extras: android.os.Bundle,
        variableMap: MutableMap<String, String>,
        builder: HyperIslandNotification,
        effectiveTheme: HyperTheme?,
        themeShape: String,
        themePadding: Int,
        availablePictures: MutableSet<String>
    ) {
        val variables = customTranslator.customVariables
        if (variables.isEmpty()) return

        for (v in variables) {
            when (v.type) {
                VariableType.IMAGE -> {
                    val picKey = "pic_var_${v.id}"
                    val bmp = resolveVariableImage(v, sbn, extras, effectiveTheme)
                    if (bmp != null) {
                        val shape = v.imageConfig?.shapeId ?: themeShape
                        val padding = v.imageConfig?.paddingPercent ?: themePadding
                        val tint = v.imageConfig?.tintColor?.let { runCatching { it.toColorInt() }.getOrNull() } ?: android.graphics.Color.TRANSPARENT
                        val processed = applyThemeToActionIcon(bmp, shape, padding, tint)
                        builder.addPicture(HyperPicture(picKey, processed))
                        builder.addPicture(HyperPicture("miui.focus.pic_${v.id}", processed))
                        availablePictures.add(picKey)
                        availablePictures.add("miui.focus.pic_${v.id}")
                        variableMap["pic.${v.id}"] = picKey
                        variableMap["pic.var.${v.id}"] = picKey
                        variableMap["var.${v.id}"] = picKey
                        variableMap[v.id] = picKey
                    } else if (v.fallbackValue.isNotBlank()) {
                        variableMap["pic.${v.id}"] = v.fallbackValue
                        variableMap["pic.var.${v.id}"] = v.fallbackValue
                        variableMap["var.${v.id}"] = v.fallbackValue
                        variableMap[v.id] = v.fallbackValue
                    }
                }
                VariableType.STEP_PROGRESS -> {
                    val stepConfig = v.stepConfig
                    val textToSearch = "${extras.getCharSequence(Notification.EXTRA_TITLE)} ${extras.getCharSequence(Notification.EXTRA_TEXT)} ${extras.getCharSequence(Notification.EXTRA_BIG_TEXT)}"
                    var currentStep = 1
                    val totalSteps = stepConfig?.totalSteps ?: 4
                    var stepLabel = ""

                    if (stepConfig != null && stepConfig.stepKeywords.isNotEmpty()) {
                        for ((keyword, stepIndex) in stepConfig.stepKeywords) {
                            if (textToSearch.contains(keyword, ignoreCase = true)) {
                                currentStep = stepIndex
                                stepLabel = keyword
                                break
                            }
                        }
                    } else if (stepConfig?.stepRegex != null) {
                        val regex = runCatching { Regex(stepConfig.stepRegex) }.getOrNull()
                        val match = regex?.find(textToSearch)
                        if (match != null) {
                            currentStep = match.groupValues.getOrNull(stepConfig.stepGroup)?.toIntOrNull() ?: 1
                        }
                    }

                    variableMap["progress.step_current"] = currentStep.toString()
                    variableMap["progress.step_total"] = totalSteps.toString()
                    variableMap["progress.step_label"] = stepLabel
                    variableMap["var.${v.id}"] = currentStep.toString()
                    variableMap[v.id] = currentStep.toString()
                }
                else -> {
                    val rawVal = extractRawString(v.source, v.extraKey, sbn, extras)
                    val extracted = if (v.regexPattern != null && rawVal != null) {
                        val regex = runCatching { Regex(v.regexPattern) }.getOrNull()
                        val match = regex?.find(rawVal)
                        if (match != null) {
                            val groupVal = runCatching { match.groups[v.regexGroup]?.value }.getOrNull()
                                ?: v.regexGroup.toIntOrNull()?.let { match.groupValues.getOrNull(it) }
                                ?: match.value
                            if (v.transformTemplate != null) {
                                v.transformTemplate.replace("$1", groupVal)
                            } else groupVal
                        } else null
                    } else rawVal

                    val finalVal = if (!extracted.isNullOrBlank()) {
                        extracted
                    } else if (v.fallbackChain.isNotEmpty()) {
                        evaluateFallbackChain(v.fallbackChain, variableMap) ?: v.fallbackValue
                    } else {
                        v.fallbackValue
                    }

                    variableMap["var.${v.id}"] = finalVal
                    variableMap[v.id] = finalVal
                }
            }
        }
    }

    private fun resolveVariableImage(
        v: CustomVariableDefinition,
        sbn: StatusBarNotification,
        extras: android.os.Bundle,
        effectiveTheme: HyperTheme?
    ): android.graphics.Bitmap? {
        val notif = sbn.notification
        return when (v.source) {
            VariableSource.THEME_RESOURCE_PATH -> {
                v.imageConfig?.themeResource?.let { repository?.getResourceBitmap(it) }
            }
            VariableSource.HTRANS_EMBEDDED_ASSET -> {
                val path = v.imageConfig?.assetPath
                if (path != null) {
                    val file = File(context.filesDir, "translators_assets/$path")
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                } else null
            }
            VariableSource.PERSON_ICON -> {
                val largeIcon = notif.getLargeIcon()
                if (largeIcon != null) loadIconBitmap(largeIcon, sbn.packageName) else null
            }
            VariableSource.NOTIFICATION_LARGE_ICON -> {
                val largeIcon = notif.getLargeIcon()
                val fromLargeIcon = if (largeIcon != null) {
                    loadIconBitmap(largeIcon, sbn.packageName)
                } else {
                    try {
                        extras.getParcelable<android.graphics.Bitmap>(Notification.EXTRA_LARGE_ICON)
                            ?: extras.getParcelable<android.graphics.Bitmap>(Notification.EXTRA_PICTURE)
                    } catch (_: Exception) {
                        null
                    }
                }
                fromLargeIcon ?: getNotificationBitmap(sbn) ?: getAppIconBitmap(sbn.packageName)
            }
            VariableSource.NOTIFICATION_APP_ICON -> {
                try {
                    val appInfo = context.packageManager.getApplicationInfo(sbn.packageName, 0)
                    val drawable = context.packageManager.getApplicationIcon(appInfo)
                    loadIconBitmap(android.graphics.drawable.Icon.createWithResource(sbn.packageName, appInfo.icon), sbn.packageName)
                } catch (_: Exception) {
                    null
                }
            }
            VariableSource.NOTIFICATION_EXTRA_BITMAP -> {
                v.extraKey?.let { key ->
                    try {
                        extras.getParcelable<android.graphics.Bitmap>(key)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            else -> null
        }
    }

    private fun extractRawString(
        source: VariableSource,
        extraKey: String?,
        sbn: StatusBarNotification,
        extras: android.os.Bundle
    ): String? {
        return when (source) {
            VariableSource.NOTIFICATION_TITLE -> extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            VariableSource.NOTIFICATION_TEXT -> extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            VariableSource.NOTIFICATION_SUBTEXT -> extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            VariableSource.NOTIFICATION_INFO_TEXT -> extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
            VariableSource.NOTIFICATION_BIG_TEXT -> extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            VariableSource.NOTIFICATION_SUMMARY_TEXT -> extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
            VariableSource.NOTIFICATION_TEXT_LINES -> extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString("\n")
            VariableSource.NOTIFICATION_EXTRA -> extraKey?.let { extras.get(it)?.toString() }
            VariableSource.SENDER_NAME -> extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            VariableSource.CONVERSATION_TITLE -> extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            VariableSource.NOTIFICATION_PROGRESS -> extras.getInt(Notification.EXTRA_PROGRESS, 0).toString()
            VariableSource.STATIC_VALUE -> extraKey
            else -> null
        }
    }

    private fun extractCustomActions(
        customTranslator: CustomTranslator,
        sbn: StatusBarNotification,
        extras: android.os.Bundle,
        rawText: String,
        variableMap: MutableMap<String, String>,
        builder: HyperIslandNotification,
        effectiveTheme: HyperTheme?,
        defaultActionBg: Int,
        themeShape: String,
        themePadding: Int
    ) {
        val notifActions = sbn.notification.actions ?: emptyArray()

        // Register default notification actions
        notifActions.forEachIndexed { idx, action ->
            val key = "miui.focus.action_$idx"
            variableMap["action.$idx"] = key
            variableMap["action.$idx.title"] = action.title?.toString() ?: "Action $idx"
            val hyperAction = HyperAction(
                key = key,
                title = action.title?.toString() ?: "",
                icon = null,
                pendingIntent = action.actionIntent,
                actionIntentType = 1,
                actionBgColor = null,
                titleColor = "#FFFFFF"
            )
            builder.addAction(hyperAction)
        }

        val customActions = customTranslator.customActions
        for (act in customActions) {
            val key = "miui.focus.action_${act.id}"
            variableMap["action.${act.id}"] = key
            val label = act.label ?: "Action"
            variableMap["action.${act.id}.title"] = label

            when (act.source) {
                CustomActionSource.NOTIFICATION_ACTION_INDEX -> {
                    val notifAction = notifActions.getOrNull(act.actionIndex)
                    if (notifAction != null) {
                        val hyperAction = HyperAction(
                            key = key,
                            title = act.label ?: notifAction.title?.toString() ?: "",
                            icon = null,
                            pendingIntent = notifAction.actionIntent,
                            actionIntentType = 1,
                            actionBgColor = null,
                            titleColor = "#FFFFFF"
                        )
                        builder.addAction(hyperAction)
                    }
                }
                CustomActionSource.SMART_ACTION -> {
                    val targetSmart = when (act.smartActionType) {
                        SmartActionCategory.OTP -> extractOtpFromText(rawText)?.let { SmartAction(SmartActionType.OTP, it, it) }
                        SmartActionCategory.URL -> extractUrlFromText(rawText)?.let { SmartAction(SmartActionType.URL, it, it) }
                        SmartActionCategory.PHONE -> extractPhoneFromText(rawText)?.let { SmartAction(SmartActionType.PHONE, it, it) }
                        else -> null
                    }
                    if (targetSmart != null) {
                        val pending = SmartActionIntents.pendingIntent(context, targetSmart, key)
                        val hyperAction = HyperAction(
                            key = key,
                            title = act.label ?: SmartActionIntents.label(context, targetSmart),
                            icon = null,
                            pendingIntent = pending,
                            actionIntentType = 1,
                            actionBgColor = null,
                            titleColor = "#FFFFFF"
                        )
                        builder.addAction(hyperAction)
                    }
                }
                CustomActionSource.INLINE_REPLY -> {
                    val notifActionWithRemoteInput = notifActions.firstOrNull { it.remoteInputs?.isNotEmpty() == true }
                    val remoteInput = notifActionWithRemoteInput?.remoteInputs?.firstOrNull()
                    val targetIntent = notifActionWithRemoteInput?.actionIntent ?: sbn.notification.contentIntent
                    if (targetIntent != null) {
                        val replyIntent = android.content.Intent(context, com.d4viddf.hyperbridge.receiver.InlineReplyReceiver::class.java).apply {
                            putExtra("pending_intent", targetIntent)
                            putExtra("result_key", remoteInput?.resultKey ?: "key_text_reply")
                            putExtra("package_name", sbn.packageName)
                        }
                        val pending = android.app.PendingIntent.getBroadcast(
                            context,
                            key.hashCode(),
                            replyIntent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
                        )
                        val hyperAction = HyperAction(
                            key = key,
                            title = act.label ?: "Reply",
                            icon = null,
                            pendingIntent = pending,
                            actionIntentType = 1,
                            actionBgColor = null,
                            titleColor = "#FFFFFF"
                        )
                        builder.addAction(hyperAction)
                    }
                }
                else -> {}
            }
        }
    }

    private fun evaluateFallbackChain(
        chain: List<String>,
        variables: Map<String, String>
    ): String? {
        for (item in chain) {
            val v = variables[item]
            if (!v.isNullOrBlank()) return v
        }
        return null
    }

    companion object {
        private const val TAG = "DynamicTranslator"

        fun interpolateStatic(
            template: String,
            variables: Map<String, String>,
            availablePictures: Set<String> = emptySet(),
            escapeJson: Boolean = true
        ): String {
            val tokenRegex = Regex("\\{([a-zA-Z0-9_.'\"|?:\$\\s-]+)\\}")
            return tokenRegex.replace(template) { match ->
                val rawExpression = match.groupValues[1].trim()
                if (isJsonFragment(rawExpression)) {
                    return@replace match.value
                }

                val alternatives = rawExpression.split(Regex("\\s*(?:\\|\\||\\?:|\\|)\\s*"))
                var matchedValue: String? = null

                for (i in alternatives.indices) {
                    val candidate = alternatives[i].trim()
                    if (isQuotedLiteral(candidate)) {
                        matchedValue = candidate.substring(1, candidate.length - 1)
                        break
                    }
                    if (isValidIdentifier(candidate)) {
                        val value = variables[candidate]
                        if (!value.isNullOrBlank()) {
                            if (candidate.startsWith("pic.") && availablePictures.isNotEmpty() &&
                                !availablePictures.contains(value) &&
                                !availablePictures.contains(value.removePrefix("miui.focus.pic_"))
                            ) {
                                continue
                            }
                            matchedValue = value
                            break
                        }
                    } else if (i == alternatives.lastIndex && !variables.containsKey(candidate) && !isJsonFragment(candidate)) {
                        // Unquoted fallback literal (e.g. {var.empty ?: Fallback Literal})
                        matchedValue = candidate
                        break
                    }
                }

                if (matchedValue == null) {
                    if (alternatives.all { isValidIdentifier(it) || isQuotedLiteral(it) }) {
                        matchedValue = ""
                    } else {
                        return@replace match.value
                    }
                }

                val raw = matchedValue ?: ""
                if (escapeJson) escapeJsonValue(raw) else raw
            }
        }

        private fun isValidIdentifier(s: String): Boolean {
            return s.matches(Regex("^[a-zA-Z0-9_.-]+$"))
        }

        private fun isQuotedLiteral(s: String): Boolean {
            return (s.startsWith("\"") && s.endsWith("\"") && s.length >= 2) ||
                   (s.startsWith("'") && s.endsWith("'") && s.length >= 2)
        }

        private fun isJsonFragment(s: String): Boolean {
            val withoutElvis = s.replace("?:", "")
            return withoutElvis.contains(":") || withoutElvis.contains(",")
        }

        private fun escapeJsonValue(value: String): String {
            return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
        }

        fun validateOrNormalizeParamV2(jsonString: String): String? {
            return try {
                val element = JsonParser.parseString(jsonString)
                if (!element.isJsonObject) {
                    runCatching { Log.w(TAG, " [validateOrNormalizeParamV2] Root element is not JsonObject: $jsonString") }
                    return null
                }
                val root = element.asJsonObject
                val paramV2 = if (root.has("param_v2") && root.get("param_v2").isJsonObject) {
                    root.getAsJsonObject("param_v2")
                } else {
                    root
                }

                // Resolve primary pic key candidate from coverInfo, baseInfo, or bgInfo
                val leftPicKey = if (paramV2.has("coverInfo") && paramV2.get("coverInfo").isJsonObject) {
                    val cover = paramV2.getAsJsonObject("coverInfo")
                    when {
                        cover.has("picCover") && cover.get("picCover").isJsonPrimitive -> cover.get("picCover").asString
                        cover.has("pic") && cover.get("pic").isJsonPrimitive -> cover.get("pic").asString
                        cover.has("pictureKey") && cover.get("pictureKey").isJsonPrimitive -> cover.get("pictureKey").asString
                        cover.has("picKey") && cover.get("picKey").isJsonPrimitive -> cover.get("picKey").asString
                        else -> null
                    }
                } else if (paramV2.has("baseInfo") && paramV2.get("baseInfo").isJsonObject) {
                    val base = paramV2.getAsJsonObject("baseInfo")
                    when {
                        base.has("pic") && base.get("pic").isJsonPrimitive -> base.get("pic").asString
                        base.has("pictureKey") && base.get("pictureKey").isJsonPrimitive -> base.get("pictureKey").asString
                        base.has("picKey") && base.get("picKey").isJsonPrimitive -> base.get("picKey").asString
                        else -> null
                    }
                } else if (paramV2.has("bgInfo") && paramV2.get("bgInfo").isJsonObject) {
                    val bg = paramV2.getAsJsonObject("bgInfo")
                    when {
                        bg.has("picBg") && bg.get("picBg").isJsonPrimitive -> bg.get("picBg").asString
                        bg.has("pic") && bg.get("pic").isJsonPrimitive -> bg.get("pic").asString
                        else -> null
                    }
                } else null

                // Ensure param_island exists and has both smallIslandArea and bigIslandArea
                val islandObj = if (paramV2.has("param_island") && paramV2.get("param_island").isJsonObject) {
                    paramV2.getAsJsonObject("param_island")
                } else {
                    val newIsland = com.google.gson.JsonObject()
                    paramV2.add("param_island", newIsland)
                    newIsland
                }

                if (!islandObj.has("islandProperty")) {
                    islandObj.addProperty("islandProperty", 1)
                }

                if (!islandObj.has("smallIslandArea")) {
                    if (paramV2.has("smallIslandArea")) {
                        islandObj.add("smallIslandArea", paramV2.get("smallIslandArea"))
                    } else {
                        val smallIsland = com.google.gson.JsonObject()
                        smallIsland.addProperty("leftImage", leftPicKey ?: "poster")
                        smallIsland.addProperty("rightImage", "hidden_pixel")
                        islandObj.add("smallIslandArea", smallIsland)
                    }
                }

                if (!islandObj.has("bigIslandArea")) {
                    if (paramV2.has("bigIslandArea")) {
                        islandObj.add("bigIslandArea", paramV2.get("bigIslandArea"))
                    } else if (paramV2.has("baseInfo") && paramV2.get("baseInfo").isJsonObject) {
                        val base = paramV2.getAsJsonObject("baseInfo")
                        val bigIsland = com.google.gson.JsonObject()
                        val left = com.google.gson.JsonObject()
                        left.addProperty("type", 1)
                        if (leftPicKey != null) {
                            left.addProperty("pic", leftPicKey)
                        } else if (base.has("picInfo")) {
                            left.add("picInfo", base.get("picInfo"))
                        }
                        val textInfo = com.google.gson.JsonObject()
                        if (base.has("title")) textInfo.add("title", base.get("title"))
                        if (base.has("subTitle")) textInfo.add("content", base.get("subTitle"))
                        else if (base.has("content")) textInfo.add("content", base.get("content"))
                        left.add("textInfo", textInfo)
                        bigIsland.add("imageTextInfoLeft", left)
                        islandObj.add("bigIslandArea", bigIsland)
                    }
                }

                val finalRoot = com.google.gson.JsonObject()
                finalRoot.add("param_v2", paramV2)
                com.google.gson.Gson().toJson(finalRoot)
            } catch (e: Exception) {
                runCatching { Log.e(TAG, " [validateOrNormalizeParamV2] JSON parse failed: ${e.message}\nJSON was:\n$jsonString", e) }
                null
            }
        }
    }

    private fun interpolate(
        template: String,
        variables: Map<String, String>,
        availablePictures: Set<String> = emptySet()
    ): String = interpolateStatic(template, variables, availablePictures)

    private fun resolveActionSlots(
        sbn: StatusBarNotification,
        customTranslator: CustomTranslator,
        rawText: String,
        theme: HyperTheme?,
        config: IslandConfig
    ): List<com.d4viddf.hyperbridge.models.BridgeAction> {
        val configuredSlots = customTranslator.presentation.actionSlots.sortedBy { it.slotPosition }
        if (configuredSlots.isEmpty()) {
            return extractBridgeActions(sbn, config, theme, includeSmartActions = true)
        }

        val bridgeActions = mutableListOf<com.d4viddf.hyperbridge.models.BridgeAction>()
        val notifActions = sbn.notification.actions ?: emptyArray()

        val defaultActionBg = if (theme != null) {
            try {
                val hex = resolveColor(theme, sbn.packageName, "#007AFF")
                hex.toColorInt()
            } catch (_: Exception) {
                "#007AFF".toColorInt()
            }
        } else {
            "#007AFF".toColorInt()
        }

        val themeShape = resolveShape(theme, sbn.packageName)
        val themePadding = resolvePadding(theme, sbn.packageName)

        for (slot in configuredSlots) {
            if (!slot.isVisible) continue

            when (slot.source) {
                ActionSource.NOTIFICATION_ACTION -> {
                    val notifAction = findNotificationAction(notifActions, slot)
                    if (notifAction != null) {
                        val label = slot.customLabel ?: notifAction.title?.toString() ?: "Action"
                        val uniqueKey = "act_${sbn.key.hashCode()}_slot_${slot.slotPosition}"

                        var actionIcon: android.graphics.drawable.Icon? = null
                        var hyperPic: HyperPicture? = null
                        if (slot.displayMode != com.d4viddf.hyperbridge.models.translator.ActionDisplayMode.TEXT_ONLY) {
                            var bmp: android.graphics.Bitmap? = null
                            if (slot.overrideIcon != null && repository != null) {
                                bmp = repository.getResourceBitmap(slot.overrideIcon)
                            }
                            if (bmp == null && notifAction.getIcon() != null) {
                                bmp = loadIconBitmap(notifAction.getIcon()!!, sbn.packageName)
                            }
                            if (bmp != null) {
                                val processed = applyThemeToActionIcon(bmp, themeShape, themePadding, defaultActionBg)
                                actionIcon = android.graphics.drawable.Icon.createWithBitmap(processed)
                                hyperPic = HyperPicture("${uniqueKey}_icon", processed)
                            }
                        }

                        val actionBgHex = if (slot.displayMode == com.d4viddf.hyperbridge.models.translator.ActionDisplayMode.TEXT_ONLY) null
                        else String.format("#%06X", (0xFFFFFF and defaultActionBg))

                        val hyperAction = HyperAction(
                            key = uniqueKey,
                            title = if (slot.displayMode == com.d4viddf.hyperbridge.models.translator.ActionDisplayMode.ICON_ONLY) "" else label,
                            icon = actionIcon,
                            pendingIntent = notifAction.actionIntent,
                            actionIntentType = 1,
                            actionBgColor = actionBgHex,
                            titleColor = "#FFFFFF"
                        )
                        bridgeActions.add(com.d4viddf.hyperbridge.models.BridgeAction(hyperAction, hyperPic))
                    }
                }
                ActionSource.SMART_ACTION -> {
                    val smartAction = resolveSmartAction(sbn.key, slot, rawText, defaultActionBg, themeShape, themePadding)
                    if (smartAction != null) {
                        bridgeActions.add(smartAction)
                    } else if (slot.fallbackToSource == ActionSource.NOTIFICATION_ACTION) {
                        val notifAction = findNotificationAction(notifActions, slot)
                        if (notifAction != null) {
                            val label = slot.customLabel ?: notifAction.title?.toString() ?: "Action"
                            val uniqueKey = "act_${sbn.key.hashCode()}_slot_${slot.slotPosition}"
                            var actionIcon: android.graphics.drawable.Icon? = null
                            var hyperPic: HyperPicture? = null
                            if (slot.displayMode != com.d4viddf.hyperbridge.models.translator.ActionDisplayMode.TEXT_ONLY) {
                                val bmp = notifAction.getIcon()?.let { loadIconBitmap(it, sbn.packageName) }
                                if (bmp != null) {
                                    val processed = applyThemeToActionIcon(bmp, themeShape, themePadding, defaultActionBg)
                                    actionIcon = android.graphics.drawable.Icon.createWithBitmap(processed)
                                    hyperPic = HyperPicture("${uniqueKey}_icon", processed)
                                }
                            }
                            val hyperAction = HyperAction(
                                key = uniqueKey,
                                title = if (slot.displayMode == com.d4viddf.hyperbridge.models.translator.ActionDisplayMode.ICON_ONLY) "" else label,
                                icon = actionIcon,
                                pendingIntent = notifAction.actionIntent,
                                actionIntentType = 1,
                                actionBgColor = null,
                                titleColor = "#FFFFFF"
                            )
                            bridgeActions.add(com.d4viddf.hyperbridge.models.BridgeAction(hyperAction, hyperPic))
                        }
                    }
                }
                ActionSource.INLINE_REPLY -> {
                    val notifActionWithRemoteInput = notifActions.firstOrNull { it.remoteInputs?.isNotEmpty() == true }
                        ?: findNotificationAction(notifActions, slot)
                    val remoteInput = notifActionWithRemoteInput?.remoteInputs?.firstOrNull()
                    val targetIntent = notifActionWithRemoteInput?.actionIntent ?: sbn.notification.contentIntent
                    if (targetIntent != null) {
                        val uniqueKey = "reply_${sbn.key.hashCode()}_slot_${slot.slotPosition}"
                        val label = slot.customLabel ?: notifActionWithRemoteInput?.title?.toString() ?: "Reply"
                        val replyIntent = android.content.Intent(context, com.d4viddf.hyperbridge.receiver.InlineReplyReceiver::class.java).apply {
                            putExtra("pending_intent", targetIntent)
                            putExtra("result_key", remoteInput?.resultKey ?: "key_text_reply")
                            putExtra("package_name", sbn.packageName)
                        }
                        val pending = android.app.PendingIntent.getBroadcast(
                            context,
                            uniqueKey.hashCode(),
                            replyIntent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE
                        )
                        var actionIcon: android.graphics.drawable.Icon? = null
                        var hyperPic: HyperPicture? = null
                        if (slot.displayMode != com.d4viddf.hyperbridge.models.translator.ActionDisplayMode.TEXT_ONLY) {
                            val bmp = notifActionWithRemoteInput?.getIcon()?.let { loadIconBitmap(it, sbn.packageName) }
                            if (bmp != null) {
                                val processed = applyThemeToActionIcon(bmp, themeShape, themePadding, defaultActionBg)
                                actionIcon = android.graphics.drawable.Icon.createWithBitmap(processed)
                                hyperPic = HyperPicture("${uniqueKey}_icon", processed)
                            }
                        }
                        val hyperAction = HyperAction(
                            key = uniqueKey,
                            title = if (slot.displayMode == com.d4viddf.hyperbridge.models.translator.ActionDisplayMode.ICON_ONLY) "" else label,
                            icon = actionIcon,
                            pendingIntent = pending,
                            actionIntentType = 1,
                            actionBgColor = null,
                            titleColor = "#FFFFFF"
                        )
                        bridgeActions.add(com.d4viddf.hyperbridge.models.BridgeAction(hyperAction, hyperPic))
                    }
                }
                ActionSource.CUSTOM_BROADCAST -> {
                    // Custom broadcast intent
                }
            }
        }

        return bridgeActions
    }

    private fun findNotificationAction(
        notifActions: Array<Notification.Action>,
        slot: ActionSlotConfig
    ): Notification.Action? {
        val matcher = slot.actionMatcher
        return when (matcher.matchBy) {
            ActionMatchBy.INDEX -> {
                val index = matcher.actionIndex ?: 0
                notifActions.getOrNull(index)
            }
            ActionMatchBy.TITLE -> {
                val regex = matcher.titleRegex?.let { runCatching { Regex(it) }.getOrNull() }
                if (regex != null) {
                    notifActions.firstOrNull { action ->
                        val title = action.title?.toString() ?: ""
                        regex.containsMatchIn(title)
                    }
                } else null
            }
        }
    }

    private fun resolveSmartAction(
        notificationKey: String,
        slot: ActionSlotConfig,
        rawText: String,
        defaultActionBg: Int,
        themeShape: String,
        themePadding: Int
    ): com.d4viddf.hyperbridge.models.BridgeAction? {
        val targetAction = when (slot.smartActionType) {
            com.d4viddf.hyperbridge.models.translator.SmartActionType.OTP_COPY -> {
                val otpCode = extractOtpFromText(rawText)
                if (otpCode != null) SmartAction(SmartActionType.OTP, otpCode, otpCode) else null
            }
            com.d4viddf.hyperbridge.models.translator.SmartActionType.OPEN_URL -> {
                val url = extractUrlFromText(rawText)
                if (url != null) SmartAction(SmartActionType.URL, url, url) else null
            }
            com.d4viddf.hyperbridge.models.translator.SmartActionType.DIAL_NUMBER -> {
                val phone = extractPhoneFromText(rawText)
                if (phone != null) SmartAction(SmartActionType.PHONE, phone, phone) else null
            }
            else -> null
        } ?: return null

        val key = SmartActionIntents.actionKey(notificationKey, targetAction)
        val title = slot.customLabel ?: SmartActionIntents.label(context, targetAction)
        val pendingIntent = SmartActionIntents.pendingIntent(context, targetAction, key)

        var actionIcon: android.graphics.drawable.Icon? = null
        var hyperPic: HyperPicture? = null
        if (slot.displayMode != com.d4viddf.hyperbridge.models.translator.ActionDisplayMode.TEXT_ONLY) {
            val iconRes = SmartActionIntents.iconRes(targetAction.type)
            val bmp = loadIconBitmap(android.graphics.drawable.Icon.createWithResource(context, iconRes), context.packageName)
            if (bmp != null) {
                val processed = applyThemeToActionIcon(bmp, themeShape, themePadding, defaultActionBg)
                actionIcon = android.graphics.drawable.Icon.createWithBitmap(processed)
                hyperPic = HyperPicture("${key}_icon", processed)
            }
        }

        val actionBgHex = if (slot.displayMode == com.d4viddf.hyperbridge.models.translator.ActionDisplayMode.TEXT_ONLY) null
        else String.format("#%08X", (0xFFFFFFFF and defaultActionBg.toLong()))

        val hyperAction = HyperAction(
            key = key,
            title = if (slot.displayMode == com.d4viddf.hyperbridge.models.translator.ActionDisplayMode.ICON_ONLY) "" else title,
            icon = actionIcon,
            pendingIntent = pendingIntent,
            actionIntentType = 1,
            actionBgColor = actionBgHex,
            titleColor = "#FFFFFF"
        )
        return com.d4viddf.hyperbridge.models.BridgeAction(hyperAction, hyperPic)
    }

    private fun extractOtpFromText(text: String): String? {
        val regex = Regex("(?i)(?:code|código|otp|passcode|verification|验证码)(?:\\s+(?:is|es))?[:\\s]*([0-9]{4,8})")
        return regex.find(text)?.groupValues?.getOrNull(1)
    }

    private fun extractUrlFromText(text: String): String? {
        val regex = Regex("https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]+")
        return regex.find(text)?.value
    }

    private fun extractPhoneFromText(text: String): String? {
        val regex = Regex("(?:\\+?\\d{1,3}[- ]?)?\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}")
        return regex.find(text)?.value
    }
}

