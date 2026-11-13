package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.data.widget.CustomWidgetRepository
import com.d4viddf.hyperbridge.data.widget.DeviceVariables
import com.d4viddf.hyperbridge.data.widget.SourceRepository
import com.d4viddf.hyperbridge.data.widget.VariableContext
import com.d4viddf.hyperbridge.data.widget.WidgetVariableEngine
import com.d4viddf.hyperbridge.integration.xiaomi.HyperIslandProtocolOptions
import com.d4viddf.hyperbridge.integration.xiaomi.KEYGUARD_ISLAND_PROPERTY
import com.d4viddf.hyperbridge.integration.xiaomi.buildCustomExtras
import com.d4viddf.hyperbridge.integration.xiaomi.buildJsonParam
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.PillLeftDesign
import com.d4viddf.hyperbridge.models.translator.PillMediaItem
import com.d4viddf.hyperbridge.models.translator.PillRightDesign
import com.d4viddf.hyperbridge.models.translator.ProgressSlotType
import com.d4viddf.hyperbridge.service.smartactions.SmartActionIntents
import com.d4viddf.hyperbridge.service.smartactions.SmartActionNotificationText
import com.d4viddf.hyperbridge.service.smartactions.SmartActionsExtractor
import com.d4viddf.hyperbridge.service.visual.NotificationVisualSource
import com.d4viddf.hyperbridge.service.widget.CustomWidgetRenderer
import com.d4viddf.hyperbridge.service.widget.WidgetActionIntents
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo
import kotlinx.coroutines.runBlocking

/**
 * Renders a custom design (Phase 5, #273): a translator whose presentation mode is WIDGET carries
 * a `widgetId`, and the island body becomes that Studio document rendered as a RemoteViews tree.
 *
 * Which notifications reach it is decided by Phase 3's `TranslatorRegistry` like every other
 * translator, so there is no package-keyed binding lookup on the hot path any more.
 */
class CustomWidgetTranslator(
    context: Context,
    repository: ThemeRepository? = null
) : BaseTranslator(context, repository) {

    private val widgetRepo = CustomWidgetRepository(context)
    private val sourceRepo = SourceRepository(context)
    private val renderer = CustomWidgetRenderer(context, WidgetVariableEngine(), widgetRepo)

    suspend fun translate(
        sbn: StatusBarNotification,
        picKey: String,
        effectiveTitle: String,
        effectiveText: String,
        config: IslandConfig,
        theme: HyperTheme?,
        widgetId: String,
        customTranslator: CustomTranslator? = null,
        isUpdate: Boolean = false,
        isKeyguardLocked: Boolean = false
    ): HyperIslandData {
        val doc = requireNotNull(widgetRepo.getWidget(widgetId)) {
            "Custom design $widgetId is missing from the Studio store"
        }

        val effectiveTheme = customTranslator?.let { resolveTheme(it, theme) } ?: theme

        val progress = sbn.notification.extras.getInt("android.progress", -1).takeIf { it >= 0 }
        val mediaResult = com.d4viddf.hyperbridge.util.MediaProgressResolver.resolveMediaProgress(sbn, context)
        val mediaProgress = mediaResult.progressPercent ?: progress

        // What the notification itself offers: its buttons, whether one of them replies inline,
        // and any Smart Action (#270) found in its text. Conditional nodes are evaluated against
        // these facts, and a node can fire the matching intent (#328).
        val actions = sbn.notification.actions?.toList().orEmpty()
        val replyAction = actions.firstOrNull { !it.remoteInputs.isNullOrEmpty() }
        val smartActions = extractSmartActions(sbn, config)

        val highlightColor = if (customTranslator?.themeBinding?.overrideHighlightColor != null) {
            customTranslator.themeBinding.overrideHighlightColor
        } else {
            resolveColor(effectiveTheme, sbn.packageName, "#00E676")
        }
        val themePrimary = effectiveTheme?.global?.highlightColor ?: highlightColor
        val themeSurface = effectiveTheme?.global?.backgroundColor ?: "#1E1E1E"

        val notif = sbn.notification
        val extras = notif.extras

        val resolvedVisual = resolveNotificationVisual(sbn)
        val contentVisualBitmap = if (
            resolvedVisual.source == NotificationVisualSource.PERSON ||
            resolvedVisual.source == NotificationVisualSource.PICTURE ||
            resolvedVisual.source == NotificationVisualSource.LARGE_ICON
        ) {
            resolvedVisual.bitmap
        } else null

        val rawAvatarBitmap: Bitmap? = try {
            notif.getLargeIcon()?.let { loadIconBitmap(it, sbn.packageName) }
                ?: (
                        extras.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
                        )
                ?: if (resolvedVisual.source == NotificationVisualSource.PERSON || resolvedVisual.source == NotificationVisualSource.LARGE_ICON) contentVisualBitmap else null
        } catch (_: Exception) { null }

        val rawPictureBitmap: Bitmap? = try {
            (
                    extras.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
                    ) ?: (extras.getParcelable<Icon>("android.pictureIcon")?.let { loadIconBitmap(it, sbn.packageName) })
               ?: (extras.getParcelable<Icon>(Notification.EXTRA_PICTURE_ICON)?.let { loadIconBitmap(it, sbn.packageName) })
               ?: if (resolvedVisual.source == NotificationVisualSource.PICTURE) contentVisualBitmap else null
        } catch (_: Exception) { null }

        val notifSmallIconBitmap: Bitmap? = try {
            notif.smallIcon?.loadDrawable(context)?.toBitmap()
        } catch (_: Exception) { null }

        val mediaSessionBmp = try {
            val token =
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, android.media.session.MediaSession.Token::class.java)
            if (token != null) {
                val controller = android.media.session.MediaController(context, token)
                controller.metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: controller.metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
                    ?: controller.metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            } else null
        } catch (_: Exception) {
            null
        }

        val mediaArtworkBmp = mediaSessionBmp ?: rawAvatarBitmap ?: rawPictureBitmap ?: contentVisualBitmap
        val notifAvatarBitmap = rawAvatarBitmap ?: mediaArtworkBmp
        val notifPictureBitmap = rawPictureBitmap ?: mediaArtworkBmp ?: rawAvatarBitmap

        val iconBitmap = try {
            context.packageManager.getApplicationIcon(sbn.packageName).toBitmap()
        } catch (_: Exception) {
            null
        }

        val resolvedColorScheme = com.d4viddf.hyperbridge.models.colorscheme.DynamicColorSchemeResolver.resolve(
            context = context,
            config = doc.globals.colorSchemeConfig,
            embeddedYaml = doc.globals.embeddedColorSchemeYaml ?: effectiveTheme?.embeddedColorSchemeYaml,
            appIconBitmap = iconBitmap,
            mediaBitmaps = mapOf(
                "album_art" to (mediaArtworkBmp ?: notifPictureBitmap),
                "picture" to notifPictureBitmap,
                "avatar" to notifAvatarBitmap
            ),
            isDark = true
        )

        val ctx = VariableContext(
            notifTitle = effectiveTitle,
            notifText = effectiveText,
            notifProgress = progress ?: mediaProgress,
            notifPackage = sbn.packageName,
            deviceBatteryPercent = DeviceVariables.batteryPercent(context),
            timeNowFormatted = DeviceVariables.timeNow(),
            notificationActionTitles = actions.map { it.title?.toString().orEmpty() },
            hasInlineReply = replyAction != null,
            smartActionTypes = smartActions.keys,
            sourceLookup = { id, field -> runBlocking { sourceRepo.lookup(id, field) } },
            themePrimary = themePrimary,
            themeAccent = highlightColor,
            themeSurface = themeSurface,
            notifAvatarBitmap = notifAvatarBitmap,
            notifPictureBitmap = notifPictureBitmap,
            notifSmallIconBitmap = notifSmallIconBitmap,
            mediaProgress = mediaProgress,
            mediaTrack = effectiveTitle,
            mediaArtist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: effectiveText,
            mediaDuration = mediaResult.durationFormatted,
            mediaPosition = mediaResult.currentFormatted,
            mediaRemaining = mediaResult.remainingFormatted,
            isMediaPlaying = mediaResult.isPlaying ?: false,
            mediaState = mediaResult.playbackStateStr,
            colorScheme = resolvedColorScheme
        )

        val intents = WidgetActionIntents(
            notificationActions = actions.map { it.actionIntent },
            inlineReply = replyAction?.actionIntent,
            smartActions = smartActions
        )

        val bridgeId = picKey.removePrefix("pic_").toIntOrNull()
        val rv = renderer.render(doc, ctx, bridgeId = bridgeId, intents = intents)

        val builder = HyperIslandNotification.Builder(context, "custom_widget_channel", effectiveTitle)
        val hiddenKey = "hidden_pixel"
        builder.addPicture(getTransparentPicture(hiddenKey))

        val pillConfig = customTranslator?.presentation?.pill
        val leftDesign = pillConfig?.leftDesign ?: PillLeftDesign.ICON_AND_TEXT
        val rightDesign = pillConfig?.rightDesign ?: PillRightDesign.AUTO

        val mediaOrder = pillConfig?.mediaOrder?.ifEmpty {
            listOf(PillMediaItem.ALBUM_COVER, PillMediaItem.TRACK, PillMediaItem.ARTIST)
        } ?: listOf(PillMediaItem.ALBUM_COVER, PillMediaItem.TRACK, PillMediaItem.ARTIST)

        val hasAlbumCover = mediaOrder.contains(PillMediaItem.ALBUM_COVER)

        val pillShapeId = customTranslator?.presentation?.leftSlot?.shape ?: resolveShape(effectiveTheme, sbn.packageName)
        val themedMediaPillBmp = if (mediaArtworkBmp != null) {
            clipBitmapToShape(mediaArtworkBmp, pillShapeId, 0)
        } else null

        if (mediaArtworkBmp != null && themedMediaPillBmp != null) {
            builder.addPicture(HyperPicture("media_pill_art", themedMediaPillBmp))
            builder.addPicture(HyperPicture("album_art", themedMediaPillBmp))
            builder.addPicture(HyperPicture("miui.focus.pic_large_icon", mediaArtworkBmp))
            builder.addPicture(HyperPicture("miui.focus.pic_sender_avatar", mediaArtworkBmp))
            builder.addPicture(HyperPicture("miui.focus.pic_imageText", mediaArtworkBmp))
        }

        val appIconPicture = if (iconBitmap != null) {
            HyperPicture(picKey, Icon.createWithBitmap(iconBitmap))
        } else {
            resolveIcon(sbn, picKey)
        }
        val defaultPicPicture = if (leftDesign == PillLeftDesign.MEDIA && themedMediaPillBmp != null && hasAlbumCover) {
            HyperPicture(picKey, themedMediaPillBmp)
        } else {
            appIconPicture
        }
        builder.addPicture(defaultPicPicture)

        val progressConfig = customTranslator?.presentation?.progressSlot
        val hasProgress = (progressConfig?.type == ProgressSlotType.PROGRESS_BAR || rightDesign == PillRightDesign.PROGRESS_PERCENT) &&
                (mediaProgress != null || progress != null)
        val progressPercent = (mediaProgress ?: progress ?: 0).coerceIn(0, 100)
        val progressColor = effectiveTheme?.defaultProgress?.activeColor ?: highlightColor

        val isTimer = (progressConfig?.type == ProgressSlotType.TIMER || rightDesign == PillRightDesign.TIMER) &&
                notif.`when` > System.currentTimeMillis()

        val mediaArtist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.ifBlank { null } ?: effectiveText

        val mediaPillText = when (mediaOrder.firstOrNull { it != PillMediaItem.ALBUM_COVER }) {
            PillMediaItem.ARTIST -> mediaArtist.ifBlank { effectiveTitle }
            PillMediaItem.TRACK -> effectiveTitle.ifBlank { mediaArtist }
            else -> ""
        }

        val leftSlotConfig = customTranslator?.presentation?.leftSlot
        val primaryPicKey = if (leftSlotConfig?.source == "HIDDEN") hiddenKey else picKey

        val pillLeftPicKey = when (leftDesign) {
            PillLeftDesign.HIDDEN -> hiddenKey
            PillLeftDesign.MEDIA -> {
                if (hasAlbumCover) {
                    if (themedMediaPillBmp != null) "album_art" else primaryPicKey
                } else hiddenKey
            }
            else -> primaryPicKey
        }

        val pillLeftText = when (leftDesign) {
            PillLeftDesign.ICON_AND_TEXT,
            PillLeftDesign.TEXT_ONLY -> effectiveTitle
            PillLeftDesign.MEDIA -> mediaPillText
            else -> ""
        }

        val pillRightText = when (rightDesign) {
            PillRightDesign.OTP_CODE -> {
                DynamicTranslator.extractOtpFromText(effectiveText)
                    ?: DynamicTranslator.extractOtpFromText(effectiveTitle)
                    ?: ""
            }
            PillRightDesign.BOARDING_PASS -> {
                DynamicTranslator.extractBoardingPassFromText(effectiveText)
                    ?: DynamicTranslator.extractBoardingPassFromText(effectiveTitle)
                    ?: ""
            }
            PillRightDesign.NOTIF_TITLE -> effectiveTitle
            PillRightDesign.MESSAGE -> effectiveText
            PillRightDesign.SENDER_NAME -> extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString() ?: effectiveTitle
            PillRightDesign.MEDIA_TRACK -> effectiveTitle
            PillRightDesign.MEDIA_ARTIST -> mediaArtist
            PillRightDesign.MEDIA_COUNTER -> {
                val isRemaining = pillConfig?.mediaCounterMode == com.d4viddf.hyperbridge.models.translator.MediaCounterMode.REMAINING
                val counter = if (isRemaining) {
                    mediaResult.remainingFormatted ?: mediaResult.currentFormatted
                } else {
                    mediaResult.currentFormatted ?: mediaResult.remainingFormatted
                }
                counter ?: mediaResult.durationFormatted ?: ""
            }
            PillRightDesign.CHAT_COUNTER -> {
                val count =
                    notif.number.takeIf { it > 0 }
                val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                if (count != null) "+$count"
                else if (!lines.isNullOrEmpty()) "+${lines.size}"
                else ""
            }
            PillRightDesign.PROGRESS_PERCENT -> {
                if (progressPercent > 0) "$progressPercent%" else ""
            }
            PillRightDesign.TIMER -> ""
            PillRightDesign.HIGHLIGHT_TEXT -> {
                val highlightTpl = customTranslator?.presentation?.textSlot?.highlightTextTemplate
                if (!highlightTpl.isNullOrBlank()) highlightTpl.replace("{notif.title}", effectiveTitle).replace("{notif.text}", effectiveText) else effectiveText
            }
            PillRightDesign.AUTO -> {
                val isMediaNotif = leftDesign == PillLeftDesign.MEDIA || extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true
                if (isMediaNotif) {
                    val isRemaining = pillConfig?.mediaCounterMode == com.d4viddf.hyperbridge.models.translator.MediaCounterMode.REMAINING
                    val counter = if (isRemaining) {
                        mediaResult.remainingFormatted ?: mediaResult.currentFormatted
                    } else {
                        mediaResult.currentFormatted ?: mediaResult.remainingFormatted
                    }
                    counter ?: mediaResult.durationFormatted ?: effectiveText
                } else {
                    effectiveText
                }
            }
            PillRightDesign.NONE -> ""
        }

        val showBigIslandPic = leftDesign != PillLeftDesign.TEXT_ONLY &&
                leftDesign != PillLeftDesign.HIDDEN &&
                leftSlotConfig?.source != "HIDDEN" &&
                (leftDesign != PillLeftDesign.MEDIA || hasAlbumCover)

        when {
            hasProgress -> {
                builder.setProgressBar(progressPercent, progressColor)
                val progressPicKey = if (showBigIslandPic) pillLeftPicKey else ""
                builder.setBigIslandProgressCircle(progressPicKey, pillLeftText, progressPercent, progressColor, true)
                builder.setSmallIslandCircularProgress(if (showBigIslandPic) pillLeftPicKey else primaryPicKey, progressPercent, progressColor, isCCW = true)
            }
            isTimer -> {
                val baseTime = notif.`when`.let { if (it > 0) it else System.currentTimeMillis() }
                val now = System.currentTimeMillis()
                val isCountdown = baseTime > now
                val timerPicKey = if (showBigIslandPic) pillLeftPicKey else ""
                if (isCountdown) {
                    builder.setBigIslandCountdown(baseTime, timerPicKey)
                } else {
                    builder.setBigIslandCountUp(baseTime, timerPicKey)
                }
                builder.setSmallIsland(if (showBigIslandPic) pillLeftPicKey else primaryPicKey)
            }
            else -> {
                val leftBigIslandText = pillLeftText
                val leftPicInfo = if (showBigIslandPic) PicInfo(1, pillLeftPicKey) else null
                val rightInfo = if (rightDesign != PillRightDesign.NONE && pillRightText.isNotBlank()) {
                    ImageTextInfoRight(type = 2, picInfo = null, textInfo = TextInfo(title = pillRightText, content = ""))
                } else null
                builder.setBigIslandInfo(
                    left = ImageTextInfoLeft(1, leftPicInfo, TextInfo(leftBigIslandText, "")),
                    right = rightInfo
                )
                builder.setSmallIsland(if (hasAlbumCover && themedMediaPillBmp != null) "album_art" else (if (showBigIslandPic) pillLeftPicKey else primaryPicKey))
            }
        }

        // Expanded island: the rendered micro-widget. HyperOS reads the expanded content from the
        // island-expand slot, not from the notification's custom view, so both must be set
        // (verified on device: with setCustomRemoteView alone the pill shows but never expands).
        builder.setCustomRemoteView(rv)
        builder.setCustomIslandExpandRemoteView(rv)
        builder.setIslandConfig(
            timeout = customTranslator?.behaviorOverride?.timeoutSeconds ?: config.timeout,
            dismissible = false,
            highlightColor = highlightColor,
            expandedTimeMs = customTranslator?.behaviorOverride?.floatTimeoutSeconds ?: config.floatTimeout
        )
        val isFloat = customTranslator?.behaviorOverride?.isFloat ?: config.isFloat ?: false
        val floatOnlyOnFirst = customTranslator?.behaviorOverride?.floatOnlyOnFirst ?: true
        builder.applyFloatingPresentation(isFloat, isUpdate, floatOnlyOnFirst)
        builder.setShowNotification(customTranslator?.behaviorOverride?.isShowShade ?: config.isShowShade ?: false)
        builder.setHideDeco(true)
        if (!isUpdate) builder.setReopen(true)

        val showOnLockscreen = customTranslator?.behaviorOverride?.showOnLockscreen == true
        val targetIslandProperty = if (showOnLockscreen) {
            if (isKeyguardLocked) KEYGUARD_ISLAND_PROPERTY else 1
        } else null

        val customExtras = if (targetIslandProperty != null) {
            builder.buildCustomExtras(HyperIslandProtocolOptions(islandProperty = targetIslandProperty))
        } else {
            builder.buildCustomExtras()
        }
        val jsonParam = if (targetIslandProperty != null) {
            builder.buildJsonParam(HyperIslandProtocolOptions(islandProperty = targetIslandProperty))
        } else {
            builder.buildJsonParam()
        }
        return HyperIslandData(customExtras, jsonParam)
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

    /**
     * Smart Actions detected on this notification, keyed by type name. Returns nothing -- and
     * reads no notification text -- when the user has the feature off, matching BaseTranslator.
     */
    private fun extractSmartActions(
        sbn: StatusBarNotification,
        config: IslandConfig
    ): Map<String, android.app.PendingIntent> {
        val smartConfig = config.smartActions ?: return emptyMap()
        if (!smartConfig.isActiveFor(sbn.packageName)) return emptyMap()

        val text = SmartActionNotificationText.collect(sbn.notification)
        return SmartActionsExtractor.extract(text, smartConfig).associate { smart ->
            val key = SmartActionIntents.actionKey(sbn.key, smart)
            smart.type.name to SmartActionIntents.pendingIntent(context, smart, key)
        }
    }
}
