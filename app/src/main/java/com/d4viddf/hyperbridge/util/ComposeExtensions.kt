package com.d4viddf.hyperbridge.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Reusable helper to parse &lt;b&gt;text&lt;/b&gt; tags from strings.xml into Bold SpanStyles.
 * Used for Changelogs, Credits, and Info screens.
 */
@Composable
fun String.parseBold(): androidx.compose.ui.text.AnnotatedString {
    // 1. Fix newline escape characters from XML
    val rawText = this.replace("\\n", "\n")

    // 2. Regex to match &lt;b&gt;content&lt;/b&gt; (XML escaped tags)
    // We also check for standard <b> just in case.
    val boldRegex = Regex("(&lt;b&gt;|<b>)(.*?)(&lt;/b&gt;|</b>)")

    return buildAnnotatedString {
        var lastIndex = 0

        boldRegex.findAll(rawText).forEach { match ->
            // Append normal text BEFORE the tag
            append(rawText.substring(lastIndex, match.range.first))

            // Append the content inside tags as BOLD
            // groupValues[2] contains the text inside the tags
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groupValues[2])
            }

            // Move index past this match
            lastIndex = match.range.last + 1
        }

        // Append any remaining normal text
        if (lastIndex < rawText.length) {
            append(rawText.substring(lastIndex))
        }
    }
}