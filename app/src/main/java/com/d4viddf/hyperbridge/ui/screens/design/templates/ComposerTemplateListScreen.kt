package com.d4viddf.hyperbridge.ui.screens.design.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons.Rounded
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.composer.ComposerTemplateRepository
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.models.composer.ComposerTemplate
import com.d4viddf.hyperbridge.ui.components.EmptyState
import kotlinx.coroutines.launch

/**
 * Lists the user's Phase 4 island templates (#272), modeled on
 * [com.d4viddf.hyperbridge.ui.screens.design.SavedAppWidgetsScreen]'s list/FAB/edit structure.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerTemplateListScreen(
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    onEdit: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ComposerTemplateRepository(AppDatabase.getDatabase(context.applicationContext).composerTemplateDao()) }
    val templates by repository.templatesFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.design_section_templates), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNew) {
                Icon(Rounded.Add, contentDescription = stringResource(R.string.design_create_template))
            }
        }
    ) { padding ->
        if (templates.isEmpty()) {
            EmptyState(
                modifier = Modifier.padding(padding),
                title = stringResource(R.string.template_list_empty_title),
                description = stringResource(R.string.template_list_empty_subtitle)
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(templates, key = { it.id }) { template ->
                    TemplateRow(
                        template = template,
                        onClick = { onEdit(template.id) },
                        onToggleEnabled = { enabled ->
                            scope.launch { repository.save(template.copy(enabled = enabled)) }
                        },
                        onDelete = { scope.launch { repository.delete(template.id) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateRow(
    template: ComposerTemplate,
    onClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(template.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(template.definition.templateType.labelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = template.enabled,
                onCheckedChange = onToggleEnabled,
                modifier = Modifier.padding(end = 4.dp)
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.cd_delete_template),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
