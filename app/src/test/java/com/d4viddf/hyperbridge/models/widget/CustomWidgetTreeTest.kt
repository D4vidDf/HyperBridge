package com.d4viddf.hyperbridge.models.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomWidgetTreeTest {

    private fun sampleDoc() = CustomWidgetDocument(
        id = "w1",
        meta = CustomWidgetMetadata(name = "Test"),
        root = LayoutContainer(
            id = "root",
            children = listOf(
                TextNode(id = "t1", template = "hello"),
                LayoutContainer(id = "group", children = listOf(TextNode(id = "nested", template = "world")))
            )
        )
    )

    @Test
    fun replaceNodeTransformsOnlyTheMatchingNode() {
        val doc = sampleDoc()
        val updated = doc.replaceNode("nested") { (it as TextNode).copy(template = "changed") }

        val nested = updated.findNode("nested") as TextNode
        assertEquals("changed", nested.template)
        assertEquals("hello", (updated.findNode("t1") as TextNode).template)
    }

    @Test
    fun replaceNodeIsNoOpForUnknownId() {
        val doc = sampleDoc()
        val updated = doc.replaceNode("does-not-exist") { it }
        assertEquals(doc, updated)
    }

    @Test
    fun addChildAppendsUnderTargetContainer() {
        val doc = sampleDoc()
        val updated = doc.addChild("group", TextNode(id = "new", template = "added"))

        val group = updated.findNode("group") as LayoutContainer
        assertEquals(2, group.children.size)
        assertEquals("added", (group.children.last() as TextNode).template)
    }

    @Test
    fun removeNodeDropsItFromItsParent() {
        val doc = sampleDoc()
        val updated = doc.removeNode("t1")

        assertNull(updated.findNode("t1"))
        assertEquals(1, updated.root.children.size)
    }

    @Test
    fun removeNodeNeverRemovesRoot() {
        val doc = sampleDoc()
        val updated = doc.removeNode("root")
        assertEquals(doc, updated)
    }

    @Test
    fun countNodesCountsRootAndAllDescendants() {
        val doc = sampleDoc()
        // root + t1 + group + nested = 4
        assertEquals(4, doc.root.countNodes())
    }

    @Test
    fun findNodeReturnsNullWhenAbsent() {
        assertTrue(sampleDoc().findNode("missing") == null)
    }

    @Test
    fun duplicateNodeCreatesCopyWithFreshIdsAndInsertsBesideOriginal() {
        val doc = CustomWidgetDocument(
            id = "w1",
            meta = CustomWidgetMetadata(name = "Test"),
            root = LayoutContainer(
                id = "root",
                layout = ContainerLayout.ABSOLUTE,
                children = listOf(
                    TextNode(id = "t1", template = "hello", bounds = NodeBounds(10, 20, 50, 20)),
                    TextNode(id = "t2", template = "world")
                )
            )
        )

        val (updated, newId) = doc.duplicateNode("t1")
        org.junit.Assert.assertNotNull(newId)
        org.junit.Assert.assertNotEquals("t1", newId)
        assertEquals(3, updated.root.children.size)

        // New node should be right after t1 (index 1)
        val duplicate = updated.findNode(newId!!) as TextNode
        assertEquals("hello", duplicate.template)
        // In ABSOLUTE layout, bounds are offset by 8dp
        assertEquals(18, duplicate.bounds.x)
        assertEquals(28, duplicate.bounds.y)
    }

    @Test
    fun duplicateNodeWorksForShapeNode() {
        val doc = CustomWidgetDocument(
            id = "w1",
            meta = CustomWidgetMetadata(name = "Test"),
            root = LayoutContainer(
                id = "root",
                layout = ContainerLayout.ABSOLUTE,
                children = listOf(
                    ShapeNode(
                        id = "s1",
                        bounds = NodeBounds(10, 20, 50, 20),
                        shapeId = "rounded_rect",
                        cornerRadiusDp = 4,
                        opacity = 0.8f
                    )
                )
            )
        )

        val (updated, newId) = doc.duplicateNode("s1")
        org.junit.Assert.assertNotNull(newId)
        org.junit.Assert.assertNotEquals("s1", newId)
        assertEquals(2, updated.root.children.size)

        val duplicate = updated.findNode(newId!!) as ShapeNode
        assertEquals("rounded_rect", duplicate.shapeId)
        assertEquals(4, duplicate.cornerRadiusDp)
        assertEquals(0.8f, duplicate.opacity)
        assertEquals(18, duplicate.bounds.x)
        assertEquals(28, duplicate.bounds.y)
    }

    @Test
    fun groupNodeWrapsTargetInContainer() {
        val doc = sampleDoc()
        val (updated, newGroupId) = doc.groupNode("nested", ContainerLayout.ROW)

        org.junit.Assert.assertNotNull(newGroupId)
        val group = updated.findNode("group") as LayoutContainer
        val newGroup = updated.findNode(newGroupId!!) as LayoutContainer

        assertEquals(ContainerLayout.ROW, newGroup.layout)
        assertEquals(1, newGroup.children.size)
        assertEquals("nested", newGroup.children.first().id)
        assertEquals(listOf(newGroupId), group.children.map { it.id })
    }

    @Test
    fun ungroupNodeInlinesChildrenIntoParent() {
        val doc = sampleDoc()
        val updated = doc.ungroupNode("group")

        // "group" should be removed, and "nested" inlined into root
        assertNull(updated.findNode("group"))
        assertEquals(listOf("t1", "nested"), updated.root.children.map { it.id })
    }

    @Test
    fun moveNodeToFrontAndBackReordersStacking() {
        val doc = CustomWidgetDocument(
            id = "w1",
            meta = CustomWidgetMetadata(name = "Test"),
            root = LayoutContainer(
                id = "root",
                children = listOf(
                    TextNode(id = "a", template = "a"),
                    TextNode(id = "b", template = "b"),
                    TextNode(id = "c", template = "c")
                )
            )
        )

        val toFront = doc.moveNodeToFront("a")
        assertEquals(listOf("b", "c", "a"), toFront.root.children.map { it.id })

        val toBack = doc.moveNodeToBack("c")
        assertEquals(listOf("c", "a", "b"), toBack.root.children.map { it.id })
    }

    @Test
    fun moveIntoRelocatesNodeAcrossContainers() {
        val doc = sampleDoc()
        // Move t1 into group
        val updated = doc.moveInto("t1", "group")

        assertEquals(1, updated.root.children.size)
        assertEquals("group", updated.root.children.first().id)

        val group = updated.findNode("group") as LayoutContainer
        assertEquals(listOf("nested", "t1"), group.children.map { it.id })
    }

    @Test
    fun updateRootContainerBackgroundColor() {
        val doc = sampleDoc()
        val updated = doc.replaceNode("root") {
            (it as LayoutContainer).copy(backgroundHex = "#123456")
        }
        assertEquals("#123456", updated.root.backgroundHex)
    }

    @Test
    fun updateGroupContainerBackgroundColorAndBinding() {
        val doc = sampleDoc()
        val updated = doc.replaceNode("group") {
            (it as LayoutContainer).copy(backgroundHex = "#ABCDEF")
                .withBinding(BindableProperty.CONTAINER_BACKGROUND.key, "{theme.card}") as LayoutContainer
        }
        val group = updated.findNode("group") as LayoutContainer
        assertEquals("#ABCDEF", group.backgroundHex)
        assertEquals("{theme.card}", group.bindings[BindableProperty.CONTAINER_BACKGROUND.key])
    }
}
