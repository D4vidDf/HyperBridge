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

/**
 * When a node is shown. A design is rendered against one notification, so an element can be made
 * conditional on what that notification actually carries -- "this button only exists when the
 * notification has an inline reply", and so on (#328).
 */
@Serializable
sealed interface NodeCondition {
    @Serializable
    @SerialName("always")
    data object Always : NodeCondition

    @Serializable
    @SerialName("has_smart_action")
    // The JSON name cannot be "type": that is the polymorphic discriminator for this hierarchy.
    data class HasSmartAction(@SerialName("smart_action") val type: String) : NodeCondition

    @Serializable
    @SerialName("has_notification_action")
    data class HasNotificationAction(val index: Int = 0) : NodeCondition

    @Serializable
    @SerialName("has_inline_reply")
    data object HasInlineReply : NodeCondition

    @Serializable
    @SerialName("has_progress")
    data object HasProgress : NodeCondition

    /** True when [template], once resolved, is not blank. */
    @Serializable
    @SerialName("not_blank")
    data class NotBlank(val template: String) : NodeCondition

    @Serializable
    @SerialName("matches")
    data class Matches(val template: String, val regex: String) : NodeCondition

    @Serializable
    @SerialName("not")
    data class Not(val condition: NodeCondition) : NodeCondition
}

@Serializable
sealed interface CustomWidgetNode {
    val id: String
    val bounds: NodeBounds

    /** Whether this node is rendered at all for a given notification. */
    val showIf: NodeCondition

    /** Optional tap target. [ButtonNode] uses its own `action` instead. */
    val onClick: ButtonAction?
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
    val gravity: TextGravity = TextGravity.START,
    override val showIf: NodeCondition = NodeCondition.Always,
    override val onClick: ButtonAction? = null
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
    val tintHex: String? = null,
    override val showIf: NodeCondition = NodeCondition.Always,
    override val onClick: ButtonAction? = null
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
    val progressColorHex: String = "#FFFFFF",
    override val showIf: NodeCondition = NodeCondition.Always,
    override val onClick: ButtonAction? = null
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

    /** Fires the notification's own action button at [index]. */
    @Serializable
    @SerialName("notification_action")
    data class NotificationAction(val index: Int = 0) : ButtonAction

    /** Fires a Smart Action (#270) detected on the notification: OTP, URL, PHONE, TRACKING. */
    @Serializable
    @SerialName("smart_action")
    data class SmartAction(@SerialName("smart_action") val type: String) : ButtonAction

    /** Sends a custom broadcast, so a design can drive another app the user owns. */
    @Serializable
    @SerialName("broadcast")
    data class Broadcast(
        val action: String,
        val packageName: String? = null,
        val extraKey: String? = null,
        val extraValue: String? = null
    ) : ButtonAction
}

@Serializable
@SerialName("button")
data class ButtonNode(
    override val id: String,
    override val bounds: NodeBounds = NodeBounds(),
    val label: String = "",
    val action: ButtonAction = ButtonAction.Dismiss,
    val backgroundHex: String? = null,
    val textColorHex: String = "#FFFFFF",
    override val showIf: NodeCondition = NodeCondition.Always
) : CustomWidgetNode {
    override val onClick: ButtonAction? get() = action
}

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
    val backgroundHex: String? = null,
    override val showIf: NodeCondition = NodeCondition.Always,
    override val onClick: ButtonAction? = null
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
    /**
     * Legacy per-package binding. Which notifications a design applies to is decided by its
     * translator now (#272 rework), so this is only read when showing where an imported
     * `.hwidget` came from; it no longer selects anything.
     */
    val boundPackage: String? = null,
    val permanentIslandEligible: Boolean = false
)
