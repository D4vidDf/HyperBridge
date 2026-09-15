package com.d4viddf.hyperbridge.models.widget

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Recursive AST for a KWGT-style micro-widget (Phase 5, issue #273).
 *
 * A [CustomWidgetDocument] wraps a single root [LayoutContainer]; every node in the tree is a
 * [CustomWidgetNode]. Nodes are rendered against a [com.d4viddf.hyperbridge.data.widget.VariableContext]
 * by [com.d4viddf.hyperbridge.service.widget.CustomWidgetRenderer], and previewed (without
 * touching RemoteViews) by the Studio's Compose canvas.
 */

@Serializable
data class NodeBounds(
    val x: Int = 0,
    val y: Int = 0,
    val widthDp: Int? = null,
    val heightDp: Int? = null
)

@Serializable
sealed interface CustomWidgetNode {
    val id: String
    val bounds: NodeBounds
}

enum class TextGravity { START, CENTER, END }

@Serializable
@SerialName("text")
data class TextNode(
    override val id: String,
    override val bounds: NodeBounds = NodeBounds(),
    val template: String = "",
    val fontSizeSp: Int = 14,
    val colorHex: String = "#FFFFFF",
    val bold: Boolean = false,
    val maxLines: Int = 1,
    val marquee: Boolean = false,
    val gravity: TextGravity = TextGravity.START
) : CustomWidgetNode

@Serializable
sealed interface ImageSource {
    @Serializable
    @SerialName("app_icon")
    data class AppIconOf(val packageTemplate: String) : ImageSource

    @Serializable
    @SerialName("contact_avatar")
    data class ContactAvatarOf(val numberOrNameTemplate: String) : ImageSource

    @Serializable
    @SerialName("custom_asset")
    data class CustomAsset(val fileName: String) : ImageSource

    @Serializable
    @SerialName("system_glyph")
    data class SystemGlyph(val glyphName: String) : ImageSource

    @Serializable
    @SerialName("source_icon")
    data class SourceIcon(val sourceId: String) : ImageSource
}

@Serializable
@SerialName("image")
data class ImageNode(
    override val id: String,
    override val bounds: NodeBounds = NodeBounds(widthDp = 24, heightDp = 24),
    val source: ImageSource = ImageSource.SystemGlyph("notification"),
    val shapeId: String = "circle",
    val tintHex: String? = null
) : CustomWidgetNode

enum class ProgressStyle { LINEAR, RING }

@Serializable
@SerialName("progress")
data class ProgressNode(
    override val id: String,
    override val bounds: NodeBounds = NodeBounds(widthDp = 64, heightDp = 8),
    val style: ProgressStyle = ProgressStyle.LINEAR,
    val valueTemplate: String = "{device.battery}",
    val maxValue: Int = 100,
    val trackColorHex: String = "#33FFFFFF",
    val progressColorHex: String = "#FFFFFF"
) : CustomWidgetNode

@Serializable
sealed interface ButtonAction {
    @Serializable
    @SerialName("open_app")
    data class OpenApp(val packageName: String) : ButtonAction

    @Serializable
    @SerialName("dismiss")
    data object Dismiss : ButtonAction

    @Serializable
    @SerialName("inline_reply")
    data object InlineReply : ButtonAction

    @Serializable
    @SerialName("deep_link")
    data class DeepLink(val uri: String) : ButtonAction
}

@Serializable
@SerialName("button")
data class ButtonNode(
    override val id: String,
    override val bounds: NodeBounds = NodeBounds(),
    val label: String = "",
    val action: ButtonAction = ButtonAction.Dismiss,
    val backgroundHex: String? = null,
    val textColorHex: String = "#FFFFFF"
) : CustomWidgetNode

enum class ContainerLayout { ROW, COLUMN, BOX, ABSOLUTE }

@Serializable
@SerialName("container")
data class LayoutContainer(
    override val id: String,
    override val bounds: NodeBounds = NodeBounds(),
    val layout: ContainerLayout = ContainerLayout.COLUMN,
    val children: List<CustomWidgetNode> = emptyList(),
    val gapDp: Int = 4,
    val paddingDp: Int = 0,
    val backgroundHex: String? = null
) : CustomWidgetNode

@Serializable
data class CustomWidgetMetadata(
    val name: String,
    val author: String = "",
    val version: Int = 1
)

/** Mirrors [com.d4viddf.hyperbridge.models.WidgetSize]'s dp heights; canvas width is fixed at 350dp. */
enum class CanvasSize(val heightDp: Int) {
    SMALL(100),
    MEDIUM(180),
    LARGE(280),
    XLARGE(380)
}

@Serializable
data class CustomWidgetDocument(
    val id: String,
    val meta: CustomWidgetMetadata,
    val canvas: CanvasSize = CanvasSize.MEDIUM,
    val root: LayoutContainer,
    /** If set, this widget replaces the normal translator output for notifications from this package. */
    val boundPackage: String? = null,
    val permanentIslandEligible: Boolean = false
)
