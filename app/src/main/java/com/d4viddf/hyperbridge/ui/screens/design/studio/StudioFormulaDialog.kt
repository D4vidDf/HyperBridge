package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.widget.WidgetVariableEngine

/**
 * Categorized variable token descriptor for the Studio Formula Editor ($fx$).
 */
data class FormulaTokenItem(
    val token: String,
    val description: String,
    val sampleValue: String
)

enum class FormulaTokenCategory(val labelRes: Int, val icon: ImageVector) {
    NOTIFICATION(R.string.studio_formula_cat_notif, Icons.Rounded.Email),
    DEVICE(R.string.studio_formula_cat_device, Icons.Rounded.PhoneAndroid),
    THEME(R.string.studio_formula_cat_theme, Icons.Rounded.Palette),
    SOURCES(R.string.studio_formula_cat_sources, Icons.Rounded.Layers)
}

val FORMULA_TOKEN_CATALOG: Map<FormulaTokenCategory, List<FormulaTokenItem>> = mapOf(
    FormulaTokenCategory.NOTIFICATION to listOf(
        FormulaTokenItem("{notif.title}", "Title", "Calendar Event"),
        FormulaTokenItem("{notif.text}", "Content", "Review in 15 mins"),
        FormulaTokenItem("{notif.progress}", "Progress (0..100)", "64"),
        FormulaTokenItem("{notif.package}", "Package name", "com.google.android.calendar")
    ),
    FormulaTokenCategory.DEVICE to listOf(
        FormulaTokenItem("{device.battery}", "Battery %", "85"),
        FormulaTokenItem("{time.now}", "Current Time", "10:30")
    ),
    FormulaTokenCategory.THEME to listOf(
        FormulaTokenItem("{theme.primary}", "Theme Primary Color", "#3DDA82"),
        FormulaTokenItem("{theme.accent}", "Theme Accent Color", "#00E5FF"),
        FormulaTokenItem("{theme.surface}", "Theme Surface Color", "#1E1E1E")
    ),
    FormulaTokenCategory.SOURCES to listOf(
        FormulaTokenItem("{source.weather.temp}", "Weather Temp", "22°C"),
        FormulaTokenItem("{source.weather.text}", "Weather Condition", "Sunny"),
        FormulaTokenItem("{source.custom.icon}", "Source Icon ID", "weather_sun")
    )
)

/**
 * KWGT-style Modal Formula Editor dialog.
 * Supports cursor insertion, live preview against active scenario, and category browsing.
 */
@Composable
fun StudioFormulaDialog(
    propertyName: String,
    initialFormula: String,
    scenario: StudioPreviewScenario = StudioPreviewScenario.STANDARD,
    onDismiss: () -> Unit,
    onApply: (String?) -> Unit
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialFormula,
                selection = TextRange(initialFormula.length)
            )
        )
    }

    var selectedCategory by remember { mutableStateOf(FormulaTokenCategory.NOTIFICATION) }
    val engine = remember { WidgetVariableEngine() }

    val resolvedValue = remember(textFieldValue.text, scenario) {
        if (textFieldValue.text.isBlank()) ""
        else engine.resolve(textFieldValue.text, scenario.toVariableContext())
    }

    fun insertToken(token: String) {
        val currentText = textFieldValue.text
        val start = textFieldValue.selection.min
        val end = textFieldValue.selection.max
        val newText = currentText.replaceRange(start, end, token)
        val newCursorPos = start + token.length
        textFieldValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPos)
        )
    }

    val parsedPreviewColor: Color? = runCatching {
        if (resolvedValue.startsWith("#") && (resolvedValue.length == 7 || resolvedValue.length == 9)) {
            Color(android.graphics.Color.parseColor(resolvedValue))
        } else null
    }.getOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = stringResource(R.string.studio_formula_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = propertyName,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.studio_formula_dialog_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Monospace Formula Input
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    label = { Text(stringResource(R.string.studio_bind_formula)) },
                    placeholder = { Text("{token}") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    trailingIcon = {
                        if (textFieldValue.text.isNotEmpty()) {
                            IconButton(onClick = { textFieldValue = TextFieldValue("") }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Live Preview Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.studio_formula_preview_label,
                                stringResource(scenario.labelRes)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (parsedPreviewColor != null) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(parsedPreviewColor)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                            }
                            if (resolvedValue.isNotBlank()) {
                                Text(
                                    text = resolvedValue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.studio_formula_preview_empty),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Category selector chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormulaTokenCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = cat == selectedCategory,
                            onClick = { selectedCategory = cat },
                            leadingIcon = {
                                Icon(cat.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(stringResource(cat.labelRes)) }
                        )
                    }
                }

                // Variable token pills for selected category
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FORMULA_TOKEN_CATALOG[selectedCategory]?.forEach { item ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            onClick = { insertToken(item.token) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.token,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = item.sampleValue,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(textFieldValue.text.ifBlank { null }) }
            ) {
                Text(stringResource(R.string.studio_formula_apply))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initialFormula.isNotBlank()) {
                    TextButton(
                        onClick = { onApply(null) }
                    ) {
                        Text(
                            text = stringResource(R.string.studio_formula_clear),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
    )
}
