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
                        maxLines = 2,
                        marquee = true,
                        gravity = TextGravity.CENTER
                    ),
                    ImageNode(
                        id = "icon",
                        source = ImageSource.AppIconOf("{notif.package}"),
                        shapeId = "cookie",
                        tintHex = "#FF0000"
                    ),
                    ImageNode(id = "avatar", source = ImageSource.ContactAvatarOf("{notif.title}")),
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
}
