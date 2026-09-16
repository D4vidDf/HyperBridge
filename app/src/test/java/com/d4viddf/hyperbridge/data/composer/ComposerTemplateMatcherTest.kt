package com.d4viddf.hyperbridge.data.composer

import com.d4viddf.hyperbridge.models.composer.ComposerTemplate
import com.d4viddf.hyperbridge.models.composer.IslandTemplateDefinition
import com.d4viddf.hyperbridge.models.composer.TemplateRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComposerTemplateMatcherTest {

    private fun template(
        id: String,
        packageNameRegex: String? = null,
        titleRegex: String? = null,
        textRegex: String? = null,
        priority: Int = 100,
        enabled: Boolean = true
    ) = ComposerTemplate(
        id = id,
        name = id,
        definition = IslandTemplateDefinition(),
        rule = TemplateRule(packageNameRegex, titleRegex, textRegex, priority),
        enabled = enabled
    )

    @Test
    fun matchesByPackageNameOnly() {
        val t = template("t1", packageNameRegex = "com\\.whatsapp")
        val result = ComposerTemplateMatcher.match(listOf(t), "com.whatsapp", "Hi", "text")
        assertEquals("t1", result?.id)

        val noMatch = ComposerTemplateMatcher.match(listOf(t), "com.telegram", "Hi", "text")
        assertNull(noMatch)
    }

    @Test
    fun matchesByTitleRegex() {
        val t = template("t1", titleRegex = "(?i)delivery")
        assertEquals("t1", ComposerTemplateMatcher.match(listOf(t), "com.app", "Your Delivery is here", "")?.id)
        assertNull(ComposerTemplateMatcher.match(listOf(t), "com.app", "Unrelated", "")?.id)
    }

    @Test
    fun matchesByTextRegex() {
        val t = template("t1", textRegex = "\\d{6}")
        assertEquals("t1", ComposerTemplateMatcher.match(listOf(t), "com.app", "OTP", "Your code is 123456")?.id)
        assertNull(ComposerTemplateMatcher.match(listOf(t), "com.app", "OTP", "no code here")?.id)
    }

    @Test
    fun higherPriorityWinsOnConflictingMatch() {
        val low = template("low", packageNameRegex = "com\\.app", priority = 10)
        val high = template("high", packageNameRegex = "com\\.app", priority = 50)
        val result = ComposerTemplateMatcher.match(listOf(low, high), "com.app", "title", "text")
        assertEquals("high", result?.id)
    }

    @Test
    fun disabledTemplatesNeverMatch() {
        val t = template("t1", packageNameRegex = "com\\.app", enabled = false)
        assertNull(ComposerTemplateMatcher.match(listOf(t), "com.app", "title", "text"))
    }

    @Test
    fun invalidRegexIsCaughtAndTreatedAsNonMatch() {
        val t = template("t1", packageNameRegex = "com.app", titleRegex = "[unterminated")
        assertNull(ComposerTemplateMatcher.match(listOf(t), "com.app", "title", "text"))
    }

    @Test
    fun templateWithNoConditionsNeverMatchesAsCatchAll() {
        val t = template("t1")
        assertNull(ComposerTemplateMatcher.match(listOf(t), "com.app", "title", "text"))
    }
}
