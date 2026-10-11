package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toBitmap
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.data.widget.CustomWidgetRepository
import com.d4viddf.hyperbridge.data.widget.DeviceVariables
import com.d4viddf.hyperbridge.data.widget.SourceRepository
import com.d4viddf.hyperbridge.data.widget.VariableContext
import com.d4viddf.hyperbridge.data.widget.WidgetVariableEngine
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.service.smartactions.SmartActionIntents
import com.d4viddf.hyperbridge.service.smartactions.SmartActionNotificationText
import com.d4viddf.hyperbridge.service.smartactions.SmartActionsExtractor
import com.d4viddf.hyperbridge.service.widget.CustomWidgetRenderer
import com.d4viddf.hyperbridge.service.widget.WidgetActionIntents
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import kotlinx.coroutines.runBlocking

import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.PillLeftDesign
import com.d4viddf.hyperbridge.models.translator.PillMediaItem
import com.d4viddf.hyperbridge.models.translator.PillRightDesign
import com.d4viddf.hyperbridge.models.translator.ProgressSlotType
import com.d4viddf.hyperbridge.service.visual.NotificationVisualSource
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.TextInfo

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
        customTranslator: CustomTranslator? = null
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
        val themeAccent = highlightColor
        val themeSurface = effectiveTheme?.global?.backgroundColor ?: "#1E1E1E"

        val notif = sbn.notification
        val extras = notif.extras

        val notifAvatarBitmap: Bitmap? = try {
            notif.getLargeIcon()?.let { loadIconBitmap(it, sbn.packageName) }
                ?: (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    extras.getParcelable(Notification.EXTRA_LARGE_ICON) as? Bitmap
                })
        } catch (_: Exception) { null }

        val notifPictureBitmap: Bitmap? = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_PICTURE, Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_PICTURE) as? Bitmap
            }
        } catch (_: Exception) { null }

        val notifSmallIconBitmap: Bitmap? = try {
            notif.smallIcon?.loadDrawable(context)?.toBitmap()
        } catch (_: Exception) { null }

        val mediaSessionBmp = try {
            val token = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, android.media.session.MediaSession.Token::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION) as? android.media.session.MediaSession.Token
            }
            if (token != null) {
                val controller = android.media.session.MediaController(context, token)
                controller.metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: controller.metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
            } else null
        } catch (_: Exception) {
            null
        }

        val mediaArtworkBmp = notifAvatarBitmap ?: mediaSessionBmp ?: notifPictureBitmap ?: run {
            val resolved = resolveNotificationVisual(sbn)
            if (resolved.source == NotificationVisualSource.PICTURE || resolved.source == NotificationVisualSource.LARGE_ICON) {
                resolved.bitmap
            } else null
        }

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
            themeAccent = themeAccent,
            themeSurface = themeSurface,
            notifAvatarBitmap = notifAvatarBitmap,
            notifPictureBitmap = notifPictureBitmap,
            notifSmallIconBitmap = notifSmallIconBitmap,
            mediaProgress = mediaProgress,
            mediaTrack = effectiveTitle,
            mediaArtist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: effectiveText,
            mediaDuration = mediaResult.durationFormatted,
            mediaPosition = mediaResult.currentFormatted,
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

        val appIconPicture = if (iconBitmap != null) {
            HyperPicture(picKey, Icon.createWithBitmap(iconBitmap))
        } else {
            resolveIcon(sbn, picKey)
        }
        builder.addPicture(appIconPicture)

        val mediaPillArtKey = "media_pill_art"
        val pillShapeId = customTranslator?.presentation?.leftSlot?.shape ?: resolveShape(effectiveTheme, sbn.packageName)
        if (mediaArtworkBmp != null) {
            val themedMediaPillBmp = applyThemeToActionIcon(mediaArtworkBmp, pillShapeId, 0, android.graphics.Color.TRANSPARENT)
            builder.addPicture(HyperPicture(mediaPillArtKey, themedMediaPillBmp))
            builder.addPicture(HyperPicture("album_art", mediaArtworkBmp))
        }

        val pillConfig = customTranslator?.presentation?.pill
        val leftDesign = pillConfig?.leftDesign ?: PillLeftDesign.ICON_AND_TEXT
        val rightDesign = pillConfig?.rightDesign ?: PillRightDesign.AUTO

        val progressConfig = customTranslator?.presentation?.progressSlot
        val hasProgress = (progressConfig?.type == ProgressSlotType.PROGRESS_BAR || rightDesign == PillRightDesign.PROGRESS_PERCENT) &&
                (mediaProgress != null || progress != null)
        val progressPercent = (mediaProgress ?: progress ?: 0).coerceIn(0, 100)
        val progressColor = effectiveTheme?.defaultProgress?.activeColor ?: highlightColor

        val isTimer = (progressConfig?.type == ProgressSlotType.TIMER || rightDesign == PillRightDesign.TIMER) &&
                notif.`when` > System.currentTimeMillis()

        val mediaOrder = pillConfig?.mediaOrder?.ifEmpty {
            listOf(PillMediaItem.ALBUM_COVER, PillMediaItem.TRACK, PillMediaItem.ARTIST)
        } ?: listOf(PillMediaItem.ALBUM_COVER, PillMediaItem.TRACK, PillMediaItem.ARTIST)

        val mediaTrack = effectiveTitle
        val mediaArtist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.ifBlank { null } ?: effectiveText

        val mediaPillText = when (mediaOrder.firstOrNull { it != PillMediaItem.ALBUM_COVER }) {
            PillMediaItem.ARTIST -> mediaArtist.ifBlank { mediaTrack }
            PillMediaItem.TRACK -> mediaTrack.ifBlank { mediaArtist }
            else -> ""
        }

        val leftSlotConfig = customTranslator?.presentation?.leftSlot
        val primaryPicKey = if (leftSlotConfig?.source == "HIDDEN") hiddenKey else picKey

        val pillLeftPicKey = when (leftDesign) {
            PillLeftDesign.HIDDEN -> hiddenKey
            PillLeftDesign.MEDIA -> {
                if (mediaOrder.contains(PillMediaItem.ALBUM_COVER)) {
                    if (mediaArtworkBmp != null) mediaPillArtKey else primaryPicKey
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
            PillRightDesign.HIGHLIGHT_TEXT -> {
                val highlightTpl = customTranslator?.presentation?.textSlot?.highlightTextTemplate
                if (!highlightTpl.isNullOrBlank()) highlightTpl.replace("{notif.title}", effectiveTitle).replace("{notif.text}", effectiveText) else effectiveText
            }
            PillRightDesign.NONE -> ""
            else -> effectiveText
        }

        val showBigIslandPic = leftDesign != PillLeftDesign.TEXT_ONLY &&
                leftDesign != PillLeftDesign.HIDDEN &&
                leftSlotConfig?.source != "HIDDEN"

        when {
            hasProgress -> {
                builder.setProgressBar(progressPercent, progressColor)
                val progressPicKey = if (showBigIslandPic) pillLeftPicKey else ""
                builder.setBigIslandProgressCircle(progressPicKey, pillLeftText, progressPercent, progressColor, true)
                builder.setSmallIslandCircularProgress(pillLeftPicKey, progressPercent, progressColor, isCCW = true)
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
                builder.setSmallIsland(pillLeftPicKey)
            }
            else -> {
                val leftBigIslandText = if (rightDesign == PillRightDesign.NONE) pillLeftText else ""
                val leftPicInfo = if (showBigIslandPic) PicInfo(1, pillLeftPicKey) else null
                builder.setBigIslandInfo(
                    left = ImageTextInfoLeft(1, leftPicInfo, TextInfo(leftBigIslandText, "")),
                    right = if (rightDesign != PillRightDesign.NONE) {
                        ImageTextInfoRight(1, PicInfo(1, hiddenKey), TextInfo(effectiveTitle, pillRightText))
                    } else {
                        ImageTextInfoRight(1, PicInfo(1, hiddenKey), TextInfo("", ""))
                    }
                )
                builder.setSmallIsland(pillLeftPicKey)
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
        builder.setIslandFirstFloat(config.isFloat == true)
        builder.setEnableFloat(config.isFloat == true)
        builder.setShowNotification(config.isShowShade == true)
        builder.setHideDeco(true)
        builder.setReopen(true)

        return HyperIslandData(builder.buildCustomExtras(), builder.buildJsonParam())
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
