package com.d4viddf.hyperbridge.models.widget

import com.d4viddf.hyperbridge.data.widget.VariableContext
import com.d4viddf.hyperbridge.data.widget.WidgetVariableEngine

/**
 * Decides whether a node is shown for a given notification (#328). Pure and plain-JUnit testable:
 * everything it needs is already resolved into the [VariableContext] by the caller.
 *
 * An unresolvable condition is never fatal -- a malformed regex, say, just means "not shown"
 * rather than a rendering crash.
 */
object NodeConditionEvaluator {

    fun isVisible(
        condition: NodeCondition,
        ctx: VariableContext,
        engine: WidgetVariableEngine = WidgetVariableEngine()
    ): Boolean = when (condition) {
        is NodeCondition.Always -> true

        is NodeCondition.HasSmartAction ->
            ctx.smartActionTypes.any { it.equals(condition.type, ignoreCase = true) }

        is NodeCondition.HasNotificationAction ->
            condition.index in ctx.notificationActionTitles.indices

        is NodeCondition.HasInlineReply -> ctx.hasInlineReply

        is NodeCondition.HasProgress -> ctx.notifProgress != null

        is NodeCondition.NotBlank ->
            engine.resolve(condition.template, ctx).isNotBlank()

        is NodeCondition.Matches -> runCatching {
            Regex(condition.regex).containsMatchIn(engine.resolve(condition.template, ctx))
        }.getOrDefault(false)

        is NodeCondition.Not -> !isVisible(condition.condition, ctx, engine)
    }

    /** The node itself, with any child that fails its own condition already dropped. */
    fun prune(node: CustomWidgetNode, ctx: VariableContext, engine: WidgetVariableEngine = WidgetVariableEngine()): CustomWidgetNode? {
        if (!isVisible(node.showIf, ctx, engine)) return null
        if (node !is LayoutContainer) return node
        return node.copy(children = node.children.mapNotNull { prune(it, ctx, engine) })
    }
}
