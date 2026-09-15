package com.d4viddf.hyperbridge.data.composer

import android.util.Log
import com.d4viddf.hyperbridge.models.composer.ComposerTemplate

/**
 * Pure matcher for Phase 4 composer templates (issue #272) — a line-for-line clone of
 * [com.d4viddf.hyperbridge.data.theme.RulesEngine]'s matching logic, kept separate so it can be
 * unit tested with plain strings (no StatusBarNotification needed) and so a future
 * TranslatorRegistry (Phase 3) can absorb it without depending on NotificationReaderService.
 */
object ComposerTemplateMatcher {

    private const val TAG = "ComposerTemplateMatcher"
    private val regexCache = mutableMapOf<String, Regex>()

    /**
     * Returns the highest-priority enabled template whose rule matches, or null if none does.
     * First match wins after sorting by priority (descending).
     */
    fun match(
        templates: List<ComposerTemplate>,
        packageName: String,
        title: String,
        text: String
    ): ComposerTemplate? {
        val sorted = templates.filter { it.enabled }.sortedByDescending { it.rule.priority }
        for (template in sorted) {
            if (matches(template, packageName, title, text)) return template
        }
        return null
    }

    private fun matches(template: ComposerTemplate, packageName: String, title: String, text: String): Boolean {
        val rule = template.rule

        if (!rule.packageNameRegex.isNullOrEmpty()) {
            if (!safeRegexMatch(rule.packageNameRegex, packageName)) return false
        }
        if (!rule.titleRegex.isNullOrEmpty()) {
            if (!safeRegexMatch(rule.titleRegex, title)) return false
        }
        if (!rule.textRegex.isNullOrEmpty()) {
            if (!safeRegexMatch(rule.textRegex, text)) return false
        }

        // A rule with no conditions at all matches everything; guard against that being an
        // accidental catch-all by requiring at least one condition to be set.
        return rule.packageNameRegex != null || rule.titleRegex != null || rule.textRegex != null
    }

    private fun safeRegexMatch(pattern: String, input: String): Boolean {
        return try {
            val regex = regexCache.getOrPut(pattern) { Regex(pattern, RegexOption.IGNORE_CASE) }
            regex.containsMatchIn(input)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid regex in composer template rule: $pattern")
            false
        }
    }
}
