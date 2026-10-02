package com.d4viddf.hyperbridge.models.widget

import com.d4viddf.hyperbridge.data.widget.VariableContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NodeConditionEvaluatorTest {

    private val richNotification = VariableContext(
        notifTitle = "Your code is 418302",
        notifText = "Never share it",
        notifProgress = 40,
        notificationActionTitles = listOf("Open", "Mute"),
        hasInlineReply = true,
        smartActionTypes = setOf("OTP")
    )

    private val bareNotification = VariableContext(
        notifTitle = "Hello",
        notifText = ""
    )

    private fun visible(condition: NodeCondition, ctx: VariableContext) =
        NodeConditionEvaluator.isVisible(condition, ctx)

    @Test
    fun alwaysIsAlwaysVisible() {
        assertTrue(visible(NodeCondition.Always, bareNotification))
    }

    @Test
    fun notificationActionConditionFollowsTheButtonCount() {
        assertTrue(visible(NodeCondition.HasNotificationAction(0), richNotification))
        assertTrue(visible(NodeCondition.HasNotificationAction(1), richNotification))
        assertFalse(visible(NodeCondition.HasNotificationAction(2), richNotification))
        assertFalse(visible(NodeCondition.HasNotificationAction(0), bareNotification))
    }

    @Test
    fun inlineReplyConditionNeedsARepliableNotification() {
        assertTrue(visible(NodeCondition.HasInlineReply, richNotification))
        assertFalse(visible(NodeCondition.HasInlineReply, bareNotification))
    }

    @Test
    fun smartActionConditionMatchesTheTypeCaseInsensitively() {
        assertTrue(visible(NodeCondition.HasSmartAction("otp"), richNotification))
        assertFalse(visible(NodeCondition.HasSmartAction("URL"), richNotification))
    }

    @Test
    fun progressConditionNeedsAProgressValue() {
        assertTrue(visible(NodeCondition.HasProgress, richNotification))
        assertFalse(visible(NodeCondition.HasProgress, bareNotification))
    }

    @Test
    fun notBlankResolvesTheTemplateFirst() {
        assertTrue(visible(NodeCondition.NotBlank("{notif.title}"), bareNotification))
        assertFalse(visible(NodeCondition.NotBlank("{notif.text}"), bareNotification))
        assertFalse(visible(NodeCondition.NotBlank("{notif.subtext}"), bareNotification))
    }

    @Test
    fun matchesRunsTheRegexOverTheResolvedValue() {
        assertTrue(visible(NodeCondition.Matches("{notif.title}", "\\d{6}"), richNotification))
        assertFalse(visible(NodeCondition.Matches("{notif.title}", "\\d{6}"), bareNotification))
    }

    @Test
    fun aBrokenRegexHidesTheElementInsteadOfCrashing() {
        assertFalse(visible(NodeCondition.Matches("{notif.title}", "([unclosed"), richNotification))
    }

    @Test
    fun notInvertsTheInnerCondition() {
        assertFalse(visible(NodeCondition.Not(NodeCondition.HasInlineReply), richNotification))
        assertTrue(visible(NodeCondition.Not(NodeCondition.HasInlineReply), bareNotification))
    }

    @Test
    fun pruneDropsTheChildrenThatFailTheirCondition() {
        val root = LayoutContainer(
            id = "root",
            children = listOf(
                TextNode(id = "title", template = "{notif.title}"),
                ButtonNode(id = "reply", label = "Reply", showIf = NodeCondition.HasInlineReply),
                ButtonNode(id = "copy", label = "Copy", showIf = NodeCondition.HasSmartAction("OTP"))
            )
        )

        val kept = NodeConditionEvaluator.prune(root, richNotification) as LayoutContainer
        assertEquals(listOf("title", "reply", "copy"), kept.children.map { it.id })

        val pruned = NodeConditionEvaluator.prune(root, bareNotification) as LayoutContainer
        assertEquals(listOf("title"), pruned.children.map { it.id })
    }

    @Test
    fun pruneDropsAWholeGroupWhenTheGroupItselfIsHidden() {
        val root = LayoutContainer(
            id = "root",
            children = listOf(
                LayoutContainer(
                    id = "actions",
                    showIf = NodeCondition.HasNotificationAction(0),
                    children = listOf(TextNode(id = "label", template = "Actions"))
                )
            )
        )

        val pruned = NodeConditionEvaluator.prune(root, bareNotification) as LayoutContainer
        assertTrue(pruned.children.isEmpty())
    }

    @Test
    fun pruneReturnsNullWhenTheNodeItselfIsHidden() {
        val hidden = TextNode(id = "t", template = "x", showIf = NodeCondition.HasInlineReply)
        assertNull(NodeConditionEvaluator.prune(hidden, bareNotification))
    }
}
