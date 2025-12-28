package com.d4viddf.hyperbridge.data.theme

import android.service.notification.StatusBarNotification
import android.util.Log
import com.d4viddf.hyperbridge.models.theme.AppThemeOverride
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.theme.RuleConditions

/**
 * THE INTERCEPTOR
 * Checks incoming notifications against the Active Theme's logic rules.
 */
class RulesEngine {

    private val TAG = "HyperRules"

    // [New] Cache regex patterns to avoid compiling them 100 times a second
    private val regexCache = mutableMapOf<String, Regex>()

    /**
     * Main Entry Point.
     * Returns a Result if a rule matches, or NULL if standard processing should continue.
     */
    fun match(
        sbn: StatusBarNotification,
        title: String,
        text: String,
        theme: HyperTheme?
    ): RuleMatchResult? {
        if (theme == null || theme.rules.isEmpty()) return null

        // Sort rules by priority (Highest first) -> First match wins
        val sortedRules = theme.rules.sortedByDescending { it.priority }

        for (rule in sortedRules) {
            if (checkConditions(rule.conditions, sbn, title, text)) {
                Log.d(TAG, "MATCHED Rule: ${rule.id} (${rule.comment}) for ${sbn.packageName}")

                return RuleMatchResult(
                    targetLayout = rule.targetLayout, // e.g., "MEDIA", "CALL"
                    overrides = rule.overrides        // Specific colors/icons for this match
                )
            }
        }

        return null // No rules matched -> Use default logic
    }

    private fun checkConditions(
        cond: RuleConditions,
        sbn: StatusBarNotification,
        title: String,
        text: String
    ): Boolean {
        // 1. Package Name Check
        if (!cond.packageName.isNullOrEmpty()) {
            // Supports regex in package name too (e.g. "com.google.android.*")
            if (!safeRegexMatch(cond.packageName, sbn.packageName)) return false
        }

        // 2. Title Regex Check
        if (!cond.titleRegex.isNullOrEmpty()) {
            if (!safeRegexMatch(cond.titleRegex, title)) return false
        }

        // 3. Text Regex Check
        if (!cond.textRegex.isNullOrEmpty()) {
            if (!safeRegexMatch(cond.textRegex, text)) return false
        }

        // 4. External State (Plugins) - Placeholder for now
        if (!cond.externalStateKey.isNullOrEmpty()) {
            // In future: Check against a StateManager.currentState map
            // For now, if a rule requires external state, we fail it safely
            return false
        }

        return true
    }

    private fun safeRegexMatch(pattern: String, input: String): Boolean {
        return try {
            val regex = regexCache.getOrPut(pattern) {
                Regex(pattern, RegexOption.IGNORE_CASE)
            }
            regex.containsMatchIn(input)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid Regex in theme: $pattern")
            false
        }
    }
}

/**
 * The output of the engine.
 * Tells the Service exactly what to do.
 */
data class RuleMatchResult(
    val targetLayout: String?,      // Maps to NotificationType name (e.g. "MEDIA")
    val overrides: AppThemeOverride? // Custom colors/icons to pass to Translator
)