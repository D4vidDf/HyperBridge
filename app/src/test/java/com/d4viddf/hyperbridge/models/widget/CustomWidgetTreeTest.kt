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
}
