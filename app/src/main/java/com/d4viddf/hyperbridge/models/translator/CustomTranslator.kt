package com.d4viddf.hyperbridge.models.translator

import com.d4viddf.hyperbridge.models.theme.ColorMode
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.theme.ThemeResource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Global or app-specific Custom Translator configuration (.htrans).
 */
@Serializable
data class CustomTranslator(
    val id: String,
    val meta: TranslatorMetadata,
    @SerialName("target_scope") val targetScope: TargetScope = TargetScope.SPECIFIC_APPS,
    @SerialName("target_packages") val targetPackages: List<String> = emptyList(),
    @SerialName("target_notification_types") val targetNotificationTypes: List<String> = emptyList(),
    val priority: Int = 100,
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    val conditions: TranslatorConditions = TranslatorConditions(),
    @SerialName("theme_binding") val themeBinding: ThemeBinding = ThemeBinding(),
    @SerialName("engine_mode") val engineMode: EngineMode = EngineMode.INHERIT,
    @SerialName("behavior_override") val behaviorOverride: BehaviorOverride = BehaviorOverride(),
    @SerialName("data_extraction") val dataExtraction: DataExtractionConfig = DataExtractionConfig(),
    val presentation: PresentationConfig = PresentationConfig()
) {
    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = true
            coerceInputValues = true
        }

        fun fromJson(jsonString: String): Result<CustomTranslator> = runCatching {
            json.decodeFromString<CustomTranslator>(jsonString)
        }

        fun toJson(translator: CustomTranslator): String {
            return json.encodeToString(serializer(), translator)
        }
    }
}

@Serializable
enum class TargetScope {
    @SerialName("GLOBAL") GLOBAL,
    @SerialName("SPECIFIC_APPS") SPECIFIC_APPS,
    @SerialName("NOTIFICATION_TYPE") NOTIFICATION_TYPE
}

@Serializable
data class TranslatorMetadata(
    val name: String,
    val author: String = "Community",
    val version: Int = 1,
    val description: String = "",
    val icon: ThemeResource? = null,
    @SerialName("icon_name") val iconName: String = "AutoAwesome",
    @SerialName("share_link") val shareLink: String? = null
)

@Serializable
data class TranslatorConditions(
    @SerialName("title_regex") val titleRegex: String? = null,
    @SerialName("text_regex") val textRegex: String? = null,
    @SerialName("subtext_regex") val subtextRegex: String? = null,
    val category: String? = null,
    @SerialName("channel_id") val channelId: String? = null,
    @SerialName("has_extras") val hasExtras: List<String> = emptyList(),
    @SerialName("has_actions") val hasActions: Boolean? = null,
    @SerialName("has_progress") val hasProgress: Boolean? = null,
    @SerialName("type_specific_conditions") val typeSpecificConditions: TypeSpecificConditions? = null
)

@Serializable
data class TypeSpecificConditions(
    val messaging: MessagingConditions? = null,
    val media: MediaConditions? = null,
    val call: CallConditions? = null,
    val navigation: NavigationConditions? = null,
    val progress: ProgressConditions? = null
)

@Serializable
data class MessagingConditions(
    @SerialName("sender_name_regex") val senderNameRegex: String? = null,
    @SerialName("conversation_title_regex") val conversationTitleRegex: String? = null,
    @SerialName("is_group_conversation") val isGroupConversation: Boolean? = null
)

@Serializable
data class MediaConditions(
    @SerialName("artist_regex") val artistRegex: String? = null,
    @SerialName("album_regex") val albumRegex: String? = null,
    @SerialName("is_playing") val isPlaying: Boolean? = null
)

@Serializable
data class CallConditions(
    @SerialName("caller_name_regex") val callerNameRegex: String? = null,
    @SerialName("call_type") val callType: CallTypeCondition? = null
)

@Serializable
enum class CallTypeCondition {
    @SerialName("INCOMING") INCOMING,
    @SerialName("ONGOING") ONGOING,
    @SerialName("MISSED") MISSED
}

@Serializable
data class NavigationConditions(
    @SerialName("instruction_regex") val instructionRegex: String? = null,
    @SerialName("distance_regex") val distanceRegex: String? = null
)

@Serializable
data class ProgressConditions(
    @SerialName("min_progress_percent") val minProgressPercent: Int? = null,
    @SerialName("max_progress_percent") val maxProgressPercent: Int? = null
)

@Serializable
data class ThemeBinding(
    @SerialName("theme_id") val themeId: String = "active",
    @SerialName("fallback_to_active_if_missing") val fallbackToActiveIfMissing: Boolean = true,
    @SerialName("embedded_theme") val embeddedTheme: HyperTheme? = null,
    @SerialName("override_highlight_color") val overrideHighlightColor: String? = null,
    @SerialName("color_mode") val colorMode: ColorMode? = null,
    @SerialName("icon_shape_id") val iconShapeId: String = "circle",
    @SerialName("icon_padding_percent") val iconPaddingPercent: Int = 15
)

@Serializable
enum class EngineMode {
    @SerialName("INHERIT") INHERIT,
    @SerialName("CUSTOM_ISLAND") CUSTOM_ISLAND,
    @SerialName("NATIVE_LIVE_UPDATE") NATIVE_LIVE_UPDATE
}

@Serializable
data class BehaviorOverride(
    @SerialName("engine_mode") val engineMode: EngineMode = EngineMode.INHERIT,
    @SerialName("is_float") val isFloat: Boolean? = null,
    @SerialName("float_timeout_seconds") val floatTimeoutSeconds: Int? = null,
    @SerialName("timeout_seconds") val timeoutSeconds: Int? = null,
    @SerialName("is_show_shade") val isShowShade: Boolean? = null,
    @SerialName("remove_original_notification") val removeOriginalNotification: Boolean? = null,
    @SerialName("dismiss_with_original") val dismissWithOriginal: Boolean? = null,
    @SerialName("enable_inline_reply") val enableInlineReply: Boolean? = null
)

@Serializable
data class DataExtractionConfig(
    @SerialName("extract_progress_from_text") val extractProgressFromText: Boolean = false,
    @SerialName("progress_regex") val progressRegex: String? = null,
    @SerialName("custom_max_progress") val customMaxProgress: Int = 100,
    @SerialName("substitute_variables") val substituteVariables: Map<String, String> = emptyMap()
)

@Serializable
data class PresentationConfig(
    val mode: PresentationMode = PresentationMode.STANDARD,
    @SerialName("template_id") val templateId: String? = null,
    @SerialName("widget_id") val widgetId: String? = null,
    @SerialName("left_slot") val leftSlot: SlotConfig = SlotConfig(),
    @SerialName("text_slot") val textSlot: TextSlotConfig = TextSlotConfig(),
    @SerialName("progress_slot") val progressSlot: ProgressSlotConfig = ProgressSlotConfig(),
    @SerialName("action_slots") val actionSlots: List<ActionSlotConfig> = emptyList(),
    @SerialName("pill") val pill: CompactPillConfig = CompactPillConfig()
)

@Serializable
data class CompactPillConfig(
    @SerialName("left_design") val leftDesign: PillLeftDesign = PillLeftDesign.ICON_AND_TEXT,
    @SerialName("right_design") val rightDesign: PillRightDesign = PillRightDesign.AUTO
)

@Serializable
enum class PillLeftDesign {
    @SerialName("ICON_AND_TEXT") ICON_AND_TEXT,
    @SerialName("ICON_ONLY") ICON_ONLY,
    @SerialName("TEXT_ONLY") TEXT_ONLY,
    @SerialName("AVATAR") AVATAR,
    @SerialName("HIDDEN") HIDDEN
}

@Serializable
enum class PillRightDesign {
    @SerialName("AUTO") AUTO,
    @SerialName("PROGRESS_PERCENT") PROGRESS_PERCENT,
    @SerialName("TIMER") TIMER,
    @SerialName("HIGHLIGHT_TEXT") HIGHLIGHT_TEXT,
    @SerialName("NONE") NONE
}

@Serializable
enum class PresentationMode {
    @SerialName("STANDARD") STANDARD,
    @SerialName("TEMPLATE") TEMPLATE,
    @SerialName("WIDGET") WIDGET
}

@Serializable
data class SlotConfig(
    val source: String = "APP_ICON",
    @SerialName("custom_icon_res") val customIconRes: ThemeResource? = null,
    val shape: String? = null
)

@Serializable
data class TextSlotConfig(
    @SerialName("title_template") val titleTemplate: String = "{notif.title}",
    @SerialName("subtitle_template") val subtitleTemplate: String = "{notif.text}",
    @SerialName("highlight_text_template") val highlightTextTemplate: String? = null
)

@Serializable
data class ProgressSlotConfig(
    val type: ProgressSlotType = ProgressSlotType.NONE,
    @SerialName("progress_source") val progressSource: String = "AUTO_DETECT",
    @SerialName("color_source") val colorSource: String = "THEME_HIGHLIGHT",
    @SerialName("show_percentage") val showPercentage: Boolean = true
)

@Serializable
enum class ProgressSlotType {
    @SerialName("NONE") NONE,
    @SerialName("PROGRESS_BAR") PROGRESS_BAR,
    @SerialName("WAYPOINT") WAYPOINT,
    @SerialName("TIMER") TIMER
}

@Serializable
data class ActionSlotConfig(
    @SerialName("slot_position") val slotPosition: Int = 0,
    @SerialName("is_visible") val isVisible: Boolean = true,
    val source: ActionSource = ActionSource.NOTIFICATION_ACTION,
    @SerialName("action_matcher") val actionMatcher: ActionMatcher = ActionMatcher(),
    @SerialName("smart_action_type") val smartActionType: SmartActionType? = null,
    @SerialName("fallback_to_source") val fallbackToSource: ActionSource? = null,
    @SerialName("display_mode") val displayMode: ActionDisplayMode = ActionDisplayMode.ICON_ONLY,
    @SerialName("override_icon") val overrideIcon: ThemeResource? = null,
    @SerialName("custom_label") val customLabel: String? = null
)

@Serializable
enum class ActionSource {
    @SerialName("NOTIFICATION_ACTION") NOTIFICATION_ACTION,
    @SerialName("SMART_ACTION") SMART_ACTION,
    @SerialName("INLINE_REPLY") INLINE_REPLY,
    @SerialName("CUSTOM_BROADCAST") CUSTOM_BROADCAST
}

@Serializable
data class ActionMatcher(
    @SerialName("match_by") val matchBy: ActionMatchBy = ActionMatchBy.INDEX,
    @SerialName("action_index") val actionIndex: Int? = 0,
    @SerialName("title_regex") val titleRegex: String? = null
)

@Serializable
enum class ActionMatchBy {
    @SerialName("INDEX") INDEX,
    @SerialName("TITLE") TITLE
}

@Serializable
enum class SmartActionType {
    @SerialName("OTP_COPY") OTP_COPY,
    @SerialName("OPEN_URL") OPEN_URL,
    @SerialName("DIAL_NUMBER") DIAL_NUMBER,
    @SerialName("TRACK_PACKAGE") TRACK_PACKAGE
}

@Serializable
enum class ActionDisplayMode {
    @SerialName("ICON_ONLY") ICON_ONLY,
    @SerialName("TEXT_ONLY") TEXT_ONLY,
    @SerialName("ICON_AND_TEXT") ICON_AND_TEXT
}
