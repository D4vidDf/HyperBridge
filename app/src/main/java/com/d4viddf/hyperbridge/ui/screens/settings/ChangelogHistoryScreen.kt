package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.util.parseBold // Import shared extension

data class VersionLog(val version: String, val titleRes: Int, val textRes: Int, val isLatest: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogHistoryScreen(onBack: () -> Unit) {
    // Define history here (Newest first)
    // Note: 0.2.0 is latest RELEASED version. 0.3.0 is DEV.
    val history = listOf(
        VersionLog("0.3.1", R.string.title_0_3_1, R.string.changelog_0_3_1, isLatest = true),
        VersionLog("0.3.0", R.string.title_0_3_0, R.string.changelog_0_3_0),
        VersionLog("0.2.0", R.string.title_0_2_0, R.string.changelog_0_2_0),
        VersionLog("0.1.0", R.string.title_0_1_0, R.string.changelog_0_1_0)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.version_history), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(history) { log ->
                ChangelogItem(log)
            }
        }
    }
}

@Composable
fun ChangelogItem(log: VersionLog) {
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if(log.isLatest) expanded = true }

    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "rotation")

    // Dynamic Colors
    val cardColor = if (log.isLatest) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (log.isLatest) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    // Accessibility
    val expandLabel = if (expanded) stringResource(R.string.cd_collapse_changelog) else stringResource(R.string.cd_expand_changelog)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = expandLabel) { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // --- HEADER ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Version Bubble
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "v${log.version}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title
                Text(
                    text = stringResource(log.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    modifier = Modifier.weight(1f)
                )

                // "NEW" Badge
                if (log.isLatest) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.badge_new),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.rotate(rotation)
                )
            }

            // --- CONTENT (BOLD PARSER) ---
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = contentColor.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Fix: Use parseBold()
                    Text(
                        text = stringResource(log.textRes).parseBold(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.9f),
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}