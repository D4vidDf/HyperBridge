package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.ui.AppInfo
import com.d4viddf.hyperbridge.ui.AppListViewModel
import com.d4viddf.hyperbridge.ui.components.BlocklistEditor
import kotlinx.coroutines.launch

/**
 * Main Screen: Global Rules + Entry point to App List
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalBlocklistScreen(
    onBack: () -> Unit,
    onNavigateToAppList: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context) }

    val globalBlockedTerms by preferences.globalBlockedTermsFlow.collectAsState(initial = emptySet())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.blocked_terms)) },
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
                .padding(16.dp)
        ) {
            // 1. GLOBAL RULES TITLE
            Text(
                text = stringResource(R.string.global_rules),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp).semantics { heading() }
            )

            // GLOBAL EDITOR CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(20.dp)
            ) {
                BlocklistEditor(
                    terms = globalBlockedTerms,
                    onUpdate = { scope.launch { preferences.setGlobalBlockedTerms(it) } },
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // 2. APP RULES TITLE
            Text(
                text = stringResource(R.string.app_specific_rules),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp).semantics { heading() }
            )

            // NAVIGATION ENTRY CARD (Fixed design)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsItem(
                    icon = Icons.Default.Apps,
                    title = stringResource(R.string.app_specific_rules),
                    subtitle = stringResource(R.string.manage_app_rules_desc),
                    onClick = onNavigateToAppList
                )
            }
        }
    }
}

/**
 * Secondary Screen: List of Apps to configure
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocklistAppListScreen(
    onBack: () -> Unit,
    viewModel: AppListViewModel = viewModel()
) {
    val activeApps by viewModel.activeAppsState.collectAsState()
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_specific_rules)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp) // Add spacing between cards
        ) {
            items(activeApps, key = { it.packageName }) { app ->
                AppBlockItem(app = app, viewModel = viewModel) { selectedApp = app }
            }
        }
    }

    // Edit Dialog
    if (selectedApp != null) {
        AppBlocklistDialog(
            app = selectedApp!!,
            viewModel = viewModel,
            onDismiss = { selectedApp = null }
        )
    }
}

@Composable
fun AppBlockItem(
    app: AppInfo,
    viewModel: AppListViewModel,
    onClick: () -> Unit
) {
    // Collect specific rules for this app to show count
    val terms by viewModel.getAppBlockedTerms(app.packageName).collectAsState(initial = emptySet())
    val count = terms.size

    val subtitle = if (count > 0) {
        stringResource(R.string.blocked_terms_count, count)
    } else {
        stringResource(R.string.no_active_rules)
    }

    val subtitleColor = if (count > 0) MaterialTheme.colorScheme.error else Color.Gray

    // Wrapped in Card for consistent look with Priority List
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                bitmap = app.icon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun AppBlocklistDialog(
    app: AppInfo,
    viewModel: AppListViewModel,
    onDismiss: () -> Unit
) {
    val blockedTerms by viewModel.getAppBlockedTerms(app.packageName).collectAsState(initial = emptySet())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.name) },
        text = {
            BlocklistEditor(
                terms = blockedTerms,
                onUpdate = { viewModel.updateAppBlockedTerms(app.packageName, it) }
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        }
    )
}