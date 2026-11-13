package com.d4viddf.hyperbridge.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.db.SourceAppEntity
import com.d4viddf.hyperbridge.data.widget.SourceRepository
import kotlinx.coroutines.launch

/**
 * Settings UI for the "island content sources" allow-list (#273 add-on): an app must appear here
 * (after broadcasting at least once) and be switched on before its `{source.<id>.text}` /
 * `{source.<id>.icon}` values are honored by any micro-widget.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceAppsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = androidx.compose.runtime.remember { SourceRepository(context) }
    val scope = rememberCoroutineScope()
    val apps by repository.allAppsFlow().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.source_apps_title)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                text = stringResource(R.string.source_apps_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )

            if (apps.isEmpty()) {
                Text(
                    text = stringResource(R.string.source_apps_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn {
                    items(apps, key = { it.packageName }) { app: SourceAppEntity ->
                        ListItem(
                            headlineContent = { Text(app.displayName) },
                            supportingContent = { Text(app.packageName) },
                            trailingContent = {
                                Switch(
                                    checked = app.allowed,
                                    onCheckedChange = { allowed ->
                                        scope.launch { repository.setAllowed(app.packageName, allowed) }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}
