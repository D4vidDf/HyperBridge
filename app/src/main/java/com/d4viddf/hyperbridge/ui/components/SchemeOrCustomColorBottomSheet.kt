package com.d4viddf.hyperbridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.d4viddf.hyperbridge.models.colorscheme.DynamicColorSchemeResolver
import com.d4viddf.hyperbridge.models.widget.CustomWidgetGlobals
import com.d4viddf.hyperbridge.ui.screens.theme.safeParseColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeOrCustomColorBottomSheet(
    initialColor: Color,
    currentColorTokenOrHex: String,
    colorScheme: ColorSchemeDefinition?,
    onDismiss: () -> Unit,
    onSelectToken: (token: String) -> Unit,
    onColorAdded: (Color) -> Unit,
    globals: CustomWidgetGlobals? = null,
    onUpdateGlobals: ((CustomWidgetGlobals) -> Unit)? = null,
    isSchemeMode: Boolean = currentColorTokenOrHex.trim().startsWith("@scheme:")
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val effectiveScheme = remember(colorScheme, globals?.colorSchemeConfig, globals?.embeddedColorSchemeYaml) {
        colorScheme
            ?: if (globals != null) {
                DynamicColorSchemeResolver.resolve(
                    context = context,
                    config = globals.colorSchemeConfig,
                    embeddedYaml = globals.embeddedColorSchemeYaml,
                    isDark = true
                )
            } else {
                DynamicColorSchemeResolver.resolve(
                    context = context,
                    config = com.d4viddf.hyperbridge.models.colorscheme.ColorSchemeConfig(),
                    isDark = true
                )
            }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
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
                    text = if (isSchemeMode) {
                        stringResource(R.string.studio_color_scheme_quick_tokens)
                    } else {
                        stringResource(R.string.colors_dialog_title)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (isSchemeMode) {
                val currentRoleKey = if (currentColorTokenOrHex.trim().startsWith("@scheme:")) {
                    currentColorTokenOrHex.trim().removePrefix("@scheme:").lowercase()
                } else null

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorSchemeRole.entries.forEach { role ->
                        val hex = effectiveScheme.getHex(role) ?: "#888888"
                        val parsed = safeParseColor(hex, effectiveScheme)
                        val isSelected = currentRoleKey.equals(role.tokenKey, ignoreCase = true)

                        Surface(
                            onClick = {
                                onSelectToken("@scheme:${role.tokenKey}")
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(parsed)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = role.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "@scheme:${role.tokenKey} ($hex)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null
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
