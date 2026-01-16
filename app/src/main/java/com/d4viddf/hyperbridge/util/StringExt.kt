package com.d4viddf.hyperbridge.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

fun String.parseBold(): AnnotatedString {
    val parts = this.split("<b>", "</b>", "&lt;b&gt;", "&lt;/b&gt;")
    return buildAnnotatedString {
        var bold = false
        for (part in parts) {
            if (bold) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                parseItalic(part)
            }
            bold = !bold
        }
    }
}

// Helper to handle <i> tags inside non-bold sections
private fun AnnotatedString.Builder.parseItalic(text: String) {
    val parts = text.split("<i>", "</i>", "&lt;i&gt;", "&lt;/i&gt;")
    var italic = false
    for (part in parts) {
        if (italic) {
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                append(part)
            }
        } else {
            append(part)
        }
        italic = !italic
    }
}