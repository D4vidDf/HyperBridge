package com.d4viddf.hyperbridge.models.colorscheme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Material 3 standard color roles.
 * [tokenKey] is the canonical key used in formula references (e.g. `{scheme.primary}`)
 * and token attributes (e.g. `@scheme:primary`).
 */
enum class ColorSchemeRole(val tokenKey: String, val label: String) {
    PRIMARY("primary", "Primary"),
    ON_PRIMARY("onPrimary", "On Primary"),
    PRIMARY_CONTAINER("primaryContainer", "Primary Container"),
    ON_PRIMARY_CONTAINER("onPrimaryContainer", "On Primary Container"),

    SECONDARY("secondary", "Secondary"),
    ON_SECONDARY("onSecondary", "On Secondary"),
    SECONDARY_CONTAINER("secondaryContainer", "Secondary Container"),
    ON_SECONDARY_CONTAINER("onSecondaryContainer", "On Secondary Container"),

    TERTIARY("tertiary", "Tertiary"),
    ON_TERTIARY("onTertiary", "On Tertiary"),
    TERTIARY_CONTAINER("tertiaryContainer", "Tertiary Container"),
    ON_TERTIARY_CONTAINER("onTertiaryContainer", "On Tertiary Container"),

    SURFACE("surface", "Surface"),
    ON_SURFACE("onSurface", "On Surface"),
    SURFACE_VARIANT("surfaceVariant", "Surface Variant"),
    ON_SURFACE_VARIANT("onSurfaceVariant", "On Surface Variant"),
    SURFACE_CONTAINER("surfaceContainer", "Surface Container"),
    SURFACE_CONTAINER_HIGH("surfaceContainerHigh", "Surface Container High"),
    SURFACE_CONTAINER_LOW("surfaceContainerLow", "Surface Container Low"),

    OUTLINE("outline", "Outline"),
    OUTLINE_VARIANT("outlineVariant", "Outline Variant"),
    ERROR("error", "Error"),
    ON_ERROR("onError", "On Error");

    companion object {
        private val KEY_MAP = entries.associateBy { it.tokenKey.lowercase() }
        private val SNAKE_MAP = entries.associateBy { it.name.lowercase() }

        fun fromKey(rawKey: String): ColorSchemeRole? {
            val normalized = rawKey.trim().replace("-", "_").lowercase()
            return SNAKE_MAP[normalized] ?: KEY_MAP[normalized.replace("_", "")]
        }
    }
}

/**
 * Defines which mechanism sources the color scheme.
 */
enum class ColorSchemeSourceType {
    @SerialName("phone_dynamic")
    PHONE_DYNAMIC,

    @SerialName("phone_expressive")
    PHONE_EXPRESSIVE,

    @SerialName("app_icon_expressive")
    APP_ICON_EXPRESSIVE,

    @SerialName("notification_media_expressive")
    NOTIFICATION_MEDIA_EXPRESSIVE,

    @SerialName("custom_preset")
    CUSTOM_PRESET
}

/**
 * Persistent configuration stored on a document or theme.
 */
@Serializable
data class ColorSchemeConfig(
    val sourceType: ColorSchemeSourceType = ColorSchemeSourceType.PHONE_DYNAMIC,
    val mediaSourceKey: String = "album_art", // "album_art", "picture", "avatar"
    val customSchemeId: String? = null,
    val isDarkMode: Boolean? = null
)

/**
 * Holds concrete resolved colors (hex string "#RRGGBB" or "#AARRGGBB" for each role).
 */
@Serializable
data class ColorSchemeDefinition(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val author: String = "User",
    val description: String = "",
    val isDark: Boolean = true,
    val roles: Map<String, String> = emptyMap()
) {
    fun getHex(role: ColorSchemeRole): String? {
        return roles[role.tokenKey]
            ?: roles[role.name.lowercase()]
            ?: roles[role.name]
    }
}
