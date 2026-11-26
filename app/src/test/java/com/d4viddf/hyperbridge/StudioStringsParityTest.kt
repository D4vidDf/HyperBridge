package com.d4viddf.hyperbridge

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StudioStringsParityTest {

    @Test
    fun verifyAllStudioStringsHaveSpanishTranslations() {
        // Resolve paths relative to working directory or project root
        val projectDir = File(System.getProperty("user.dir") ?: ".")
        val baseStringsFile = File(projectDir, "app/src/main/res/values/strings.xml").let {
            if (it.exists()) it else File(projectDir, "src/main/res/values/strings.xml")
        }
        val spanishStringsFile = File(projectDir, "app/src/main/res/values-es-rES/strings.xml").let {
            if (it.exists()) it else File(projectDir, "src/main/res/values-es-rES/strings.xml")
        }

        assertTrue("base strings.xml should exist at ${baseStringsFile.absolutePath}", baseStringsFile.exists())
        assertTrue("spanish strings.xml should exist at ${spanishStringsFile.absolutePath}", spanishStringsFile.exists())

        val stringNameRegex = Regex("""<string\s+name="([^"]+)"""")

        val baseKeys = stringNameRegex.findAll(baseStringsFile.readText())
            .map { it.groupValues[1] }
            .filter { it.startsWith("studio_") }
            .toSet()

        val spanishKeys = stringNameRegex.findAll(spanishStringsFile.readText())
            .map { it.groupValues[1] }
            .filter { it.startsWith("studio_") }
            .toSet()

        val missingInSpanish = baseKeys - spanishKeys
        assertTrue(
            "The following studio strings are missing from values-es-rES/strings.xml: $missingInSpanish",
            missingInSpanish.isEmpty()
        )
    }
}
