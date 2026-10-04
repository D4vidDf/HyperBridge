package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.d4viddf.hyperbridge.ui.components.CustomColorBottomSheet

private val QUICK_COLORS = listOf(
    "#FFFFFF", "#000000", "#FF453A", "#FF9F0A", "#FFD60A",
    "#30D158", "#66D4CF", "#0A84FF", "#5E5CE6", "#BF5AF2"
)

@Composable
fun StudioColorField(
    label: String,
    colorHex: String,
    onColorHexChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    boundFormula: String? = null,
    onFormulaChange: ((String?) -> Unit)? = null,
    onRequestFormulaEditor: (() -> Unit)? = null
) {
    var showColorPicker by remember { mutableStateOf(false) }

    val parsedColor = runCatching {
        val clean = colorHex.removePrefix("#")
        when (clean.length) {
            6 -> Color(android.graphics.Color.parseColor("#$clean"))
            8 -> Color(android.graphics.Color.parseColor("#$clean"))
            else -> Color.Gray
        }
    }.getOrDefault(Color.Gray)

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Clickable swatch preview circle that opens Theme Creator CustomColorBottomSheet
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

            // Palette swatch chips with leading bottomsheet color picker button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { showColorPicker = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = stringResource(R.string.colors_dialog_title),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

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

    if (showColorPicker) {
        CustomColorBottomSheet(
            initialColor = parsedColor,
            onDismiss = { showColorPicker = false },
            onColorAdded = { newColor ->
                val clean = colorHex.removePrefix("#")
                val hex = if (clean.length == 8) {
                    val alpha = clean.take(2)
                    val rgb = String.format("%06X", 0xFFFFFF and newColor.toArgb())
                    "#$alpha$rgb"
                } else {
                    String.format("#%06X", 0xFFFFFF and newColor.toArgb())
                }
                onColorHexChange(hex)
                showColorPicker = false
            }
        )
    }
}
