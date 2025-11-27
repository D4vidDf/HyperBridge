package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d4viddf.hyperbridge.R

// Data Model
data class VersionLog(
    val version: String,
    val titleRes: Int,
    val textRes: Int,
    val isLatest: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogHistoryScreen(onBack: () -> Unit) {
    // History Data
    val history = listOf(
        VersionLog("0.2.0", R.string.title_0_2_0, R.string.changelog_0_2_0, isLatest = true),
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

    // Styling based on Latest status
    val cardColor = if (log.isLatest) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (log.isLatest) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Version Number
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

                // --- CHANGED: TEXT BADGE ---
                if (log.isLatest) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary, // Pop color
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
                // ---------------------------

                // Arrow
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.rotate(rotation)
                )
            }

            // Content
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = contentColor.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

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

/**
 * Robust helper to parse <b>text</b> into Bold SpanStyles using Regex.
 */
@Composable
fun String.parseBold(): androidx.compose.ui.text.AnnotatedString {
    // 1. Fix newline escape characters often found in XML strings
    val rawText = this.replace("\\n", "\n")

    // 2. Use Regex to find content inside <b> tags
    val boldRegex = Regex("<b>(.*?)</b>")

    return buildAnnotatedString {
        var lastIndex = 0

        // Iterate over all matches
        boldRegex.findAll(rawText).forEach { match ->
            // Append normal text BEFORE the bold tag
            append(rawText.substring(lastIndex, match.range.first))

            // Append the BOLD content (Group 1 of the regex)
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }

            // Move index past this match
            lastIndex = match.range.last + 1
        }

        // Append any remaining normal text after the last match
        if (lastIndex < rawText.length) {
            append(rawText.substring(lastIndex))
        }
    }
}