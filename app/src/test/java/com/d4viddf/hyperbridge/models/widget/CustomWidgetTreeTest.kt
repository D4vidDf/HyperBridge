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

    @Test
    fun groupNodePreservesCanvasCoordinates() {
        val doc = CustomWidgetDocument(
            id = "w1",
            meta = CustomWidgetMetadata(name = "Test"),
            root = LayoutContainer(
                id = "root",
                children = listOf(
                    TextNode(id = "t1", bounds = NodeBounds(x = 100, y = 50, widthDp = 80, heightDp = 24))
                )
            )
        )

        val (updated, groupId) = doc.groupNode("t1")
        val group = updated.findNode(groupId!!) as LayoutContainer
        val child = updated.findNode("t1") as TextNode

        // The group container adopts the child's original bounds
        assertEquals(100, group.bounds.x)
        assertEquals(50, group.bounds.y)
        assertEquals(80, group.bounds.widthDp)
        assertEquals(24, group.bounds.heightDp)

        // The child inside the group is reset to (0, 0) relative to the group
        assertEquals(0, child.bounds.x)
        assertEquals(0, child.bounds.y)

        // The child's absolute position on canvas is preserved
        assertEquals(Pair(100, 50), updated.absolutePositionOf("t1"))
    }

    @Test
    fun ungroupNodePreservesCanvasCoordinates() {
        val doc = CustomWidgetDocument(
            id = "w1",
            meta = CustomWidgetMetadata(name = "Test"),
            root = LayoutContainer(
                id = "root",
                children = listOf(
                    LayoutContainer(
                        id = "group1",
                        bounds = NodeBounds(x = 80, y = 40, widthDp = 120, heightDp = 60),
                        children = listOf(
                            TextNode(id = "t1", bounds = NodeBounds(x = 20, y = 10, widthDp = 50, heightDp = 20))
                        )
                    )
                )
            )
        )

        val updated = doc.ungroupNode("group1")
        assertNull(updated.findNode("group1"))

        val child = updated.findNode("t1") as TextNode
        // Child's position in parent is shifted by group's bounds (80 + 20, 40 + 10)
        assertEquals(100, child.bounds.x)
        assertEquals(50, child.bounds.y)
        assertEquals(Pair(100, 50), updated.absolutePositionOf("t1"))
    }

    @Test
    fun moveIntoPreservesCanvasCoordinates() {
        val doc = CustomWidgetDocument(
            id = "w1",
            meta = CustomWidgetMetadata(name = "Test"),
            root = LayoutContainer(
                id = "root",
                children = listOf(
                    TextNode(id = "t1", bounds = NodeBounds(x = 120, y = 80, widthDp = 40, heightDp = 20)),
                    LayoutContainer(
                        id = "group1",
                        bounds = NodeBounds(x = 50, y = 30, widthDp = 100, heightDp = 60),
                        children = emptyList()
                    )
                )
            )
        )

        // Move t1 into group1
        val intoGroup = doc.moveInto("t1", "group1")
        val childInGroup = intoGroup.findNode("t1") as TextNode
        // Relative coordinates inside group1 should be (120 - 50, 80 - 30) = (70, 50)
        assertEquals(70, childInGroup.bounds.x)
        assertEquals(50, childInGroup.bounds.y)
        assertEquals(Pair(120, 80), intoGroup.absolutePositionOf("t1"))

        // Move t1 back out to root
        val backToRoot = intoGroup.moveInto("t1", "root")
        val childInRoot = backToRoot.findNode("t1") as TextNode
        assertEquals(120, childInRoot.bounds.x)
        assertEquals(80, childInRoot.bounds.y)
        assertEquals(Pair(120, 80), backToRoot.absolutePositionOf("t1"))
    }

    @Test
    fun moveIntoBetweenNestedContainersPreservesCanvasCoordinates() {
        val doc = CustomWidgetDocument(
            id = "w1",
            meta = CustomWidgetMetadata(name = "Test"),
            root = LayoutContainer(
                id = "root",
                children = listOf(
                    LayoutContainer(
                        id = "g1",
                        bounds = NodeBounds(x = 30, y = 20),
                        children = listOf(
                            TextNode(id = "item", bounds = NodeBounds(x = 10, y = 15))
                        )
                    ),
                    LayoutContainer(
                        id = "g2",
                        bounds = NodeBounds(x = 60, y = 10),
                        children = emptyList()
                    )
                )
            )
        )

        // item absolute pos in g1: (30 + 10, 20 + 15) = (40, 35)
        assertEquals(Pair(40, 35), doc.absolutePositionOf("item"))

        val moved = doc.moveInto("item", "g2")
        val itemInG2 = moved.findNode("item") as TextNode
        // Relative pos in g2: (40 - 60, 35 - 10) = (-20, 25)
        assertEquals(-20, itemInG2.bounds.x)
        assertEquals(25, itemInG2.bounds.y)
        assertEquals(Pair(40, 35), moved.absolutePositionOf("item"))
    }

    @Test
    fun moveIntoContainerWithPaddingPreservesCanvasCoordinates() {
        val doc = CustomWidgetDocument(
            id = "w1",
            meta = CustomWidgetMetadata(name = "Test"),
            root = LayoutContainer(
                id = "root",
                children = listOf(
                    TextNode(id = "t1", bounds = NodeBounds(x = 100, y = 50)),
                    LayoutContainer(
                        id = "groupWithPad",
                        bounds = NodeBounds(x = 40, y = 30),
                        paddingDp = 8,
                        children = emptyList()
                    )
                )
            )
        )

        val updated = doc.moveInto("t1", "groupWithPad")
        val child = updated.findNode("t1") as TextNode
        // Target container offset is (40 + 8, 30 + 8) = (48, 38).
        // Relative coordinates: (100 - 48, 50 - 38) = (52, 12).
        assertEquals(52, child.bounds.x)
        assertEquals(12, child.bounds.y)
        assertEquals(Pair(100, 50), updated.absolutePositionOf("t1"))
    }

    @Test
    fun groupAdaptsDimensionsToRowLayoutChildren() {
        val rowGroup = LayoutContainer(
            id = "row1",
            layout = ContainerLayout.ROW,
            gapDp = 8,
            paddingDp = 4,
            children = listOf(
                TextNode(id = "c1", bounds = NodeBounds(widthDp = 50, heightDp = 30)),
                TextNode(id = "c2", bounds = NodeBounds(widthDp = 60, heightDp = 40))
            )
        )

        // Row width: 50 + 60 + 8 + 2*4 = 126
        // Row height: max(30, 40) + 2*4 = 48
        assertEquals(126, rowGroup.adaptedContentWidth())
        assertEquals(48, rowGroup.adaptedContentHeight())
    }

    @Test
    fun groupAdaptsDimensionsToColumnLayoutChildren() {
        val colGroup = LayoutContainer(
            id = "col1",
            layout = ContainerLayout.COLUMN,
            gapDp = 8,
            paddingDp = 4,
            children = listOf(
                TextNode(id = "c1", bounds = NodeBounds(widthDp = 50, heightDp = 30)),
                TextNode(id = "c2", bounds = NodeBounds(widthDp = 60, heightDp = 40))
            )
        )

        // Column width: max(50, 60) + 2*4 = 68
        // Column height: 30 + 40 + 8 + 2*4 = 86
        assertEquals(68, colGroup.adaptedContentWidth())
        assertEquals(86, colGroup.adaptedContentHeight())
    }

    @Test
    fun groupAdaptsDimensionsToBoxLayoutChildren() {
        val boxGroup = LayoutContainer(
            id = "box1",
            layout = ContainerLayout.BOX,
            paddingDp = 0,
            children = listOf(
                TextNode(id = "c1", bounds = NodeBounds(x = 0, y = 0, widthDp = 50, heightDp = 30)),
                TextNode(id = "c2", bounds = NodeBounds(x = 40, y = 20, widthDp = 60, heightDp = 35))
            )
        )

        // Box width: max(0+50, 40+60) = 100
        // Box height: max(0+30, 20+35) = 55
        assertEquals(100, boxGroup.adaptedContentWidth())
        assertEquals(55, boxGroup.adaptedContentHeight())
    }

    @Test
    fun moveIntoAdaptsGroupDimensionsWhenAddingAndRemovingChildren() {
        val doc = CustomWidgetDocument(
            id = "w1",
            meta = CustomWidgetMetadata(name = "Test"),
            root = LayoutContainer(
                id = "root",
                children = listOf(
                    LayoutContainer(
                        id = "group1",
                        layout = ContainerLayout.ROW,
                        gapDp = 6,
                        paddingDp = 0,
                        bounds = NodeBounds(x = 10, y = 10),
                        children = listOf(
                            TextNode(id = "item1", bounds = NodeBounds(widthDp = 40, heightDp = 20))
                        )
                    ),
                    TextNode(id = "item2", bounds = NodeBounds(x = 100, y = 10, widthDp = 50, heightDp = 30))
                )
            )
        )

        // Initially group1 has item1 (40x20)
        val initialGroup = doc.findNode("group1") as LayoutContainer
        assertEquals(40, initialGroup.adaptedContentWidth())
        assertEquals(20, initialGroup.adaptedContentHeight())

        // Move item2 into group1
        val docWithMoved = doc.moveInto("item2", "group1")
        val expandedGroup = docWithMoved.findNode("group1") as LayoutContainer
        // In ROW layout: 40 + 50 + 6 (gap) = 96 width, max(20, 30) = 30 height
        assertEquals(96, expandedGroup.bounds.widthDp)
        assertEquals(30, expandedGroup.bounds.heightDp)

        // Move item2 back to root
        val docRestored = docWithMoved.moveInto("item2", "root")
        val shrunkGroup = docRestored.findNode("group1") as LayoutContainer
        assertEquals(40, shrunkGroup.bounds.widthDp)
        assertEquals(20, shrunkGroup.bounds.heightDp)
    }
}
