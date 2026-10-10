package com.d4viddf.hyperbridge.ui.screens.design.studio

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.d4viddf.hyperbridge.data.widget.CustomWidgetRepository
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.translator.CustomTranslator
import com.d4viddf.hyperbridge.models.translator.PresentationConfig
import com.d4viddf.hyperbridge.models.translator.PresentationMode
import com.d4viddf.hyperbridge.models.translator.TargetScope
import com.d4viddf.hyperbridge.models.translator.TranslatorMetadata
import com.d4viddf.hyperbridge.models.widget.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    @Test
    fun duplicateNodeCreatesDuplicateAndPushesUndo() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        vm.duplicateNode("text-1")
        assertTrue(vm.isDirty)
        assertTrue(vm.canUndo.value)
        val newSelectedId = vm.selectedNodeId.value
        org.junit.Assert.assertNotNull(newSelectedId)
        org.junit.Assert.assertNotEquals("text-1", newSelectedId)

        val root = vm.document.value.root as LayoutContainer
        assertEquals(3, root.children.size)

        vm.undo()
        assertEquals(2, (vm.document.value.root as LayoutContainer).children.size)
    }

    @Test
    fun groupAndUngroupNodeManipulatesTreeHierarchy() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        vm.groupNode("text-1", ContainerLayout.BOX)
        val groupId = vm.selectedNodeId.value
        org.junit.Assert.assertNotNull(groupId)

        val group = vm.document.value.findNode(groupId!!) as LayoutContainer
        assertEquals(1, group.children.size)
        assertEquals("text-1", group.children.first().id)

        vm.ungroupNode(groupId)
        assertNull(vm.document.value.findNode(groupId))
        assertEquals(2, (vm.document.value.root as LayoutContainer).children.size)
    }

    @Test
    fun toggleLockUpdatesNodeLockedState() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        assertFalse(doc.findNode("text-1")!!.locked)
        vm.toggleLock("text-1")
        assertTrue(vm.document.value.findNode("text-1")!!.locked)
        vm.toggleLock("text-1")
        assertFalse(vm.document.value.findNode("text-1")!!.locked)
    }

    @Test
    fun toggleEditorVisibilityUpdatesHiddenNodeIds() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        assertTrue(vm.hiddenNodeIds.value.isEmpty())
        vm.toggleEditorVisibility("text-1")
        assertTrue(vm.hiddenNodeIds.value.contains("text-1"))
        vm.toggleEditorVisibility("text-1")
        assertFalse(vm.hiddenNodeIds.value.contains("text-1"))
    }

    @Test
    fun renameNodeSetsCustomName() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        vm.renameNode("text-1", "Header Title")
        assertEquals("Header Title", vm.document.value.findNode("text-1")!!.name)

        vm.renameNode("text-1", "")
        assertNull(vm.document.value.findNode("text-1")!!.name)
    }

    @Test
    fun moveNodeToFrontAndBackAdjustsStackingInViewModel() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        vm.moveNodeToFront("text-1")
        val root = vm.document.value.root as LayoutContainer
        assertEquals("text-2", root.children[0].id)
        assertEquals("text-1", root.children[1].id)

        vm.moveNodeToBack("text-1")
        val reRoot = vm.document.value.root as LayoutContainer
        assertEquals("text-1", reRoot.children[0].id)
        assertEquals("text-2", reRoot.children[1].id)
    }

    @Test
    fun secondaryConstructorsInstantiateCleanly() {
        val vmFromApp = StudioViewModel(app)
        assertEquals("New Design", vmFromApp.document.value.meta.name)
        assertEquals("Widgets", vmFromApp.document.value.meta.icon)

        val vmFromHandle = StudioViewModel(app, SavedStateHandle())
        assertEquals("New Design", vmFromHandle.document.value.meta.name)
        assertEquals("Widgets", vmFromHandle.document.value.meta.icon)
    }

    @Test
    fun metaIconCanBeUpdatedAndPersistsInDraft() {
        val savedStateHandle = SavedStateHandle()
        val doc = createSampleDocument()
        val vm1 = StudioViewModel(app, savedStateHandle, repository = repository, initialDocument = doc)

        vm1.updateDocument { it.copy(meta = it.meta.copy(icon = "MusicNote")) }
        assertEquals("MusicNote", vm1.document.value.meta.icon)

        val vm2 = StudioViewModel(app, savedStateHandle, repository = repository, initialDocument = doc)
        assertEquals("MusicNote", vm2.document.value.meta.icon)
    }

    @Test
    fun targetScopeAndPackagesCanBeUpdated() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        assertEquals(TargetScope.NOTIFICATION_TYPE, vm.targetScope.value)
        assertTrue(vm.targetPackages.value.isEmpty())

        vm.setTargetScope(TargetScope.SPECIFIC_APPS)
        assertEquals(TargetScope.SPECIFIC_APPS, vm.targetScope.value)

        vm.addTargetPackage("com.spotify.music")
        vm.addTargetPackage("org.telegram.messenger")
        // Duplicate should be ignored
        vm.addTargetPackage("com.spotify.music")
        // Blank should be ignored
        vm.addTargetPackage("   ")

        assertEquals(listOf("com.spotify.music", "org.telegram.messenger"), vm.targetPackages.value)

        vm.removeTargetPackage("com.spotify.music")
        assertEquals(listOf("org.telegram.messenger"), vm.targetPackages.value)
    }

    @Test
    fun createOrUpdateTranslatorBuildsExpectedTranslator() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        val translator = vm.createOrUpdateTranslator(
            doc = doc,
            targetScope = TargetScope.SPECIFIC_APPS,
            targetPackages = listOf("com.whatsapp"),
            notificationType = NotificationType.STANDARD,
            existing = null
        )

        assertEquals(TargetScope.SPECIFIC_APPS, translator.targetScope)
        assertEquals(listOf("com.whatsapp"), translator.targetPackages)
        assertTrue(translator.targetNotificationTypes.isEmpty())
        assertEquals(doc.id, translator.presentation.widgetId)
        assertEquals(doc.meta.name, translator.meta.name)
        assertEquals(doc.meta.icon, translator.meta.iconName)
    }

    @Test
    fun createOrUpdateTranslatorNotificationTypeBuildsExpectedTranslator() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        val translator = vm.createOrUpdateTranslator(
            doc = doc,
            targetScope = TargetScope.NOTIFICATION_TYPE,
            targetPackages = listOf("com.whatsapp"),
            notificationType = NotificationType.MEDIA,
            existing = null
        )

        assertEquals(TargetScope.NOTIFICATION_TYPE, translator.targetScope)
        assertTrue(translator.targetPackages.isEmpty())
        assertEquals(listOf(NotificationType.MEDIA.name), translator.targetNotificationTypes)
    }

    @Test
    fun updateGlobalsModifiesDocumentGlobals() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        val newGlobals = CustomWidgetGlobals(
            primaryColorHex = "#FFEEAA",
            accentColorHex = "#AABB00",
            fontFamily = TextFontFamily.CURSIVE
        )
        vm.updateGlobals(newGlobals)

        assertEquals("#FFEEAA", vm.document.value.globals.primaryColorHex)
        assertEquals("#AABB00", vm.document.value.globals.accentColorHex)
        assertEquals(TextFontFamily.CURSIVE, vm.document.value.globals.fontFamily)
    }

    @Test
    fun applyFontToAllTextNodesUpdatesAllTextElementsInHierarchy() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        vm.applyFontToAllTextNodes(TextFontFamily.MONOSPACE)

        val root = vm.document.value.root
        val text1 = root.children.find { it.id == "text-1" } as TextNode
        val text2 = root.children.find { it.id == "text-2" } as TextNode
        assertEquals(TextFontFamily.MONOSPACE, text1.fontFamily)
        assertEquals(TextFontFamily.MONOSPACE, text2.fontFamily)
        assertEquals(TextFontFamily.MONOSPACE, vm.document.value.globals.fontFamily)
    }

    @Test
    fun createTranslatorForDesignInstantiatesAndBindsTranslator() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        assertNull(vm.boundTranslator.value)
        vm.createTranslatorForDesign()
        val bound = vm.boundTranslator.value
        assertNotNull(bound)
        assertEquals(doc.id, bound!!.presentation.widgetId)
        assertEquals(doc.meta.name, bound.meta.name)
    }

    @Test
    fun moveIntoRelocatesNodeBetweenContainers() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        // Group text-2 into a new container
        vm.groupNode("text-2")
        val rootWithGroup = vm.document.value.root
        val group = rootWithGroup.children.find { it is LayoutContainer } as LayoutContainer

        // Move text-1 into the group
        vm.moveInto("text-1", group.id)
        val updatedDoc = vm.document.value
        val updatedGroup = updatedDoc.findNode(group.id) as LayoutContainer
        assertTrue(updatedGroup.children.any { it.id == "text-1" })
        assertTrue(updatedDoc.root.children.none { it.id == "text-1" })

        // Move text-1 back out of the group to root
        vm.moveInto("text-1", "root")
        val finalDoc = vm.document.value
        val finalGroup = finalDoc.findNode(group.id) as LayoutContainer
        assertTrue(finalDoc.root.children.any { it.id == "text-1" })
        assertTrue(finalGroup.children.none { it.id == "text-1" })
    }

    @Test
    fun moveIntoViaViewModelPreservesCanvasCoordinates() {
        val doc = CustomWidgetDocument(
            id = "test-doc",
            meta = CustomWidgetMetadata(name = "Test Widget"),
            canvas = CanvasSize.MEDIUM,
            root = LayoutContainer(
                id = "root",
                layout = ContainerLayout.ABSOLUTE,
                children = listOf(
                    TextNode(id = "text-1", bounds = NodeBounds(x = 140, y = 90)),
                    TextNode(id = "text-2", bounds = NodeBounds(x = 60, y = 40))
                )
            )
        )
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        // Group text-2
        vm.groupNode("text-2")
        val groupId = vm.selectedNodeId.value!!
        val group = vm.document.value.findNode(groupId) as LayoutContainer
        assertEquals(60, group.bounds.x)
        assertEquals(40, group.bounds.y)
        val text2InGroup = vm.document.value.findNode("text-2") as TextNode
        assertEquals(0, text2InGroup.bounds.x)
        assertEquals(0, text2InGroup.bounds.y)
        assertEquals(Pair(60, 40), vm.document.value.absolutePositionOf("text-2"))

        // Move text-1 into the group
        vm.moveInto("text-1", groupId)
        val text1InGroup = vm.document.value.findNode("text-1") as TextNode
        assertEquals(80, text1InGroup.bounds.x) // 140 - 60 = 80
        assertEquals(50, text1InGroup.bounds.y) // 90 - 40 = 50
        assertEquals(Pair(140, 90), vm.document.value.absolutePositionOf("text-1"))

        // Move text-1 back to root
        vm.moveInto("text-1", "root")
        val text1InRoot = vm.document.value.findNode("text-1") as TextNode
        assertEquals(140, text1InRoot.bounds.x)
        assertEquals(90, text1InRoot.bounds.y)
        assertEquals(Pair(140, 90), vm.document.value.absolutePositionOf("text-1"))
    }

    @Test
    fun createNewDocumentResetsDocumentAndUndoHistory() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        // Modify document to add undo entry and set dirty flag
        vm.updateNode(TextNode(id = "text-1", template = "Modified"))
        assertTrue(vm.isDirty)
        assertTrue(vm.canUndo.value)

        // Calling createNewDocument resets everything to clean state
        vm.createNewDocument()

        assertFalse(vm.isDirty)
        assertFalse(vm.canUndo.value)
        assertFalse(vm.canRedo.value)
        assertNull(vm.selectedNodeId.value)
        assertTrue(vm.document.value.id.isNotEmpty())
    }

    @Test
    fun loadWidgetWithNullResetsDirtyOrModifiedDocument() {
        val doc = createSampleDocument()
        val vm = StudioViewModel(app, SavedStateHandle(), repository = repository, initialDocument = doc)

        vm.updateNode(TextNode(id = "text-1", template = "Modified"))
        assertTrue(vm.isDirty)

        // Calling loadWidget(null, emptyList()) should reset when isDirty
        vm.loadWidget(null, emptyList())

        assertFalse(vm.isDirty)
        assertFalse(vm.canUndo.value)
        assertFalse(vm.canRedo.value)
    }
}

