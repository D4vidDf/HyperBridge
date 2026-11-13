package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.util.DocumentationUrls

data class VersionLog(val version: String, val titleRes: Int, val isLatest: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogHistoryScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    // Define history here (Newest first)
    val history = listOf(
        VersionLog("0.5.7", R.string.title_0_5_7, isLatest = true),
        VersionLog("0.5.6", R.string.title_0_5_6),
        VersionLog("0.5.5", R.string.title_0_5_5),
        VersionLog("0.5.2", R.string.title_0_5_2),
        VersionLog("0.5.1", R.string.title_0_5_1),
        VersionLog("0.5.0", R.string.title_0_5_0),
        VersionLog("0.4.2", R.string.title_0_4_2),
        VersionLog("0.4.0", R.string.title_0_4_0),
        VersionLog("0.3.1", R.string.title_0_3_1),
        VersionLog("0.3.0", R.string.title_0_3_0),
        VersionLog("0.2.0", R.string.title_0_2_0),
        VersionLog("0.1.0", R.string.title_0_1_0)
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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.changelog_notice),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(history) { log ->
                ChangelogItem(
                    log = log,
                    onClick = {
                        uriHandler.openUri(DocumentationUrls.getChangelogUrl())
                    }
                )
            }
        }
    }
}

@Composable
fun ChangelogItem(log: VersionLog, onClick: () -> Unit) {
    val cardColor = if (log.isLatest) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (log.isLatest) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = stringResource(R.string.view_full_changelog),
                tint = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}