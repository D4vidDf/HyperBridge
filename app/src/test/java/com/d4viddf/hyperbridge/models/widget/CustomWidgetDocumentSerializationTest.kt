package com.d4viddf.hyperbridge.models.widget

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomWidgetDocumentSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Test
    fun roundTripsADocumentContainingEveryNodeType() {
        val doc = CustomWidgetDocument(
            id = "widget-1",
            meta = CustomWidgetMetadata(name = "Weather", author = "Noel", version = 2),
            canvas = CanvasSize.LARGE,
            boundPackage = "com.example.weather",
            permanentIslandEligible = true,
            root = LayoutContainer(
                id = "root",
                layout = ContainerLayout.COLUMN,
                gapDp = 8,
                paddingDp = 4,
                backgroundHex = "#101010",
                children = listOf(
                    TextNode(
                        id = "title",
                        template = "{notif.title}",
                        fontSizeSp = 16,
                        colorHex = "#FFFFFF",
                        bold = true,
                        italic = true,
                        maxLines = 2,
                        marquee = true,
                        gravity = TextGravity.CENTER,
                        opacity = 0.9f
                    ),
                    ShapeNode(
                        id = "shape-bg",
                        shapeId = "rounded_rect",
                        fillColorHex = "#333333",
                        strokeColorHex = "#555555",
                        strokeWidthDp = 2,
                        cornerRadiusDp = 8,
                        opacity = 0.75f
                    ),
                    ImageNode(
                        id = "icon",
                        source = ImageSource.AppIconOf("{notif.package}"),
                        shapeId = "cookie",
                        tintHex = "#FF0000"
                    ),
                    ImageNode(id = "avatar", source = ImageSource.ContactAvatarOf("{notif.title}")),
                    ImageNode(id = "notif-media", source = ImageSource.NotifMedia("avatar")),
                    ImageNode(id = "asset", source = ImageSource.CustomAsset("logo.png")),
                    ImageNode(id = "glyph", source = ImageSource.SystemGlyph("battery")),
                    ImageNode(id = "src-icon", source = ImageSource.SourceIcon("weather")),
                    ProgressNode(
                        id = "battery",
                        style = ProgressStyle.RING,
                        valueTemplate = "{device.battery}",
                        maxValue = 100,
                        trackColorHex = "#222222",
                        progressColorHex = "#00FF00"
                    ),
                    ButtonNode(id = "open", label = "Open", action = ButtonAction.OpenApp("com.example.weather")),
                    ButtonNode(id = "dismiss", label = "Dismiss", action = ButtonAction.Dismiss),
                    ButtonNode(id = "reply", label = "Reply", action = ButtonAction.InlineReply),
                    ButtonNode(id = "link", label = "Link", action = ButtonAction.DeepLink("https://example.com")),
                    LayoutContainer(
                        id = "row",
                        layout = ContainerLayout.ABSOLUTE,
                        children = listOf(
                            TextNode(id = "nested-text", template = "{source.weather.text}")
                        )
                    )
                )
            )
        )

        val encoded = json.encodeToString(CustomWidgetDocument.serializer(), doc)
        val decoded = json.decodeFromString(CustomWidgetDocument.serializer(), encoded)

        assertEquals(doc, decoded)
    }

    @Test
    fun roundTripsNodesWithCustomNameLockedAndBindings() {
        val doc = CustomWidgetDocument(
            id = "widget-bindings",
            meta = CustomWidgetMetadata(name = "Bindings Test"),
            canvas = CanvasSize.SMALL,
            root = LayoutContainer(
                id = "root",
                name = "Main Root Container",
                locked = true,
                bindings = mapOf("backgroundHex" to "{theme.cardBackground}"),
                children = listOf(
                    TextNode(
                        id = "text-1",
                        name = "Custom Header Text",
                        locked = false,
                        bindings = mapOf(
                            "colorHex" to "{theme.accentColor}",
                            "fontSizeSp" to "{settings.headerSize}"
                        ),
                        template = "Hello World"
                    )
                )
            )
        )

        val encoded = json.encodeToString(CustomWidgetDocument.serializer(), doc)
        val decoded = json.decodeFromString(CustomWidgetDocument.serializer(), encoded)

        assertEquals(doc, decoded)
        val rootNode = decoded.root as LayoutContainer
        assertEquals("Main Root Container", rootNode.name)
        assertEquals(true, rootNode.locked)
        assertEquals("{theme.cardBackground}", rootNode.bindings["backgroundHex"])

        val textNode = rootNode.children.first() as TextNode
        assertEquals("Custom Header Text", textNode.name)
        assertEquals(false, textNode.locked)
        assertEquals("{theme.accentColor}", textNode.bindings["colorHex"])
    }

    @Test
    fun deserializesLegacyJsonWithoutNameLockedOrBindings() {
        val legacyJson = """
            {
              "id": "legacy-widget",
              "meta": { "name": "Legacy", "author": "User", "version": 1 },
              "canvas": "MEDIUM",
              "root": {
                "type": "container",
                "id": "root",
                "children": [
                  {
                    "type": "text",
                    "id": "txt",
                    "template": "Legacy text"
                  }
                ]
              }
            }
        """.trimIndent()

        val decoded = json.decodeFromString(CustomWidgetDocument.serializer(), legacyJson)
        assertEquals("Widgets", decoded.meta.icon)
        val rootNode = decoded.root as LayoutContainer
        assertEquals(null, rootNode.name)
        assertEquals(false, rootNode.locked)
        assertEquals(emptyMap<String, String>(), rootNode.bindings)

        val textNode = rootNode.children.first() as TextNode
        assertEquals(null, textNode.name)
        assertEquals(false, textNode.locked)
        assertEquals(emptyMap<String, String>(), textNode.bindings)
        assertEquals(1f, textNode.opacity)
        assertEquals(false, textNode.italic)
    }

    @Test
    fun serializesAndDeserializesCustomIcon() {
        val doc = CustomWidgetDocument(
            id = "custom-icon-widget",
            meta = CustomWidgetMetadata(name = "Nav Design", icon = "Navigation"),
            root = LayoutContainer(id = "root", layout = ContainerLayout.ABSOLUTE)
        )

        val encoded = json.encodeToString(CustomWidgetDocument.serializer(), doc)
        val decoded = json.decodeFromString(CustomWidgetDocument.serializer(), encoded)
        assertEquals("Navigation", decoded.meta.icon)
    }
}
