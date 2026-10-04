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

    @Serializable
    @SerialName("all")
    data class All(val conditions: List<NodeCondition> = emptyList()) : NodeCondition

    @Serializable
    @SerialName("any")
    data class Any(val conditions: List<NodeCondition> = emptyList()) : NodeCondition
}

@Serializable
sealed interface CustomWidgetNode {
    val id: String
    val bounds: NodeBounds

    /** Optional custom layer name set by the user in the studio/inspector. */
    val name: String? get() = null

    /** When true, node is locked against canvas drag and resize. */
    val locked: Boolean get() = false

    /** Opacity of this layer (0.0 to 1.0). Defaults to 1.0 (fully opaque). */
    val opacity: Float get() = 1f

    /**
     * Dynamic variable bindings (property key -> "{token}" template).
     * Takes precedence over static properties at render time if non-blank.
     */
    val bindings: Map<String, String> get() = emptyMap()

    /** Whether this node is rendered at all for a given notification. */
    val showIf: NodeCondition

    /** Optional tap target. [ButtonNode] uses its own `action` instead. */
    val onClick: ButtonAction?
}

enum class TextGravity { START, CENTER, END }

@Serializable
enum class TextFontFamily {
    @SerialName("default")
    DEFAULT,
    @SerialName("sans_serif")
    SANS_SERIF,
    @SerialName("serif")
    SERIF,
    @SerialName("monospace")
    MONOSPACE,
    @SerialName("cursive")
    CURSIVE,
    @SerialName("casual")
    CASUAL,
    @SerialName("condensed")
    CONDENSED
}

@Serializable
enum class TextSizingType {
    @SerialName("fixed_font_height")
    FIXED_FONT_HEIGHT,
    @SerialName("fit_width")
    FIT_WIDTH,
    @SerialName("fixed_width")
    FIXED_WIDTH,
    @SerialName("fit_box")
    FIT_BOX
}

@Serializable
enum class TextFilterMode {
    @SerialName("normal")
    NORMAL,
    @SerialName("clear")
    CLEAR,
    @SerialName("src")
    SRC,
    @SerialName("dst")
    DST,
    @SerialName("xor")
    XOR,
    @SerialName("darken")
    DARKEN,
    @SerialName("lighten")
    LIGHTEN,
    @SerialName("screen")
    SCREEN,
    @SerialName("add")
    ADD,
    @SerialName("overlay")
    OVERLAY,
    @SerialName("multiply")
    MULTIPLY
}

@Serializable
enum class TextMaskType {
    @SerialName("none")
    NONE,
    @SerialName("blur_background")
    BLUR_BACKGROUND
}

@Serializable
enum class TextTextureType {
    @SerialName("none")
    NONE,
    @SerialName("horizontal_gradient")
    HORIZONTAL_GRADIENT,
    @SerialName("vertical_gradient")
    VERTICAL_GRADIENT,
    @SerialName("radial_gradient")
    RADIAL_GRADIENT,
    @SerialName("sweep_gradient")
    SWEEP_GRADIENT,
    @SerialName("bitmap")
    BITMAP
}

@Serializable
data class TextShadowConfig(
    val enabled: Boolean = false,
    val blurRadius: Int = 4,
    val direction: Int = 45,
    val distance: Int = 4,
    val colorHex: String = "#80000000"
)

@Serializable
data class TextEfxConfig(
    val mask: TextMaskType = TextMaskType.NONE,
    val maskBlurRadius: Int = 10,
    val maskAttenuation: Int = 50,
    val texture: TextTextureType = TextTextureType.NONE,
    val textureColorHex: String? = null,
    val textureWidthDp: Int? = null,
    val textureHeightDp: Int? = null,
    val textureParallel: Boolean = false,
    val textureBitmapUri: String? = null,
    val shadow: TextShadowConfig = TextShadowConfig()
)

@Serializable
@SerialName("text")
data class TextNode(
    override val id: String,
    override val bounds: NodeBounds = NodeBounds(),
    override val name: String? = null,
    override val locked: Boolean = false,
    override val opacity: Float = 1f,
    override val bindings: Map<String, String> = emptyMap(),
    val template: String = "",
    val fontSizeSp: Int = 14,
    val colorHex: String = "#FFFFFF",
    val bold: Boolean = false,
    val italic: Boolean = false,
    val maxLines: Int = 1,
    val marquee: Boolean = false,
    val gravity: TextGravity = TextGravity.START,
    val fontFamily: TextFontFamily = TextFontFamily.DEFAULT,
    val sizingType: TextSizingType = TextSizingType.FIXED_FONT_HEIGHT,
    val boxWidthDp: Int? = null,
    val filterMode: TextFilterMode = TextFilterMode.NORMAL,
    val efx: TextEfxConfig = TextEfxConfig(),
    override val showIf: NodeCondition = NodeCondition.Always,
    override val onClick: ButtonAction? = null
) : CustomWidgetNode

@Serializable
sealed interface ImageSource {
    @Serializable
    @SerialName("notif_media")
    data class NotifMedia(val mediaType: String = "avatar") : ImageSource

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
    override val name: String? = null,
    override val locked: Boolean = false,
    override val opacity: Float = 1f,
    override val bindings: Map<String, String> = emptyMap(),
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
    override val name: String? = null,
    override val locked: Boolean = false,
    override val opacity: Float = 1f,
    override val bindings: Map<String, String> = emptyMap(),
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
    override val name: String? = null,
    override val locked: Boolean = false,
    override val opacity: Float = 1f,
    override val bindings: Map<String, String> = emptyMap(),
    val label: String = "",
    val action: ButtonAction = ButtonAction.NotificationAction(0),
    val backgroundHex: String? = null,
    val textColorHex: String = "#FFFFFF",
    override val showIf: NodeCondition = NodeCondition.Always
) : CustomWidgetNode {
    override val onClick: ButtonAction? get() = action
}

@Serializable
@SerialName("shape")
data class ShapeNode(
    override val id: String,
    override val bounds: NodeBounds = NodeBounds(widthDp = 48, heightDp = 48),
    override val name: String? = null,
    override val locked: Boolean = false,
    override val opacity: Float = 1f,
    override val bindings: Map<String, String> = emptyMap(),
    val shapeId: String = "circle",
    val fillColorHex: String? = "#33FFFFFF",
    val strokeColorHex: String? = null,
    val strokeWidthDp: Int = 0,
    val cornerRadiusDp: Int = 8,
    override val showIf: NodeCondition = NodeCondition.Always,
    override val onClick: ButtonAction? = null
) : CustomWidgetNode

enum class ContainerLayout { ROW, COLUMN, BOX, ABSOLUTE }

@Serializable
enum class ContainerBackgroundType {
    @SerialName("solid")
    SOLID,
    @SerialName("picture")
    PICTURE
}

@Serializable
@SerialName("container")
data class LayoutContainer(
    override val id: String,
    override val bounds: NodeBounds = NodeBounds(),
    override val name: String? = null,
    override val locked: Boolean = false,
    override val opacity: Float = 1f,
    override val bindings: Map<String, String> = emptyMap(),
    val layout: ContainerLayout = ContainerLayout.COLUMN,
    val children: List<CustomWidgetNode> = emptyList(),
    val gapDp: Int = 4,
    val paddingDp: Int = 0,
    val backgroundHex: String? = null,
    val backgroundType: ContainerBackgroundType = ContainerBackgroundType.SOLID,
    val backgroundImageSource: ImageSource? = null,
    val backgroundImageUri: String? = null,
    override val showIf: NodeCondition = NodeCondition.Always,
    override val onClick: ButtonAction? = null
) : CustomWidgetNode

@Serializable
data class CustomWidgetGlobals(
    val primaryColorHex: String = "#FFFFFF",
    val accentColorHex: String = "#0A84FF",
    val fontFamily: TextFontFamily = TextFontFamily.DEFAULT
)

@Serializable
data class CustomWidgetMetadata(
    val name: String,
    val author: String = "",
    val version: Int = 1,
    val icon: String = "Widgets"
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
    val permanentIslandEligible: Boolean = false,
    val globals: CustomWidgetGlobals = CustomWidgetGlobals()
)

/**
 * Standard property keys supported for variable bindings on CustomWidgetNode.
 */
enum class BindableProperty(val key: String) {
    TEXT_TEMPLATE("template"),
    TEXT_COLOR("colorHex"),
    TEXT_FONT_SIZE("fontSizeSp"),
    PROGRESS_VALUE("valueTemplate"),
    PROGRESS_COLOR("progressColorHex"),
    PROGRESS_TRACK_COLOR("trackColorHex"),
    BUTTON_LABEL("label"),
    BUTTON_TEXT_COLOR("textColorHex"),
    BUTTON_BACKGROUND("backgroundHex"),
    IMAGE_TINT("tintHex"),
    CONTAINER_BACKGROUND("backgroundHex"),
    BOUNDS_WIDTH("widthDp"),
    BOUNDS_HEIGHT("heightDp"),
    SHAPE_FILL("fillColorHex"),
    SHAPE_STROKE("strokeColorHex"),
    SHAPE_STROKE_WIDTH("strokeWidthDp"),
    OPACITY("opacity")
}

fun CustomWidgetNode.withBounds(bounds: NodeBounds): CustomWidgetNode = when (this) {
    is TextNode -> copy(bounds = bounds)
    is ImageNode -> copy(bounds = bounds)
    is ProgressNode -> copy(bounds = bounds)
    is ButtonNode -> copy(bounds = bounds)
    is LayoutContainer -> copy(bounds = bounds)
    is ShapeNode -> copy(bounds = bounds)
}

fun CustomWidgetNode.withShowIf(condition: NodeCondition): CustomWidgetNode = when (this) {
    is TextNode -> copy(showIf = condition)
    is ImageNode -> copy(showIf = condition)
    is ProgressNode -> copy(showIf = condition)
    is ButtonNode -> copy(showIf = condition)
    is LayoutContainer -> copy(showIf = condition)
    is ShapeNode -> copy(showIf = condition)
}

fun CustomWidgetNode.withOnClick(action: ButtonAction?): CustomWidgetNode = when (this) {
    is TextNode -> copy(onClick = action)
    is ImageNode -> copy(onClick = action)
    is ProgressNode -> copy(onClick = action)
    is ButtonNode -> copy(action = action ?: ButtonAction.NotificationAction(0))
    is LayoutContainer -> copy(onClick = action)
    is ShapeNode -> copy(onClick = action)
}

fun CustomWidgetNode.withName(name: String?): CustomWidgetNode = when (this) {
    is TextNode -> copy(name = name)
    is ImageNode -> copy(name = name)
    is ProgressNode -> copy(name = name)
    is ButtonNode -> copy(name = name)
    is LayoutContainer -> copy(name = name)
    is ShapeNode -> copy(name = name)
}

fun CustomWidgetNode.withLocked(locked: Boolean): CustomWidgetNode = when (this) {
    is TextNode -> copy(locked = locked)
    is ImageNode -> copy(locked = locked)
    is ProgressNode -> copy(locked = locked)
    is ButtonNode -> copy(locked = locked)
    is LayoutContainer -> copy(locked = locked)
    is ShapeNode -> copy(locked = locked)
}

fun CustomWidgetNode.withOpacity(opacity: Float): CustomWidgetNode = when (this) {
    is TextNode -> copy(opacity = opacity)
    is ImageNode -> copy(opacity = opacity)
    is ProgressNode -> copy(opacity = opacity)
    is ButtonNode -> copy(opacity = opacity)
    is LayoutContainer -> copy(opacity = opacity)
    is ShapeNode -> copy(opacity = opacity)
}

fun CustomWidgetNode.withBindings(bindings: Map<String, String>): CustomWidgetNode = when (this) {
    is TextNode -> copy(bindings = bindings)
    is ImageNode -> copy(bindings = bindings)
    is ProgressNode -> copy(bindings = bindings)
    is ButtonNode -> copy(bindings = bindings)
    is LayoutContainer -> copy(bindings = bindings)
    is ShapeNode -> copy(bindings = bindings)
}

fun CustomWidgetNode.withBinding(key: String, formula: String?): CustomWidgetNode {
    val updated = if (formula.isNullOrBlank()) {
        bindings - key
    } else {
        bindings + (key to formula)
    }
    return withBindings(updated)
}

