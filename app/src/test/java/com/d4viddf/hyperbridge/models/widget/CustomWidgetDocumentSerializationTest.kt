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

    @Test
    fun roundTripsTextNodeWithSizingFilterAndEfx() {
        val doc = CustomWidgetDocument(
            id = "text-efx-widget",
            meta = CustomWidgetMetadata(name = "Text EFX Test"),
            root = LayoutContainer(
                id = "root",
                children = listOf(
                    TextNode(
                        id = "text-styled",
                        template = "Hello EFX",
                        fontFamily = TextFontFamily.SERIF,
                        sizingType = TextSizingType.FIXED_WIDTH,
                        boxWidthDp = 180,
                        filterMode = TextFilterMode.MULTIPLY,
                        efx = TextEfxConfig(
                            mask = TextMaskType.BLUR_BACKGROUND,
                            maskBlurRadius = 15,
                            maskAttenuation = 75,
                            texture = TextTextureType.HORIZONTAL_GRADIENT,
                            textureColorHex = "#FF5722",
                            textureWidthDp = 120,
                            textureHeightDp = 40,
                            textureParallel = true,
                            textureBitmapUri = "file:///android_asset/sample.png",
                            shadow = TextShadowConfig(
                                enabled = true,
                                blurRadius = 8,
                                direction = 90,
                                distance = 6,
                                colorHex = "#AA000000"
                            )
                        )
                    )
                )
            )
        )

        val encoded = json.encodeToString(CustomWidgetDocument.serializer(), doc)
        val decoded = json.decodeFromString(CustomWidgetDocument.serializer(), encoded)

        assertEquals(doc, decoded)
        val textNode = (decoded.root as LayoutContainer).children.first() as TextNode
        assertEquals(TextFontFamily.SERIF, textNode.fontFamily)
        assertEquals(TextSizingType.FIXED_WIDTH, textNode.sizingType)
        assertEquals(180, textNode.boxWidthDp)
        assertEquals(TextFilterMode.MULTIPLY, textNode.filterMode)
        assertEquals(TextMaskType.BLUR_BACKGROUND, textNode.efx.mask)
        assertEquals(15, textNode.efx.maskBlurRadius)
        assertEquals(true, textNode.efx.shadow.enabled)
        assertEquals(90, textNode.efx.shadow.direction)
    }

    @Test
    fun roundTripsDocumentGlobalsAndBackgroundType() {
        val doc = CustomWidgetDocument(
            id = "widget-globals",
            meta = CustomWidgetMetadata(name = "Globals Test"),
            globals = CustomWidgetGlobals(
                primaryColorHex = "#AABBCC",
                accentColorHex = "#112233",
                fontFamily = TextFontFamily.MONOSPACE
            ),
            root = LayoutContainer(
                id = "root",
                backgroundType = ContainerBackgroundType.PICTURE,
                backgroundImageUri = "content://media/external/images/123",
                backgroundImageSource = ImageSource.NotifMedia("picture")
            )
        )

        val encoded = json.encodeToString(CustomWidgetDocument.serializer(), doc)
        val decoded = json.decodeFromString(CustomWidgetDocument.serializer(), encoded)

        assertEquals(doc, decoded)
        assertEquals("#AABBCC", decoded.globals.primaryColorHex)
        assertEquals("#112233", decoded.globals.accentColorHex)
        assertEquals(TextFontFamily.MONOSPACE, decoded.globals.fontFamily)
        assertEquals(ContainerBackgroundType.PICTURE, decoded.root.backgroundType)
        assertEquals("content://media/external/images/123", decoded.root.backgroundImageUri)
        assertEquals(ImageSource.NotifMedia("picture"), decoded.root.backgroundImageSource)
    }

    @Test
    fun roundTripsImageNodeWithNewProperties() {
        val doc = CustomWidgetDocument(
            id = "image-test",
            meta = CustomWidgetMetadata(name = "Image Props"),
            root = LayoutContainer(
                id = "root",
                children = listOf(
                    ImageNode(
                        id = "img_custom",
                        bounds = NodeBounds(x = 10, y = 20, widthDp = 64, heightDp = 64, rotation = 45f),
                        source = ImageSource.SystemGlyph("battery"),
                        shapeId = "rounded",
                        cornerRadiusDp = 16,
                        mode = ImageMode.SVG,
                        scaleType = ImageScaleType.CENTER_CROP,
                        blurRadius = 12,
                        attenuation = 30,
                        filterMode = TextFilterMode.MULTIPLY,
                        tintHex = "#FF1122",
                        tintEnabled = true
                    )
                )
            )
        )

        val encoded = json.encodeToString(CustomWidgetDocument.serializer(), doc)
        val decoded = json.decodeFromString(CustomWidgetDocument.serializer(), encoded)

        assertEquals(doc, decoded)
        val img = decoded.root.children.first() as ImageNode
        assertEquals(45f, img.bounds.rotation)
        assertEquals("rounded", img.shapeId)
        assertEquals(16, img.cornerRadiusDp)
        assertEquals(ImageMode.SVG, img.mode)
        assertEquals(ImageScaleType.CENTER_CROP, img.scaleType)
        assertEquals(12, img.blurRadius)
        assertEquals(30, img.attenuation)
        assertEquals(TextFilterMode.MULTIPLY, img.filterMode)
        assertEquals("#FF1122", img.tintHex)
        assertEquals(true, img.tintEnabled)
    }

    @Test
    fun deserializesLegacyImageNodeWithoutNewProperties() {
        val legacyJson = """
            {
              "id": "legacy-img-widget",
              "meta": { "name": "Legacy Image" },
              "root": {
                "type": "container",
                "id": "root",
                "children": [
                  {
                    "type": "image",
                    "id": "img_old"
                  }
                ]
              }
            }
        """.trimIndent()

        val decoded = json.decodeFromString(CustomWidgetDocument.serializer(), legacyJson)
        val img = decoded.root.children.first() as ImageNode
        assertEquals(0f, img.bounds.rotation)
        assertEquals(8, img.cornerRadiusDp)
        assertEquals(ImageMode.BITMAP, img.mode)
        assertEquals(ImageScaleType.FIT_CENTER, img.scaleType)
        assertEquals(0, img.blurRadius)
        assertEquals(0, img.attenuation)
        assertEquals(TextFilterMode.NORMAL, img.filterMode)
        assertEquals(false, img.tintEnabled)
        assertEquals(null, img.tintHex)
    }

    @Test
    fun deserializesLegacyImageNodeWithTintHexDefaultsTintEnabledToTrue() {
        val legacyJson = """
            {
              "id": "legacy-tint-widget",
              "meta": { "name": "Legacy Tint" },
              "root": {
                "type": "container",
                "id": "root",
                "children": [
                  {
                    "type": "image",
                    "id": "img_tinted",
                    "tintHex": "#FF0000"
                  }
                ]
              }
            }
        """.trimIndent()

        val decoded = json.decodeFromString(CustomWidgetDocument.serializer(), legacyJson)
        val img = decoded.root.children.first() as ImageNode
        assertEquals("#FF0000", img.tintHex)
        assertEquals(true, img.tintEnabled)
    }

    @Test
    fun roundTripsProgressNodeWithNewProperties() {
        val node = ProgressNode(
            id = "prog_1",
            style = ProgressStyle.LINEAR,
            mode = ProgressIndicatorMode.WAVE,
            colorMode = ProgressColorMode.GRADIENT,
            strokeWidthDp = 6,
            filterMode = TextFilterMode.MULTIPLY,
            valueTemplate = "{media.progress}",
            maxValue = 100,
            trackColorHex = "#44000000",
            progressColorHex = "#FF00FF",
            gradientEndColorHex = "#00FFFF",
            currentSource = "media",
            multiColorsHex = listOf("#FF0000", "#00FF00", "#0000FF"),
            roundCaps = false,
            thumbType = ProgressIndicatorThumb.CUSTOM_PIC,
            thumbSizeDp = 16,
            thumbColorHex = "#FFAA00",
            thumbImageSource = ImageSource.NotifMedia("album_art")
        )
        val doc = CustomWidgetDocument(
            id = "test_doc",
            meta = CustomWidgetMetadata(name = "Prog Test"),
            root = LayoutContainer(id = "root", children = listOf(node))
        )
        val encoded = json.encodeToString(CustomWidgetDocument.serializer(), doc)
        val decoded = json.decodeFromString(CustomWidgetDocument.serializer(), encoded)
        val decodedProgress = decoded.root.children.first() as ProgressNode

        assertEquals(ProgressIndicatorMode.WAVE, decodedProgress.mode)
        assertEquals(ProgressColorMode.GRADIENT, decodedProgress.colorMode)
        assertEquals(6, decodedProgress.strokeWidthDp)
        assertEquals(TextFilterMode.MULTIPLY, decodedProgress.filterMode)
        assertEquals("{media.progress}", decodedProgress.valueTemplate)
        assertEquals("#00FFFF", decodedProgress.gradientEndColorHex)
        assertEquals("media", decodedProgress.currentSource)
        assertEquals(listOf("#FF0000", "#00FF00", "#0000FF"), decodedProgress.multiColorsHex)
        assertEquals(false, decodedProgress.roundCaps)
        assertEquals(ProgressIndicatorThumb.CUSTOM_PIC, decodedProgress.thumbType)
        assertEquals(16, decodedProgress.thumbSizeDp)
        assertEquals("#FFAA00", decodedProgress.thumbColorHex)
        assertEquals(ImageSource.NotifMedia("album_art"), decodedProgress.thumbImageSource)
    }

    @Test
    fun deserializesLegacyProgressNodeWithoutNewPropertiesWithDefaults() {
        val legacyJson = """
            {
              "id": "legacy-prog-widget",
              "meta": { "name": "Legacy Prog" },
              "root": {
                "type": "container",
                "id": "root",
                "children": [
                  {
                    "type": "progress",
                    "id": "battery_bar",
                    "valueTemplate": "{device.battery}"
                  }
                ]
              }
            }
        """.trimIndent()

        val decoded = json.decodeFromString(CustomWidgetDocument.serializer(), legacyJson)
        val prog = decoded.root.children.first() as ProgressNode
        assertEquals(ProgressIndicatorMode.LINE, prog.mode)
        assertEquals(ProgressColorMode.FLAT, prog.colorMode)
        assertEquals(4, prog.strokeWidthDp)
        assertEquals(TextFilterMode.NORMAL, prog.filterMode)
        assertEquals("{device.battery}", prog.valueTemplate)
        assertEquals("#38BDF8", prog.gradientEndColorHex)
        assertEquals("system", prog.currentSource)
        assertEquals(listOf("#4CAF50", "#FFEB3B", "#FF9800", "#F44336"), prog.multiColorsHex)
        assertEquals(ProgressIndicatorThumb.NONE, prog.thumbType)
        assertEquals(12, prog.thumbSizeDp)
        assertEquals(null, prog.thumbColorHex)
        assertEquals(null, prog.thumbImageSource)
    }
}
