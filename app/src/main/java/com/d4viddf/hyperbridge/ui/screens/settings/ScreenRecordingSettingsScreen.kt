package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.ScreenRecordingLeftDesign
import com.d4viddf.hyperbridge.models.ScreenRecordingRightDesign
import com.d4viddf.hyperbridge.ui.components.formatSeconds
import com.d4viddf.hyperbridge.ui.components.timeoutSteps
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenRecordingSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val leftDesign by preferences.screenRecordingLeftDesignFlow.collectAsState(
        initial = ScreenRecordingLeftDesign.ICON_AND_TEXT
    )
    val rightDesign by preferences.screenRecordingRightDesignFlow.collectAsState(
        initial = ScreenRecordingRightDesign.TIMER
    )
    val savedTimeout by preferences.screenRecordingTimeoutFlow.collectAsState(
        initial = AppPreferences.SYSTEM_ISLAND_DEFAULT_TIMEOUT
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_recording_customization_title)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                stringResource(R.string.preview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            ScreenRecordingIslandPreview(left = leftDesign, right = rightDesign)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                stringResource(R.string.group_configuration),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(16.dp)) {
                    ScreenRecordingDropdown(
                        label = stringResource(R.string.left_content),
                        options = ScreenRecordingLeftDesign.entries,
                        selected = leftDesign,
                        labelFor = { option ->
                            when (option) {
                                ScreenRecordingLeftDesign.ICON_ONLY -> stringResource(R.string.screen_recording_left_option_icon_only)
                                ScreenRecordingLeftDesign.ICON_AND_TEXT -> stringResource(R.string.screen_recording_left_option_icon_and_text)
                                ScreenRecordingLeftDesign.TEXT_ONLY -> stringResource(R.string.screen_recording_left_option_text_only)
                            }
                        },
                        onSelect = { newLeft ->
                            scope.launch { preferences.setScreenRecordingLeftDesign(newLeft) }
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    ScreenRecordingDropdown(
                        label = stringResource(R.string.right_content),
                        options = ScreenRecordingRightDesign.entries,
                        selected = rightDesign,
                        labelFor = { option ->
                            when (option) {
                                ScreenRecordingRightDesign.TIMER -> stringResource(R.string.screen_recording_right_option_timer)
                                ScreenRecordingRightDesign.NONE -> stringResource(R.string.screen_recording_right_option_none)
                            }
                        },
                        onSelect = { newRight ->
                            scope.launch { preferences.setScreenRecordingRightDesign(newRight) }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Completion Auto-Hide Timeout Section
            val isTimeoutEnabled = savedTimeout > 0
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.auto_hide_island),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.screen_recording_saved_auto_hide_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isTimeoutEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    preferences.setScreenRecordingTimeout(if (enabled) 4 else 0)
                                }
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = isTimeoutEnabled,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = formatSeconds(savedTimeout),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            val currentIndex = timeoutSteps.indexOf(savedTimeout).coerceAtLeast(0).toFloat()
                            Slider(
                                value = currentIndex,
                                onValueChange = { index ->
                                    val selectedSeconds = timeoutSteps[index.toInt()]
                                    scope.launch { preferences.setScreenRecordingTimeout(selectedSeconds) }
                                },
                                valueRange = 0f..(timeoutSteps.size - 1).toFloat(),
                                steps = timeoutSteps.size - 2
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.good_to_know),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.screen_recording_customization_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ScreenRecordingIslandPreview(
    left: ScreenRecordingLeftDesign,
    right: ScreenRecordingRightDesign
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(330.dp)
                    .height(46.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black)
            ) {
                // Camera Cutout
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F1F))
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT SIDE
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        when (left) {
                            ScreenRecordingLeftDesign.ICON_ONLY -> {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFB382F))
                                )
                            }
                            ScreenRecordingLeftDesign.ICON_AND_TEXT -> {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFB382F))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.screen_recording_compact),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            ScreenRecordingLeftDesign.TEXT_ONLY -> {
                                Text(
                                    text = stringResource(R.string.screen_recording_compact),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // RIGHT SIDE
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        when (right) {
                            ScreenRecordingRightDesign.TIMER -> {
                                Text(
                                    text = "00:05",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            ScreenRecordingRightDesign.NONE -> {
                                // Empty right side
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ScreenRecordingDropdown(
    label: String,
    options: List<T>,
    selected: T,
    labelFor: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = labelFor(selected),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    .fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(labelFor(option)) },
                        onClick = { onSelect(option); expanded = false }
                    )
                }
            }
        }
    }
}
