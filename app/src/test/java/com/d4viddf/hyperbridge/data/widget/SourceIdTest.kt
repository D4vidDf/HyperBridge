package com.d4viddf.hyperbridge.data.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceIdTest {

    @Test
    fun acceptsPlainIds() {
        listOf("weather", "meteo.isla", "now_playing-2").forEach {
            assertTrue(it, SourceRepository.isValidSourceId(it))
        }
    }

    @Test
    fun rejectsIdsThatWouldEscapeTheIconsDir() {
        listOf("", "..", "../../shared_prefs/x", "a/b", "a\\b", ".x", "x".repeat(65)).forEach {
            assertFalse(it, SourceRepository.isValidSourceId(it))
        }
    }
}
