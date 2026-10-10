package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.colorscheme.ColorSchemeRole
import com.d4viddf.hyperbridge.ui.screens.theme.safeParseColor

private val QUICK_COLORS = listOf(
    "#FFFFFF", "#000000", "#FF453A", "#FF9F0A", "#FFD60A",
    "#30D158", "#66D4CF", "#0A84FF", "#5E5CE6", "#BF5AF2"
)

private val COMMON_SCHEME_ROLES = listOf(
    ColorSchemeRole.PRIMARY,
    ColorSchemeRole.ON_PRIMARY,
    ColorSchemeRole.PRIMARY_CONTAINER,
    ColorSchemeRole.SECONDARY,
    ColorSchemeRole.ON_SECONDARY,
    ColorSchemeRole.TERTIARY,
    ColorSchemeRole.SURFACE,
    ColorSchemeRole.ON_SURFACE,
    ColorSchemeRole.SURFACE_CONTAINER,
    ColorSchemeRole.OUTLINE,
    ColorSchemeRole.ERROR
)

@Composable
fun StudioColorField(
    label: String,
    colorHex: String,
    onColorHexChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    boundFormula: String? = null,
    onFormulaChange: ((String?) -> Unit)? = null,
    onRequestFormulaEditor: (() -> Unit)? = null,
    colorScheme: com.d4viddf.hyperbridge.models.colorscheme.ColorSchemeDefinition? = null,
    globals: com.d4viddf.hyperbridge.models.widget.CustomWidgetGlobals? = null,
    onUpdateGlobals: ((com.d4viddf.hyperbridge.models.widget.CustomWidgetGlobals) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val effectiveColorScheme = remember(colorScheme, globals?.colorSchemeConfig, globals?.embeddedColorSchemeYaml) {
        if (globals != null) {
            com.d4viddf.hyperbridge.models.colorscheme.DynamicColorSchemeResolver.resolve(
                context = context,
                config = globals.colorSchemeConfig,
                embeddedYaml = globals.embeddedColorSchemeYaml,
                isDark = true
            )
        } else {
            colorScheme ?: com.d4viddf.hyperbridge.models.colorscheme.DynamicColorSchemeResolver.resolve(
                context = context,
                config = com.d4viddf.hyperbridge.models.colorscheme.ColorSchemeConfig(),
                isDark = true
            )
        }
    }

    val parsedColor = remember(colorHex, effectiveColorScheme) {
        val trimmed = colorHex.trim()
        if (trimmed.startsWith("@scheme:")) {
            safeParseColor(trimmed, effectiveColorScheme)
        } else {
            runCatching {
                val clean = trimmed.removePrefix("#")
                when (clean.length) {
                    6, 8 -> Color(android.graphics.Color.parseColor("#$clean"))
                    else -> Color.Gray
                }
            }.getOrDefault(Color.Gray)
        }
    }
    var showColorPicker by remember { mutableStateOf(false) }

    val isSchemeMode = colorHex.trim().startsWith("@scheme:")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (onFormulaChange != null || onRequestFormulaEditor != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            if (onRequestFormulaEditor != null) {
                                onRequestFormulaEditor()
                            } else if (boundFormula == null) {
                                onFormulaChange?.invoke("")
                            } else {
                                onFormulaChange?.invoke(null)
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Calculate,
                        contentDescription = stringResource(R.string.studio_bind_formula),
                        tint = if (boundFormula != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "fx",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (boundFormula != null) FontWeight.Bold else FontWeight.Normal,
                        color = if (boundFormula != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        if (boundFormula != null && onFormulaChange != null) {
            OutlinedTextField(
                value = boundFormula,
                onValueChange = onFormulaChange,
                placeholder = { Text("{theme.accent}") },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                singleLine = true,
                trailingIcon = if (onRequestFormulaEditor != null) {
                    {
                        IconButton(onClick = onRequestFormulaEditor) {
                            Icon(
                                imageVector = Icons.Rounded.Calculate,
                                contentDescription = "Edit formula",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            // Segmented mode selector: Color Scheme vs Custom Color
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isSchemeMode,
                    onClick = {
                        if (!isSchemeMode) {
                            val defaultRole = if (label.contains("background", ignoreCase = true) || label.contains("fondo", ignoreCase = true)) {
                                "surface"
                            } else if (label.contains("stroke", ignoreCase = true) || label.contains("borde", ignoreCase = true)) {
                                "outline"
                            } else {
                                "primary"
                            }
                            onColorHexChange("@scheme:$defaultRole")
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.ColorLens, null, modifier = Modifier.size(14.dp))
                    },
                    label = { Text(stringResource(R.string.studio_color_mode_scheme), style = MaterialTheme.typography.labelSmall) }
                )

                FilterChip(
                    selected = !isSchemeMode,
                    onClick = {
                        if (isSchemeMode) {
                            val hex = String.format("#%06X", 0xFFFFFF and parsedColor.toArgb())
                            onColorHexChange(hex)
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Palette, null, modifier = Modifier.size(14.dp))
                    },
                    label = { Text(stringResource(R.string.studio_color_mode_custom), style = MaterialTheme.typography.labelSmall) }
                )
            }

            Spacer(Modifier.height(8.dp))

            if (isSchemeMode) {
                val currentTokenKey = colorHex.trim().removePrefix("@scheme:").lowercase()
                val currentRole = ColorSchemeRole.fromKey(currentTokenKey)
                val currentRoleLabel = currentRole?.label ?: currentTokenKey.replaceFirstChar { it.uppercase() }
                val currentRoleHex = currentRole?.let { effectiveColorScheme.getHex(it) }
                    ?: effectiveColorScheme.roles[currentTokenKey]
                    ?: "#888888"

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showColorPicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentRoleLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "@scheme:$currentTokenKey ($currentRoleHex)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Quick Scheme Role Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    COMMON_SCHEME_ROLES.forEach { role ->
                        val token = "@scheme:${role.tokenKey}"
                        val isSelected = colorHex.equals(token, ignoreCase = true)
                        val roleColor = safeParseColor(token, effectiveColorScheme)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onColorHexChange(token) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(roleColor)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                            },
                            label = { Text(role.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }

                    OutlinedButton(
                        onClick = { showColorPicker = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(stringResource(R.string.studio_color_scheme_all_roles), style = MaterialTheme.typography.labelSmall)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(parsedColor)
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable { showColorPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        val luminance = runCatching {
                            ColorUtils.calculateLuminance(parsedColor.toArgb())
                        }.getOrDefault(0.5)
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = stringResource(R.string.colors_dialog_title),
                            tint = if (luminance > 0.5) Color.Black.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    OutlinedTextField(
                        value = colorHex,
                        onValueChange = onColorHexChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        trailingIcon = {
                            IconButton(onClick = { showColorPicker = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = stringResource(R.string.colors_dialog_title),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QUICK_COLORS.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (colorHex.equals(hex, ignoreCase = true)) 2.dp else 1.dp,
                                    color = if (colorHex.equals(hex, ignoreCase = true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                                .clickable { onColorHexChange(hex) }
                        )
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        com.d4viddf.hyperbridge.ui.components.SchemeOrCustomColorBottomSheet(
            initialColor = if (parsedColor.alpha == 0f) Color.DarkGray else parsedColor,
            currentColorTokenOrHex = colorHex,
            colorScheme = effectiveColorScheme,
            onDismiss = { showColorPicker = false },
            onSelectToken = { token ->
                onColorHexChange(token)
                showColorPicker = false
            },
            onColorAdded = { newColor ->
                val clean = colorHex.removePrefix("#")
                val hex = if (clean.length == 8) {
                    val alpha = clean.take(2)
                    val effectiveAlpha = if (alpha.equals("00", ignoreCase = true)) "FF" else alpha
                    val rgb = String.format("%06X", 0xFFFFFF and newColor.toArgb())
                    "#$effectiveAlpha$rgb"
                } else {
                    String.format("#%06X", 0xFFFFFF and newColor.toArgb())
                }
                onColorHexChange(hex)
                showColorPicker = false
            },
            globals = globals,
            onUpdateGlobals = onUpdateGlobals
        )
    }
}
