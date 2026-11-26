package com.d4viddf.hyperbridge.models.widget

import android.graphics.Bitmap
import com.d4viddf.hyperbridge.data.widget.VariableContext
import com.d4viddf.hyperbridge.ui.components.island.IslandPreviewSample
import com.d4viddf.hyperbridge.ui.screens.design.studio.StudioPreviewScenario
import kotlinx.serialization.Serializable
import java.io.File

/**
 * User-configurable preview & share placeholder configuration for Studio Custom Widgets and Translators.
 *
 * Covers every dynamic token and condition supported by [com.d4viddf.hyperbridge.data.widget.WidgetVariableEngine]
 * and [com.d4viddf.hyperbridge.ui.components.island.HyperOsIslandPreview], allowing creators to test
 * real-world notification/media states on canvas and export showcase PNGs with custom media, icons, and texts.
 */
@Serializable
data class PreviewPlaceholderConfig(
    val presetName: String = StudioPreviewScenario.STANDARD.name,
    val isCustomized: Boolean = false,

    // 1. Notification & App Identity
    val notifTitle: String = "Calendar Event",
    val notifText: String = "Design Review in 15 mins",
    val subtext: String = "Room 4B",
    val sender: String = "Elena Rostova",
    val conversation: String = "Design Team",
    val appName: String = "Calendar",
    val notifPackage: String = "com.google.android.calendar",

    // 2. Images & Icons (file name in preview_placeholders/<id>/, "preset:<id>", or "app_icon")
    val pictureRef: String? = null,
    val avatarRef: String? = null,
    val smallIconRef: String? = null,

    // 3. Media Playback
    val mediaTrack: String = "Midnight City",
    val mediaArtist: String = "M83",
    val mediaPosition: String = "01:49",
    val mediaDuration: String = "04:03",
    val mediaProgress: Int = 45,
    val isMediaPlaying: Boolean = false,
    val mediaState: String = "PAUSED",

    // 4. Progress & Pill Timers
    val hasNotifProgress: Boolean = false,
    val notifProgress: Int = 64,
    val timerText: String = "04:25",
    val countdownText: String = "00:05",

    // 5. Actions & Conditions
    val notificationActionTitles: List<String> = listOf("Open", "Dismiss"),
    val hasInlineReply: Boolean = false,
    val smartActionTypes: Set<String> = emptySet(),

    // 6. Device, Time & Theme Fallbacks
    val timeNowFormatted: String = "10:30",
    val deviceBatteryPercent: Int = 85,
    val themePrimary: String = "#3DDA82",
    val themeAccent: String = "#00E5FF",
    val themeSurface: String = "#1E1E1E",

    // 7. Custom Content Sources ("<sourceId>.<field>" -> value)
    val sourceValues: Map<String, String> = emptyMap()
) {

    fun matchingScenarioOrNull(): StudioPreviewScenario? {
        return StudioPreviewScenario.entries.firstOrNull { it.name == presetName }
    }

    fun toVariableContext(
        pictureBitmap: Bitmap? = null,
        avatarBitmap: Bitmap? = null,
        smallIconBitmap: Bitmap? = null,
        placeholderDir: File? = null
    ): VariableContext {
        val effectiveProgress = if (hasNotifProgress) notifProgress.coerceIn(0, 100) else null
        return VariableContext(
            notifTitle = notifTitle,
            notifText = notifText,
            notifProgress = effectiveProgress,
            notifPackage = notifPackage.ifBlank { null },
            deviceBatteryPercent = deviceBatteryPercent.coerceIn(0, 100),
            timeNowFormatted = timeNowFormatted,
            themePrimary = themePrimary.ifBlank { "#3DDA82" },
            themeAccent = themeAccent.ifBlank { "#00E5FF" },
            themeSurface = themeSurface.ifBlank { "#1E1E1E" },
            mediaProgress = mediaProgress.coerceIn(0, 100),
            mediaTrack = mediaTrack,
            mediaArtist = mediaArtist,
            mediaDuration = mediaDuration,
            mediaPosition = mediaPosition,
            isMediaPlaying = isMediaPlaying,
            mediaState = if (mediaState.isNotBlank()) mediaState else if (isMediaPlaying) "PLAYING" else "PAUSED",
            notifAvatarBitmap = avatarBitmap,
            notifPictureBitmap = pictureBitmap,
            notifSmallIconBitmap = smallIconBitmap,
            notificationActionTitles = notificationActionTitles.filter { it.isNotBlank() },
            hasInlineReply = hasInlineReply,
            smartActionTypes = smartActionTypes,
            sourceLookup = { id, field ->
                val rawVal = sourceValues["$id.$field"]
                if (field == "icon" && !rawVal.isNullOrBlank() && placeholderDir != null) {
                    val f = File(placeholderDir, rawVal)
                    if (f.exists()) f.absolutePath else rawVal
                } else {
                    rawVal
                }
            }
        )
    }

    fun toIslandPreviewSample(
        pictureBitmap: Bitmap? = null,
        avatarBitmap: Bitmap? = null,
        smallIconBitmap: Bitmap? = null
    ): IslandPreviewSample {
        val effectivePct = if (hasNotifProgress) {
            notifProgress.coerceIn(0, 100)
        } else {
            mediaProgress.coerceIn(0, 100)
        }
        return IslandPreviewSample(
            title = notifTitle,
            text = notifText,
            subtext = subtext,
            sender = sender,
            conversation = conversation,
            app = appName.ifBlank { notifPackage.substringAfterLast('.').replaceFirstChar { it.uppercase() } },
            track = mediaTrack,
            artist = mediaArtist,
            progressPercent = effectivePct,
            timerText = timerText,
            countdownText = countdownText,
            clockText = if (presetName == StudioPreviewScenario.CUSTOM.name) {
                timeNowFormatted
            } else {
                timeNowFormatted.ifBlank { "12:00" }
            },
            pictureBitmap = pictureBitmap,
            avatarBitmap = avatarBitmap,
            smallIconBitmap = smallIconBitmap
        )
    }

    companion object {
        const val REF_APP_ICON = "app_icon"
        const val PRESET_ART_NEON = "preset:art_neon"
        const val PRESET_ART_CYBER = "preset:art_cyber"
        const val PRESET_ART_AURORA = "preset:art_aurora"
        const val PRESET_ART_SUNSET = "preset:art_sunset"

        const val PRESET_AVATAR_CORAL = "preset:avatar_coral"
        const val PRESET_AVATAR_SKY = "preset:avatar_sky"
        const val PRESET_AVATAR_MINT = "preset:avatar_mint"

        val BUILT_IN_ART_PRESETS = listOf(
            PRESET_ART_NEON,
            PRESET_ART_CYBER,
            PRESET_ART_AURORA,
            PRESET_ART_SUNSET
        )

        val BUILT_IN_AVATAR_PRESETS = listOf(
            PRESET_AVATAR_CORAL,
            PRESET_AVATAR_SKY,
            PRESET_AVATAR_MINT
        )

        val ALL_SMART_ACTION_TYPES = listOf("OTP", "URL", "PHONE", "TRACKING")

        fun fromScenario(scenario: StudioPreviewScenario): PreviewPlaceholderConfig = when (scenario) {
            StudioPreviewScenario.STANDARD -> PreviewPlaceholderConfig(
                presetName = scenario.name,
                isCustomized = false,
                notifTitle = "Calendar Event",
                notifText = "Design Review in 15 mins",
                subtext = "Room 4B",
                sender = "Alex",
                conversation = "Team Calendar",
                appName = "Calendar",
                notifPackage = "com.google.android.calendar",
                hasNotifProgress = false,
                notifProgress = 64,
                deviceBatteryPercent = 85,
                timeNowFormatted = "10:30",
                notificationActionTitles = listOf("Open", "Dismiss"),
                hasInlineReply = false,
                smartActionTypes = emptySet(),
                isMediaPlaying = false,
                mediaState = "PAUSED"
            )
            StudioPreviewScenario.MESSAGE -> PreviewPlaceholderConfig(
                presetName = scenario.name,
                isCustomized = false,
                notifTitle = "Elena Rostova",
                notifText = "Hey! Did you check the new island layout?",
                subtext = "2 unread messages",
                sender = "Elena",
                conversation = "HyperBridge Design",
                appName = "Telegram",
                notifPackage = "org.telegram.messenger",
                avatarRef = PRESET_AVATAR_CORAL,
                hasNotifProgress = false,
                notifProgress = 50,
                deviceBatteryPercent = 78,
                timeNowFormatted = "11:42",
                notificationActionTitles = listOf("Mark as read", "Reply"),
                hasInlineReply = true,
                smartActionTypes = setOf("URL"),
                isMediaPlaying = false,
                mediaState = "PAUSED"
            )
            StudioPreviewScenario.PROGRESS -> PreviewPlaceholderConfig(
                presetName = scenario.name,
                isCustomized = false,
                notifTitle = "Downloading update...",
                notifText = "HyperOS 2.0 Patch (64%)",
                subtext = "1.4 GB / 2.2 GB",
                sender = "Updater",
                conversation = "System Updates",
                appName = "Downloads",
                notifPackage = "com.android.providers.downloads",
                hasNotifProgress = true,
                notifProgress = 64,
                deviceBatteryPercent = 52,
                timeNowFormatted = "14:15",
                notificationActionTitles = listOf("Pause", "Cancel"),
                hasInlineReply = false,
                smartActionTypes = emptySet(),
                isMediaPlaying = false,
                mediaState = "PAUSED"
            )
            StudioPreviewScenario.OTP -> PreviewPlaceholderConfig(
                presetName = scenario.name,
                isCustomized = false,
                notifTitle = "Bank Security Code",
                notifText = "Your verification code is 849201. Valid for 5 min.",
                subtext = "849201",
                sender = "Bank Auth",
                conversation = "Security Alerts",
                appName = "Bank",
                notifPackage = "com.bank.app",
                hasNotifProgress = false,
                notifProgress = 100,
                deviceBatteryPercent = 90,
                timeNowFormatted = "16:05",
                notificationActionTitles = listOf("Copy code"),
                hasInlineReply = false,
                smartActionTypes = setOf("OTP"),
                isMediaPlaying = false,
                mediaState = "PAUSED"
            )
            StudioPreviewScenario.MEDIA -> PreviewPlaceholderConfig(
                presetName = scenario.name,
                isCustomized = false,
                notifTitle = "Midnight City",
                notifText = "M83 • Hurry Up, We're Dreaming",
                subtext = "Lossless • 24-bit",
                sender = "M83",
                conversation = "Now Playing",
                appName = "Spotify",
                notifPackage = "com.spotify.music",
                pictureRef = PRESET_ART_NEON,
                hasNotifProgress = true,
                notifProgress = 45,
                mediaProgress = 45,
                mediaTrack = "Midnight City",
                mediaArtist = "M83",
                mediaDuration = "04:03",
                mediaPosition = "01:49",
                isMediaPlaying = true,
                mediaState = "PLAYING",
                deviceBatteryPercent = 65,
                timeNowFormatted = "21:30",
                notificationActionTitles = listOf("Previous", "Play", "Next"),
                hasInlineReply = false,
                smartActionTypes = emptySet()
            )
            StudioPreviewScenario.CUSTOM -> PreviewPlaceholderConfig(
                presetName = scenario.name,
                isCustomized = false,
                notifTitle = "",
                notifText = "",
                subtext = "",
                sender = "",
                conversation = "",
                appName = "",
                notifPackage = "",
                pictureRef = null,
                avatarRef = null,
                smallIconRef = null,
                mediaTrack = "",
                mediaArtist = "",
                mediaPosition = "",
                mediaDuration = "",
                mediaProgress = 0,
                isMediaPlaying = false,
                mediaState = "PAUSED",
                hasNotifProgress = false,
                notifProgress = 0,
                timerText = "",
                countdownText = "",
                notificationActionTitles = emptyList(),
                hasInlineReply = false,
                smartActionTypes = emptySet(),
                timeNowFormatted = "",
                deviceBatteryPercent = 0,
                themePrimary = "",
                themeAccent = "",
                themeSurface = "",
                sourceValues = emptyMap()
            )
        }

        private val SOURCE_TOKEN_REGEX = Regex("""\{source\.([a-zA-Z0-9_-]+)\.([a-zA-Z0-9_-]+)\}""")

        /**
         * Scans a [CustomWidgetDocument] tree for any `{source.<id>.<field>}` tokens and
         * [ImageSource.SourceIcon] references so the placeholder editor can auto-populate inputs.
         */
        fun extractDocumentSourceKeys(doc: CustomWidgetDocument): List<Pair<String, String>> {
            val found = linkedSetOf<Pair<String, String>>()

            fun scanString(str: String?) {
                if (str.isNullOrBlank()) return
                SOURCE_TOKEN_REGEX.findAll(str).forEach { match ->
                    found.add(match.groupValues[1] to match.groupValues[2])
                }
            }

            fun scanCondition(cond: NodeCondition) {
                when (cond) {
                    is NodeCondition.NotBlank -> scanString(cond.template)
                    is NodeCondition.Matches -> scanString(cond.template)
                    is NodeCondition.Not -> scanCondition(cond.condition)
                    is NodeCondition.All -> cond.conditions.forEach(::scanCondition)
                    is NodeCondition.Any -> cond.conditions.forEach(::scanCondition)
                    else -> Unit
                }
            }

            fun scanNode(node: CustomWidgetNode) {
                node.bindings.values.forEach(::scanString)
                scanCondition(node.showIf)
                when (node) {
                    is TextNode -> scanString(node.template)
                    is ProgressNode -> scanString(node.valueTemplate)
                    is ButtonNode -> {
                        scanString(node.label)
                        scanString(node.conditionalLabel)
                        scanCondition(node.condition)
                    }
                    is ImageNode -> {
                        when (val src = node.source) {
                            is ImageSource.SourceIcon -> found.add(src.sourceId to "icon")
                            is ImageSource.AppIconOf -> scanString(src.packageTemplate)
                            is ImageSource.ContactAvatarOf -> scanString(src.numberOrNameTemplate)
                            else -> Unit
                        }
                    }
                    is ShapeNode -> Unit
                    is LayoutContainer -> {
                        if (node.backgroundImageSource is ImageSource.SourceIcon) {
                            found.add(node.backgroundImageSource.sourceId to "icon")
                        }
                        node.children.forEach(::scanNode)
                    }
                }
            }

            scanNode(doc.root)
            return found.toList()
        }
    }
}
