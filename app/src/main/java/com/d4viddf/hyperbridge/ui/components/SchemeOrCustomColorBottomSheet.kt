package com.d4viddf.hyperbridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.colorscheme.ColorSchemeDefinition
import com.d4viddf.hyperbridge.models.colorscheme.ColorSchemeRole
import com.d4viddf.hyperbridge.models.colorscheme.ColorSchemeSourceType
import com.d4viddf.hyperbridge.models.colorscheme.DynamicColorSchemeResolver
import com.d4viddf.hyperbridge.models.widget.CustomWidgetGlobals
import com.d4viddf.hyperbridge.ui.screens.theme.safeParseColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SchemeOrCustomColorBottomSheet(
    initialColor: Color,
    currentColorTokenOrHex: String,
    colorScheme: ColorSchemeDefinition?,
    onDismiss: () -> Unit,
    onSelectToken: (token: String) -> Unit,
    onColorAdded: (Color) -> Unit,
    globals: CustomWidgetGlobals? = null,
    onUpdateGlobals: ((CustomWidgetGlobals) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val effectiveScheme = remember(colorScheme, globals?.colorSchemeConfig, globals?.embeddedColorSchemeYaml) {
        if (globals != null) {
            DynamicColorSchemeResolver.resolve(
                context = context,
                config = globals.colorSchemeConfig,
                embeddedYaml = globals.embeddedColorSchemeYaml,
                isDark = true
            )
        } else {
            colorScheme ?: DynamicColorSchemeResolver.resolve(
                context = context,
                config = com.d4viddf.hyperbridge.models.colorscheme.ColorSchemeConfig(),
                isDark = true
            )
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember {
        mutableIntStateOf(if (currentColorTokenOrHex.trim().startsWith("@scheme:")) 0 else 1)
    }

    // Extract initial HSL for custom picker
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(initialColor.toArgb(), hsl)
    var hue by remember { mutableFloatStateOf(hsl[0]) }
    var saturation by remember { mutableFloatStateOf(hsl[1]) }
    var lightness by remember { mutableFloatStateOf(hsl[2]) }

    val customPreviewColor = remember(hue, saturation, lightness) {
        Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness)))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }

                Text(
                    text = stringResource(R.string.colors_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Tab Row: Palette Tokens vs Custom Color
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.studio_color_scheme_tab_tokens)) },
                    icon = { Icon(Icons.Rounded.ColorLens, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.studio_color_scheme_tab_custom)) },
                    icon = { Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(Modifier.height(20.dp))

            if (selectedTab == 0) {
                if (globals != null && onUpdateGlobals != null) {
                    Text(
                        text = stringResource(R.string.studio_color_scheme_source),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val sources = listOf(
                            ColorSchemeSourceType.PHONE_DYNAMIC to R.string.studio_color_scheme_phone_dynamic,
                            ColorSchemeSourceType.PHONE_EXPRESSIVE to R.string.studio_color_scheme_phone_expressive,
                            ColorSchemeSourceType.APP_ICON_EXPRESSIVE to R.string.studio_color_scheme_app_icon,
                            ColorSchemeSourceType.NOTIFICATION_MEDIA_EXPRESSIVE to R.string.studio_color_scheme_notif_media,
                            ColorSchemeSourceType.CUSTOM_PRESET to R.string.studio_color_scheme_custom_preset
                        )

                        sources.forEach { (sourceType, labelRes) ->
                            FilterChip(
                                selected = globals.colorSchemeConfig.sourceType == sourceType,
                                onClick = {
                                    val newCfg = globals.colorSchemeConfig.copy(sourceType = sourceType)
                                    onUpdateGlobals(globals.copy(colorSchemeConfig = newCfg))
                                },
                                label = { Text(stringResource(labelRes)) }
                            )
                        }
                    }

                    if (globals.colorSchemeConfig.sourceType == ColorSchemeSourceType.NOTIFICATION_MEDIA_EXPRESSIVE) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val mediaKeys = listOf(
                                "album_art" to R.string.studio_color_scheme_media_album_art,
                                "picture" to R.string.studio_color_scheme_media_picture,
                                "avatar" to R.string.studio_color_scheme_media_avatar
                            )
                            mediaKeys.forEach { (key, labelRes) ->
                                FilterChip(
                                    selected = globals.colorSchemeConfig.mediaSourceKey == key,
                                    onClick = {
                                        val newCfg = globals.colorSchemeConfig.copy(mediaSourceKey = key)
                                        onUpdateGlobals(globals.copy(colorSchemeConfig = newCfg))
                                    },
                                    label = { Text(stringResource(labelRes)) }
                                )
                            }
                        }
                    }

                    if (globals.colorSchemeConfig.sourceType == ColorSchemeSourceType.CUSTOM_PRESET) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DynamicColorSchemeResolver.BUILT_IN_PRESETS.forEach { preset ->
                                val isSelected = (globals.colorSchemeConfig.customSchemeId == preset.id) ||
                                    (globals.colorSchemeConfig.customSchemeId == null && preset.id == "default")
                                FilterChip(
                                    selected = isSelected && globals.embeddedColorSchemeYaml == null,
                                    onClick = {
                                        val newCfg = globals.colorSchemeConfig.copy(customSchemeId = preset.id)
                                        onUpdateGlobals(globals.copy(colorSchemeConfig = newCfg, embeddedColorSchemeYaml = null))
                                    },
                                    label = { Text(preset.name) }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                }

                // Tokens Grid
                val currentRoleKey = if (currentColorTokenOrHex.startsWith("@scheme:")) {
                    currentColorTokenOrHex.removePrefix("@scheme:")
                } else null

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ColorSchemeRole.entries.forEach { role ->
                        val hex = effectiveScheme.getHex(role) ?: "#888888"
                        val parsed = safeParseColor(hex, effectiveScheme)
                        val isSelected = currentRoleKey == role.tokenKey

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSelectToken("@scheme:${role.tokenKey}")
                                    onDismiss()
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(parsed)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            )
                            Column {
                                Text(
                                    text = role.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = hex,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                // Custom Color Slider Pickers
                Text(
                    text = stringResource(R.string.colors_label_hue),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
                GradientSlider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Red, Color.Yellow, Color.Green,
                            Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                        )
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.colors_label_saturation),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
                GradientSlider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..1f,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(ColorUtils.HSLToColor(floatArrayOf(hue, 0f, lightness))),
                            Color(ColorUtils.HSLToColor(floatArrayOf(hue, 1f, lightness)))
                        )
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.colors_label_lightness),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
                GradientSlider(
                    value = lightness,
                    onValueChange = { lightness = it },
                    valueRange = 0f..1f,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black,
                            Color(ColorUtils.HSLToColor(floatArrayOf(hue, saturation, 0.5f))),
                            Color.White
                        )
                    )
                )
                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(customPreviewColor)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = {
                            onColorAdded(customPreviewColor)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text(stringResource(R.string.done))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
