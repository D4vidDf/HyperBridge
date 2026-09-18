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
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

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
        val highlightColor = resolveColor(effectiveTheme, sbn.packageName, "#007AFF")

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

        // 5. Left Slot (Avatar / App Icon) & Hidden Pixel
        val hiddenKey = "hidden_pixel"
        builder.addPicture(resolveIcon(sbn, picKey))
        builder.addPicture(getTransparentPicture(hiddenKey))

        // 6. Base Info & Chat Info
        builder.setBaseInfo(
            type = 2,
            title = finalTitle,
            content = finalText
        )
        builder.setIconTextInfo(
            picKey = picKey,
            title = finalTitle,
            content = finalText
        )

        // 7. Progress or Standard Big Island Info
        if (customTranslator.presentation.progressSlot.type != ProgressSlotType.NONE && progressValue != null) {
            val progressPercent = if (maxProgressValue > 0) {
                ((progressValue.toFloat() / maxProgressValue.toFloat()) * 100).toInt().coerceIn(0, 100)
            } else {
                progressValue.coerceIn(0, 100)
            }
            builder.setProgressBar(progressPercent, highlightColor)
            builder.setBigIslandProgressCircle(picKey, "", progressPercent, highlightColor, true)
            builder.setSmallIslandCircularProgress(picKey, progressPercent, highlightColor, isCCW = true)
        } else {
            builder.setBigIslandInfo(
                left = ImageTextInfoLeft(1, PicInfo(1, picKey), TextInfo("", "")),
                right = ImageTextInfoRight(1, PicInfo(1, hiddenKey), TextInfo(finalTitle, finalText))
            )
            builder.setSmallIsland(picKey)
        }

        // 8. Action Slots & Smart Actions
        val bridgeActions = resolveActionSlots(
            sbn = sbn,
            customTranslator = customTranslator,
            rawText = rawText,
            theme = effectiveTheme,
            config = config
        )

        if (bridgeActions.isNotEmpty()) {
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
            textActions.forEach { builder.addHiddenAction(it) }
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

        for (slot in configuredSlots) {
            if (!slot.isVisible) continue

            when (slot.source) {
                ActionSource.NOTIFICATION_ACTION -> {
                    val notifAction = findNotificationAction(notifActions, slot)
                    if (notifAction != null) {
                        val label = slot.customLabel ?: notifAction.title?.toString() ?: "Action"
                        val uniqueKey = "act_${sbn.key.hashCode()}_slot_${slot.slotPosition}"
                        val hyperAction = HyperAction(
                            key = uniqueKey,
                            title = label,
                            icon = notifAction.getIcon(),
                            pendingIntent = notifAction.actionIntent,
                            actionIntentType = 1,
                            actionBgColor = null,
                            titleColor = "#FFFFFF"
                        )
                        bridgeActions.add(com.d4viddf.hyperbridge.models.BridgeAction(hyperAction, null))
                    }
                }
                ActionSource.SMART_ACTION -> {
                    val smartAction = resolveSmartAction(sbn.key, slot, rawText)
                    if (smartAction != null) {
                        bridgeActions.add(smartAction)
                    } else if (slot.fallbackToSource == ActionSource.NOTIFICATION_ACTION) {
                        val notifAction = findNotificationAction(notifActions, slot)
                        if (notifAction != null) {
                            val label = slot.customLabel ?: notifAction.title?.toString() ?: "Action"
                            val uniqueKey = "act_${sbn.key.hashCode()}_slot_${slot.slotPosition}"
                            val hyperAction = HyperAction(
                                key = uniqueKey,
                                title = label,
                                icon = notifAction.getIcon(),
                                pendingIntent = notifAction.actionIntent,
                                actionIntentType = 1,
                                actionBgColor = null,
                                titleColor = "#FFFFFF"
                            )
                            bridgeActions.add(com.d4viddf.hyperbridge.models.BridgeAction(hyperAction, null))
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
                        val hyperAction = HyperAction(
                            key = uniqueKey,
                            title = label,
                            icon = notifActionWithRemoteInput?.getIcon(),
                            pendingIntent = pending,
                            actionIntentType = 1,
                            actionBgColor = null,
                            titleColor = "#FFFFFF"
                        )
                        bridgeActions.add(com.d4viddf.hyperbridge.models.BridgeAction(hyperAction, null))
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
        rawText: String
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

        val hyperAction = HyperAction(
            key = key,
            title = title,
            icon = null,
            pendingIntent = pendingIntent,
            actionIntentType = 1,
            actionBgColor = null,
            titleColor = "#FFFFFF"
        )
        return com.d4viddf.hyperbridge.models.BridgeAction(hyperAction, null)
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

