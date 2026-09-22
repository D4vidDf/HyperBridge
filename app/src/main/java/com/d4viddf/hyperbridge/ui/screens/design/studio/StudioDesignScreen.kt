package com.d4viddf.hyperbridge.ui.screens.design.studio

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.widget.CustomWidgetRepository
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.PresentationConfig
import com.d4viddf.hyperbridge.models.translator.PresentationMode
import com.d4viddf.hyperbridge.models.translator.TargetScope
import com.d4viddf.hyperbridge.models.translator.TranslatorMetadata
import com.d4viddf.hyperbridge.models.widget.ButtonNode
import com.d4viddf.hyperbridge.models.widget.ContainerLayout
import com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument
import com.d4viddf.hyperbridge.models.widget.CustomWidgetMetadata
import com.d4viddf.hyperbridge.models.widget.CustomWidgetNode
import com.d4viddf.hyperbridge.models.widget.ImageNode
import com.d4viddf.hyperbridge.models.widget.LayoutContainer
import com.d4viddf.hyperbridge.models.widget.ProgressNode
import com.d4viddf.hyperbridge.models.widget.TextNode
import com.d4viddf.hyperbridge.models.widget.WidgetDimensionValidator
import com.d4viddf.hyperbridge.models.widget.addChild
import com.d4viddf.hyperbridge.models.widget.findNode
import com.d4viddf.hyperbridge.models.widget.moveNode
import com.d4viddf.hyperbridge.models.widget.parentOf
import com.d4viddf.hyperbridge.models.widget.removeNode
import com.d4viddf.hyperbridge.models.widget.replaceNode
import kotlinx.coroutines.launch
import java.util.UUID

private const val CANVAS_WIDTH_DP = 350

/**
 * Studio Design (#328): the element-by-element island builder, renamed from "Widget Studio" so it
 * is not mistaken for Android's app widgets.
 *
 * Laid out the way KWGT does it: the canvas and the controls are on screen at the same time, the
 * tab row switches between the elements of the design, the top bar saves or restores the last
 * saved state, and the Add button opens a screen of element types.
 *
 * Saving writes two things: the `.hwidget` document, and the translator that makes it a design
 * (`presentation.mode = WIDGET`), so the island pipeline picks it up like any other translator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioDesignScreen(
    widgetId: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { CustomWidgetRepository(context) }
    val scope = rememberCoroutineScope()
    val translatorViewModel: com.d4viddf.hyperbridge.ui.screens.translators.TranslatorViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()
    val allTranslators by translatorViewModel.allTranslators.collectAsState()

    val blankDocument = remember(widgetId) {
        CustomWidgetDocument(
            id = widgetId ?: UUID.randomUUID().toString(),
            meta = CustomWidgetMetadata(name = context.getString(R.string.studio_new_design)),
            // Free positioning by default: the canvas is drag-and-drop, and a stack of overlapping
            // elements is what the layer controls act on.
            root = LayoutContainer(id = "root", layout = ContainerLayout.ABSOLUTE, paddingDp = 8)
        )
    }

    var doc by remember { mutableStateOf(blankDocument) }
    var savedDoc by remember { mutableStateOf(blankDocument) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var notificationType by remember { mutableStateOf(NotificationType.STANDARD) }
    var showAddElement by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(widgetId) {
        if (widgetId != null) {
            repository.getWidget(widgetId)?.let {
                doc = it
                savedDoc = it
            }
        }
    }

    // The design this document already belongs to, if any: that is where "shown for" lives.
    val existingDesign = allTranslators.firstOrNull { it.presentation.widgetId == doc.id }
    LaunchedEffect(existingDesign?.id) {
        existingDesign?.targetNotificationTypes?.firstOrNull()?.let { name ->
            NotificationType.entries.firstOrNull { it.name == name }?.let { notificationType = it }
        }
    }

    val isDirty = doc != savedDoc
    val elements = remember(doc) { doc.root.children }
    val selectedNode: CustomWidgetNode? = selectedNodeId?.let { doc.findNode(it) }

    if (showAddElement) {
        AddElementScreen(
            onBack = { showAddElement = false },
            onPick = { element ->
                val node = element.create()
                val parentId = (selectedNode as? LayoutContainer)?.id ?: doc.root.id
                doc = doc.addChild(parentId, node)
                selectedNodeId = node.id
                showAddElement = false
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = doc.meta.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val file = repository.exportWidget(doc.id)
                            if (file != null) {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val chooser = Intent.createChooser(intent, context.getString(R.string.studio_export))
                                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooser)
                            }
                        }
                    }) {
                        Icon(Icons.Rounded.IosShare, stringResource(R.string.studio_export))
                    }

                    // Restore: back to the last saved state, the KWGT way out of a bad edit.
                    IconButton(
                        onClick = {
                            doc = savedDoc
                            selectedNodeId = null
                            validationMessage = null
                        },
                        enabled = isDirty
                    ) {
                        Icon(Icons.Rounded.Restore, stringResource(R.string.studio_restore))
                    }

                    TextButton(onClick = {
                        val validation = WidgetDimensionValidator.validate(doc)
                        val clamped = validation.clamped
                        doc = clamped
                        savedDoc = clamped
                        validationMessage = if (validation.errors.isNotEmpty()) {
                            context.getString(R.string.studio_validation_errors, validation.errors.size)
                        } else null

                        scope.launch {
                            repository.saveWidget(clamped)
                            translatorViewModel.saveTranslator(
                                designFor(clamped, notificationType, existingDesign)
                            )
                            Toast.makeText(context, R.string.studio_saved, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text(stringResource(R.string.studio_save))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddElement = true }) {
                Icon(Icons.Rounded.Add, stringResource(R.string.studio_add_element_title))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            StudioCanvas(
                root = doc.root,
                canvasHeightDp = doc.canvas.heightDp,
                selectedId = selectedNodeId,
                onSelect = { selectedNodeId = it },
                onMove = { id, dx, dy ->
                    doc = doc.replaceNode(id) { node ->
                        node.withBounds(node.bounds.movedBy(dx, dy, CANVAS_WIDTH_DP, doc.canvas.heightDp))
                    }
                },
                onResize = { id, width, height ->
                    doc = doc.replaceNode(id) { node ->
                        node.withBounds(node.bounds.copy(widthDp = width, heightDp = height))
                    }
                },
                modifier = Modifier.padding(16.dp)
            )

            validationMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Tabs switch between the elements of the design; the first one is the design itself.
            val tabIndex = elements.indexOfFirst { it.id == selectedNodeId } + 1
            ScrollableTabRow(selectedTabIndex = tabIndex, edgePadding = 16.dp) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { selectedNodeId = null },
                    text = { Text(stringResource(R.string.studio_tab_design)) }
                )
                elements.forEach { node ->
                    Tab(
                        selected = selectedNodeId == node.id,
                        onClick = { selectedNodeId = node.id },
                        text = {
                            Text(
                                text = elementLabel(node),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                if (selectedNode == null) {
                    StudioDesignSettings(
                        name = doc.meta.name,
                        canvas = doc.canvas,
                        notificationType = notificationType,
                        onNameChange = { doc = doc.copy(meta = doc.meta.copy(name = it)) },
                        onCanvasChange = { doc = doc.copy(canvas = it) },
                        onNotificationTypeChange = { notificationType = it }
                    )
                } else {
                    val siblings = doc.parentOf(selectedNode.id)?.children.orEmpty()
                    val index = siblings.indexOfFirst { it.id == selectedNode.id }
                    StudioInspector(
                        node = selectedNode,
                        isRoot = selectedNode.id == doc.root.id,
                        canMoveUp = index >= 0 && index < siblings.lastIndex,
                        canMoveDown = index > 0,
                        onChange = { updated -> doc = doc.replaceNode(updated.id) { updated } },
                        onMoveLayer = { delta -> doc = doc.moveNode(selectedNode.id, delta) },
                        onDelete = {
                            doc = doc.removeNode(selectedNode.id)
                            selectedNodeId = null
                        }
                    )
                }
                Spacer(Modifier.height(96.dp))
            }
        }
    }
}

/** The translator that turns a saved document into a design the island pipeline can match. */
private fun designFor(
    doc: CustomWidgetDocument,
    notificationType: NotificationType,
    existing: CustomTranslator?
): CustomTranslator {
    val presentation = PresentationConfig(mode = PresentationMode.WIDGET, widgetId = doc.id)
    return existing?.copy(
        meta = existing.meta.copy(name = doc.meta.name),
        targetScope = TargetScope.NOTIFICATION_TYPE,
        targetNotificationTypes = listOf(notificationType.name),
        presentation = presentation
    ) ?: CustomTranslator(
        id = UUID.randomUUID().toString(),
        meta = TranslatorMetadata(name = doc.meta.name, iconName = "Widgets"),
        targetScope = TargetScope.NOTIFICATION_TYPE,
        targetNotificationTypes = listOf(notificationType.name),
        presentation = presentation
    )
}

private fun elementLabel(node: CustomWidgetNode): String = when (node) {
    is TextNode -> node.template.ifBlank { "Text" }
    is ImageNode -> "Image"
    is ProgressNode -> "Progress"
    is ButtonNode -> node.label.ifBlank { "Button" }
    is LayoutContainer -> "Group (${node.layout})"
}
