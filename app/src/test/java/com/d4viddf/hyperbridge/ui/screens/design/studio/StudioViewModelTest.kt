package com.d4viddf.hyperbridge.ui.screens.design.studio

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.d4viddf.hyperbridge.data.widget.CustomWidgetRepository
import com.d4viddf.hyperbridge.models.widget.ButtonAction
import com.d4viddf.hyperbridge.models.widget.ButtonNode
import com.d4viddf.hyperbridge.models.widget.CanvasSize
import com.d4viddf.hyperbridge.models.widget.ContainerLayout
import com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument
import com.d4viddf.hyperbridge.models.widget.CustomWidgetMetadata
import com.d4viddf.hyperbridge.models.widget.LayoutContainer
import com.d4viddf.hyperbridge.models.widget.TextNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class StudioViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var app: FakeApplication
    private lateinit var repository: CustomWidgetRepository

    class FakeApplication(
        private val files: File,
        private val cache: File
    ) : Application() {
        override fun getFilesDir(): File = files
        override fun getCacheDir(): File = cache
        override fun getPackageName(): String = "com.d4viddf.hyperbridge"
        override fun getApplicationContext(): Context = this
    }

    @Before
    fun setUp() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        app = FakeApplication(filesDir, cacheDir)
        repository = CustomWidgetRepository(app)
    }

    private fun createSampleDocument(): CustomWidgetDocument {
        return CustomWidgetDocument(
            id = "test-doc",
            meta = CustomWidgetMetadata(name = "Test Widget"),
            canvas = CanvasSize.MEDIUM,
            root = LayoutContainer(
                id = "root",
                layout = ContainerLayout.COLUMN,
                children = listOf(
                    TextNode(id = "text-1", template = "Item 1"),
                    TextNode(id = "text-2", template = "Item 2")
                )
            )
        )
    }

    @Test
    fun initialStateIsCleanAndCanUndoIsFalse() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        assertEquals("test-doc", vm.document.value.id)
        assertFalse(vm.isDirty)
        assertFalse(vm.canUndo.value)
        assertFalse(vm.canRedo.value)
        assertNull(vm.selectedNodeId.value)
    }

    @Test
    fun updateNodePushesUndoAndMarksDirty() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        val updatedNode = TextNode(id = "text-1", template = "Modified Item 1")
        vm.updateNode(updatedNode)

        assertTrue(vm.isDirty)
        assertTrue(vm.canUndo.value)
        assertFalse(vm.canRedo.value)

        val root = vm.document.value.root as LayoutContainer
        val child = root.children.first() as TextNode
        assertEquals("Modified Item 1", child.template)

        // Undo reverts
        vm.undo()
        assertFalse(vm.canUndo.value)
        assertTrue(vm.canRedo.value)
        val undoneRoot = vm.document.value.root as LayoutContainer
        assertEquals("Item 1", (undoneRoot.children.first() as TextNode).template)

        // Redo reapplies
        vm.redo()
        assertTrue(vm.canUndo.value)
        assertFalse(vm.canRedo.value)
        val redoneRoot = vm.document.value.root as LayoutContainer
        assertEquals("Modified Item 1", (redoneRoot.children.first() as TextNode).template)
    }

    @Test
    fun addNodeAppendsChildAndSelectsIt() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        val newNode = ButtonNode(id = "btn-1", label = "Click Me", action = ButtonAction.Dismiss)
        vm.addNode("root", newNode)

        assertTrue(vm.isDirty)
        assertEquals("btn-1", vm.selectedNodeId.value)
        val root = vm.document.value.root as LayoutContainer
        assertEquals(3, root.children.size)
        assertEquals("btn-1", root.children.last().id)
    }

    @Test
    fun removeNodeDeletesChildAndAdjustsSelection() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        vm.selectNode("text-1")
        assertEquals("text-1", vm.selectedNodeId.value)

        vm.removeNode("text-1")
        val root = vm.document.value.root as LayoutContainer
        assertEquals(1, root.children.size)
        assertEquals("text-2", root.children.first().id)
        assertNull(vm.selectedNodeId.value)
    }

    @Test
    fun moveLayerReordersChildren() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        // Move text-2 up by -1
        vm.moveLayer("text-2", -1)
        val root = vm.document.value.root as LayoutContainer
        assertEquals("text-2", root.children[0].id)
        assertEquals("text-1", root.children[1].id)
    }

    @Test
    fun restoreLastSavedRevertsAllUnsavedChanges() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        vm.updateNode(TextNode(id = "text-1", template = "Changed"))
        assertTrue(vm.isDirty)

        vm.restoreLastSaved()
        val root = vm.document.value.root as LayoutContainer
        assertEquals("Item 1", (root.children.first() as TextNode).template)
    }

    @Test
    fun savedDraftPersistsAcrossProcessDeath() {
        val savedStateHandle = SavedStateHandle()
        val doc = createSampleDocument()
        val vm1 = StudioViewModel(app, savedStateHandle, repository = repository, initialDocument = doc)

        vm1.updateNode(TextNode(id = "text-1", template = "Persistent Change"))

        // Simulate recreated ViewModel with the same SavedStateHandle
        val vm2 = StudioViewModel(app, savedStateHandle, repository = repository, initialDocument = doc)
        val root = vm2.document.value.root as LayoutContainer
        assertEquals("Persistent Change", (root.children.first() as TextNode).template)
        assertTrue(vm2.isDirty)
    }

    @Test
    fun historyCapsAtMaxHistorySize() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        for (i in 1..40) {
            vm.updateNode(TextNode(id = "text-1", template = "Change $i"))
        }

        // We can undo at most 30 times
        var undoCount = 0
        while (vm.canUndo.value) {
            vm.undo()
            undoCount++
        }
        assertEquals(30, undoCount)
    }
}
