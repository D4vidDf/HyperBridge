package com.d4viddf.hyperbridge.models.widget

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Layer ordering and the tap/visibility round trip added for #328. */
class CustomWidgetLayerTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun document(vararg ids: String) = CustomWidgetDocument(
        id = "doc",
        meta = CustomWidgetMetadata(name = "Doc"),
        root = LayoutContainer(
            id = "root",
            layout = ContainerLayout.ABSOLUTE,
            children = ids.map { TextNode(id = it, template = it) }
        )
    )

    private fun order(doc: CustomWidgetDocument) = doc.root.children.map { it.id }

    @Test
    fun bringingForwardMovesTheElementUpTheStack() {
        val doc = document("a", "b", "c")
        assertEquals(listOf("a", "c", "b"), order(doc.moveNode("b", 1)))
    }

    @Test
    fun sendingBackMovesTheElementDownTheStack() {
        val doc = document("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), order(doc.moveNode("b", -1)))
    }

    @Test
    fun movingPastTheEndsIsClampedRatherThanWrapped() {
        val doc = document("a", "b", "c")
        assertEquals(listOf("a", "b", "c"), order(doc.moveNode("c", 1)))
        assertEquals(listOf("a", "b", "c"), order(doc.moveNode("a", -1)))
        assertEquals(listOf("b", "c", "a"), order(doc.moveNode("a", 5)))
    }

    @Test
    fun movingAnUnknownIdLeavesTheTreeAlone() {
        val doc = document("a", "b")
        assertEquals(listOf("a", "b"), order(doc.moveNode("nope", 1)))
    }

    @Test
    fun aNestedChildIsMovedInsideItsOwnGroup() {
        val doc = CustomWidgetDocument(
            id = "doc",
            meta = CustomWidgetMetadata(name = "Doc"),
            root = LayoutContainer(
                id = "root",
                children = listOf(
                    LayoutContainer(
                        id = "group",
                        children = listOf(TextNode(id = "x", template = "x"), TextNode(id = "y", template = "y"))
                    )
                )
            )
        )

        val moved = doc.moveNode("y", -1)
        val group = moved.findNode("group") as LayoutContainer
        assertEquals(listOf("y", "x"), group.children.map { it.id })
    }

    @Test
    fun parentOfFindsTheOwningGroupAndNothingForTheRoot() {
        val doc = document("a")
        assertEquals("root", doc.parentOf("a")?.id)
        assertNull(doc.parentOf("root"))
    }

    @Test
    fun conditionsAndTapActionsSurviveTheHwidgetRoundTrip() {
        val doc = CustomWidgetDocument(
            id = "doc",
            meta = CustomWidgetMetadata(name = "Doc"),
            root = LayoutContainer(
                id = "root",
                layout = ContainerLayout.ABSOLUTE,
                children = listOf(
                    TextNode(
                        id = "code",
                        template = "{notif.title}",
                        showIf = NodeCondition.HasSmartAction("OTP"),
                        onClick = ButtonAction.SmartAction("OTP")
                    ),
                    ButtonNode(
                        id = "reply",
                        label = "Reply",
                        action = ButtonAction.NotificationAction(1),
                        showIf = NodeCondition.HasInlineReply
                    )
                )
            )
        )

        val encoded = json.encodeToString(CustomWidgetDocument.serializer(), doc)
        val restored = json.decodeFromString(CustomWidgetDocument.serializer(), encoded)

        val text = restored.findNode("code") as TextNode
        assertEquals(NodeCondition.HasSmartAction("OTP"), text.showIf)
        assertEquals(ButtonAction.SmartAction("OTP"), text.onClick)

        val button = restored.findNode("reply") as ButtonNode
        assertEquals(ButtonAction.NotificationAction(1), button.action)
        // A button's tap action is what it is clicked for, so onClick mirrors it.
        assertEquals(button.action, button.onClick)
        assertNotNull(restored.findNode("root"))
    }
}
