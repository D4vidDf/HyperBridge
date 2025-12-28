package com.d4viddf.hyperbridge.models.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * THE ROOT OBJECT
 * This represents the entire contents of a "theme.json" file.
 */
@Serializable
data class HyperTheme(
    val id: String,                 // Unique UUID (generated on import)
    val meta: ThemeMetadata,        // Name, Author, Version

    // --- 1. GLOBAL DEFAULTS ---
    val global: GlobalConfig,

    // --- 2. MODULE DEFAULTS ---
    @SerialName("default_actions")
    val defaultActions: Map<String, ActionConfig> = emptyMap(),

    @SerialName("default_progress")
    val defaultProgress: ProgressModule = ProgressModule(),

    @SerialName("default_navigation")
    val defaultNavigation: NavigationModule = NavigationModule(),

    // --- 3. PER-APP OVERRIDES ---
    // Key = Package Name (e.g., "com.whatsapp")
    val apps: Map<String, AppThemeOverride> = emptyMap(),

    // --- 4. LOGIC ENGINE ---
    val rules: List<ThemeRule> = emptyList()
)

@Serializable
data class ThemeMetadata(
    val name: String,
    val author: String,
    val version: Int = 1,
    val description: String = "",

    @SerialName("provider_package")
    val providerPackage: String? = null,

    @SerialName("provider_url")
    val providerUrl: String? = null
)

@Serializable
data class GlobalConfig(
    @SerialName("highlight_color")
    val highlightColor: String? = null,

    @SerialName("background_color")
    val backgroundColor: String? = null,

    @SerialName("text_color")
    val textColor: String? = "#FFFFFF",

    @SerialName("default_action_style")
    val defaultActionStyle: ActionConfig = ActionConfig()
)

@Serializable
data class AppThemeOverride(
    @SerialName("highlight_color")
    val highlightColor: String? = null,

    val actions: Map<String, ActionConfig>? = null,

    val progress: ProgressModule? = null,
    val navigation: NavigationModule? = null
)

// ==========================================
//              MODULES
// ==========================================

@Serializable
data class ActionConfig(
    val mode: ActionButtonMode = ActionButtonMode.ICON,

    val icon: ThemeResource? = null,

    @SerialName("background_color")
    val backgroundColor: String? = null,

    @SerialName("tint_color")
    val tintColor: String? = null,

    @SerialName("text_color")
    val textColor: String? = null
)

@Serializable
data class ProgressModule(
    @SerialName("active_color")
    val activeColor: String? = null,

    @SerialName("active_icon")
    val activeIcon: ThemeResource? = null,

    @SerialName("finished_color")
    val finishedColor: String? = null,

    @SerialName("finished_icon")
    val finishedIcon: ThemeResource? = null,

    @SerialName("show_percentage")
    val showPercentage: Boolean = true
)

/**
 * NAVIGATION MODULE
 * Defines Maps/Waze look.
 * Keys match the NavTranslator internals (pic_forward, pic_end)
 */
@Serializable
data class NavigationModule(
    @SerialName("progress_bar_color")
    val progressBarColor: String? = null,

    // Matched to "picForwardKey" in Translator
    @SerialName("pic_forward")
    val forwardIcon: ThemeResource? = null,

    // Matched to "picEndKey" in Translator
    @SerialName("pic_end")
    val endIcon: ThemeResource? = null,

    @SerialName("swap_sides")
    val swapSides: Boolean = false
)

// ==========================================
//           LOGIC & RESOURCES
// ==========================================

@Serializable
data class ThemeRule(
    val id: String,
    val comment: String? = null,
    val priority: Int = 100,

    val conditions: RuleConditions,

    @SerialName("target_layout")
    val targetLayout: String? = null,

    val overrides: AppThemeOverride? = null
)

@Serializable
data class RuleConditions(
    @SerialName("package_name")
    val packageName: String? = null,

    @SerialName("title_regex")
    val titleRegex: String? = null,

    @SerialName("text_regex")
    val textRegex: String? = null,

    @SerialName("external_state_key")
    val externalStateKey: String? = null,

    @SerialName("external_state_value")
    val externalStateValue: String? = null
)

@Serializable
data class ThemeResource(
    val type: ResourceType,
    val value: String
)

enum class ResourceType {
    PRESET_DRAWABLE,
    LOCAL_FILE,
    URI_CONTENT
}

enum class ActionButtonMode {
    ICON, TEXT, BOTH
}