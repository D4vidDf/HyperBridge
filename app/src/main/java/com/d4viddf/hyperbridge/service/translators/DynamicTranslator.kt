package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.SmartAction
import com.d4viddf.hyperbridge.models.SmartActionType
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.translator.ActionMatchBy
import com.d4viddf.hyperbridge.models.translator.ActionSlotConfig
import com.d4viddf.hyperbridge.models.translator.ActionSource
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.ProgressSlotType
import com.d4viddf.hyperbridge.service.smartactions.SmartActionIntents
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo
import androidx.core.graphics.toColorInt

class DynamicTranslator(
    context: Context,
    repository: ThemeRepository? = null
) : BaseTranslator(context, repository) {

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
        extractedSenderName: String? = null,
        extractedMediaArtist: String? = null,
        extractedConversationTitle: String? = null
    ): HyperIslandData {
        val notif = sbn.notification
        val extras = notif.extras

        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val rawSubtext = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        // 1. Resolve Effective Theme
        val effectiveTheme = resolveTheme(customTranslator, activeTheme)

        // 2. Extract Progress & Numeric Data
        val (progressValue, maxProgressValue) = extractProgress(customTranslator, notif, rawTitle, rawText)

        // 3. Resolve Dynamic Text Templates
        val variableMap = buildVariableMap(
            sbn = sbn,
            title = rawTitle,
            text = rawText,
            subtext = rawSubtext,
            sender = extractedSenderName,
            mediaArtist = extractedMediaArtist,
            conversationTitle = extractedConversationTitle,
            progress = progressValue?.toString()
        )
        val finalTitle = interpolate(customTranslator.presentation.textSlot.titleTemplate, variableMap)
        val finalText = interpolate(customTranslator.presentation.textSlot.subtitleTemplate, variableMap)

        // 4. Build HyperIsland Data
        val isFloat = config.isFloat ?: customTranslator.behaviorOverride.isFloat ?: false
        val isShade = config.isShowShade ?: customTranslator.behaviorOverride.isShowShade ?: false
        val defaultHighlight = if (customTranslator.presentation.progressSlot.type == ProgressSlotType.TIMER) "#FF9500" else "#007AFF"
        val highlightColor = if (customTranslator.themeBinding.overrideHighlightColor != null) {
            customTranslator.themeBinding.overrideHighlightColor
        } else {
            resolveColor(effectiveTheme, sbn.packageName, defaultHighlight)
        }

        val builder = HyperIslandNotification.Builder(
            context,
            stableBusinessId(picKey),
            finalTitle
        )

        builder.applyFloatingPresentation(isFloat, isUpdate)
        builder.setIslandConfig(
            timeout = customTranslator.behaviorOverride.timeoutSeconds ?: config.timeout,
            dismissible = true,
            highlightColor = highlightColor,
            expandedTimeMs = customTranslator.behaviorOverride.floatTimeoutSeconds ?: config.floatTimeout
        )
        builder.setShowNotification(isShade)
        if (!isUpdate) builder.setReopen(true)

        // 5. Left Slot Graphic & Hidden Pixel
        val hiddenKey = "hidden_pixel"
        val leftSlotConfig = customTranslator.presentation.leftSlot
        val leftPic = when (leftSlotConfig.source) {
            "HIDDEN" -> getTransparentPicture(picKey)
            else -> {
                // If custom icon res exists in repo/theme
                var customBmp: android.graphics.Bitmap? = null
                if (leftSlotConfig.customIconRes != null && repository != null) {
                    customBmp = repository.getResourceBitmap(leftSlotConfig.customIconRes)
                }
                if (customBmp != null) {
                    val shapeId = leftSlotConfig.shape ?: resolveShape(effectiveTheme, sbn.packageName)
                    val pad = resolvePadding(effectiveTheme, sbn.packageName)
                    val themedBmp = applyThemeToActionIcon(customBmp, shapeId, pad, android.graphics.Color.TRANSPARENT)
                    HyperPicture(picKey, themedBmp)
                } else {
                    resolveIcon(sbn, picKey)
                }
            }
        }

        builder.addPicture(leftPic)
        builder.addPicture(getTransparentPicture(hiddenKey))

        // 6. Base Info & Chat Info (Expanded Island)
        val progressConfig = customTranslator.presentation.progressSlot
        val isTimer = progressConfig.type == ProgressSlotType.TIMER
        val baseTime = sbn.notification.`when`.let { if (it > 0) it else System.currentTimeMillis() }
        val now = System.currentTimeMillis()
        val isCountdown = baseTime > now
        val timerType = if (isCountdown) -1 else 1

        val chatTimerInfo = if (isTimer) {
            io.github.d4viddf.hyperisland_kit.models.TimerInfo(
                timerType,
                baseTime,
                if (isCountdown) baseTime - now else now - baseTime,
                now
            )
        } else {
            null
        }

        val bridgeActions = resolveActionSlots(
            sbn = sbn,
            customTranslator = customTranslator,
            rawText = rawText,
            theme = effectiveTheme,
            config = config
        )
        val actionKeys = bridgeActions.map { it.action.key }

        builder.setChatInfo(
            title = finalTitle,
            content = finalText,
            pictureKey = if (leftSlotConfig.source == "HIDDEN") hiddenKey else picKey,
            appPkg = sbn.packageName,
            timer = chatTimerInfo,
            actionKeys = if (actionKeys.isNotEmpty()) actionKeys else null
        )

        builder.setBaseInfo(
            type = 2,
            title = finalTitle,
            content = finalText
        )
        builder.setIconTextInfo(
            picKey = if (leftSlotConfig.source == "HIDDEN") hiddenKey else picKey,
            title = finalTitle,
            content = finalText
        )

        // 7. Progress & Expanded Bar
        val hasProgress = progressConfig.type == ProgressSlotType.PROGRESS_BAR && progressValue != null
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

        if (hasProgress) {
            builder.setProgressBar(progressPercent, progressColor)
        }

        // 8. Compact Pill Presentation (Big & Small Island)
        val pillConfig = customTranslator.presentation.pill
        val leftDesign = pillConfig.leftDesign
        val rightDesign = pillConfig.rightDesign

        val pillLeftPicKey = when (leftDesign) {
            com.d4viddf.hyperbridge.models.translator.PillLeftDesign.HIDDEN -> hiddenKey
            else -> if (leftSlotConfig.source == "HIDDEN") hiddenKey else picKey
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

        val shouldShowProgressCircle = when (rightDesign) {
            com.d4viddf.hyperbridge.models.translator.PillRightDesign.PROGRESS_PERCENT -> hasProgress
            com.d4viddf.hyperbridge.models.translator.PillRightDesign.AUTO -> hasProgress
            else -> false
        }

        when {
            shouldShowProgressCircle -> {
                builder.setBigIslandProgressCircle(pillLeftPicKey, pillLeftText, progressPercent, progressColor, true)
                builder.setSmallIslandCircularProgress(pillLeftPicKey, progressPercent, progressColor, isCCW = true)
            }
            rightDesign == com.d4viddf.hyperbridge.models.translator.PillRightDesign.TIMER || isTimer -> {
                if (isCountdown) {
                    builder.setBigIslandCountdown(baseTime, pillLeftPicKey)
                } else {
                    builder.setBigIslandCountUp(baseTime, pillLeftPicKey)
                }
                builder.setSmallIsland(pillLeftPicKey)
            }
            else -> {
                builder.setBigIslandInfo(
                    left = ImageTextInfoLeft(1, PicInfo(1, pillLeftPicKey), TextInfo(pillLeftText, "")),
                    right = if (rightDesign != com.d4viddf.hyperbridge.models.translator.PillRightDesign.NONE) {
                        ImageTextInfoRight(1, PicInfo(1, hiddenKey), TextInfo(finalTitle, pillRightText))
                    } else {
                        ImageTextInfoRight(1, PicInfo(1, hiddenKey), TextInfo("", ""))
                    }
                )
                builder.setSmallIsland(pillLeftPicKey)
            }
        }

        // 9. Register Actions & Hidden Actions
        if (bridgeActions.isNotEmpty()) {
            val textActions = bridgeActions.map { it.action }.toTypedArray()
            builder.setTextButtons(*textActions)
            textActions.forEach {
                builder.addAction(it)
                builder.addHiddenAction(it)
            }
            bridgeActions.forEach { it.actionImage?.let { pic -> builder.addPicture(pic) } }
        }

        return HyperIslandData(builder.buildResourceBundle(), builder.buildJsonParam())
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
        title: String,
        text: String,
        subtext: String?,
        sender: String?,
        mediaArtist: String?,
        conversationTitle: String?,
        progress: String?
    ): Map<String, String> {
        val map = mutableMapOf<String, String>()
        map["notif.title"] = title
        map["notif.text"] = text
        map["notif.subtext"] = subtext ?: ""
        map["notif.sender"] = sender ?: ""
        map["notif.conversation_title"] = conversationTitle ?: ""
        map["notif.media_artist"] = mediaArtist ?: ""
        map["notif.progress"] = progress ?: ""
        map["app.name"] = sbn.packageName
        return map
    }

    private fun interpolate(template: String, variables: Map<String, String>): String {
        var result = template
        for ((key, value) in variables) {
            result = result.replace("{$key}", value)
        }
        return result
    }

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
                        else String.format("#%08X", (0xFFFFFFFF and defaultActionBg.toLong()))

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

