package com.d4viddf.hyperbridge.models.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetDimensionValidatorTest {

    private fun doc(root: LayoutContainer, canvas: CanvasSize = CanvasSize.MEDIUM) = CustomWidgetDocument(
        id = "w1",
        meta = CustomWidgetMetadata(name = "Test"),
        canvas = canvas,
        root = root
    )

    @Test
    fun clampsOutOfBoundsNodePosition() {
        val root = LayoutContainer(
            id = "root",
            children = listOf(TextNode(id = "t1", bounds = NodeBounds(x = -10, y = 9999, widthDp = 5000, heightDp = 5000)))
        )
        val result = WidgetDimensionValidator.validate(doc(root))

        assertTrue(result.errors.isNotEmpty())
        val clampedText = result.clamped.root.children.first() as TextNode
        assertEquals(0, clampedText.bounds.x)
        assertEquals(CanvasSize.MEDIUM.heightDp, clampedText.bounds.y)
        assertEquals(WidgetDimensionValidator.CANVAS_WIDTH_DP, clampedText.bounds.widthDp)
        assertEquals(CanvasSize.MEDIUM.heightDp, clampedText.bounds.heightDp)
    }

    @Test
    fun clampsFontSizeOutsideSaneBounds() {
        val root = LayoutContainer(id = "root", children = listOf(TextNode(id = "t1", fontSizeSp = 200)))
        val result = WidgetDimensionValidator.validate(doc(root))

        val clampedText = result.clamped.root.children.first() as TextNode
        assertEquals(WidgetDimensionValidator.MAX_FONT_SP, clampedText.fontSizeSp)
        assertTrue(result.errors.any { it.contains("font size") })
    }

    @Test
    fun flagsButDoesNotBlockOverlappingAbsoluteChildren() {
        val root = LayoutContainer(
            id = "root",
            layout = ContainerLayout.ABSOLUTE,
            children = listOf(
                TextNode(id = "a", bounds = NodeBounds(x = 0, y = 0, widthDp = 50, heightDp = 20)),
                TextNode(id = "b", bounds = NodeBounds(x = 10, y = 5, widthDp = 50, heightDp = 20))
            )
        )
        val result = WidgetDimensionValidator.validate(doc(root))

        assertTrue(result.errors.any { it.contains("overlap") })
        // Overlap is a warning only: nothing about the tree shape changes.
        assertEquals(2, result.clamped.root.children.size)
    }

    @Test
    fun flagsExcessiveRecursionDepth() {
        var current: CustomWidgetNode = TextNode(id = "leaf")
        repeat(WidgetDimensionValidator.MAX_DEPTH + 2) { i ->
            current = LayoutContainer(id = "c$i", children = listOf(current))
        }
        val result = WidgetDimensionValidator.validate(doc(current as LayoutContainer))

        assertTrue(result.errors.any { it.contains("recursion depth") })
    }

    @Test
    fun flagsExcessiveNodeCount() {
        val children = (1..WidgetDimensionValidator.MAX_NODE_COUNT + 5).map { TextNode(id = "t$it") }
        val root = LayoutContainer(id = "root", children = children)
        val result = WidgetDimensionValidator.validate(doc(root))

        assertTrue(result.errors.any { it.contains("node count") })
    }

    @Test
    fun cutsTheTreeOffAtMaxDepth() {
        var current: CustomWidgetNode = TextNode(id = "leaf")
        repeat(500) { i ->
            current = LayoutContainer(id = "c$i", children = listOf(current))
        }
        val result = WidgetDimensionValidator.validate(doc(current as LayoutContainer))

        assertTrue(result.pruned)
        assertTrue(depthOf(result.clamped.root) <= WidgetDimensionValidator.MAX_DEPTH)
    }

    @Test
    fun cutsExtraNodesPastMaxNodeCount() {
        val children = (1..WidgetDimensionValidator.MAX_NODE_COUNT * 10).map { TextNode(id = "t$it") }
        val result = WidgetDimensionValidator.validate(doc(LayoutContainer(id = "root", children = children)))

        assertTrue(result.pruned)
        // The root counts as a node too.
        assertEquals(WidgetDimensionValidator.MAX_NODE_COUNT - 1, result.clamped.root.children.size)
        assertEquals(1, result.errors.count { it.contains("node count") })
    }

    @Test
    fun aTreeWithinTheLimitsIsNotPruned() {
        val root = LayoutContainer(id = "root", children = listOf(TextNode(id = "a"), ButtonNode(id = "b", label = "B")))
        val result = WidgetDimensionValidator.validate(doc(root))

        assertEquals(false, result.pruned)
        assertEquals(root, result.clamped.root)
    }

    private fun depthOf(node: CustomWidgetNode): Int =
        if (node is LayoutContainer && node.children.isNotEmpty()) 1 + node.children.maxOf(::depthOf) else 1

    @Test
    fun flagsButDoesNotBlockTooManyButtons() {
        val children = (1..WidgetDimensionValidator.MAX_RECOMMENDED_BUTTONS + 1).map {
            ButtonNode(id = "b$it", label = "B$it")
        }
        val root = LayoutContainer(id = "root", children = children)
        val result = WidgetDimensionValidator.validate(doc(root))

        assertTrue(result.errors.any { it.contains("buttons") })
        // Warning only: every button node survives the clamp.
        assertEquals(children.size, result.clamped.root.children.size)
    }

    @Test
    fun leavesAWellFormedDocumentUnchanged() {
        val root = LayoutContainer(
            id = "root",
            children = listOf(TextNode(id = "t1", bounds = NodeBounds(x = 10, y = 10, widthDp = 100, heightDp = 20), fontSizeSp = 14))
        )
        val result = WidgetDimensionValidator.validate(doc(root))

        assertTrue(result.errors.isEmpty())
        assertEquals(doc(root), result.clamped)
    }
}
