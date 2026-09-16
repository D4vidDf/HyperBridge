package com.d4viddf.hyperbridge.ui.screens.design.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.composer.ComposerTemplateRepository
import com.d4viddf.hyperbridge.data.db.AppDatabase
import com.d4viddf.hyperbridge.models.composer.ComposerTemplate
import com.d4viddf.hyperbridge.models.composer.ComposerTemplateType
import com.d4viddf.hyperbridge.models.composer.IslandTemplateDefinition
import com.d4viddf.hyperbridge.models.composer.ProgressSlotConfig
import com.d4viddf.hyperbridge.models.composer.TemplateRule
import com.d4viddf.hyperbridge.ui.AppListViewModel
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The Phase 4 (#272) WYSIWYG island template editor: a template-type switcher, a live preview
 * bound to the in-progress definition, the slot editors gated by that type's capabilities, and
 * the rule binder — then Save persists the result via [ComposerTemplateRepository].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandComposerScreen(
    templateId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    appListViewModel: AppListViewModel = viewModel()
) {
    val context = LocalContext.current
    val repository = remember { ComposerTemplateRepository(AppDatabase.getDatabase(context.applicationContext).composerTemplateDao()) }
    val scope = rememberCoroutineScope()

    val libraryApps by appListViewModel.libraryAppsState.collectAsState()
    val activeApps by appListViewModel.activeAppsState.collectAsState()
    val allApps = remember(libraryApps, activeApps) { (activeApps + libraryApps).distinctBy { it.packageName } }

    var id by remember { mutableStateOf(templateId ?: UUID.randomUUID().toString()) }
    var name by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf(IslandTemplateDefinition()) }
    var rule by remember { mutableStateOf(TemplateRule()) }
    var enabled by remember { mutableStateOf(true) }
    var createdAt by remember { mutableStateOf(0L) }
    val defaultName = stringResource(R.string.template_composer_title_new)

    LaunchedEffect(templateId) {
        if (templateId != null) {
            repository.getById(templateId)?.let { existing ->
                id = existing.id
                name = existing.name
                definition = existing.definition
                rule = existing.rule
                enabled = existing.enabled
                createdAt = existing.createdAt
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (templateId != null) stringResource(R.string.template_composer_title)
                        else stringResource(R.string.template_composer_title_new),
                        fontWeight = FontWeight.Bold
                    )
                },
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
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                Button(
                    onClick = {
                        scope.launch {
                            repository.save(
                                ComposerTemplate(
                                    id = id,
                                    name = name.ifBlank { defaultName },
                                    definition = definition,
                                    rule = rule,
                                    enabled = enabled,
                                    createdAt = createdAt
                                )
                            )
                            onSaved()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.template_composer_save))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.template_composer_name_label)) },
                placeholder = { Text(stringResource(R.string.template_composer_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ScrollableTabRow(selectedTabIndex = definition.templateType.ordinal, edgePadding = 0.dp) {
                ComposerTemplateType.entries.forEach { type ->
                    Tab(
                        selected = definition.templateType == type,
                        onClick = {
                            // Switching type resets the slots that type doesn't support, so a
                            // half-configured progress bar can't leak into a template that hides it.
                            definition = defaultDefinitionFor(type).copy(
                                leftGraphic = definition.leftGraphic,
                                text = definition.text,
                                highlightColor = definition.highlightColor,
                                progress = if (type.supportsProgress) definition.progress else ProgressSlotConfig(),
                                buttons = if (type.supportsButtons) definition.buttons else emptyList()
                            )
                        },
                        text = { Text(stringResource(type.labelRes)) }
                    )
                }
            }

            Text(stringResource(R.string.template_composer_preview_title), style = MaterialTheme.typography.labelLarge)
            IslandTemplatePreview(definition = definition, sample = PreviewSampleData())

            HorizontalDivider()
            Text(stringResource(R.string.template_composer_section_left_graphic), style = MaterialTheme.typography.titleMedium)
            LeftGraphicPicker(value = definition.leftGraphic, onChange = { definition = definition.copy(leftGraphic = it) })

            HorizontalDivider()
            Text(stringResource(R.string.template_composer_section_text), style = MaterialTheme.typography.titleMedium)
            TextSlotEditor(value = definition.text, onChange = { definition = definition.copy(text = it) })

            if (definition.templateType.supportsProgress) {
                HorizontalDivider()
                Text(stringResource(R.string.template_composer_section_progress), style = MaterialTheme.typography.titleMedium)
                ProgressSlotEditor(value = definition.progress, onChange = { definition = definition.copy(progress = it) })
            }

            if (definition.templateType.supportsButtons) {
                HorizontalDivider()
                Text(stringResource(R.string.template_composer_section_buttons), style = MaterialTheme.typography.titleMedium)
                ActionButtonListEditor(
                    value = definition.buttons,
                    maxButtons = 3,
                    availableNativeActions = 3,
                    onChange = { definition = definition.copy(buttons = it) }
                )
            }

            HorizontalDivider()
            Text(stringResource(R.string.template_composer_section_rule), style = MaterialTheme.typography.titleMedium)
            TemplateRuleEditor(value = rule, apps = allApps, onChange = { rule = it })
        }
    }
}
