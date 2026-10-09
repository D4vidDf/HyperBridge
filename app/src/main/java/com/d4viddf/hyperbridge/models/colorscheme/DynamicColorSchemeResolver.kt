package com.d4viddf.hyperbridge.models.colorscheme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlin.math.roundToInt

/**
 * Resolves or extracts dynamic & expressive [ColorSchemeDefinition] instances from:
 * 1. System dynamic Monet (Phone Dynamic M3)
 * 2. System wallpaper expressive (Phone Expressive)
 * 3. App icon vibrant seeds (App Icon Expressive)
 * 4. Notification media bitmaps (Notification Media Expressive: album art, picture, avatar)
 * 5. Custom / user-defined presets
 */
object DynamicColorSchemeResolver {

    /**
     * Resolves the effective [ColorSchemeDefinition] according to [config].
     */
    fun resolve(
        context: Context,
        config: ColorSchemeConfig,
        embeddedYaml: String? = null,
        presetLookup: ((String) -> ColorSchemeDefinition?)? = null,
        appIconBitmap: Bitmap? = null,
        mediaBitmaps: Map<String, Bitmap?> = emptyMap(),
        isDark: Boolean = true
    ): ColorSchemeDefinition {
        // 1. If embedded YAML is present, parse it
        if (!embeddedYaml.isNullOrBlank()) {
            return runCatching { YamlColorSchemeParser.parse(embeddedYaml) }.getOrNull()
                ?: generateDefaultFallback(isDark)
        }

        // 2. Based on source type
        return when (config.sourceType) {
            ColorSchemeSourceType.PHONE_DYNAMIC -> {
                generatePhoneDynamicScheme(context, isDark)
            }
            ColorSchemeSourceType.PHONE_EXPRESSIVE -> {
                generatePhoneExpressiveScheme(context, isDark)
            }
            ColorSchemeSourceType.APP_ICON_EXPRESSIVE -> {
                if (appIconBitmap != null) {
                    generateFromBitmap(appIconBitmap, "App Icon Expressive", isDark)
                } else {
                    generatePhoneDynamicScheme(context, isDark)
                }
            }
            ColorSchemeSourceType.NOTIFICATION_MEDIA_EXPRESSIVE -> {
                val targetBitmap = mediaBitmaps[config.mediaSourceKey]
                    ?: mediaBitmaps["album_art"]
                    ?: mediaBitmaps["picture"]
                    ?: mediaBitmaps["avatar"]
                    ?: appIconBitmap

                if (targetBitmap != null) {
                    generateFromBitmap(targetBitmap, "Notification Media Expressive", isDark)
                } else {
                    generatePhoneDynamicScheme(context, isDark)
                }
            }
            ColorSchemeSourceType.CUSTOM_PRESET -> {
                val presetId = config.customSchemeId
                val preset = presetId?.let { id ->
                    presetLookup?.invoke(id) ?: BUILT_IN_PRESETS.firstOrNull { it.id == id }?.let { synthesizeExpressiveScheme(it.seedColor, it.name, isDark) }
                }
                preset ?: BUILT_IN_PRESETS.first().let { synthesizeExpressiveScheme(it.seedColor, it.name, isDark) }
            }
        }
    }

    data class Preset(val id: String, val name: String, val seedColor: Int)

    val BUILT_IN_PRESETS: List<Preset> = listOf(
        Preset("default", "HyperOS Classic", 0xFF0A84FF.toInt()),
        Preset("emerald", "Emerald Mint", 0xFF10B981.toInt()),
        Preset("sunset", "Sunset Orange", 0xFFFF5722.toInt()),
        Preset("ocean", "Ocean Breeze", 0xFF00BCD4.toInt()),
        Preset("violet", "Neon Violet", 0xFF8B5CF6.toInt()),
        Preset("monochrome", "Monochrome", 0xFF9E9E9E.toInt())
    )

    /**
     * Extracts seed color from bitmap and synthesizes an Expressive Material 3 tonal palette.
     */
    fun generateFromBitmap(bitmap: Bitmap, name: String, isDark: Boolean): ColorSchemeDefinition {
        val palette = runCatching { Palette.from(bitmap).generate() }.getOrNull()
        val seedColor = palette?.dominantSwatch?.rgb
            ?: palette?.vibrantSwatch?.rgb
            ?: palette?.mutedSwatch?.rgb
            ?: 0xFF0A84FF.toInt()

        return synthesizeExpressiveScheme(seedColor, name, isDark)
    }

    /**
     * Synthesizes an expressive Material 3 tonal color scheme from a single seed color.
     */
    fun synthesizeExpressiveScheme(seedColor: Int, name: String, isDark: Boolean): ColorSchemeDefinition {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(seedColor, hsl)

        val hue = hsl[0]
        val sat = hsl[1].coerceIn(0.4f, 1f)

        // Expressive offsets: Secondary shifts by +30°, Tertiary shifts by +60°
        val secHue = (hue + 30f) % 360f
        val tertHue = (hue + 60f) % 360f

        val roles = mutableMapOf<String, String>()

        if (isDark) {
            val primaryInt = hslToColor(hue, sat, 0.75f)
            val onPrimaryInt = hslToColor(hue, sat, 0.15f)
            val primaryContainerInt = hslToColor(hue, sat, 0.25f)
            val onPrimaryContainerInt = hslToColor(hue, sat, 0.88f)

            val secInt = hslToColor(secHue, (sat * 0.75f).coerceIn(0.2f, 0.8f), 0.70f)
            val onSecInt = hslToColor(secHue, sat, 0.15f)
            val secContainerInt = hslToColor(secHue, (sat * 0.6f).coerceIn(0.2f, 0.7f), 0.25f)
            val onSecContainerInt = hslToColor(secHue, sat, 0.88f)

            val tertInt = hslToColor(tertHue, (sat * 0.85f).coerceIn(0.3f, 0.9f), 0.75f)
            val onTertInt = hslToColor(tertHue, sat, 0.15f)
            val tertContainerInt = hslToColor(tertHue, (sat * 0.7f).coerceIn(0.2f, 0.8f), 0.25f)
            val onTertContainerInt = hslToColor(tertHue, sat, 0.90f)

            val surfaceInt = hslToColor(hue, 0.08f, 0.07f)
            val onSurfaceInt = hslToColor(hue, 0.05f, 0.90f)
            val surfaceVariantInt = hslToColor(hue, 0.12f, 0.18f)
            val onSurfaceVariantInt = hslToColor(hue, 0.08f, 0.75f)
            val surfaceContainerInt = hslToColor(hue, 0.09f, 0.12f)
            val surfaceContainerHighInt = hslToColor(hue, 0.10f, 0.16f)
            val surfaceContainerLowInt = hslToColor(hue, 0.08f, 0.09f)

            val outlineInt = hslToColor(hue, 0.10f, 0.55f)
            val outlineVariantInt = hslToColor(hue, 0.10f, 0.30f)

            roles[ColorSchemeRole.PRIMARY.tokenKey] = toHex(primaryInt)
            roles[ColorSchemeRole.ON_PRIMARY.tokenKey] = toHex(onPrimaryInt)
            roles[ColorSchemeRole.PRIMARY_CONTAINER.tokenKey] = toHex(primaryContainerInt)
            roles[ColorSchemeRole.ON_PRIMARY_CONTAINER.tokenKey] = toHex(onPrimaryContainerInt)

            roles[ColorSchemeRole.SECONDARY.tokenKey] = toHex(secInt)
            roles[ColorSchemeRole.ON_SECONDARY.tokenKey] = toHex(onSecInt)
            roles[ColorSchemeRole.SECONDARY_CONTAINER.tokenKey] = toHex(secContainerInt)
            roles[ColorSchemeRole.ON_SECONDARY_CONTAINER.tokenKey] = toHex(onSecContainerInt)

            roles[ColorSchemeRole.TERTIARY.tokenKey] = toHex(tertInt)
            roles[ColorSchemeRole.ON_TERTIARY.tokenKey] = toHex(onTertInt)
            roles[ColorSchemeRole.TERTIARY_CONTAINER.tokenKey] = toHex(tertContainerInt)
            roles[ColorSchemeRole.ON_TERTIARY_CONTAINER.tokenKey] = toHex(onTertContainerInt)

            roles[ColorSchemeRole.SURFACE.tokenKey] = toHex(surfaceInt)
            roles[ColorSchemeRole.ON_SURFACE.tokenKey] = toHex(onSurfaceInt)
            roles[ColorSchemeRole.SURFACE_VARIANT.tokenKey] = toHex(surfaceVariantInt)
            roles[ColorSchemeRole.ON_SURFACE_VARIANT.tokenKey] = toHex(onSurfaceVariantInt)
            roles[ColorSchemeRole.SURFACE_CONTAINER.tokenKey] = toHex(surfaceContainerInt)
            roles[ColorSchemeRole.SURFACE_CONTAINER_HIGH.tokenKey] = toHex(surfaceContainerHighInt)
            roles[ColorSchemeRole.SURFACE_CONTAINER_LOW.tokenKey] = toHex(surfaceContainerLowInt)

            roles[ColorSchemeRole.OUTLINE.tokenKey] = toHex(outlineInt)
            roles[ColorSchemeRole.OUTLINE_VARIANT.tokenKey] = toHex(outlineVariantInt)
            roles[ColorSchemeRole.ERROR.tokenKey] = "#FFB4AB"
            roles[ColorSchemeRole.ON_ERROR.tokenKey] = "#690005"
        } else {
            val primaryInt = hslToColor(hue, sat, 0.35f)
            val onPrimaryInt = Color.WHITE
            val primaryContainerInt = hslToColor(hue, sat, 0.88f)
            val onPrimaryContainerInt = hslToColor(hue, sat, 0.10f)

            val secInt = hslToColor(secHue, (sat * 0.7f).coerceIn(0.2f, 0.7f), 0.40f)
            val onSecInt = Color.WHITE
            val secContainerInt = hslToColor(secHue, (sat * 0.6f).coerceIn(0.2f, 0.6f), 0.88f)
            val onSecContainerInt = hslToColor(secHue, sat, 0.12f)

            val tertInt = hslToColor(tertHue, (sat * 0.8f).coerceIn(0.2f, 0.8f), 0.38f)
            val onTertInt = Color.WHITE
            val tertContainerInt = hslToColor(tertHue, (sat * 0.7f).coerceIn(0.2f, 0.7f), 0.88f)
            val onTertContainerInt = hslToColor(tertHue, sat, 0.10f)

            val surfaceInt = hslToColor(hue, 0.05f, 0.98f)
            val onSurfaceInt = hslToColor(hue, 0.05f, 0.10f)
            val surfaceVariantInt = hslToColor(hue, 0.10f, 0.90f)
            val onSurfaceVariantInt = hslToColor(hue, 0.08f, 0.30f)
            val surfaceContainerInt = hslToColor(hue, 0.06f, 0.94f)
            val surfaceContainerHighInt = hslToColor(hue, 0.07f, 0.91f)
            val surfaceContainerLowInt = hslToColor(hue, 0.05f, 0.96f)

            val outlineInt = hslToColor(hue, 0.08f, 0.50f)
            val outlineVariantInt = hslToColor(hue, 0.08f, 0.75f)

            roles[ColorSchemeRole.PRIMARY.tokenKey] = toHex(primaryInt)
            roles[ColorSchemeRole.ON_PRIMARY.tokenKey] = toHex(onPrimaryInt)
            roles[ColorSchemeRole.PRIMARY_CONTAINER.tokenKey] = toHex(primaryContainerInt)
            roles[ColorSchemeRole.ON_PRIMARY_CONTAINER.tokenKey] = toHex(onPrimaryContainerInt)

            roles[ColorSchemeRole.SECONDARY.tokenKey] = toHex(secInt)
            roles[ColorSchemeRole.ON_SECONDARY.tokenKey] = toHex(onSecInt)
            roles[ColorSchemeRole.SECONDARY_CONTAINER.tokenKey] = toHex(secContainerInt)
            roles[ColorSchemeRole.ON_SECONDARY_CONTAINER.tokenKey] = toHex(onSecContainerInt)

            roles[ColorSchemeRole.TERTIARY.tokenKey] = toHex(tertInt)
            roles[ColorSchemeRole.ON_TERTIARY.tokenKey] = toHex(onTertInt)
            roles[ColorSchemeRole.TERTIARY_CONTAINER.tokenKey] = toHex(tertContainerInt)
            roles[ColorSchemeRole.ON_TERTIARY_CONTAINER.tokenKey] = toHex(onTertContainerInt)

            roles[ColorSchemeRole.SURFACE.tokenKey] = toHex(surfaceInt)
            roles[ColorSchemeRole.ON_SURFACE.tokenKey] = toHex(onSurfaceInt)
            roles[ColorSchemeRole.SURFACE_VARIANT.tokenKey] = toHex(surfaceVariantInt)
            roles[ColorSchemeRole.ON_SURFACE_VARIANT.tokenKey] = toHex(onSurfaceVariantInt)
            roles[ColorSchemeRole.SURFACE_CONTAINER.tokenKey] = toHex(surfaceContainerInt)
            roles[ColorSchemeRole.SURFACE_CONTAINER_HIGH.tokenKey] = toHex(surfaceContainerHighInt)
            roles[ColorSchemeRole.SURFACE_CONTAINER_LOW.tokenKey] = toHex(surfaceContainerLowInt)

            roles[ColorSchemeRole.OUTLINE.tokenKey] = toHex(outlineInt)
            roles[ColorSchemeRole.OUTLINE_VARIANT.tokenKey] = toHex(outlineVariantInt)
            roles[ColorSchemeRole.ERROR.tokenKey] = "#BA1A1A"
            roles[ColorSchemeRole.ON_ERROR.tokenKey] = "#FFFFFF"
        }

        return ColorSchemeDefinition(
            id = "expressive_${name.lowercase().replace(" ", "_")}",
            name = name,
            author = "System",
            description = "Expressive palette synthesized from seed",
            isDark = isDark,
            roles = roles
        )
    }

    /**
     * Resolves standard system dynamic Monet scheme.
     */
    fun generatePhoneDynamicScheme(context: Context, isDark: Boolean): ColorSchemeDefinition {
        // Try Android 12+ system dynamic resource colors if available
        val systemAccent = runCatching {
            val resId = android.R.color.system_accent1_500
            context.getColor(resId)
        }.getOrNull() ?: 0xFF3DDA82.toInt()

        return synthesizeExpressiveScheme(systemAccent, "Phone Dynamic", isDark)
    }

    /**
     * Resolves phone expressive scheme with boosted chromaticity.
     */
    fun generatePhoneExpressiveScheme(context: Context, isDark: Boolean): ColorSchemeDefinition {
        val systemAccent = runCatching {
            val resId = android.R.color.system_accent1_400
            context.getColor(resId)
        }.getOrNull() ?: 0xFF0A84FF.toInt()

        return synthesizeExpressiveScheme(systemAccent, "Phone Expressive", isDark)
    }

    /**
     * Standard dark/light fallback.
     */
    fun generateDefaultFallback(isDark: Boolean): ColorSchemeDefinition {
        val primarySeed = if (isDark) 0xFF3DDA82.toInt() else 0xFF006C4C.toInt()
        return synthesizeExpressiveScheme(primarySeed, "HyperBridge Default", isDark)
    }

    private fun hslToColor(h: Float, s: Float, l: Float): Int {
        val outHsl = floatArrayOf(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
        return ColorUtils.HSLToColor(outHsl)
    }

    private fun toHex(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }
}
