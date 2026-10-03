package com.d4viddf.hyperbridge.ui.screens.design.studio

import com.d4viddf.hyperbridge.data.widget.WidgetVariableEngine
import com.d4viddf.hyperbridge.models.widget.NodeCondition
import com.d4viddf.hyperbridge.models.widget.NodeConditionEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioPreviewScenarioTest {

    private val engine = WidgetVariableEngine()

    @Test
    fun standardScenarioProvidesBasicNotificationValues() {
        val ctx = StudioPreviewScenario.STANDARD.toVariableContext()
        assertNotNull(ctx.notifTitle)
        assertNotNull(ctx.notifText)
        assertEquals(null, ctx.notifProgress)
        assertFalse(ctx.hasInlineReply)
        assertTrue(ctx.smartActionTypes.isEmpty())

        assertTrue(NodeConditionEvaluator.isVisible(NodeCondition.Always, ctx, engine))
        assertFalse(NodeConditionEvaluator.isVisible(NodeCondition.HasProgress, ctx, engine))
        assertFalse(NodeConditionEvaluator.isVisible(NodeCondition.HasInlineReply, ctx, engine))
        assertFalse(NodeConditionEvaluator.isVisible(NodeCondition.HasSmartAction("OTP"), ctx, engine))
    }

    @Test
    fun messageScenarioEnablesInlineReplyCondition() {
        val ctx = StudioPreviewScenario.MESSAGE.toVariableContext()
        assertTrue(ctx.hasInlineReply)
        assertTrue(NodeConditionEvaluator.isVisible(NodeCondition.HasInlineReply, ctx, engine))
        assertFalse(NodeConditionEvaluator.isVisible(NodeCondition.HasProgress, ctx, engine))
    }

    @Test
    fun progressScenarioEnablesProgressConditionAndResolvesProgressToken() {
        val ctx = StudioPreviewScenario.PROGRESS.toVariableContext()
        assertNotNull(ctx.notifProgress)
        assertTrue(NodeConditionEvaluator.isVisible(NodeCondition.HasProgress, ctx, engine))

        val resolved = engine.resolve("{notif.progress}%", ctx)
        assertEquals("64%", resolved)
    }

    @Test
    fun otpScenarioMatchesOtpSmartActionCondition() {
        val ctx = StudioPreviewScenario.OTP.toVariableContext()
        assertTrue(ctx.smartActionTypes.contains("OTP"))
        assertTrue(NodeConditionEvaluator.isVisible(NodeCondition.HasSmartAction("OTP"), ctx, engine))
        assertTrue(NodeConditionEvaluator.isVisible(NodeCondition.HasSmartAction("otp"), ctx, engine))
        assertFalse(NodeConditionEvaluator.isVisible(NodeCondition.HasSmartAction("URL"), ctx, engine))
    }

    @Test
    fun mediaScenarioProvidesMultipleActions() {
        val ctx = StudioPreviewScenario.MEDIA.toVariableContext()
        assertEquals(3, ctx.notificationActionTitles.size)
        assertTrue(NodeConditionEvaluator.isVisible(NodeCondition.HasNotificationAction(0), ctx, engine))
        assertTrue(NodeConditionEvaluator.isVisible(NodeCondition.HasNotificationAction(1), ctx, engine))
        assertTrue(NodeConditionEvaluator.isVisible(NodeCondition.HasNotificationAction(2), ctx, engine))
        assertFalse(NodeConditionEvaluator.isVisible(NodeCondition.HasNotificationAction(3), ctx, engine))
    }
}
