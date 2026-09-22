package com.d4viddf.hyperbridge.models.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetImportGuardTest {

    @Test
    fun acceptsGeneratedIds() {
        assertTrue(WidgetImportGuard.isSafeId("3f2b8c1e-5d4a-4b6e-9f00-123456789abc"))
        assertTrue(WidgetImportGuard.isSafeId("demo-ntfy-widget"))
    }

    @Test
    fun rejectsIdsThatEscapeTheWidgetsDir() {
        listOf("", ".", "..", "../databases", "a/b", "a\\b", "..\\x", ".hidden").forEach {
            assertFalse(it, WidgetImportGuard.isSafeId(it))
        }
    }

    @Test
    fun measuresNestingAndIgnoresBracketsInStrings() {
        assertEquals(0, WidgetImportGuard.jsonNestingDepth("\"[[[{{{\""))
        assertEquals(3, WidgetImportGuard.jsonNestingDepth("""{"a":[{"b":"]]]\"}}}"}]}"""))
    }

    @Test
    fun flagsAHostilelyDeepDocument() {
        val deep = "{\"children\":[".repeat(5_000) + "]}".repeat(5_000)
        assertTrue(WidgetImportGuard.jsonNestingDepth(deep) > WidgetImportGuard.MAX_JSON_NESTING)
    }
}
