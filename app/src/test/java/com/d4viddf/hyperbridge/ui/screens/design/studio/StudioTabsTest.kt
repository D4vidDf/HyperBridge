package com.d4viddf.hyperbridge.ui.screens.design.studio

import com.d4viddf.hyperbridge.models.widget.*
import org.junit.Assert.assertEquals
import org.junit.Test

class StudioTabsTest {

    @Test
    fun `tabsFor root container returns items, background, global, design, scope`() {
        val root = LayoutContainer(id = "root_container")
        val tabs = StudioTab.tabsFor(root, isRoot = true)
        assertEquals(
            listOf(StudioTab.ITEMS, StudioTab.BACKGROUND, StudioTab.GLOBAL, StudioTab.DESIGN, StudioTab.SCOPE),
            tabs
        )
    }

    @Test
    fun `tabsFor null node returns root tabs`() {
        val tabs = StudioTab.tabsFor(null, isRoot = true)
        assertEquals(
            listOf(StudioTab.ITEMS, StudioTab.BACKGROUND, StudioTab.GLOBAL, StudioTab.DESIGN, StudioTab.SCOPE),
            tabs
        )
    }

    @Test
    fun `tabsFor child container returns items, info, position, container, colors, visibility`() {
        val child = LayoutContainer(id = "child_group")
        val tabs = StudioTab.tabsFor(child, isRoot = false)
        assertEquals(
            listOf(StudioTab.ITEMS, StudioTab.INFO, StudioTab.POSITION, StudioTab.CONTAINER, StudioTab.COLORS, StudioTab.VISIBILITY),
            tabs
        )
    }

    @Test
    fun `tabsFor progress bar returns info, position, colors, value, actions, visibility`() {
        val progress = ProgressNode(id = "progress_bar")
        val tabs = StudioTab.tabsFor(progress, isRoot = false)
        assertEquals(
            listOf(StudioTab.INFO, StudioTab.POSITION, StudioTab.COLORS, StudioTab.VALUE, StudioTab.ACTIONS, StudioTab.VISIBILITY),
            tabs
        )
    }

    @Test
    fun `tabsFor text node returns info, colors, efx, position, visibility`() {
        val text = TextNode(id = "title_text")
        val tabs = StudioTab.tabsFor(text, isRoot = false)
        assertEquals(
            listOf(StudioTab.INFO, StudioTab.COLORS, StudioTab.EFX, StudioTab.POSITION, StudioTab.VISIBILITY),
            tabs
        )
    }

    @Test
    fun `tabsFor button node returns info, position, colors, value, actions, visibility`() {
        val button = ButtonNode(id = "btn_action")
        val tabs = StudioTab.tabsFor(button, isRoot = false)
        assertEquals(
            listOf(StudioTab.INFO, StudioTab.POSITION, StudioTab.COLORS, StudioTab.VALUE, StudioTab.ACTIONS, StudioTab.VISIBILITY),
            tabs
        )
    }

    @Test
    fun `tabsFor image node returns value, position, efx, actions, info, visibility`() {
        val image = ImageNode(id = "avatar_img")
        val tabs = StudioTab.tabsFor(image, isRoot = false)
        assertEquals(
            listOf(StudioTab.VALUE, StudioTab.POSITION, StudioTab.EFX, StudioTab.ACTIONS, StudioTab.INFO, StudioTab.VISIBILITY),
            tabs
        )
    }

    @Test
    fun `tabsFor shape node returns info, position, colors, value, actions, visibility`() {
        val shape = ShapeNode(id = "shape_bg")
        val tabs = StudioTab.tabsFor(shape, isRoot = false)
        assertEquals(
            listOf(StudioTab.INFO, StudioTab.POSITION, StudioTab.COLORS, StudioTab.VALUE, StudioTab.ACTIONS, StudioTab.VISIBILITY),
            tabs
        )
    }
}
