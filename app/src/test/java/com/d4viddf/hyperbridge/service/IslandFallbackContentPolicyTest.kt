package com.d4viddf.hyperbridge.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IslandFallbackContentPolicyTest {

    @Test
    fun repeatsTheSourceWhenTheTapOpensTheSourceApp() {
        assertTrue(IslandFallbackContentPolicy.showsSourceContent(true, "Alice", "Calling…"))
        assertTrue(IslandFallbackContentPolicy.showsSourceContent(true, "", "Download complete"))
        assertTrue(IslandFallbackContentPolicy.showsSourceContent(true, "Alice", ""))
    }

    @Test
    fun keepsTheDiagnosticsHintWithoutFocusPermission() {
        assertFalse(IslandFallbackContentPolicy.showsSourceContent(false, "Alice", "Calling…"))
    }

    @Test
    fun keepsTheDiagnosticsHintWhenTheSourceIsEmpty() {
        assertFalse(IslandFallbackContentPolicy.showsSourceContent(true, "", ""))
        assertFalse(IslandFallbackContentPolicy.showsSourceContent(true, "  ", " "))
    }
}
