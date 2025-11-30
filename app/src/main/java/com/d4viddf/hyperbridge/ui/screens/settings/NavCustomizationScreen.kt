package com.d4viddf.hyperbridge.ui.screens.settings

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.models.NavContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavCustomizationScreen(
    onBack: () -> Unit,
    packageName: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val globalLayout by preferences.globalNavLayoutFlow.collectAsState(initial = NavContent.DISTANCE_ETA to NavContent.INSTRUCTION)
    val appLayout by if (packageName != null) {
        preferences.getAppNavLayout(packageName).collectAsState(initial = null to null)
    } else {
        remember { mutableStateOf(null to null) }
    }

    val isGlobalMode = packageName == null
    val isUsingGlobalDefault = !isGlobalMode && appLayout.first == null
    val currentLeft = if (isGlobalMode || isUsingGlobalDefault) globalLayout.first else (appLayout.first ?: globalLayout.first)
    val currentRight = if (isGlobalMode || isUsingGlobalDefault) globalLayout.second else (appLayout.second ?: globalLayout.second)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_layout_title)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            Text("Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            NavPreview(currentLeft, currentRight)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                stringResource(R.string.group_configuration),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!isGlobalMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isUsingGlobalDefault) {
                                scope.launch { preferences.updateAppNavLayout(packageName, globalLayout.first, globalLayout.second) }
                            } else {
                                scope.launch { preferences.updateAppNavLayout(packageName, null, null) }
                            }
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isUsingGlobalDefault, onCheckedChange = null)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.use_global_default), style = MaterialTheme.typography.bodyLarge)
                }
            }

            val controlsEnabled = isGlobalMode || !isUsingGlobalDefault
            if (controlsEnabled) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp)) {
                        NavDropdown(
                            label = stringResource(R.string.left_content),
                            selected = currentLeft,
                            onSelect = { newLeft ->
                                scope.launch {
                                    if (isGlobalMode) preferences.setGlobalNavLayout(newLeft, currentRight)
                                    else preferences.updateAppNavLayout(packageName, newLeft, currentRight)
                                }
                            }
                        )
                        Spacer(Modifier.height(16.dp))
                        NavDropdown(
                            label = stringResource(R.string.right_content),
                            selected = currentRight,
                            onSelect = { newRight ->
                                scope.launch {
                                    if (isGlobalMode) preferences.setGlobalNavLayout(currentLeft, newRight)
                                    else preferences.updateAppNavLayout(packageName, currentLeft, newRight)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NavPreview(left: NavContent, right: NavContent) {
    val leftLabel = stringResource(getNavContentLabelRes(left))
    val rightLabel = stringResource(getNavContentLabelRes(right))
    val cd = stringResource(R.string.cd_nav_preview, leftLabel, rightLabel)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = cd }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            // --- UPDATED ISLAND SIZE ---
            Box(
                modifier = Modifier
                    .width(330.dp) // Increased from 280dp to 340dp to fit Dist+ETA
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
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT SIDE
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.TurnRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        NavContentRenderer(left, Alignment.Start)
                    }

                    // SPACER
                    Spacer(modifier = Modifier.width(32.dp))

                    // RIGHT SIDE
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        NavContentRenderer(right, Alignment.End)
                    }
                }
            }
        }
    }
}

@Composable
fun NavContentRenderer(type: NavContent, align: Alignment.Horizontal) {

    // Fade Logic: Only applies to long text (Instruction)
    val fadeBrush = if (type == NavContent.INSTRUCTION) {
        if (align == Alignment.Start) {
            Brush.horizontalGradient(0.85f to Color.White, 1.0f to Color.Transparent)
        } else {
            Brush.horizontalGradient(0.0f to Color.Transparent, 0.15f to Color.White)
        }
    } else null

    val textStyle = if (fadeBrush != null) {
        TextStyle(brush = fadeBrush, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    } else {
        TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }

    val timeStyle = TextStyle(color = Color.White, fontWeight = FontWeight.Normal, fontSize = 14.sp)

    when (type) {
        NavContent.INSTRUCTION -> {
            Text(
                text = stringResource(R.string.nav_preview_instruction),
                style = textStyle,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
        NavContent.DISTANCE -> {
            Text(
                text = stringResource(R.string.nav_preview_distance),
                style = textStyle // Solid White
            )
        }
        NavContent.ETA -> {
            Text(
                text = stringResource(R.string.nav_preview_time),
                style = timeStyle // Standard weight
            )
        }
        NavContent.DISTANCE_ETA -> {
            // Combined Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.nav_preview_distance), style = textStyle.copy(fontSize = 13.sp))
                Spacer(Modifier.width(4.dp))
                Text("•", color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.nav_preview_time), style = timeStyle.copy(fontSize = 13.sp))
            }
        }
        NavContent.NONE -> { /* Empty */ }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavDropdown(label: String, selected: NavContent, onSelect: (NavContent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = stringResource(getNavContentLabelRes(selected)),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                NavContent.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(getNavContentLabelRes(option))) },
                        onClick = { onSelect(option); expanded = false }
                    )
                }
            }
        }
    }
}

private fun getNavContentLabelRes(content: NavContent): Int {
    return when(content) {
        NavContent.INSTRUCTION -> R.string.nav_content_instruction
        NavContent.DISTANCE -> R.string.nav_content_distance
        NavContent.ETA -> R.string.nav_content_eta
        NavContent.DISTANCE_ETA -> R.string.nav_content_distance_eta
        NavContent.NONE -> R.string.nav_content_none
    }
}