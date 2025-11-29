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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

            // PREVIEW
            Text("Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            NavPreview(currentLeft, currentRight)

            Spacer(modifier = Modifier.height(32.dp))

            // CONFIGURATION
            Text(stringResource(R.string.group_configuration), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp), // Breathing room
            contentAlignment = Alignment.Center
        ) {
            // --- THE ISLAND (Compact Pill) ---
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(46.dp) // FIXED: Same height as camera roughly (Pill shape)
                    .clip(RoundedCornerShape(50)) // Fully rounded
                    .background(Color.Black)
            ) {
                // --- CAMERA CUTOUT ---
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(30.dp) // Camera Size
                        .clip(CircleShape)
                        .background(Color(0xFF1F1F1F))
                )

                // --- CONTENT ROW ---
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT ZONE
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // Fixed Direction Icon (Always on Left)
                        Icon(
                            imageVector = Icons.Default.TurnRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(Modifier.width(6.dp))

                        // User Configured Text
                        NavContentRenderer(left, Alignment.Start)
                    }

                    // Spacer for Camera
                    Spacer(modifier = Modifier.width(34.dp))

                    // RIGHT ZONE
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
    // Font sizes adjusted for the smaller pill height
    when (type) {
        NavContent.INSTRUCTION -> {
            Text(
                text = "Turn Right",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
        NavContent.DISTANCE -> {
            Text(
                text = "200m",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        NavContent.ETA -> {
            // FIXED: Time isn't colored green anymore, just white/grey
            Text(
                text = "10:30",
                color = Color.White,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp
            )
        }
        NavContent.DISTANCE_ETA -> {
            // Compact layout for double info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("200m", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(4.dp))
                Text("•", color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Text("10:30", color = Color.LightGray, fontSize = 13.sp)
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

fun getMockText(type: NavContent): String {
    return when(type) {
        NavContent.INSTRUCTION -> "Turn Right"
        NavContent.DISTANCE -> "200m"
        NavContent.ETA -> "10:30"
        NavContent.DISTANCE_ETA -> "200m • 10:30"
        NavContent.NONE -> ""
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