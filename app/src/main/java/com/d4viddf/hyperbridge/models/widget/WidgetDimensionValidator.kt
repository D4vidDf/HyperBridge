package com.d4viddf.hyperbridge.models.widget

/**
 * Pure, Android-free validator/clamper for a [CustomWidgetDocument] against HyperOS island
 * constraints: a fixed-width canvas, a bounded node count/recursion depth (RemoteViews trees get
 * expensive fast), and sane per-node dimensions. Runs before every save and before a widget is
 * bound to a notification/permanent-island slot.
 */
object WidgetDimensionValidator {

    const val CANVAS_WIDTH_DP = 350
    const val MAX_DEPTH = 6
    const val MAX_NODE_COUNT = 40
    const val MIN_FONT_SP = 8
    const val MAX_FONT_SP = 28
    /** HyperOS islands realistically fit 2-3 tappable buttons; more is a warning, not a hard block. */
    const val MAX_RECOMMENDED_BUTTONS = 3

    data class ValidationResult(
        val errors: List<String>,
        val clamped: CustomWidgetDocument
    ) {
        val isValid: Boolean get() = errors.isEmpty()
    }

    fun validate(doc: CustomWidgetDocument): ValidationResult {
        val errors = mutableListOf<String>()
        val canvasHeight = doc.canvas.heightDp
        var totalNodes = 0
        var buttonCount = 0

        fun clampBounds(bounds: NodeBounds): NodeBounds {
            val x = bounds.x.coerceIn(0, CANVAS_WIDTH_DP)
            val y = bounds.y.coerceIn(0, canvasHeight)
            val w = bounds.widthDp?.coerceIn(1, CANVAS_WIDTH_DP)
            val h = bounds.heightDp?.coerceIn(1, canvasHeight)
            return if (x == bounds.x && y == bounds.y && w == bounds.widthDp && h == bounds.heightDp) {
                bounds
            } else {
                errors.add("Node bounds out of canvas range, clamped: $bounds -> x=$x,y=$y,w=$w,h=$h")
                NodeBounds(x, y, w, h)
            }
        }

        fun clampNode(node: CustomWidgetNode, depth: Int): CustomWidgetNode {
            totalNodes++
            if (depth > MAX_DEPTH) {
                errors.add("Node ${node.id} exceeds max recursion depth ($MAX_DEPTH)")
            }
            if (totalNodes > MAX_NODE_COUNT) {
                errors.add("Widget exceeds max node count ($MAX_NODE_COUNT)")
            }
            val clampedBounds = clampBounds(node.bounds)
            return when (node) {
                is TextNode -> {
                    val fontSize = node.fontSizeSp.coerceIn(MIN_FONT_SP, MAX_FONT_SP)
                    if (fontSize != node.fontSizeSp) {
                        errors.add("Text node ${node.id} font size clamped: ${node.fontSizeSp} -> $fontSize")
                    }
                    node.copy(bounds = clampedBounds, fontSizeSp = fontSize)
                }
                is ImageNode -> node.copy(bounds = clampedBounds)
                is ProgressNode -> node.copy(bounds = clampedBounds)
                is ButtonNode -> {
                    buttonCount++
                    node.copy(bounds = clampedBounds)
                }
                is LayoutContainer -> {
                    val clampedChildren = node.children.map { clampNode(it, depth + 1) }
                    if (node.layout == ContainerLayout.ABSOLUTE) {
                        checkOverlaps(node.children, node.id, errors)
                    }
                    node.copy(bounds = clampedBounds, children = clampedChildren)
                }
            }
        }

        val clampedRoot = clampNode(doc.root, depth = 1) as LayoutContainer
        if (buttonCount > MAX_RECOMMENDED_BUTTONS) {
            errors.add("Warning: widget has $buttonCount buttons; HyperOS islands realistically fit at most $MAX_RECOMMENDED_BUTTONS")
        }
        return ValidationResult(errors.toList(), doc.copy(root = clampedRoot))
    }

    /** Overlap is a warning only (added to [errors] but never blocks a save). */
    private fun checkOverlaps(children: List<CustomWidgetNode>, containerId: String, errors: MutableList<String>) {
        for (i in children.indices) {
            for (j in i + 1 until children.size) {
                val a = children[i].bounds
                val b = children[j].bounds
                val aw = a.widthDp ?: 0
                val ah = a.heightDp ?: 0
                val bw = b.widthDp ?: 0
                val bh = b.heightDp ?: 0
                val overlapsX = a.x < b.x + bw && b.x < a.x + aw
                val overlapsY = a.y < b.y + bh && b.y < a.y + ah
                if (overlapsX && overlapsY) {
                    errors.add("Warning: nodes ${children[i].id} and ${children[j].id} overlap in container $containerId")
                }
            }
        }
    }
}
