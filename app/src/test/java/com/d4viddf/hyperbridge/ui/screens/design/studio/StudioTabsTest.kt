package com.d4viddf.hyperbridge.ui.screens.design.studio

import com.d4viddf.hyperbridge.models.widget.*
import org.junit.Assert.assertEquals
import org.junit.Test

class StudioTabsTest {

    @Test
    fun `tabsFor root container returns items, container, design, scope`() {
        val root = LayoutContainer(id = "root_container")
        val tabs = StudioTab.tabsFor(root, isRoot = true)
        assertEquals(
            listOf(StudioTab.ITEMS, StudioTab.CONTAINER, StudioTab.DESIGN, StudioTab.SCOPE),
            tabs
        )
    }

    @Test
    fun `tabsFor null node returns root tabs`() {
        val tabs = StudioTab.tabsFor(null, isRoot = true)
        assertEquals(
            listOf(StudioTab.ITEMS, StudioTab.CONTAINER, StudioTab.DESIGN, StudioTab.SCOPE),
            tabs
        )
    }

    @Test
    fun `tabsFor child container returns items, item, position, container, colors`() {
        val child = LayoutContainer(id = "child_group")
        val tabs = StudioTab.tabsFor(child, isRoot = false)
        assertEquals(
            listOf(StudioTab.ITEMS, StudioTab.ITEM, StudioTab.POSITION, StudioTab.CONTAINER, StudioTab.COLORS),
            tabs
        )
    }

    @Test
    fun `tabsFor progress bar returns item, position, colors, value, actions`() {
        val progress = ProgressNode(id = "progress_bar")
        val tabs = StudioTab.tabsFor(progress, isRoot = false)
        assertEquals(
            listOf(StudioTab.ITEM, StudioTab.POSITION, StudioTab.COLORS, StudioTab.VALUE, StudioTab.ACTIONS),
            tabs
        )
    }

    @Test
    fun `tabsFor text node returns item, position, colors, value, actions`() {
        val text = TextNode(id = "title_text")
        val tabs = StudioTab.tabsFor(text, isRoot = false)
        assertEquals(
            listOf(StudioTab.ITEM, StudioTab.POSITION, StudioTab.COLORS, StudioTab.VALUE, StudioTab.ACTIONS),
            tabs
        )
    }

    @Test
    fun `tabsFor button node returns item, position, colors, value, actions`() {
        val button = ButtonNode(id = "btn_action")
        val tabs = StudioTab.tabsFor(button, isRoot = false)
        assertEquals(
            listOf(StudioTab.ITEM, StudioTab.POSITION, StudioTab.COLORS, StudioTab.VALUE, StudioTab.ACTIONS),
            tabs
        )
    }

    @Test
    fun `tabsFor image node returns item, position, colors, value, actions`() {
        val image = ImageNode(id = "avatar_img")
        val tabs = StudioTab.tabsFor(image, isRoot = false)
        assertEquals(
            listOf(StudioTab.ITEM, StudioTab.POSITION, StudioTab.COLORS, StudioTab.VALUE, StudioTab.ACTIONS),
            tabs
        )
    }

    @Test
    fun `tabsFor shape node returns item, position, colors, value, actions`() {
        val shape = ShapeNode(id = "shape_bg")
        val tabs = StudioTab.tabsFor(shape, isRoot = false)
        assertEquals(
            listOf(StudioTab.ITEM, StudioTab.POSITION, StudioTab.COLORS, StudioTab.VALUE, StudioTab.ACTIONS),
            tabs
        )
    }
}
