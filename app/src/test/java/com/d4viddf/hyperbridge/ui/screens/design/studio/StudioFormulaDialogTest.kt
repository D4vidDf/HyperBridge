package com.d4viddf.hyperbridge.ui.screens.design.studio

import com.d4viddf.hyperbridge.data.widget.WidgetVariableEngine
import com.d4viddf.hyperbridge.models.widget.BindableProperty
import com.d4viddf.hyperbridge.models.widget.ButtonNode
import com.d4viddf.hyperbridge.models.widget.ImageNode
import com.d4viddf.hyperbridge.models.widget.ProgressNode
import com.d4viddf.hyperbridge.models.widget.TextNode
import com.d4viddf.hyperbridge.models.widget.withBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioFormulaDialogTest {

    private val engine = WidgetVariableEngine()

    @Test
    fun tokenCatalogContainsAllExpectedCategories() {
        assertEquals(4, FormulaTokenCategory.entries.size)
        assertTrue(FORMULA_TOKEN_CATALOG.containsKey(FormulaTokenCategory.NOTIFICATION))
        assertTrue(FORMULA_TOKEN_CATALOG.containsKey(FormulaTokenCategory.DEVICE))
        assertTrue(FORMULA_TOKEN_CATALOG.containsKey(FormulaTokenCategory.THEME))
        assertTrue(FORMULA_TOKEN_CATALOG.containsKey(FormulaTokenCategory.SOURCES))
    }

    @Test
    fun notificationTokensResolveAcrossScenarios() {
        val standardCtx = StudioPreviewScenario.STANDARD.toVariableContext()
        assertEquals("Calendar Event", engine.resolve("{notif.title}", standardCtx))
        assertEquals("com.google.android.calendar", engine.resolve("{notif.package}", standardCtx))

        val otpCtx = StudioPreviewScenario.OTP.toVariableContext()
        assertTrue(engine.resolve("{notif.text}", otpCtx).contains("849201"))

        val progressCtx = StudioPreviewScenario.PROGRESS.toVariableContext()
        assertEquals("64", engine.resolve("{notif.progress}", progressCtx))
    }

    @Test
    fun deviceAndThemeTokensResolveAcrossScenarios() {
        StudioPreviewScenario.entries.forEach { scenario ->
            val ctx = scenario.toVariableContext()
            assertNotNull(engine.resolve("{device.battery}", ctx))
            assertNotNull(engine.resolve("{time.now}", ctx))
            assertEquals("#3DDA82", engine.resolve("{theme.primary}", ctx))
            assertEquals("#00E5FF", engine.resolve("{theme.accent}", ctx))
            assertEquals("#1E1E1E", engine.resolve("{theme.surface}", ctx))
        }
    }

    @Test
    fun formulaCursorInsertionSimulation() {
        // Simulating cursor insertion helper:
        fun insert(currentText: String, start: Int, end: Int, token: String): Pair<String, Int> {
            val newText = currentText.replaceRange(start, end, token)
            return Pair(newText, start + token.length)
        }

        // 1. Insert at end
        val (res1, cur1) = insert("Hello ", 6, 6, "{notif.title}")
        assertEquals("Hello {notif.title}", res1)
        assertEquals(19, cur1)

        // 2. Insert in middle
        val (res2, cur2) = insert("Battery: %", 9, 9, "{device.battery}")
        assertEquals("Battery: {device.battery}%", res2)
        assertEquals(25, cur2)

        // 3. Replace selection
        val (res3, cur3) = insert("Replace THIS token", 8, 12, "{theme.accent}")
        assertEquals("Replace {theme.accent} token", res3)
        assertEquals(22, cur3)
    }

    @Test
    fun nodeBindingHelperFunctionsRoundTrip() {
        val text = TextNode(id = "txt", template = "Title")
        val boundText = text.withBinding(BindableProperty.TEXT_COLOR.key, "{theme.accent}")
        assertEquals("{theme.accent}", boundText.bindings[BindableProperty.TEXT_COLOR.key])

        val clearedText = boundText.withBinding(BindableProperty.TEXT_COLOR.key, null)
        assertNull(clearedText.bindings[BindableProperty.TEXT_COLOR.key])

        val image = ImageNode(id = "img")
        val boundImg = image.withBinding(BindableProperty.IMAGE_TINT.key, "{theme.primary}")
        assertEquals("{theme.primary}", boundImg.bindings[BindableProperty.IMAGE_TINT.key])

        val progress = ProgressNode(id = "prog")
        val boundProg = progress.withBinding(BindableProperty.PROGRESS_COLOR.key, "{theme.accent}")
        assertEquals("{theme.accent}", boundProg.bindings[BindableProperty.PROGRESS_COLOR.key])

        val button = ButtonNode(id = "btn", label = "Click")
        val boundBtn = button.withBinding(BindableProperty.BUTTON_BACKGROUND.key, "{theme.primary}")
        assertEquals("{theme.primary}", boundBtn.bindings[BindableProperty.BUTTON_BACKGROUND.key])
    }
}
