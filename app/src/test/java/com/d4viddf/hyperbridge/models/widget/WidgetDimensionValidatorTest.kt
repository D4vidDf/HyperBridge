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
