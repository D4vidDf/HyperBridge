package com.d4viddf.hyperbridge.models.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HyperTheme(
    val id: String,
    val meta: ThemeMetadata,
    val global: GlobalConfig,
    @SerialName("call_config") val callConfig: CallModule = CallModule(),
    @SerialName("default_actions") val defaultActions: Map<String, ActionConfig> = emptyMap(),
    @SerialName("default_progress") val defaultProgress: ProgressModule = ProgressModule(),
    @SerialName("default_navigation") val defaultNavigation: NavigationModule = NavigationModule(),
    val apps: Map<String, AppThemeOverride> = emptyMap(),
    val rules: List<ThemeRule> = emptyList()
)

@Serializable
data class ThemeMetadata(
    val name: String,
    val author: String,
    val version: Int = 1,
    val description: String = ""
)

@Serializable
data class GlobalConfig(
    @SerialName("highlight_color") val highlightColor: String? = null,
    @SerialName("background_color") val backgroundColor: String? = null,
    @SerialName("text_color") val textColor: String? = "#FFFFFF",
    @SerialName("use_app_colors") val useAppColors: Boolean = false,

    // SHAPE CONFIGURATION
    @SerialName("icon_shape_id") val iconShapeId: String = "circle", // "circle", "square", "squircle", "cookie", "flower"
    @SerialName("icon_padding_percent") val iconPaddingPercent: Int = 15
)

// ... (Rest of the models: CallModule, AppThemeOverride, etc. remain unchanged from previous version)
@Serializable data class CallModule(@SerialName("answer_icon") val answerIcon: ThemeResource? = null, @SerialName("decline_icon") val declineIcon: ThemeResource? = null, @SerialName("answer_color") val answerColor: String? = "#34C759", @SerialName("decline_color") val declineColor: String? = "#FF3B30")
@Serializable data class AppThemeOverride(@SerialName("highlight_color") val highlightColor: String? = null, val actions: Map<String, ActionConfig>? = null, val progress: ProgressModule? = null, val navigation: NavigationModule? = null)
@Serializable data class ActionConfig(val mode: ActionButtonMode = ActionButtonMode.ICON, val icon: ThemeResource? = null, @SerialName("background_color") val backgroundColor: String? = null, @SerialName("tint_color") val tintColor: String? = null, @SerialName("text_color") val textColor: String? = null)
@Serializable data class ProgressModule(@SerialName("active_color") val activeColor: String? = null, @SerialName("active_icon") val activeIcon: ThemeResource? = null, @SerialName("finished_color") val finishedColor: String? = null, @SerialName("finished_icon") val finishedIcon: ThemeResource? = null, @SerialName("show_percentage") val showPercentage: Boolean = true)
@Serializable data class NavigationModule(@SerialName("progress_bar_color") val progressBarColor: String? = null, @SerialName("pic_forward") val forwardIcon: ThemeResource? = null, @SerialName("pic_end") val endIcon: ThemeResource? = null, @SerialName("swap_sides") val swapSides: Boolean = false)
@Serializable data class ThemeRule(val id: String, val comment: String? = null, val priority: Int = 100, val conditions: RuleConditions, @SerialName("target_layout") val targetLayout: String? = null, val overrides: AppThemeOverride? = null)
@Serializable data class RuleConditions(@SerialName("package_name") val packageName: String? = null, @SerialName("title_regex") val titleRegex: String? = null, @SerialName("text_regex") val textRegex: String? = null, @SerialName("external_state_key") val externalStateKey: String? = null)
@Serializable data class ThemeResource(val type: ResourceType, val value: String)
enum class ResourceType { PRESET_DRAWABLE, LOCAL_FILE, URI_CONTENT }
enum class ActionButtonMode { ICON, TEXT, BOTH }