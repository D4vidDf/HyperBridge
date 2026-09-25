package com.d4viddf.hyperbridge.ui.screens.design

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.widget.CustomWidgetRepository
import com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument
import com.d4viddf.hyperbridge.ui.components.EmptyState
import kotlinx.coroutines.launch

/** List screen for Phase 5 (#273) custom micro-widgets; thin sibling of [SavedAppWidgetsScreen] (Android AppWidgets, unrelated). */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SavedCustomWidgetsScreen(
    onBack: () -> Unit,
    onEditWidget: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { CustomWidgetRepository(context) }
    val scope = rememberCoroutineScope()

    var widgets by remember { mutableStateOf<List<CustomWidgetDocument>?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTrigger) {
        widgets = repository.getAvailableWidgets()
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    repository.installWidgetFromUri(uri)
                    refreshTrigger++
                } catch (_: Exception) { /* corrupt or invalid .hwidget: silently ignored, matches ThemeRepository's install error handling */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.studio_title)) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }) {
                        Icon(Icons.Outlined.FileUpload, stringResource(R.string.studio_import))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNew) {
                Icon(Icons.Default.Add, stringResource(R.string.studio_new_design))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                widgets == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() }
                widgets!!.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        title = stringResource(R.string.studio_empty),
                        description = "",
                        icon = Icons.Outlined.Widgets
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(widgets!!, key = { it.id }) { widget ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth().clickable { onEditWidget(widget.id) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(widget.meta.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            widget.boundPackage ?: stringResource(R.string.permanent_island_widget_none),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            repository.deleteWidget(widget.id)
                                            refreshTrigger++
                                        }
                                    }) {
                                        Icon(Icons.Outlined.Delete, stringResource(R.string.studio_delete_element))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
