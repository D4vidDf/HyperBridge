package com.d4viddf.hyperbridge.models.widget

/**
 * Checks a `.hwidget` has to pass before anything in it is trusted. A `.hwidget` is a file anyone
 * can share, so its id, its size and its nesting are all attacker-controlled.
 */
object WidgetImportGuard {

    /** Unpacked size cap for the whole archive (a zip bomb otherwise fills the cache dir). */
    const val MAX_UNPACKED_BYTES = 20L * 1024 * 1024

    /** widget.json is a few KB for any real design. */
    const val MAX_CONFIG_BYTES = 256L * 1024

    /**
     * Bracket depth widget.json may reach. A node level costs about three (`{` node, `[` children,
     * `{` child), so this leaves plenty of room above [WidgetDimensionValidator.MAX_DEPTH] while
     * keeping kotlinx's recursive decoder far away from a StackOverflowError.
     */
    const val MAX_JSON_NESTING = 64

    private val SAFE_ID = Regex("[A-Za-z0-9_-][A-Za-z0-9._-]{0,127}")

    /** True when [id] can be used as a folder name under the widgets dir as is. */
    fun isSafeId(id: String): Boolean = SAFE_ID.matches(id) && id != "." && id != ".."

    /** Deepest `{`/`[` nesting in [json], ignoring brackets inside strings. */
    fun jsonNestingDepth(json: String): Int {
        var depth = 0
        var max = 0
        var inString = false
        var escaped = false
        for (c in json) {
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
                continue
            }
            when (c) {
                '"' -> inString = true
                '{', '[' -> {
                    depth++
                    if (depth > max) max = depth
                }
                '}', ']' -> depth--
            }
        }
        return max
    }
}
