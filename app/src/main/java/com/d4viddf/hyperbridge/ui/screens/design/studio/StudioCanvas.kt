package com.d4viddf.hyperbridge.ui.screens.design.studio

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.layout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.d4viddf.hyperbridge.data.widget.VariableContext
import com.d4viddf.hyperbridge.data.widget.WidgetVariableEngine
import com.d4viddf.hyperbridge.models.widget.*
import com.d4viddf.hyperbridge.ui.screens.theme.safeParseColor

/**
 * The Studio's canvas: a pure-Compose re-implementation of the AST, deliberately separate from the
 * RemoteViews [com.d4viddf.hyperbridge.service.widget.CustomWidgetRenderer] used at dispatch time,
 * because RemoteViews has no selection, drag or resize affordances of its own.
 *
 * The canvas is always dark. A HyperOS island is dark whatever the phone's theme is, so tying the
 * backdrop to `colorScheme` made the preview light-on-light in dark mode (#328).
 */
val StudioCanvasBackground = Color(0xFF141414)

private val previewEngine = WidgetVariableEngine()

@Composable
fun StudioCanvas(
    root: LayoutContainer,
    canvasHeightDp: Int,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onMove: (id: String, dxDp: Int, dyDp: Int) -> Unit,
    onResize: (id: String, widthDp: Int, heightDp: Int) -> Unit,
    modifier: Modifier = Modifier,
    zoom: Float = 1f,
    isGridVisible: Boolean = false,
    isWireframeMode: Boolean = false,
    hiddenNodeIds: Set<String> = emptySet(),
    scenario: StudioPreviewScenario = StudioPreviewScenario.STANDARD,
    context: VariableContext = scenario.toVariableContext(),
    globals: CustomWidgetGlobals = CustomWidgetGlobals()
) {
    val rootBgFormula = root.bindings[BindableProperty.CONTAINER_BACKGROUND.key]
    val resolvedRootBg = if (!isWireframeMode && !rootBgFormula.isNullOrBlank()) {
        previewEngine.resolve(rootBgFormula, context).ifBlank { root.backgroundHex }
    } else root.backgroundHex
    val canvasBg = resolvedRootBg?.takeIf { it.isNotBlank() }?.let { safeParseColor(it) } ?: StudioCanvasBackground

    val contextLocal = LocalContext.current
    val rootPictureBitmap: ImageBitmap? = remember(root.backgroundImageUri, root.backgroundType) {
        if (!isWireframeMode && root.backgroundType == ContainerBackgroundType.PICTURE && !root.backgroundImageUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(root.backgroundImageUri)
                contextLocal.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(canvasHeightDp.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(canvasBg)
            .drawBehind {
                if (isGridVisible) {
                    val stepPx = 16.dp.toPx()
                    val gridColor = Color.White.copy(alpha = 0.08f)
                    var x = 0f
                    while (x < size.width) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                        x += stepPx
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        y += stepPx
                    }
                }
            }
            .pointerInput(root.id) {
                detectTapGestures { onSelect(root.id) }
            }
            .padding(8.dp)
    ) {
        if (!isWireframeMode && root.backgroundType == ContainerBackgroundType.PICTURE) {
            if (rootPictureBitmap != null) {
                Image(
                    bitmap = rootPictureBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                )
            } else if (root.backgroundImageSource != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    val notifMedia = root.backgroundImageSource as? ImageSource.NotifMedia
                    val icon = when (notifMedia?.mediaType) {
                        "album_art" -> Icons.Rounded.MusicNote
                        "picture" -> Icons.Rounded.Image
                        else -> Icons.Rounded.Notifications
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }

        // Enforce LTR Cartesian layout coordinate space so RTL system locales never mirror element coordinates
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                    }
            ) {
                CanvasNode(
                    node = root,
                    selectedId = selectedId,
                    onSelect = onSelect,
                    onMove = onMove,
                    onResize = onResize,
                    isWireframeMode = isWireframeMode,
                    hiddenNodeIds = hiddenNodeIds,
                    context = context,
                    zoom = zoom,
                    globals = globals
                )
            }
        }
    }
}

@Composable
private fun CanvasNode(
    node: CustomWidgetNode,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onMove: (id: String, dxDp: Int, dyDp: Int) -> Unit,
    onResize: (id: String, widthDp: Int, heightDp: Int) -> Unit,
    draggable: Boolean = false,
    isWireframeMode: Boolean = false,
    hiddenNodeIds: Set<String> = emptySet(),
    context: VariableContext = VariableContext(),
    zoom: Float = 1f,
    globals: CustomWidgetGlobals = CustomWidgetGlobals()
) {
    if (hiddenNodeIds.contains(node.id)) return

    val density = LocalDensity.current
    val currentNode by rememberUpdatedState(node)
    val isSelected = node.id == selectedId
    val canMove = draggable && !node.locked
    val canResize = !node.locked && node.id != "root"

    // Evaluate conditional visibility
    val isConditionMet = isWireframeMode || NodeConditionEvaluator.isVisible(node.showIf, context, previewEngine)
    if (!isConditionMet && !isSelected) {
        // Condition not met and element is not currently selected -> do not render on live canvas
        return
    }

    var accumulatedDx by remember(node.id) { mutableFloatStateOf(0f) }
    var accumulatedDy by remember(node.id) { mutableFloatStateOf(0f) }

    val isRootNode = node.id == "root"
    val defaultW = defaultWidthOf(node)
    val defaultH = if (node is TextNode && node.maxLines > 1) {
        (node.fontSizeSp * 1.4f * node.maxLines).roundToInt().coerceAtLeast(24)
    } else defaultHeightOf(node)

    val nodeWidth = if (node is TextNode && (node.sizingType == TextSizingType.FIXED_WIDTH || node.sizingType == TextSizingType.FIT_BOX) && node.boxWidthDp != null) {
        node.boxWidthDp
    } else {
        node.bounds.widthDp ?: defaultW
    }
    val baseNodeHeight = node.bounds.heightDp ?: defaultH
    val nodeHeight = if (node is TextNode && node.maxLines > 1 && node.bounds.heightDp != null) {
        maxOf(baseNodeHeight, (node.fontSizeSp * 1.35f * node.maxLines).roundToInt())
    } else baseNodeHeight

    val containerModifier = (if (isRootNode) {
        Modifier.fillMaxSize()
    } else {
        Modifier
            .width(nodeWidth.dp)
            .height(nodeHeight.dp)
    })
        .pointerInput(node.id, canMove) {
            detectTapGestures { onSelect(node.id) }
        }
        .then(
            if (canMove) {
                Modifier.pointerInput(node.id) {
                    detectDragGestures(
                        onDragStart = {
                            accumulatedDx = 0f
                            accumulatedDy = 0f
                            onSelect(node.id)
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        with(density) {
                            val effectiveZoom = zoom.coerceAtLeast(0.1f)
                            accumulatedDx += (dragAmount.x / effectiveZoom).toDp().value
                            accumulatedDy += (dragAmount.y / effectiveZoom).toDp().value
                        }
                        val dx = accumulatedDx.toInt()
                        val dy = accumulatedDy.toInt()
                        if (dx != 0 || dy != 0) {
                            accumulatedDx -= dx
                            accumulatedDy -= dy
                            onMove(currentNode.id, dx, dy)
                        }
                    }
                }
            } else Modifier
        )
        .then(
            if (isSelected) {
                Modifier.border(
                    width = 1.5.dp,
                    color = if (!isConditionMet) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                )
            } else Modifier
        )
        .then(
            Modifier.graphicsLayer {
                val conditionAlpha = if (!isConditionMet) 0.5f else 1f
                alpha = (node.opacity * conditionAlpha).coerceIn(0f, 1f)
                rotationZ = node.bounds.rotation
            }
        )

    Box(modifier = containerModifier) {
        val contentModifier = Modifier.fillMaxSize()

        when (node) {
            is LayoutContainer -> CanvasContainer(
                node = node,
                modifier = contentModifier,
                selectedId = selectedId,
                onSelect = onSelect,
                onMove = onMove,
                onResize = onResize,
                isWireframeMode = isWireframeMode,
                hiddenNodeIds = hiddenNodeIds,
                context = context,
                zoom = zoom,
                globals = globals
            )
            is TextNode -> {
                val colorFormula = node.bindings[BindableProperty.TEXT_COLOR.key]
                val resolvedColor = if (!isWireframeMode && !colorFormula.isNullOrBlank()) {
                    previewEngine.resolve(colorFormula, context).ifBlank { node.colorHex }
                } else node.colorHex

                val textAlign = when (node.gravity) {
                    TextGravity.START -> androidx.compose.ui.text.style.TextAlign.Start
                    TextGravity.CENTER -> androidx.compose.ui.text.style.TextAlign.Center
                    TextGravity.END -> androidx.compose.ui.text.style.TextAlign.End
                }

                val textShadow = if (node.efx.shadow.enabled) {
                    val rad = Math.toRadians(node.efx.shadow.direction.toDouble())
                    val dx = (node.efx.shadow.distance * kotlin.math.cos(rad)).toFloat()
                    val dy = (node.efx.shadow.distance * kotlin.math.sin(rad)).toFloat()
                    androidx.compose.ui.graphics.Shadow(
                        color = safeParseColor(node.efx.shadow.colorHex),
                        offset = androidx.compose.ui.geometry.Offset(dx, dy),
                        blurRadius = node.efx.shadow.blurRadius.toFloat()
                    )
                } else null

                val effectiveFontFamily = if (node.fontFamily == TextFontFamily.DEFAULT) globals.fontFamily else node.fontFamily
                val composeFontFamily = when (effectiveFontFamily) {
                    TextFontFamily.DEFAULT -> androidx.compose.ui.text.font.FontFamily.Default
                    TextFontFamily.SANS_SERIF -> androidx.compose.ui.text.font.FontFamily.SansSerif
                    TextFontFamily.SERIF -> androidx.compose.ui.text.font.FontFamily.Serif
                    TextFontFamily.MONOSPACE -> androidx.compose.ui.text.font.FontFamily.Monospace
                    TextFontFamily.CURSIVE -> androidx.compose.ui.text.font.FontFamily.Cursive
                    TextFontFamily.CASUAL -> androidx.compose.ui.text.font.FontFamily(android.graphics.Typeface.create("casual", android.graphics.Typeface.NORMAL))
                    TextFontFamily.CONDENSED -> androidx.compose.ui.text.font.FontFamily(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL))
                }

                val startColor = safeParseColor(resolvedColor)
                val endColor = safeParseColor(node.efx.textureColorHex ?: "#FF5722")
                val gradientColors = if (node.efx.textureParallel) {
                    listOf(endColor, startColor)
                } else {
                    listOf(startColor, endColor)
                }

                val density = LocalDensity.current
                val textureW = with(density) { (node.efx.textureWidthDp ?: nodeWidth).dp.toPx() }.coerceAtLeast(10f)
                val textureH = with(density) { (node.efx.textureHeightDp ?: nodeHeight).dp.toPx() }.coerceAtLeast(10f)

                val textureBrush: androidx.compose.ui.graphics.Brush? = when (node.efx.texture) {
                    TextTextureType.NONE -> null
                    TextTextureType.HORIZONTAL_GRADIENT -> {
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = gradientColors,
                            startX = 0f,
                            endX = textureW
                        )
                    }
                    TextTextureType.VERTICAL_GRADIENT -> {
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = gradientColors,
                            startY = 0f,
                            endY = textureH
                        )
                    }
                    TextTextureType.RADIAL_GRADIENT -> {
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = gradientColors,
                            center = androidx.compose.ui.geometry.Offset(textureW / 2f, textureH / 2f),
                            radius = maxOf(textureW, textureH) / 2f
                        )
                    }
                    TextTextureType.SWEEP_GRADIENT -> {
                        androidx.compose.ui.graphics.Brush.sweepGradient(
                            colors = listOf(gradientColors[0], gradientColors[1], gradientColors[0]),
                            center = androidx.compose.ui.geometry.Offset(textureW / 2f, textureH / 2f)
                        )
                    }
                    TextTextureType.BITMAP -> {
                        var bmpBrush: androidx.compose.ui.graphics.Brush? = null
                        val uriStr = node.efx.textureBitmapUri
                        if (!uriStr.isNullOrBlank()) {
                            try {
                                val file = java.io.File(uriStr)
                                if (file.exists()) {
                                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                    if (bitmap != null) {
                                        val shader = android.graphics.BitmapShader(
                                            bitmap,
                                            android.graphics.Shader.TileMode.REPEAT,
                                            android.graphics.Shader.TileMode.REPEAT
                                        )
                                        bmpBrush = androidx.compose.ui.graphics.ShaderBrush(shader)
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                        bmpBrush ?: androidx.compose.ui.graphics.Brush.linearGradient(gradientColors)
                    }
                }

                Text(
                    text = if (isWireframeMode) node.template else previewEngine.resolve(node.template, context).ifBlank { node.template },
                    color = if (textureBrush != null) androidx.compose.ui.graphics.Color.Unspecified else safeParseColor(resolvedColor),
                    fontSize = TextUnit(node.fontSizeSp.toFloat(), TextUnitType.Sp),
                    fontWeight = if (node.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (node.italic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                    fontFamily = composeFontFamily,
                    textAlign = textAlign,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = textureBrush,
                        shadow = textShadow
                    ),
                    maxLines = node.maxLines,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = node.maxLines > 1,
                    modifier = contentModifier
                )
            }

            is ShapeNode -> {
                val fillFormula = node.bindings[BindableProperty.SHAPE_FILL.key]
                val strokeFormula = node.bindings[BindableProperty.SHAPE_STROKE.key]
                val resolvedFill = if (!isWireframeMode && !fillFormula.isNullOrBlank()) {
                    previewEngine.resolve(fillFormula, context).ifBlank { node.fillColorHex }
                } else node.fillColorHex
                val resolvedStroke = if (!isWireframeMode && !strokeFormula.isNullOrBlank()) {
                    previewEngine.resolve(strokeFormula, context).ifBlank { node.strokeColorHex }
                } else node.strokeColorHex

                val fillColor = resolvedFill?.let { safeParseColor(it) } ?: Color.Transparent
                val strokeColor = resolvedStroke?.let { safeParseColor(it) } ?: Color.Transparent

                @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                val shape: androidx.compose.ui.graphics.Shape = when (node.shapeId) {
                    "rectangle", "square" -> androidx.compose.ui.graphics.RectangleShape
                    "rounded", "rounded_rect" -> RoundedCornerShape(node.cornerRadiusDp.dp)
                    "circle", "ellipse" -> CircleShape
                    else -> com.d4viddf.hyperbridge.ui.screens.theme.getShapeFromId(node.shapeId).toShape()
                }

                val borderModifier = if (node.strokeWidthDp > 0 && resolvedStroke != null) {
                    Modifier.border(node.strokeWidthDp.dp, strokeColor, shape)
                } else Modifier

                Box(
                    modifier = contentModifier
                        .then(borderModifier)
                        .background(fillColor, shape)
                        .clip(shape)
                )
            }

            is ImageNode -> {
                val tintFormula = node.bindings[BindableProperty.IMAGE_TINT.key]
                val resolvedTint = if (node.tintEnabled) {
                    if (!isWireframeMode && !tintFormula.isNullOrBlank()) {
                        previewEngine.resolve(tintFormula, context).ifBlank { node.tintHex }
                    } else node.tintHex
                } else null

                @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                val shape: androidx.compose.ui.graphics.Shape = when (node.shapeId) {
                    "rectangle", "square" -> androidx.compose.ui.graphics.RectangleShape
                    "rounded", "rounded_rect" -> RoundedCornerShape(node.cornerRadiusDp.dp)
                    "circle", "ellipse" -> CircleShape
                    else -> com.d4viddf.hyperbridge.ui.screens.theme.getShapeFromId(node.shapeId).toShape()
                }
                val tintColor = resolvedTint?.let { safeParseColor(it) }

                val imageBlendMode = when (node.filterMode) {
                    TextFilterMode.NORMAL -> androidx.compose.ui.graphics.BlendMode.SrcOver
                    TextFilterMode.CLEAR -> androidx.compose.ui.graphics.BlendMode.Clear
                    TextFilterMode.SRC -> androidx.compose.ui.graphics.BlendMode.Src
                    TextFilterMode.DST -> androidx.compose.ui.graphics.BlendMode.Dst
                    TextFilterMode.XOR -> androidx.compose.ui.graphics.BlendMode.Xor
                    TextFilterMode.DARKEN -> androidx.compose.ui.graphics.BlendMode.Darken
                    TextFilterMode.LIGHTEN -> androidx.compose.ui.graphics.BlendMode.Lighten
                    TextFilterMode.SCREEN -> androidx.compose.ui.graphics.BlendMode.Screen
                    TextFilterMode.ADD -> androidx.compose.ui.graphics.BlendMode.Plus
                    TextFilterMode.OVERLAY -> androidx.compose.ui.graphics.BlendMode.Overlay
                    TextFilterMode.MULTIPLY -> androidx.compose.ui.graphics.BlendMode.Multiply
                }

                val imageModifier = contentModifier
                    .then(if (node.blurRadius > 0) Modifier.blur(node.blurRadius.dp) else Modifier)
                    .background(
                        tintColor?.copy(alpha = 0.25f) ?: Color.White.copy(alpha = 0.08f),
                        shape
                    )
                    .border(
                        1.dp,
                        tintColor?.copy(alpha = 0.5f) ?: Color.White.copy(alpha = 0.2f),
                        shape
                    )
                    .clip(shape)

                Box(
                    modifier = imageModifier,
                    contentAlignment = Alignment.Center
                ) {
                    val iconVector = when (val src = node.source) {
                        is ImageSource.SystemGlyph -> studioGlyphIcon(src.glyphName)
                        is ImageSource.AppIconOf -> Icons.Rounded.Android
                        is ImageSource.NotifMedia -> when (src.mediaType) {
                            "album_art" -> Icons.Rounded.MusicNote
                            "small_icon" -> Icons.Rounded.Notifications
                            "picture" -> Icons.Rounded.Image
                            else -> Icons.Rounded.Notifications
                        }
                        is ImageSource.ContactAvatarOf -> Icons.Rounded.Call
                        is ImageSource.SourceIcon -> Icons.Rounded.Widgets
                        is ImageSource.CustomAsset -> Icons.Rounded.Image
                    }
                    val baseSize = minOf(nodeWidth, nodeHeight)
                    val iconWidth = when (node.scaleType) {
                        ImageScaleType.FIT_WIDTH -> nodeWidth.dp
                        ImageScaleType.FIT_HEIGHT -> (nodeHeight * 0.75f).coerceAtLeast(12f).dp
                        ImageScaleType.FIT_CENTER -> (baseSize * 0.65f).coerceAtLeast(12f).dp
                        ImageScaleType.CENTER_CROP -> maxOf(nodeWidth, nodeHeight).dp
                    }
                    val iconHeight = when (node.scaleType) {
                        ImageScaleType.FIT_WIDTH -> (nodeWidth * 0.75f).coerceAtLeast(12f).dp
                        ImageScaleType.FIT_HEIGHT -> nodeHeight.dp
                        ImageScaleType.FIT_CENTER -> (baseSize * 0.65f).coerceAtLeast(12f).dp
                        ImageScaleType.CENTER_CROP -> maxOf(nodeWidth, nodeHeight).dp
                    }

                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = tintColor ?: Color.White,
                        modifier = Modifier
                            .size(width = iconWidth, height = iconHeight)
                            .then(
                                if (node.filterMode != TextFilterMode.NORMAL) {
                                    Modifier.graphicsLayer {
                                        blendMode = imageBlendMode
                                    }
                                } else Modifier
                            )
                    )

                    if (node.attenuation > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = (node.attenuation / 100f).coerceIn(0f, 1f)))
                        )
                    }
                }
            }

            is ProgressNode -> {
                val value = if (isWireframeMode) 50 else previewEngine.resolve(node.valueTemplate, context).toIntOrNull() ?: 0
                val progressColorFormula = node.bindings[BindableProperty.PROGRESS_COLOR.key]
                val gradientEndColorFormula = node.bindings[BindableProperty.PROGRESS_GRADIENT_END_COLOR.key]
                val trackColorFormula = node.bindings[BindableProperty.PROGRESS_TRACK_COLOR.key]

                val resolvedProgressColor = if (!isWireframeMode && !progressColorFormula.isNullOrBlank()) {
                    previewEngine.resolve(progressColorFormula, context).ifBlank { node.progressColorHex }
                } else node.progressColorHex

                val resolvedGradientEndColor = if (!isWireframeMode && !gradientEndColorFormula.isNullOrBlank()) {
                    previewEngine.resolve(gradientEndColorFormula, context).ifBlank { node.gradientEndColorHex }
                } else node.gradientEndColorHex

                val resolvedTrackColor = if (!isWireframeMode && !trackColorFormula.isNullOrBlank()) {
                    previewEngine.resolve(trackColorFormula, context).ifBlank { node.trackColorHex }
                } else node.trackColorHex

                val fraction = (value.toFloat() / node.maxValue.coerceAtLeast(1)).coerceIn(0f, 1f)
                val trackColor = safeParseColor(resolvedTrackColor)

                val colorList: List<Color> = when (node.colorMode) {
                    ProgressColorMode.FLAT -> listOf(safeParseColor(resolvedProgressColor))
                    ProgressColorMode.GRADIENT -> listOf(safeParseColor(resolvedProgressColor), safeParseColor(resolvedGradientEndColor))
                    ProgressColorMode.CURRENT -> {
                        val cur = when (node.currentSource) {
                            "notification" -> Color(0xFF38BDF8)
                            "media" -> Color(0xFFA855F7)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        listOf(cur)
                    }
                    ProgressColorMode.MULTICOLOR -> {
                        if (node.multiColorsHex.isNotEmpty()) {
                            node.multiColorsHex.map { safeParseColor(it) }
                        } else {
                            listOf(Color(0xFF4CAF50), Color(0xFFFFEB3B), Color(0xFFFF9800), Color(0xFFF44336))
                        }
                    }
                }

                val progressBrush = if (colorList.size > 1) {
                    androidx.compose.ui.graphics.Brush.horizontalGradient(colorList)
                } else {
                    androidx.compose.ui.graphics.SolidColor(colorList.first())
                }

                val strokeCap = if (node.roundCaps) androidx.compose.ui.graphics.StrokeCap.Round else androidx.compose.ui.graphics.StrokeCap.Butt
                when {
                    node.style == ProgressStyle.RING || node.mode == ProgressIndicatorMode.CIRCLE -> {
                        val strokeWidthPx = node.strokeWidthDp.dp.coerceAtLeast(2.dp)
                        Canvas(modifier = contentModifier) {
                            val strokePx = strokeWidthPx.toPx()
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokePx, cap = strokeCap)
                            )
                            if (fraction > 0f) {
                                val ringBrush = if (colorList.size > 1) {
                                    androidx.compose.ui.graphics.Brush.sweepGradient(colorList + colorList.first())
                                } else {
                                    androidx.compose.ui.graphics.SolidColor(colorList.first())
                                }
                                drawArc(
                                    brush = ringBrush,
                                    startAngle = -90f,
                                    sweepAngle = fraction * 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokePx, cap = strokeCap)
                                )
                            }
                        }
                    }
                    node.mode == ProgressIndicatorMode.DIVIDED -> {
                        Row(
                            modifier = contentModifier,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val totalSegments = 10
                            val filledSegments = (fraction * totalSegments).toInt()
                            val segmentShape = if (node.roundCaps) RoundedCornerShape(2.dp) else androidx.compose.ui.graphics.RectangleShape
                            for (i in 0 until totalSegments) {
                                val segColor = if (i < filledSegments) {
                                    when {
                                        colorList.size == 1 -> colorList.first()
                                        node.colorMode == ProgressColorMode.GRADIENT -> {
                                            val ratio = i.toFloat() / (totalSegments - 1).coerceAtLeast(1)
                                            androidx.compose.ui.graphics.lerp(colorList.first(), colorList.last(), ratio)
                                        }
                                        else -> colorList[i % colorList.size]
                                    }
                                } else trackColor

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(segmentShape)
                                        .background(segColor)
                                )
                            }
                        }
                    }
                    node.mode == ProgressIndicatorMode.WAVE -> {
                        WavyProgressCanvas(
                            fraction = fraction,
                            progressBrush = progressBrush,
                            trackColor = trackColor,
                            strokeWidth = node.strokeWidthDp.dp,
                            modifier = contentModifier,
                            roundCaps = node.roundCaps
                        )
                    }
                    else -> {
                        val barRadius = if (node.roundCaps) ((node.bounds.heightDp ?: 8).dp / 2f) else 0.dp
                        Box(
                            modifier = contentModifier
                                .clip(RoundedCornerShape(barRadius))
                                .background(trackColor)
                        ) {
                            if (fraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction)
                                        .clip(RoundedCornerShape(barRadius))
                                        .background(progressBrush)
                                )
                            }
                        }
                    }
                }
            }

            is ButtonNode -> {
                val bgFormula = node.bindings[BindableProperty.BUTTON_BACKGROUND.key]
                val textFormula = node.bindings[BindableProperty.BUTTON_TEXT_COLOR.key]

                val resolvedBg = if (!isWireframeMode && !bgFormula.isNullOrBlank()) {
                    previewEngine.resolve(bgFormula, context).ifBlank { node.backgroundHex }
                } else node.backgroundHex

                val resolvedText = if (!isWireframeMode && !textFormula.isNullOrBlank()) {
                    previewEngine.resolve(textFormula, context).ifBlank { node.textColorHex }
                } else node.textColorHex

                Box(
                    modifier = contentModifier
                        .background(
                            resolvedBg?.let { safeParseColor(it) } ?: MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = node.label, color = safeParseColor(resolvedText), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        // Easy-to-grab, high-contrast resize grip for the selected element
        if (isSelected && canResize) {
            var currentWidthDp by remember(node.id) { mutableFloatStateOf(0f) }
            var currentHeightDp by remember(node.id) { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(0, 0) {
                            placeable.place(0, 0)
                        }
                    }
                    .offset(x = (-22).dp, y = (-22).dp)
                    .size(44.dp)
                    .pointerInput(node.id) {
                        detectDragGestures(
                            onDragStart = {
                                currentWidthDp = (currentNode.bounds.widthDp ?: defaultWidthOf(currentNode)).toFloat()
                                currentHeightDp = (currentNode.bounds.heightDp ?: defaultHeightOf(currentNode)).toFloat()
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            with(density) {
                                val effectiveZoom = zoom.coerceAtLeast(0.1f)
                                currentWidthDp = (currentWidthDp + (dragAmount.x / effectiveZoom).toDp().value)
                                    .coerceIn(MIN_SIZE_DP.toFloat(), CANVAS_WIDTH_DP.toFloat())
                                currentHeightDp = (currentHeightDp + (dragAmount.y / effectiveZoom).toDp().value)
                                    .coerceAtLeast(MIN_SIZE_DP.toFloat())
                            }
                            onResize(currentNode.id, currentWidthDp.roundToInt(), currentHeightDp.roundToInt())
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                    modifier = Modifier.size(16.dp)
                ) {}
            }
        }
    }
}

@Composable
private fun CanvasContainer(
    node: LayoutContainer,
    modifier: Modifier,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onMove: (id: String, dxDp: Int, dyDp: Int) -> Unit,
    onResize: (id: String, widthDp: Int, heightDp: Int) -> Unit,
    isWireframeMode: Boolean = false,
    hiddenNodeIds: Set<String> = emptySet(),
    context: VariableContext,
    zoom: Float = 1f,
    globals: CustomWidgetGlobals = CustomWidgetGlobals()
) {
    val bgFormula = node.bindings[BindableProperty.CONTAINER_BACKGROUND.key]
    val resolvedBg = if (!isWireframeMode && !bgFormula.isNullOrBlank()) {
        previewEngine.resolve(bgFormula, context).ifBlank { node.backgroundHex }
    } else node.backgroundHex
    val backgroundColor = resolvedBg?.takeIf { it.isNotBlank() }?.let { safeParseColor(it) }

    val freePositioning = node.layout == ContainerLayout.ABSOLUTE || node.layout == ContainerLayout.BOX
    val containerModifier = (if (node.bounds.widthDp == null) modifier.fillMaxSize() else modifier)
        .then(
            if (node.id != "root" && backgroundColor != null && backgroundColor != Color.Transparent) {
                Modifier.background(backgroundColor)
            } else Modifier
        )
        .then(
            if (node.paddingDp > 0) {
                Modifier.padding(node.paddingDp.dp)
            } else Modifier
        )

    when (node.layout) {
        ContainerLayout.ROW -> Row(
            modifier = containerModifier,
            horizontalArrangement = Arrangement.spacedBy(node.gapDp.dp)
        ) {
            node.children.forEach {
                CanvasNode(
                    node = it,
                    selectedId = selectedId,
                    onSelect = onSelect,
                    onMove = onMove,
                    onResize = onResize,
                    draggable = false,
                    isWireframeMode = isWireframeMode,
                    hiddenNodeIds = hiddenNodeIds,
                    context = context,
                    zoom = zoom,
                    globals = globals
                )
            }
        }

        ContainerLayout.COLUMN -> Column(
            modifier = containerModifier,
            verticalArrangement = Arrangement.spacedBy(node.gapDp.dp)
        ) {
            node.children.forEach {
                CanvasNode(
                    node = it,
                    selectedId = selectedId,
                    onSelect = onSelect,
                    onMove = onMove,
                    onResize = onResize,
                    draggable = false,
                    isWireframeMode = isWireframeMode,
                    hiddenNodeIds = hiddenNodeIds,
                    context = context,
                    zoom = zoom,
                    globals = globals
                )
            }
        }

        ContainerLayout.BOX, ContainerLayout.ABSOLUTE -> Box(modifier = containerModifier) {
            node.children.forEach { child ->
                val childModifier = if (node.layout == ContainerLayout.ABSOLUTE || node.layout == ContainerLayout.BOX) {
                    Modifier.absoluteOffset(child.bounds.x.dp, child.bounds.y.dp)
                } else Modifier

                Box(modifier = childModifier) {
                    CanvasNode(
                        node = child,
                        selectedId = selectedId,
                        onSelect = onSelect,
                        onMove = onMove,
                        onResize = onResize,
                        draggable = freePositioning,
                        isWireframeMode = isWireframeMode,
                        hiddenNodeIds = hiddenNodeIds,
                        context = context,
                        zoom = zoom,
                        globals = globals
                    )
                }
            }
        }
    }
}

private const val MIN_SIZE_DP = 12
private const val CANVAS_WIDTH_DP = 350

private fun defaultWidthOf(node: CustomWidgetNode): Int = when (node) {
    is ImageNode -> 24
    is ProgressNode -> 64
    is ButtonNode -> 80
    is LayoutContainer -> node.adaptedContentWidth() ?: 120
    is TextNode -> 80
    is ShapeNode -> 48
}

private fun defaultHeightOf(node: CustomWidgetNode): Int = when (node) {
    is ImageNode -> 24
    is ProgressNode -> 12
    is ButtonNode -> 36
    is LayoutContainer -> node.adaptedContentHeight() ?: 60
    is TextNode -> 24
    is ShapeNode -> 48
}

/** Applies a drag to a node's bounds, keeping it inside the canvas. */
fun NodeBounds.movedBy(dxDp: Int, dyDp: Int, canvasWidthDp: Int, canvasHeightDp: Int): NodeBounds = copy(
    x = (x + dxDp).coerceIn(0, (canvasWidthDp - (widthDp ?: 0)).coerceAtLeast(0)),
    y = (y + dyDp).coerceIn(0, (canvasHeightDp - (heightDp ?: 0)).coerceAtLeast(0))
)

internal fun studioGlyphIcon(glyphName: String): androidx.compose.ui.graphics.vector.ImageVector = when (glyphName.lowercase()) {
    "notification", "notif" -> Icons.Rounded.Notifications
    "play" -> Icons.Rounded.PlayArrow
    "pause" -> Icons.Rounded.SmartButton
    "message", "mail" -> Icons.Rounded.Email
    "call", "phone" -> Icons.Rounded.Call
    "check" -> Icons.Rounded.Check
    "close" -> Icons.Rounded.Clear
    else -> Icons.Rounded.Image
}

@Composable
internal fun WavyProgressCanvas(
    fraction: Float,
    progressBrush: androidx.compose.ui.graphics.Brush,
    trackColor: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
    roundCaps: Boolean = true
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val strokePx = strokeWidth.toPx().coerceAtLeast(2f)
        val strokeCap = if (roundCaps) androidx.compose.ui.graphics.StrokeCap.Round else androidx.compose.ui.graphics.StrokeCap.Butt

        val trackPath = androidx.compose.ui.graphics.Path()
        val numCycles = 4f
        val step = 2f
        var x = 0f
        trackPath.moveTo(0f, midY)
        while (x <= width) {
            val y = midY + kotlin.math.sin(x / width * numCycles * 2 * Math.PI).toFloat() * (height / 3f)
            trackPath.lineTo(x, y)
            x += step
        }
        drawPath(
            path = trackPath,
            color = trackColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokePx, cap = strokeCap)
        )

        if (fraction > 0f) {
            val activeWidth = width * fraction
            val activePath = androidx.compose.ui.graphics.Path()
            activePath.moveTo(0f, midY)
            x = 0f
            while (x <= activeWidth) {
                val y = midY + kotlin.math.sin(x / width * numCycles * 2 * Math.PI).toFloat() * (height / 3f)
                activePath.lineTo(x, y)
                x += step
            }
            drawPath(
                path = activePath,
                brush = progressBrush,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokePx, cap = strokeCap)
            )
        }
    }
}

@Composable
internal fun WavyProgressCanvas(
    fraction: Float,
    progressColor: Color,
    trackColor: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
    roundCaps: Boolean = true
) {
    WavyProgressCanvas(
        fraction = fraction,
        progressBrush = androidx.compose.ui.graphics.SolidColor(progressColor),
        trackColor = trackColor,
        strokeWidth = strokeWidth,
        modifier = modifier,
        roundCaps = roundCaps
    )
}

