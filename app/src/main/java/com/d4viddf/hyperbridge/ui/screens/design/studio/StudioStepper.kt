package com.d4viddf.hyperbridge.ui.screens.design.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R

/**
 * KWGT-style Stepper control with:
 *  ◀◀ (-10)  − (-1)  [ Value ]  + (+1)  ▶▶ (+10)
 * plus an optional formula toggle (fx) that turns the row into a dynamic {token} binding expression.
 */
@Composable
fun StudioStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    unitSuffix: String = "",
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    step: Int = 1,
    fastStep: Int = 10,
    boundFormula: String? = null,
    onFormulaChange: ((String?) -> Unit)? = null,
    onRequestFormulaEditor: (() -> Unit)? = null
) {
    var isEditingValue by remember { mutableStateOf(false) }
    var textInput by remember(value) { mutableStateOf(value.toString()) }

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
            // Formula editor row
            OutlinedTextField(
                value = boundFormula,
                onValueChange = onFormulaChange,
                placeholder = { Text("{token}") },
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
            // Numeric stepper row: ◀◀  −  [ value ]  +  ▶▶
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { onValueChange((value - fastStep).coerceIn(min, max)) },
                        modifier = Modifier.size(36.dp),
                        enabled = value > min
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FastRewind,
                            contentDescription = "-$fastStep",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { onValueChange((value - step).coerceIn(min, max)) },
                        modifier = Modifier.size(36.dp),
                        enabled = value > min
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = "-$step",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { isEditingValue = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unitSuffix.isNotBlank()) "$value $unitSuffix" else "$value",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }

                    IconButton(
                        onClick = { onValueChange((value + step).coerceIn(min, max)) },
                        modifier = Modifier.size(36.dp),
                        enabled = value < max
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "+$step",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { onValueChange((value + fastStep).coerceIn(min, max)) },
                        modifier = Modifier.size(36.dp),
                        enabled = value < max
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FastForward,
                            contentDescription = "+$fastStep",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (isEditingValue) {
        AlertDialog(
            onDismissRequest = { isEditingValue = false },
            title = { Text(label) },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        textInput.toIntOrNull()?.let { num ->
                            onValueChange(num.coerceIn(min, max))
                        }
                        isEditingValue = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingValue = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
