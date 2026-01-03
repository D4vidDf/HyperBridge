package com.d4viddf.hyperbridge.ui.screens.theme.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Info // Added Import
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface // Added Import
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.theme.ActionButtonMode
import com.d4viddf.hyperbridge.models.theme.ActionConfig
import kotlin.text.ifEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionConfigSheet(
    initialKeyword: String?,
    initialConfig: ActionConfig?,
    onDismiss: () -> Unit,
    onSave: (String, ActionConfig) -> Unit
) {
    var keyword by remember { mutableStateOf(initialKeyword ?: "") }
    var mode by remember { mutableStateOf(initialConfig?.mode ?: ActionButtonMode.ICON) }
    var bgColor by remember { mutableStateOf(initialConfig?.backgroundColor ?: "#ffffff") }
    var tintColor by remember { mutableStateOf(initialConfig?.tintColor ?: "#000000") }

    val isEditing = initialKeyword != null

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.action_sheet_title), style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(16.dp))

            // --- WIP WARNING ---
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.action_wip_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- LIVE PREVIEW ---
            Text(stringResource(R.string.action_preview), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))

            // Simulation of Notification Action Button
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .widthIn(min = 48.dp)
                    .clip(if (mode == ActionButtonMode.ICON) CircleShape else RoundedCornerShape(24.dp))
                    .background(safeParseColor(bgColor))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mode != ActionButtonMode.TEXT) {
                        Icon(
                            imageVector = Icons.Default.Star, // Placeholder for selected icon
                            contentDescription = null,
                            tint = safeParseColor(tintColor),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (mode != ActionButtonMode.ICON) {
                        if (mode == ActionButtonMode.BOTH) Spacer(Modifier.width(8.dp))
                        Text(
                            text = keyword.ifEmpty { stringResource(R.string.action_default_preview_text) },
                            color = safeParseColor(tintColor),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- INPUTS ---

            // Keyword Input
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text(stringResource(R.string.action_label_keyword)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isEditing // Lock keyword if editing existing rule
            )

            Spacer(Modifier.height(16.dp))

            // Mode Selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = ActionButtonMode.entries.toTypedArray()
                modes.forEachIndexed { index, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { mode = m },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                    ) {
                        Text(
                            text = when (m) {
                                ActionButtonMode.ICON -> stringResource(R.string.action_mode_icon)
                                ActionButtonMode.TEXT -> stringResource(R.string.action_mode_text)
                                ActionButtonMode.BOTH -> stringResource(R.string.action_mode_both)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Color Inputs
            OutlinedTextField(
                value = bgColor,
                onValueChange = { bgColor = it },
                label = { Text(stringResource(R.string.action_label_bg_color)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Box(Modifier.size(24.dp).background(safeParseColor(bgColor), CircleShape).border(1.dp, Color.Gray, CircleShape))
                }
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = tintColor,
                onValueChange = { tintColor = it },
                label = { Text(stringResource(R.string.action_label_tint_color)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Box(Modifier.size(24.dp).background(safeParseColor(tintColor), CircleShape).border(1.dp, Color.Gray, CircleShape))
                }
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if(keyword.isNotBlank()) {
                        val newConfig = ActionConfig(
                            mode = mode,
                            backgroundColor = bgColor,
                            tintColor = tintColor
                            // Icon resource logic would go here
                        )
                        onSave(keyword, newConfig)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = keyword.isNotBlank()
            ) {
                Text(stringResource(R.string.creator_action_save))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// Helper for color parsing safely
fun safeParseColor(hex: String): Color {
    return try {
        Color(hex.toColorInt())
    } catch (e: Exception) {
        Color.Magenta // Error color
    }
}