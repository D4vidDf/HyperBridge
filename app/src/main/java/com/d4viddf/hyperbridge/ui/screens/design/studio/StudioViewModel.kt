package com.d4viddf.hyperbridge.ui.screens.design.studio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.widget.CustomWidgetRepository
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.PresentationConfig
import com.d4viddf.hyperbridge.models.translator.PresentationMode
import com.d4viddf.hyperbridge.models.translator.TargetScope
import com.d4viddf.hyperbridge.models.translator.TranslatorMetadata
import com.d4viddf.hyperbridge.models.widget.ContainerLayout
import com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument
import com.d4viddf.hyperbridge.models.widget.CustomWidgetMetadata
import com.d4viddf.hyperbridge.models.widget.CustomWidgetNode
import com.d4viddf.hyperbridge.models.widget.LayoutContainer
import com.d4viddf.hyperbridge.models.widget.WidgetDimensionValidator
import com.d4viddf.hyperbridge.models.widget.addChild
import com.d4viddf.hyperbridge.models.widget.moveNode
import com.d4viddf.hyperbridge.models.widget.removeNode
import com.d4viddf.hyperbridge.models.widget.replaceNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Manages Studio Design editing state, persistent document draft across orientation changes/process death,
 * undo/redo stack, and dirty-state tracking.
 */
class StudioViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val repository: CustomWidgetRepository = CustomWidgetRepository(application),
    initialDocument: CustomWidgetDocument? = null
) : AndroidViewModel(application) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val _document = MutableStateFlow(initialDocument ?: createInitialDocument())
    val document: StateFlow<CustomWidgetDocument> = _document.asStateFlow()

    private val _savedDocument = MutableStateFlow(initialDocument ?: _document.value)
    val savedDocument: StateFlow<CustomWidgetDocument> = _savedDocument.asStateFlow()

    val isDirty: Boolean
        get() = _document.value != _savedDocument.value

    private val _selectedNodeId = MutableStateFlow<String?>(null)
    val selectedNodeId: StateFlow<String?> = _selectedNodeId.asStateFlow()

    private val _notificationType = MutableStateFlow(NotificationType.STANDARD)
    val notificationType: StateFlow<NotificationType> = _notificationType.asStateFlow()

    private val _validationMessage = MutableStateFlow<String?>(null)
    val validationMessage: StateFlow<String?> = _validationMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // Undo / Redo history
    private val undoStack = mutableListOf<CustomWidgetDocument>()
    private val redoStack = mutableListOf<CustomWidgetDocument>()
    private val maxHistorySize = 30

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var existingTranslator: CustomTranslator? = null

    init {
        val draftJson = savedStateHandle.get<String>(KEY_DRAFT_DOC)
        if (draftJson != null) {
            try {
                val draft = json.decodeFromString(CustomWidgetDocument.serializer(), draftJson)
                _document.value = draft
            } catch (_: Exception) { /* ignore parse error and use initial */ }
        }
    }

    fun loadWidget(widgetId: String?, allTranslators: List<CustomTranslator>) {
        if (widgetId == null) {
            // New widget draft or existing draft from savedStateHandle
            return
        }

        viewModelScope.launch {
            val loaded = repository.getWidget(widgetId)
            if (loaded != null) {
                _document.value = loaded
                _savedDocument.value = loaded
                persistDraft(loaded)
                undoStack.clear()
                redoStack.clear()
                updateHistoryFlags()
            }

            existingTranslator = allTranslators.firstOrNull { it.presentation.widgetId == widgetId }
            existingTranslator?.targetNotificationTypes?.firstOrNull()?.let { typeName ->
                NotificationType.entries.firstOrNull { it.name == typeName }?.let {
                    _notificationType.value = it
                }
            }
        }
    }

    fun selectNode(nodeId: String?) {
        _selectedNodeId.value = nodeId
    }

    fun updateNode(updated: CustomWidgetNode) {
        updateDocument { it.replaceNode(updated.id) { updated } }
    }

    /**
     * Applies a document transformation and records it in the undo stack.
     */
    fun updateDocument(transform: (CustomWidgetDocument) -> CustomWidgetDocument) {
        val current = _document.value
        val updated = transform(current)
        if (current != updated) {
            pushHistory(current)
            _document.value = updated
            persistDraft(updated)
        }
    }

    /**
     * Applies interactive changes (such as dragging or resizing in progress) without recording to undo history.
     */
    fun updateDocumentTransformed(updated: CustomWidgetDocument) {
        if (_document.value != updated) {
            _document.value = updated
            persistDraft(updated)
        }
    }

    /**
     * Commits a continuous gesture (e.g. at drag/resize finish) into the undo stack.
     */
    fun commitGestureChange(previousDocument: CustomWidgetDocument) {
        if (previousDocument != _document.value) {
            pushHistory(previousDocument)
            persistDraft(_document.value)
        }
    }

    fun addNode(parentId: String, child: CustomWidgetNode) {
        updateDocument { it.addChild(parentId, child) }
        _selectedNodeId.value = child.id
    }

    fun removeNode(nodeId: String) {
        updateDocument { it.removeNode(nodeId) }
        if (_selectedNodeId.value == nodeId) {
            _selectedNodeId.value = null
        }
    }

    fun moveLayer(nodeId: String, delta: Int) {
        updateDocument { it.moveNode(nodeId, delta) }
    }

    fun setNotificationType(type: NotificationType) {
        _notificationType.value = type
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val current = _document.value
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(current)
            _document.value = previous
            persistDraft(previous)
            updateHistoryFlags()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val current = _document.value
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(current)
            _document.value = next
            persistDraft(next)
            updateHistoryFlags()
        }
    }

    fun restoreLastSaved() {
        val saved = _savedDocument.value
        pushHistory(_document.value)
        _document.value = saved
        _selectedNodeId.value = null
        _validationMessage.value = null
        persistDraft(saved)
    }

    fun save(
        onSaveTranslator: (CustomTranslator) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val validation = WidgetDimensionValidator.validate(_document.value)
            val clamped = validation.clamped

            _document.value = clamped
            _savedDocument.value = clamped
            persistDraft(clamped)

            _validationMessage.value = if (validation.errors.isNotEmpty()) {
                try {
                    getApplication<Application>().getString(R.string.studio_validation_errors, validation.errors.size)
                } catch (_: Throwable) {
                    "${validation.errors.size} error(s)"
                }
            } else null

            repository.saveWidget(clamped)
            val translator = createOrUpdateTranslator(clamped, _notificationType.value, existingTranslator)
            existingTranslator = translator
            onSaveTranslator(translator)

            _isSaving.value = false
            onSuccess()
        }
    }

    suspend fun exportWidget(): File? {
        return repository.exportWidget(_document.value.id)
    }

    private fun pushHistory(doc: CustomWidgetDocument) {
        undoStack.add(doc)
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        updateHistoryFlags()
    }

    private fun updateHistoryFlags() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun persistDraft(doc: CustomWidgetDocument) {
        savedStateHandle[KEY_DRAFT_DOC] = json.encodeToString(CustomWidgetDocument.serializer(), doc)
    }

    private fun createInitialDocument(): CustomWidgetDocument {
        val defaultName = try {
            getApplication<Application>()?.getString(R.string.studio_new_design) ?: "New Design"
        } catch (_: Throwable) {
            "New Design"
        }
        return CustomWidgetDocument(
            id = UUID.randomUUID().toString(),
            meta = CustomWidgetMetadata(
                name = defaultName
            ),
            root = LayoutContainer(id = "root", layout = ContainerLayout.ABSOLUTE, paddingDp = 8)
        )
    }

    private fun createOrUpdateTranslator(
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

    companion object {
        private const val KEY_DRAFT_DOC = "key_studio_draft_doc"
    }
}
