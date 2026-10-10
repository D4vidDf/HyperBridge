package com.d4viddf.hyperbridge.service.widget

import android.app.PendingIntent
import kotlin.math.roundToInt
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.net.Uri
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.graphics.shapes.toPath
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.widget.CustomWidgetRepository
import com.d4viddf.hyperbridge.data.widget.VariableContext
import com.d4viddf.hyperbridge.data.widget.WidgetVariableEngine
import com.d4viddf.hyperbridge.models.widget.*
import com.d4viddf.hyperbridge.receiver.WidgetActionReceiver
import com.d4viddf.hyperbridge.ui.screens.theme.getShapeFromId

/**
 * Compiles a [CustomWidgetDocument] AST into a [RemoteViews] tree. HyperOS's structured island
 * API (`setBigIslandInfo`) is a fixed 2-slot template, so a genuinely spatial, multi-node canvas
 * has to go through `setCustomRemoteView` - the same escape hatch
 * [com.d4viddf.hyperbridge.service.translators.WidgetTranslator] already uses for AppWidget
 * snapshots. Bottom-up `addView` calls build the tree; `setViewLayoutWidth/Height`/`setViewLayoutMargin`
 * (RemoteViews APIs added in API 31, always available at this app's minSdk 35) position each node.
 */
class CustomWidgetRenderer(
    private val context: Context,
    private val engine: WidgetVariableEngine = WidgetVariableEngine(),
    private val widgetRepository: CustomWidgetRepository = CustomWidgetRepository(context)
) {

    /**
     * [bridgeId], when known, lets a Dismiss button cancel the actual posted notification.
     * [intents] carries the notification's own action buttons and Smart Actions, so a node can
     * fire one of them (#328).
     */
    fun render(
        doc: CustomWidgetDocument,
        ctx: VariableContext,
        bridgeId: Int? = null,
        intents: WidgetActionIntents = WidgetActionIntents(),
        targetIslandWidthDp: Float? = null
    ): RemoteViews {
        val root = RemoteViews(context.packageName, R.layout.layout_widget_canvas_root)
        // Conditional nodes (#328) are dropped before rendering, so a button bound to an inline
        // reply simply does not exist on a notification that has none.
        val visibleRoot = NodeConditionEvaluator.prune(doc.root, ctx, engine) as? LayoutContainer
            ?: LayoutContainer(id = doc.root.id, layout = doc.root.layout)
        val islandWidthDp = targetIslandWidthDp ?: getIslandWidthDp(context)
        val scaleX = islandWidthDp / BASE_CANVAS_WIDTH_DP.toFloat()

        val built = renderNode(doc, visibleRoot, ctx, bridgeId, intents, scaleX, islandWidthDp)
        root.removeAllViews(R.id.widget_canvas_insertion_point)
        root.setViewLayoutWidth(R.id.widget_canvas_insertion_point, islandWidthDp, TypedValue.COMPLEX_UNIT_DIP)
        root.setViewLayoutHeight(R.id.widget_canvas_insertion_point, doc.canvas.heightDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
        root.addView(R.id.widget_canvas_insertion_point, built)
        return root
    }

    private fun renderNode(
        doc: CustomWidgetDocument,
        node: CustomWidgetNode,
        ctx: VariableContext,
        bridgeId: Int?,
        intents: WidgetActionIntents,
        scaleX: Float = 1f,
        islandWidthDp: Float? = null
    ): RemoteViews {
        val rv = when (node) {
            is LayoutContainer -> renderContainer(doc, node, ctx, bridgeId, intents, scaleX, islandWidthDp)
            is TextNode -> renderText(doc, node, ctx, scaleX)
            is ImageNode -> renderImage(doc, node, ctx, scaleX)
            is ProgressNode -> renderProgress(doc, node, ctx, scaleX)
            is ButtonNode -> renderButton(doc, node, bridgeId, intents, ctx, scaleX)
            is ShapeNode -> renderShape(node, ctx, scaleX)
        }
        val defaultWidth = when (node) {
            is ButtonNode -> 80
            is ImageNode -> 24
            is ShapeNode -> 48
            is ProgressNode -> 120
            else -> null
        }
        val defaultHeight = when (node) {
            is ButtonNode -> 36
            is ImageNode -> 24
            is ShapeNode -> 48
            is ProgressNode -> 8
            else -> null
        }
        val rawWidthDp = if (node is LayoutContainer && node.bounds.widthDp == null && node.id != doc.root.id) {
            node.adaptedContentWidth()
        } else if (node.id == doc.root.id) {
            islandWidthDp?.roundToInt() ?: (BASE_CANVAS_WIDTH_DP.toFloat() * scaleX).roundToInt()
        } else if (node is TextNode && (node.sizingType == TextSizingType.FIXED_WIDTH || node.sizingType == TextSizingType.FIT_BOX) && node.boxWidthDp != null) {
            node.boxWidthDp
        } else {
            resolveInt(node, BindableProperty.BOUNDS_WIDTH, node.bounds.widthDp ?: defaultWidth, ctx)
        }
        val widthDp = if (node.id == doc.root.id) rawWidthDp else rawWidthDp?.let { (it * scaleX).roundToInt() }
        val heightDp = if (node is LayoutContainer && node.bounds.heightDp == null && node.id != doc.root.id) {
            node.adaptedContentHeight()
        } else if (node.id == doc.root.id) {
            doc.canvas.heightDp
        } else {
            resolveInt(node, BindableProperty.BOUNDS_HEIGHT, node.bounds.heightDp ?: defaultHeight, ctx)
        }
        val effectiveHeightDp = when {
            node is TextNode && node.maxLines > 1 && heightDp != null -> {
                val minNeeded = (node.fontSizeSp * 1.35f * node.maxLines).toInt()
                maxOf(heightDp, minNeeded)
            }
            node is ProgressNode && (node.style == ProgressStyle.RING || node.mode == ProgressIndicatorMode.CIRCLE) -> {
                val base = maxOf(widthDp ?: 36, heightDp ?: 36, 36)
                if (node.thumbType != ProgressIndicatorThumb.NONE) maxOf(base, node.thumbSizeDp + 4) else base
            }
            node is ProgressNode && node.thumbType != ProgressIndicatorThumb.NONE -> {
                maxOf(heightDp ?: 8, node.thumbSizeDp + 4)
            }
            node is ProgressNode && node.mode == ProgressIndicatorMode.WAVE -> {
                if (heightDp == null || heightDp < 16) 16 else heightDp
            }
            else -> heightDp
        }
        val effectiveWidthDp = when {
            node is ProgressNode && (node.style == ProgressStyle.RING || node.mode == ProgressIndicatorMode.CIRCLE) -> {
                val base = maxOf(widthDp ?: 36, heightDp ?: 36, 36)
                if (node.thumbType != ProgressIndicatorThumb.NONE) maxOf(base, node.thumbSizeDp + 4) else base
            }
            else -> widthDp
        }
        applySize(rv, rootViewId(node), effectiveWidthDp, effectiveHeightDp)

        val resolvedOpacity = resolveFloat(node, BindableProperty.OPACITY, node.opacity, ctx) ?: 1f
        rv.setFloat(rootViewId(node), "setAlpha", resolvedOpacity.coerceIn(0f, 1f))

        val resolvedRotation = resolveFloat(node, BindableProperty.ROTATION, node.bounds.rotation, ctx) ?: node.bounds.rotation
        if (resolvedRotation != 0f) {
            try {
                rv.setFloat(rootViewId(node), "setRotation", resolvedRotation)
            } catch (_: Exception) { /* best-effort rotation */ }
        }

        // Any element can carry a tap action, not just buttons (#328); ButtonNode wired its own.
        if (node !is ButtonNode) {
            node.onClick?.let { action ->
                resolveAction(requestCode(doc, node, bridgeId), action, bridgeId, intents, ctx)?.let { pending ->
                    rv.setOnClickPendingIntent(rootViewId(node), pending)
                }
            }
        }
        return rv
    }

    private fun rootViewId(node: CustomWidgetNode): Int = when (node) {
        is LayoutContainer -> R.id.widget_container_root
        is TextNode -> R.id.node_text
        is ImageNode -> R.id.node_image
        is ProgressNode -> R.id.node_progress
        is ButtonNode -> R.id.node_button
        is ShapeNode -> R.id.node_shape
    }

    private fun applySize(rv: RemoteViews, viewId: Int, widthDp: Int?, heightDp: Int?) {
        if (widthDp != null) rv.setViewLayoutWidth(viewId, widthDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
        if (heightDp != null) rv.setViewLayoutHeight(viewId, heightDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
    }

    private fun resolveString(node: CustomWidgetNode, prop: BindableProperty, staticValue: String?, ctx: VariableContext): String? {
        val formula = node.bindings[prop.key]
        if (!formula.isNullOrBlank()) {
            val resolved = engine.resolve(formula, ctx).trim()
            if (resolved.isNotEmpty()) return resolved
        }
        return staticValue
    }

    private fun resolveColor(node: CustomWidgetNode, prop: BindableProperty, staticHex: String?, ctx: VariableContext): Int? {
        val formula = node.bindings[prop.key]
        if (!formula.isNullOrBlank()) {
            val resolved = engine.resolve(formula, ctx).trim()
            if (resolved.isNotEmpty()) {
                val hexCandidate = if (resolved.startsWith("@scheme:")) {
                    val roleKey = resolved.removePrefix("@scheme:")
                    val role = com.d4viddf.hyperbridge.models.colorscheme.ColorSchemeRole.fromKey(roleKey)
                    if (role != null) ctx.colorScheme?.getHex(role) else ctx.colorScheme?.roles?.get(roleKey)
                } else resolved
                if (!hexCandidate.isNullOrBlank()) {
                    try {
                        return hexCandidate.toColorInt()
                    } catch (_: Exception) {}
                }
            }
        }
        val target = staticHex?.trim() ?: return null
        val effectiveHex = if (target.startsWith("@scheme:")) {
            val roleKey = target.removePrefix("@scheme:")
            val role = com.d4viddf.hyperbridge.models.colorscheme.ColorSchemeRole.fromKey(roleKey)
            if (role != null) ctx.colorScheme?.getHex(role) else ctx.colorScheme?.roles?.get(roleKey)
        } else target

        return effectiveHex?.let {
            try { it.toColorInt() } catch (_: Exception) { null }
        }
    }

    private fun resolveInt(node: CustomWidgetNode, prop: BindableProperty, staticValue: Int?, ctx: VariableContext): Int? {
        val formula = node.bindings[prop.key]
        if (!formula.isNullOrBlank()) {
            val resolved = engine.resolve(formula, ctx).trim()
            val parsed = resolved.toIntOrNull()
            if (parsed != null) return parsed
        }
        return staticValue
    }

    private fun resolveFloat(node: CustomWidgetNode, prop: BindableProperty, staticValue: Float?, ctx: VariableContext): Float? {
        val formula = node.bindings[prop.key]
        if (!formula.isNullOrBlank()) {
            val resolved = engine.resolve(formula, ctx).trim()
            val parsed = resolved.toFloatOrNull()
            if (parsed != null) return parsed
        }
        return staticValue
    }

    private fun resolveBoolean(node: CustomWidgetNode, prop: BindableProperty, staticValue: Boolean, ctx: VariableContext): Boolean {
        val formula = node.bindings[prop.key]
        if (!formula.isNullOrBlank()) {
            val resolved = engine.resolve(formula, ctx).trim()
            if (resolved.equals("true", ignoreCase = true) || resolved == "1") return true
            if (resolved.equals("false", ignoreCase = true) || resolved == "0") return false
        }
        return staticValue
    }

    private fun renderContainer(
        doc: CustomWidgetDocument,
        node: LayoutContainer,
        ctx: VariableContext,
        bridgeId: Int?,
        intents: WidgetActionIntents,
        scaleX: Float = 1f,
        islandWidthDp: Float? = null
    ): RemoteViews {
        val layoutRes = when (node.layout) {
            ContainerLayout.ROW -> R.layout.layout_widget_container_row
            ContainerLayout.COLUMN -> R.layout.layout_widget_container_column
            ContainerLayout.BOX, ContainerLayout.ABSOLUTE -> R.layout.layout_widget_container_box
        }
        val rv = RemoteViews(context.packageName, layoutRes)
        val bgColor = resolveColor(node, BindableProperty.CONTAINER_BACKGROUND, node.backgroundHex, ctx)
        if (bgColor != null && node.backgroundType != ContainerBackgroundType.PICTURE) {
            try {
                rv.setInt(R.id.widget_container_root, "setBackgroundColor", bgColor)
            } catch (_: Exception) { /* ignore invalid color */ }
        }
        rv.setViewPadding(
            R.id.widget_container_root,
            dpToPx(node.paddingDp), dpToPx(node.paddingDp), dpToPx(node.paddingDp), dpToPx(node.paddingDp)
        )

        node.children.forEachIndexed { index, child ->
            val childRv = renderNode(doc, child, ctx, bridgeId, intents, scaleX, islandWidthDp)
            if (node.layout == ContainerLayout.ABSOLUTE || node.layout == ContainerLayout.BOX) {
                val scaledX = (child.bounds.x * scaleX).roundToInt()
                childRv.setViewLayoutMargin(rootViewId(child), RemoteViews.MARGIN_LEFT, scaledX.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                childRv.setViewLayoutMargin(rootViewId(child), RemoteViews.MARGIN_TOP, child.bounds.y.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
            } else if (index > 0 && node.gapDp > 0) {
                val marginSide = if (node.layout == ContainerLayout.ROW) RemoteViews.MARGIN_LEFT else RemoteViews.MARGIN_TOP
                val scaledGap = if (node.layout == ContainerLayout.ROW) (node.gapDp * scaleX).roundToInt() else node.gapDp
                childRv.setViewLayoutMargin(rootViewId(child), marginSide, scaledGap.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
            }
            rv.addView(R.id.widget_container_root, childRv)
        }
        return rv
    }

    private fun renderText(doc: CustomWidgetDocument, node: TextNode, ctx: VariableContext, scaleX: Float = 1f): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.layout_widget_node_text)
        val template = resolveString(node, BindableProperty.TEXT_TEMPLATE, node.template, ctx).orEmpty()
        val resolved = engine.resolve(template, ctx)

        val hasStyle = node.bold || node.italic
        val effectiveFontFamily = if (node.fontFamily == TextFontFamily.DEFAULT) doc.globals.fontFamily else node.fontFamily
        val familyName = when (effectiveFontFamily) {
            TextFontFamily.DEFAULT -> null
            TextFontFamily.SANS_SERIF -> "sans-serif"
            TextFontFamily.SERIF -> "serif"
            TextFontFamily.MONOSPACE -> "monospace"
            TextFontFamily.CURSIVE -> "cursive"
            TextFontFamily.CASUAL -> "casual"
            TextFontFamily.CONDENSED -> "sans-serif-condensed"
        }

        val textToSet: CharSequence = if (hasStyle || familyName != null) {
            android.text.SpannableString(resolved).apply {
                if (hasStyle) {
                    val style = when {
                        node.bold && node.italic -> android.graphics.Typeface.BOLD_ITALIC
                        node.bold -> android.graphics.Typeface.BOLD
                        node.italic -> android.graphics.Typeface.ITALIC
                        else -> android.graphics.Typeface.NORMAL
                    }
                    setSpan(
                        android.text.style.StyleSpan(style),
                        0,
                        length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (familyName != null) {
                    setSpan(
                        android.text.style.TypefaceSpan(familyName),
                        0,
                        length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        } else {
            resolved
        }
        rv.setTextViewText(R.id.node_text, textToSet)

        val fontSize = resolveInt(node, BindableProperty.TEXT_FONT_SIZE, node.fontSizeSp, ctx) ?: node.fontSizeSp
        rv.setTextViewTextSize(R.id.node_text, TypedValue.COMPLEX_UNIT_SP, fontSize.toFloat())

        val textColor = resolveColor(node, BindableProperty.TEXT_COLOR, node.colorHex, ctx)
        if (textColor != null) {
            rv.setTextColor(R.id.node_text, textColor)
        }

        val gravityFlag = when (node.gravity) {
            TextGravity.START -> android.view.Gravity.START
            TextGravity.CENTER -> android.view.Gravity.CENTER
            TextGravity.END -> android.view.Gravity.END
        }
        rv.setInt(R.id.node_text, "setGravity", gravityFlag)

        if (node.maxLines > 1) {
            rv.setBoolean(R.id.node_text, "setSingleLine", false)
            rv.setInt(R.id.node_text, "setMaxLines", node.maxLines)
        } else {
            rv.setBoolean(R.id.node_text, "setSingleLine", true)
            rv.setInt(R.id.node_text, "setMaxLines", 1)
        }
        return rv
    }

    private fun renderImage(doc: CustomWidgetDocument, node: ImageNode, ctx: VariableContext, scaleX: Float = 1f): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.layout_widget_node_image)
        val rawWidthDp = resolveInt(node, BindableProperty.BOUNDS_WIDTH, node.bounds.widthDp ?: 24, ctx) ?: 24
        val widthDp = (rawWidthDp * scaleX).roundToInt()
        val heightDp = resolveInt(node, BindableProperty.BOUNDS_HEIGHT, node.bounds.heightDp ?: 24, ctx) ?: 24
        val widthPx = dpToPx(widthDp).coerceAtLeast(1)
        val heightPx = dpToPx(heightDp).coerceAtLeast(1)

        val tintColor = if (node.tintEnabled) {
            resolveColor(node, BindableProperty.IMAGE_TINT, node.tintHex, ctx)
        } else null

        val resolvedBmp = resolveImageBitmap(doc, node, ctx, widthPx, heightPx)
        val isGlyph = node.source is ImageSource.SystemGlyph || resolvedBmp == null
        val bitmap = resolvedBmp ?: systemGlyphBitmap(
            when (val src = node.source) {
                is ImageSource.NotifMedia -> if (src.mediaType == "album_art") "play" else "notification"
                is ImageSource.AppIconOf -> "notification"
                is ImageSource.ContactAvatarOf -> "call"
                else -> "notification"
            },
            widthPx,
            heightPx
        )

        if (bitmap != null) {
            val shaped = applyShape(
                source = bitmap,
                shapeId = node.shapeId,
                cornerRadiusDp = node.cornerRadiusDp,
                widthPx = widthPx,
                heightPx = heightPx,
                scaleType = node.scaleType,
                isGlyph = isGlyph,
                tintColor = tintColor,
                tintEnabled = node.tintEnabled,
                attenuation = node.attenuation,
                filterMode = node.filterMode,
                drawContainerDecorations = true
            )
            rv.setImageViewBitmap(R.id.node_image, shaped)
        }
        return rv
    }

    private fun renderShape(node: ShapeNode, ctx: VariableContext, scaleX: Float = 1f): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.layout_widget_node_shape)
        val rawWidthDp = resolveInt(node, BindableProperty.BOUNDS_WIDTH, node.bounds.widthDp, ctx) ?: 48
        val widthDp = (rawWidthDp * scaleX).roundToInt()
        val heightDp = resolveInt(node, BindableProperty.BOUNDS_HEIGHT, node.bounds.heightDp, ctx) ?: 48
        val widthPx = dpToPx(widthDp).coerceAtLeast(1)
        val heightPx = dpToPx(heightDp).coerceAtLeast(1)

        val fillColor = resolveColor(node, BindableProperty.SHAPE_FILL, node.fillColorHex, ctx)
        val strokeColor = resolveColor(node, BindableProperty.SHAPE_STROKE, node.strokeColorHex, ctx)
        val strokeWidthPx = dpToPx(resolveInt(node, BindableProperty.SHAPE_STROKE_WIDTH, node.strokeWidthDp, ctx) ?: 0)

        val bitmap = createBitmapWithDensity(widthPx, heightPx)
        val canvas = Canvas(bitmap)

        val fillPaint = fillColor?.let {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = it
                style = Paint.Style.FILL
            }
        }

        val strokePaint = if (strokeColor != null && strokeWidthPx > 0) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = strokeColor
                style = Paint.Style.STROKE
                strokeWidth = strokeWidthPx.toFloat()
            }
        } else null

        val strokeInset = (strokeWidthPx / 2f)
        val rect = RectF(
            strokeInset,
            strokeInset,
            widthPx.toFloat() - strokeInset,
            heightPx.toFloat() - strokeInset
        )

        when (node.shapeId) {
            "rectangle", "square" -> {
                fillPaint?.let { canvas.drawRect(rect, it) }
                strokePaint?.let { canvas.drawRect(rect, it) }
            }
            "rounded", "rounded_rect" -> {
                val radiusPx = dpToPx(node.cornerRadiusDp).toFloat()
                fillPaint?.let { canvas.drawRoundRect(rect, radiusPx, radiusPx, it) }
                strokePaint?.let { canvas.drawRoundRect(rect, radiusPx, radiusPx, it) }
            }
            "circle", "ellipse" -> {
                fillPaint?.let { canvas.drawOval(rect, it) }
                strokePaint?.let { canvas.drawOval(rect, it) }
            }
            else -> {
                val polygon = getShapeFromId(node.shapeId)
                val path = polygon.toPath()
                val bounds = RectF()
                path.computeBounds(bounds, true)
                val matrix = Matrix()
                matrix.setRectToRect(bounds, rect, Matrix.ScaleToFit.FILL)
                path.transform(matrix)

                fillPaint?.let { canvas.drawPath(path, it) }
                strokePaint?.let { canvas.drawPath(path, it) }
            }
        }

        rv.setImageViewBitmap(R.id.node_shape, bitmap)
        return rv
    }

    private fun resolveImageBitmap(
        doc: CustomWidgetDocument,
        node: ImageNode,
        ctx: VariableContext,
        widthPx: Int = 96,
        heightPx: Int = 96
    ): Bitmap? {
        return try {
            when (val source = node.source) {
                is ImageSource.NotifMedia -> {
                    when (source.mediaType) {
                        "avatar" -> ctx.notifAvatarBitmap ?: ctx.notifPictureBitmap ?: ctx.notifSmallIconBitmap
                        "picture" -> ctx.notifPictureBitmap ?: ctx.notifAvatarBitmap
                        "album_art" -> ctx.notifAvatarBitmap ?: ctx.notifPictureBitmap
                        "small_icon" -> ctx.notifSmallIconBitmap ?: ctx.notifAvatarBitmap
                        else -> ctx.notifAvatarBitmap ?: ctx.notifPictureBitmap ?: ctx.notifSmallIconBitmap
                    }
                }
                is ImageSource.AppIconOf -> {
                    val pkg = engine.resolve(source.packageTemplate, ctx)
                    if (pkg.isBlank()) null else drawableToBitmap(context.packageManager.getApplicationIcon(pkg), widthPx, heightPx)
                }
                is ImageSource.ContactAvatarOf -> ctx.notifAvatarBitmap
                is ImageSource.CustomAsset -> {
                    val file = widgetRepository.assetFile(doc.id, source.fileName)
                    if (file.exists()) android.graphics.BitmapFactory.decodeFile(file.absolutePath) else null
                }
                is ImageSource.SystemGlyph -> systemGlyphBitmap(source.glyphName, widthPx, heightPx)
                is ImageSource.SourceIcon -> {
                    val path = engine.resolve("{source.${source.sourceId}.icon}", ctx)
                    if (path.isBlank()) null else android.graphics.BitmapFactory.decodeFile(path)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun systemGlyphBitmap(glyphName: String, widthPx: Int = 96, heightPx: Int = 96): Bitmap? {
        return try {
            val bitmap = createBitmapWithDensity(widthPx, heightPx)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
            drawGlyph(canvas, glyphName, rect, paint)
            bitmap
        } catch (_: Exception) {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground) ?: return null
            drawableToBitmap(drawable, widthPx, heightPx)
        }
    }

    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable, widthPx: Int = 96, heightPx: Int = 96): Bitmap {
        val bitmap = createBitmapWithDensity(widthPx, heightPx)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, widthPx, heightPx)
        drawable.draw(canvas)
        return bitmap
    }

    private fun applyShape(
        source: Bitmap,
        shapeId: String,
        cornerRadiusDp: Int = 8,
        widthPx: Int = 96,
        heightPx: Int = 96,
        scaleType: ImageScaleType = ImageScaleType.FIT_CENTER,
        isGlyph: Boolean = false,
        tintColor: Int? = null,
        tintEnabled: Boolean = false,
        attenuation: Int = 0,
        filterMode: TextFilterMode = TextFilterMode.NORMAL,
        drawContainerDecorations: Boolean = false
    ): Bitmap {
        val output = createBitmapWithDensity(widthPx, heightPx)
        val canvas = Canvas(output)
        val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())

        val shapePath = Path()
        when (shapeId) {
            "rectangle", "square" -> shapePath.addRect(rect, Path.Direction.CW)
            "rounded", "rounded_rect" -> {
                val radiusPx = dpToPx(cornerRadiusDp).toFloat().coerceIn(0f, minOf(widthPx, heightPx) / 2f)
                shapePath.addRoundRect(rect, radiusPx, radiusPx, Path.Direction.CW)
            }
            "circle", "ellipse" -> shapePath.addOval(rect, Path.Direction.CW)
            else -> {
                val polygon = getShapeFromId(shapeId)
                val polyPath = polygon.toPath()
                val bounds = RectF()
                polyPath.computeBounds(bounds, true)
                val matrix = Matrix()
                matrix.setRectToRect(bounds, rect, Matrix.ScaleToFit.FILL)
                polyPath.transform(matrix)
                shapePath.addPath(polyPath)
            }
        }

        if (drawContainerDecorations) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = if (tintColor != null) {
                    val a = (Color.alpha(tintColor) * 0.25f).roundToInt().coerceIn(0, 255)
                    Color.argb(a, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                } else {
                    Color.argb((255 * 0.08f).roundToInt(), 255, 255, 255)
                }
            }
            canvas.drawPath(shapePath, bgPaint)
        }

        val bounds = computeImageBounds(
            srcW = source.width.toFloat(),
            srcH = source.height.toFloat(),
            widthPx = widthPx.toFloat(),
            heightPx = heightPx.toFloat(),
            scaleType = scaleType,
            isGlyph = isGlyph,
            minGlyphSize = dpToPx(12).toFloat()
        )
        val destRect = bounds.toRectF()

        val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val effectiveTint = when {
            tintColor != null && (tintEnabled || isGlyph) -> tintColor
            isGlyph -> Color.WHITE
            else -> null
        }
        if (effectiveTint != null) {
            imgPaint.colorFilter = PorterDuffColorFilter(effectiveTint, PorterDuff.Mode.SRC_IN)
        }

        when (filterMode) {
            TextFilterMode.SCREEN -> PorterDuff.Mode.SCREEN
            TextFilterMode.OVERLAY -> PorterDuff.Mode.OVERLAY
            TextFilterMode.MULTIPLY -> PorterDuff.Mode.MULTIPLY
            else -> null
        }?.let { mode ->
            imgPaint.xfermode = PorterDuffXfermode(mode)
        }

        canvas.save()
        canvas.clipPath(shapePath)
        canvas.drawBitmap(source, null, destRect, imgPaint)

        if (attenuation > 0) {
            val attAlpha = ((attenuation / 100f).coerceIn(0f, 1f) * 255).roundToInt()
            val attPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = Color.argb(attAlpha, 0, 0, 0)
            }
            canvas.drawRect(rect, attPaint)
        }
        canvas.restore()

        if (drawContainerDecorations) {
            val strokeWidthPx = dpToPx(1).toFloat().coerceAtLeast(1f)
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeWidthPx
                color = if (tintColor != null) {
                    val a = (Color.alpha(tintColor) * 0.5f).roundToInt().coerceIn(0, 255)
                    Color.argb(a, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                } else {
                    Color.argb((255 * 0.2f).roundToInt(), 255, 255, 255)
                }
            }
            canvas.drawPath(shapePath, strokePaint)
        }

        return output
    }

    private fun renderProgress(doc: CustomWidgetDocument, node: ProgressNode, ctx: VariableContext, scaleX: Float = 1f): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.layout_widget_node_progress_linear)
        val isCircular = node.style == ProgressStyle.RING || node.mode == ProgressIndicatorMode.CIRCLE
        val rawWidthDp = resolveInt(node, BindableProperty.BOUNDS_WIDTH, node.bounds.widthDp, ctx) ?: (if (isCircular) 36 else 64)
        val heightDp = resolveInt(node, BindableProperty.BOUNDS_HEIGHT, node.bounds.heightDp, ctx) ?: (if (isCircular) 36 else 8)
        val effectiveWidthDp = when {
            isCircular -> {
                val base = maxOf(rawWidthDp, heightDp, 36)
                if (node.thumbType != ProgressIndicatorThumb.NONE) maxOf(base, node.thumbSizeDp + 4) else base
            }
            node.thumbType != ProgressIndicatorThumb.NONE -> maxOf(rawWidthDp, node.thumbSizeDp + 4)
            else -> rawWidthDp
        }
        val effectiveHeightDp = when {
            isCircular -> {
                val base = maxOf(rawWidthDp, heightDp, 36)
                if (node.thumbType != ProgressIndicatorThumb.NONE) maxOf(base, node.thumbSizeDp + 4) else base
            }
            node.thumbType != ProgressIndicatorThumb.NONE -> maxOf(heightDp, node.thumbSizeDp + 4)
            node.mode == ProgressIndicatorMode.WAVE && heightDp < 16 -> 16
            else -> heightDp
        }

        val scaledWidthDp = if (isCircular) effectiveWidthDp else (effectiveWidthDp * scaleX).roundToInt()
        val widthPx = dpToPx(scaledWidthDp).coerceAtLeast(1)
        val heightPx = dpToPx(effectiveHeightDp).coerceAtLeast(1)

        val valueTemplate = resolveString(node, BindableProperty.PROGRESS_VALUE, node.valueTemplate, ctx).orEmpty()
        val resolvedValue = engine.resolve(valueTemplate, ctx).toIntOrNull() ?: 0
        val fraction = (resolvedValue.toFloat() / node.maxValue.coerceAtLeast(1)).coerceIn(0f, 1f)

        val trackColor = resolveColor(node, BindableProperty.PROGRESS_TRACK_COLOR, node.trackColorHex, ctx)
            ?: 0x33FFFFFF
        val progressColor = resolveColor(node, BindableProperty.PROGRESS_COLOR, node.progressColorHex, ctx)
            ?: Color.WHITE
        val gradientEndColor = resolveColor(node, BindableProperty.PROGRESS_GRADIENT_END_COLOR, node.gradientEndColorHex, ctx)
            ?: 0xFF38BDF8.toInt()

        val colorList: List<Int> = when (node.colorMode) {
            ProgressColorMode.FLAT -> listOf(progressColor)
            ProgressColorMode.GRADIENT -> listOf(progressColor, gradientEndColor)
            ProgressColorMode.CURRENT -> {
                val cur = when (node.currentSource) {
                    "notification" -> 0xFF38BDF8.toInt()
                    "media" -> 0xFFA855F7.toInt()
                    else -> ctx.themePrimary?.let { try { it.toColorInt() } catch (_: Exception) { null } } ?: 0xFF38BDF8.toInt()
                }
                listOf(cur)
            }
            ProgressColorMode.MULTICOLOR -> {
                if (node.multiColorsHex.isNotEmpty()) {
                    val parsed = node.multiColorsHex.mapNotNull { try { it.toColorInt() } catch (_: Exception) { null } }
                    if (parsed.isNotEmpty()) parsed else listOf(0xFF4CAF50.toInt(), 0xFFFFEB3B.toInt(), 0xFFFF9800.toInt(), 0xFFF44336.toInt())
                } else {
                    listOf(0xFF4CAF50.toInt(), 0xFFFFEB3B.toInt(), 0xFFFF9800.toInt(), 0xFFF44336.toInt())
                }
            }
        }

        val strokeWidthDp = resolveInt(node, BindableProperty.PROGRESS_STROKE_WIDTH, node.strokeWidthDp, ctx) ?: node.strokeWidthDp
        val strokeWidthPx = dpToPx(strokeWidthDp.coerceAtLeast(1)).toFloat().coerceAtLeast(2f)

        val roundCaps = resolveBoolean(node, BindableProperty.PROGRESS_ROUND_CAPS, node.roundCaps, ctx)
        val strokeCap = if (roundCaps) Paint.Cap.ROUND else Paint.Cap.BUTT

        val thumbSizeDp = resolveInt(node, BindableProperty.PROGRESS_THUMB_SIZE, node.thumbSizeDp, ctx) ?: node.thumbSizeDp
        val thumbSizePx = dpToPx(thumbSizeDp.coerceIn(4, 48)).coerceAtLeast(4)
        val thumbRadiusPx = thumbSizePx / 2f
        val thumbColor = resolveColor(node, BindableProperty.PROGRESS_THUMB_COLOR, node.thumbColorHex, ctx) ?: progressColor

        val bitmap = createBitmapWithDensity(widthPx, heightPx)
        val canvas = Canvas(bitmap)

        when {
            isCircular -> {
                val size = minOf(widthPx, heightPx).toFloat()
                val effectiveStroke = strokeWidthPx.coerceAtMost(size / 3f).coerceAtLeast(1f)
                val inset = maxOf(effectiveStroke / 2f, thumbRadiusPx)
                val left = (widthPx - size) / 2f + inset
                val top = (heightPx - size) / 2f + inset
                val arcRect = RectF(left, top, left + size - inset * 2f, top + size - inset * 2f)

                val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = trackColor
                    style = Paint.Style.STROKE
                    this.strokeWidth = effectiveStroke
                    this.strokeCap = strokeCap
                }
                canvas.drawArc(arcRect, 0f, 360f, false, trackPaint)

                if (fraction > 0f) {
                    val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        this.strokeWidth = effectiveStroke
                        this.strokeCap = strokeCap
                        if (colorList.size > 1) {
                            val colors = (colorList + colorList.first()).toIntArray()
                            val sweepGradient = SweepGradient(widthPx / 2f, heightPx / 2f, colors, null)
                            val matrix = Matrix()
                            matrix.setRotate(-90f, widthPx / 2f, heightPx / 2f)
                            sweepGradient.setLocalMatrix(matrix)
                            shader = sweepGradient
                        } else {
                            color = colorList.first()
                        }
                    }
                    canvas.drawArc(arcRect, -90f, fraction * 360f, false, progressPaint)
                }

                if (node.thumbType != ProgressIndicatorThumb.NONE) {
                    val currentAngleDeg = -90f + fraction * 360f
                    val arcRadius = (size - inset * 2f) / 2f
                    val angleRad = Math.toRadians(currentAngleDeg.toDouble())
                    val cx = (widthPx / 2f) + arcRadius * kotlin.math.cos(angleRad).toFloat()
                    val cy = (heightPx / 2f) + arcRadius * kotlin.math.sin(angleRad).toFloat()
                    drawProgressThumb(canvas, doc, node, ctx, cx, cy, thumbSizePx, thumbColor)
                }
            }

            node.mode == ProgressIndicatorMode.DIVIDED -> {
                val totalSegments = 10
                val spacingPx = dpToPx(4).toFloat()
                val totalSpacing = spacingPx * (totalSegments - 1)
                val segmentWidth = ((widthPx.toFloat() - totalSpacing) / totalSegments).coerceAtLeast(1f)
                val radiusPx = if (roundCaps) dpToPx(2).toFloat().coerceAtMost(segmentWidth / 2f).coerceAtMost(heightPx / 2f) else 0f
                val filledSegments = (fraction * totalSegments).toInt()

                val segPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                }

                for (i in 0 until totalSegments) {
                    val segColor = if (i < filledSegments) {
                        when {
                            colorList.size == 1 -> colorList.first()
                            node.colorMode == ProgressColorMode.GRADIENT -> {
                                val ratio = i.toFloat() / (totalSegments - 1).coerceAtLeast(1)
                                ColorUtils.blendARGB(colorList.first(), colorList.last(), ratio)
                            }
                            else -> colorList[i % colorList.size]
                        }
                    } else trackColor

                    segPaint.color = segColor
                    val left = i * (segmentWidth + spacingPx)
                    val right = left + segmentWidth
                    val rect = RectF(left, 0f, right, heightPx.toFloat())
                    canvas.drawRoundRect(rect, radiusPx, radiusPx, segPaint)
                }

                if (node.thumbType != ProgressIndicatorThumb.NONE && filledSegments > 0) {
                    val lastSegRight = (filledSegments - 1) * (segmentWidth + spacingPx) + segmentWidth
                    val cx = lastSegRight.coerceIn(thumbRadiusPx, widthPx.toFloat() - thumbRadiusPx)
                    val cy = heightPx / 2f
                    drawProgressThumb(canvas, doc, node, ctx, cx, cy, thumbSizePx, thumbColor)
                }
            }

            node.mode == ProgressIndicatorMode.WAVE -> {
                val w = widthPx.toFloat()
                val h = heightPx.toFloat()
                val midY = h / 2f
                val effectiveStroke = strokeWidthPx.coerceAtMost(h / 2f).coerceAtLeast(1f)
                val halfStroke = effectiveStroke / 2f
                val maxAmplitude = (midY - maxOf(halfStroke, thumbRadiusPx)).coerceAtLeast(1f)
                val amplitude = minOf(h / 3f, maxAmplitude)

                val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = trackColor
                    style = Paint.Style.STROKE
                    this.strokeWidth = effectiveStroke
                    this.strokeCap = strokeCap
                }
                val trackPath = Path()
                val numCycles = 4f
                val step = 2f
                var x = 0f
                trackPath.moveTo(0f, midY)
                while (x <= w) {
                    val y = midY + kotlin.math.sin(x / w * numCycles * 2 * Math.PI).toFloat() * amplitude
                    trackPath.lineTo(x, y)
                    x += step
                }
                canvas.drawPath(trackPath, trackPaint)

                if (fraction > 0f) {
                    val activeWidth = w * fraction
                    val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        this.strokeWidth = effectiveStroke
                        this.strokeCap = strokeCap
                        if (colorList.size > 1) {
                            val colors = colorList.toIntArray()
                            shader = LinearGradient(0f, 0f, w, 0f, colors, null, Shader.TileMode.CLAMP)
                        } else {
                            color = colorList.first()
                        }
                    }
                    val activePath = Path()
                    activePath.moveTo(0f, midY)
                    x = 0f
                    while (x <= activeWidth) {
                        val y = midY + kotlin.math.sin(x / w * numCycles * 2 * Math.PI).toFloat() * amplitude
                        activePath.lineTo(x, y)
                        x += step
                    }
                    canvas.drawPath(activePath, activePaint)
                }

                if (node.thumbType != ProgressIndicatorThumb.NONE) {
                    val cx = (w * fraction).coerceIn(0f, w)
                    val cy = midY + kotlin.math.sin(cx / w * numCycles * 2 * Math.PI).toFloat() * amplitude
                    drawProgressThumb(canvas, doc, node, ctx, cx, cy, thumbSizePx, thumbColor)
                }
            }

            else -> {
                val radiusPx = if (roundCaps) heightPx / 2f else 0f

                val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = trackColor
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()), radiusPx, radiusPx, trackPaint)

                if (fraction > 0f) {
                    val activeWidth = (widthPx.toFloat() * fraction).coerceAtLeast(1f)
                    val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        if (colorList.size > 1) {
                            val colors = colorList.toIntArray()
                            shader = LinearGradient(0f, 0f, widthPx.toFloat(), 0f, colors, null, Shader.TileMode.CLAMP)
                        } else {
                            color = colorList.first()
                        }
                    }
                    val fillRadius = radiusPx.coerceAtMost(activeWidth / 2f)
                    canvas.drawRoundRect(RectF(0f, 0f, activeWidth, heightPx.toFloat()), fillRadius, fillRadius, progressPaint)
                }

                if (node.thumbType != ProgressIndicatorThumb.NONE) {
                    val trackUsableWidth = widthPx.toFloat() - 2f * thumbRadiusPx
                    val cx = if (trackUsableWidth > 0) thumbRadiusPx + trackUsableWidth * fraction else widthPx.toFloat() * fraction
                    val cy = heightPx / 2f
                    drawProgressThumb(canvas, doc, node, ctx, cx, cy, thumbSizePx, thumbColor)
                }
            }
        }

        rv.setImageViewBitmap(R.id.node_progress, bitmap)
        return rv
    }

    private fun drawProgressThumb(
        canvas: Canvas,
        doc: CustomWidgetDocument,
        node: ProgressNode,
        ctx: VariableContext,
        cx: Float,
        cy: Float,
        sizePx: Int,
        thumbColor: Int
    ) {
        if (node.thumbType == ProgressIndicatorThumb.NONE) return
        val radius = sizePx / 2f
        when (node.thumbType) {
            ProgressIndicatorThumb.NONE -> Unit
            ProgressIndicatorThumb.ROUNDED -> {
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = thumbColor
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cx, cy, radius, fillPaint)

                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = dpToPx(1).toFloat().coerceAtLeast(1f)
                }
                canvas.drawCircle(cx, cy, radius, strokePaint)
            }
            ProgressIndicatorThumb.CUSTOM_PIC -> {
                val badge = resolveThumbBitmap(doc, node, ctx, sizePx)
                if (badge != null) {
                    canvas.drawBitmap(badge, cx - radius, cy - radius, null)
                } else {
                    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = thumbColor
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(cx, cy, radius, fillPaint)
                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        style = Paint.Style.STROKE
                        strokeWidth = dpToPx(1).toFloat().coerceAtLeast(1f)
                    }
                    canvas.drawCircle(cx, cy, radius, strokePaint)
                }
            }
        }
    }

    private fun resolveThumbBitmap(
        doc: CustomWidgetDocument,
        node: ProgressNode,
        ctx: VariableContext,
        sizePx: Int
    ): Bitmap? {
        val source = node.thumbImageSource ?: ImageSource.NotifMedia("album_art")
        val raw = try {
            when (source) {
                is ImageSource.NotifMedia -> {
                    when (source.mediaType) {
                        "avatar" -> ctx.notifAvatarBitmap ?: ctx.notifPictureBitmap ?: ctx.notifSmallIconBitmap
                        "picture" -> ctx.notifPictureBitmap ?: ctx.notifAvatarBitmap
                        "album_art" -> ctx.notifAvatarBitmap ?: ctx.notifPictureBitmap
                        "small_icon" -> ctx.notifSmallIconBitmap ?: ctx.notifAvatarBitmap
                        else -> ctx.notifAvatarBitmap ?: ctx.notifPictureBitmap ?: ctx.notifSmallIconBitmap
                    }
                }
                is ImageSource.AppIconOf -> {
                    val pkg = engine.resolve(source.packageTemplate, ctx)
                    if (pkg.isBlank()) null else drawableToBitmap(context.packageManager.getApplicationIcon(pkg))
                }
                is ImageSource.ContactAvatarOf -> ctx.notifAvatarBitmap
                is ImageSource.CustomAsset -> {
                    val file = widgetRepository.assetFile(doc.id, source.fileName)
                    if (file.exists()) android.graphics.BitmapFactory.decodeFile(file.absolutePath) else null
                }
                is ImageSource.SystemGlyph -> systemGlyphBitmap(source.glyphName)
                is ImageSource.SourceIcon -> {
                    val path = engine.resolve("{source.${source.sourceId}.icon}", ctx)
                    if (path.isBlank()) null else android.graphics.BitmapFactory.decodeFile(path)
                }
            }
        } catch (_: Exception) {
            null
        } ?: return null

        val output = createBitmapWithDensity(sizePx, sizePx)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        canvas.drawOval(rect, paint)

        val srcPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        val scaled = Bitmap.createScaledBitmap(raw, sizePx, sizePx, true).apply {
            density = context.resources.displayMetrics.densityDpi
        }
        canvas.drawBitmap(scaled, 0f, 0f, srcPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(1).toFloat().coerceAtLeast(1f)
        }
        val strokeInset = strokePaint.strokeWidth / 2f
        canvas.drawOval(RectF(strokeInset, strokeInset, sizePx - strokeInset, sizePx - strokeInset), strokePaint)
        return output
    }

    private fun drawGlyph(canvas: Canvas, glyphName: String, rect: RectF, paint: Paint) {
        val cx = rect.centerX()
        val cy = rect.centerY()
        val size = minOf(rect.width(), rect.height())
        if (size <= 0f) return

        // Support custom icons (URI or file path), rounded by default
        if (glyphName.startsWith("content://") || glyphName.startsWith("file://") || glyphName.startsWith("/")) {
            try {
                val bmp = if (glyphName.startsWith("/")) {
                    android.graphics.BitmapFactory.decodeFile(glyphName)
                } else {
                    val uri = Uri.parse(glyphName)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        android.graphics.BitmapFactory.decodeStream(stream)
                    }
                }
                if (bmp != null) {
                    val sizePx = size.roundToInt().coerceAtLeast(1)
                    val shaped = applyShape(bmp, "rounded_rect", cornerRadiusDp = 6, widthPx = sizePx, heightPx = sizePx)
                    canvas.drawBitmap(shaped, null, rect, null)
                    return
                }
            } catch (_: Exception) {}
        }

        when (glyphName.lowercase()) {
            "play", "play_arrow" -> {
                val path = Path()
                path.moveTo(cx - size * 0.35f, cy - size * 0.45f)
                path.lineTo(cx + size * 0.45f, cy)
                path.lineTo(cx - size * 0.35f, cy + size * 0.45f)
                path.close()
                val fillPaint = Paint(paint).apply { style = Paint.Style.FILL }
                canvas.drawPath(path, fillPaint)
            }
            "pause" -> {
                val fillPaint = Paint(paint).apply { style = Paint.Style.FILL }
                val barW = size * 0.22f
                val barH = size * 0.8f
                canvas.drawRoundRect(RectF(cx - size * 0.35f, cy - barH / 2f, cx - size * 0.35f + barW, cy + barH / 2f), barW / 3f, barW / 3f, fillPaint)
                canvas.drawRoundRect(RectF(cx + size * 0.35f - barW, cy - barH / 2f, cx + size * 0.35f, cy + barH / 2f), barW / 3f, barW / 3f, fillPaint)
            }
            "skip_next", "next" -> {
                val fillPaint = Paint(paint).apply { style = Paint.Style.FILL }
                val path = Path()
                path.moveTo(cx - size * 0.4f, cy - size * 0.4f)
                path.lineTo(cx + size * 0.1f, cy)
                path.lineTo(cx - size * 0.4f, cy + size * 0.4f)
                path.close()
                canvas.drawPath(path, fillPaint)
                val barW = size * 0.18f
                canvas.drawRoundRect(RectF(cx + size * 0.15f, cy - size * 0.4f, cx + size * 0.15f + barW, cy + size * 0.4f), barW / 3f, barW / 3f, fillPaint)
            }
            "skip_previous", "prev", "previous" -> {
                val fillPaint = Paint(paint).apply { style = Paint.Style.FILL }
                val barW = size * 0.18f
                canvas.drawRoundRect(RectF(cx - size * 0.15f - barW, cy - size * 0.4f, cx - size * 0.15f, cy + size * 0.4f), barW / 3f, barW / 3f, fillPaint)
                val path = Path()
                path.moveTo(cx + size * 0.4f, cy - size * 0.4f)
                path.lineTo(cx - size * 0.1f, cy)
                path.lineTo(cx + size * 0.4f, cy + size * 0.4f)
                path.close()
                canvas.drawPath(path, fillPaint)
            }
            "stop" -> {
                val fillPaint = Paint(paint).apply { style = Paint.Style.FILL }
                val r = size * 0.38f
                canvas.drawRoundRect(RectF(cx - r, cy - r, cx + r, cy + r), r * 0.25f, r * 0.25f, fillPaint)
            }
            "close", "clear" -> {
                val strokePaint = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (size * 0.18f).coerceAtLeast(2f)
                    strokeCap = Paint.Cap.ROUND
                }
                val d = size * 0.35f
                canvas.drawLine(cx - d, cy - d, cx + d, cy + d, strokePaint)
                canvas.drawLine(cx + d, cy - d, cx - d, cy + d, strokePaint)
            }
            "check" -> {
                val strokePaint = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (size * 0.18f).coerceAtLeast(2f)
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                val path = Path()
                path.moveTo(cx - size * 0.38f, cy)
                path.lineTo(cx - size * 0.1f, cy + size * 0.35f)
                path.lineTo(cx + size * 0.38f, cy - size * 0.35f)
                canvas.drawPath(path, strokePaint)
            }
            "add", "plus" -> {
                val strokePaint = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (size * 0.18f).coerceAtLeast(2f)
                    strokeCap = Paint.Cap.ROUND
                }
                val d = size * 0.35f
                canvas.drawLine(cx - d, cy, cx + d, cy, strokePaint)
                canvas.drawLine(cx, cy - d, cx, cy + d, strokePaint)
            }
            "remove", "minus" -> {
                val strokePaint = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (size * 0.18f).coerceAtLeast(2f)
                    strokeCap = Paint.Cap.ROUND
                }
                val d = size * 0.35f
                canvas.drawLine(cx - d, cy, cx + d, cy, strokePaint)
            }
            "notification", "notif" -> {
                val bellPath = Path()
                bellPath.moveTo(cx, cy - size * 0.36f)
                bellPath.cubicTo(cx - size * 0.22f, cy - size * 0.36f, cx - size * 0.28f, cy, cx - size * 0.36f, cy + size * 0.22f)
                bellPath.lineTo(cx + size * 0.36f, cy + size * 0.22f)
                bellPath.cubicTo(cx + size * 0.28f, cy, cx + size * 0.22f, cy - size * 0.36f, cx, cy - size * 0.36f)
                bellPath.close()
                val fillPaint = Paint(paint).apply { style = Paint.Style.FILL }
                canvas.drawPath(bellPath, fillPaint)
                canvas.drawCircle(cx, cy + size * 0.32f, (size * 0.08f).coerceAtLeast(2f), fillPaint)
            }
            "favorite", "heart", "like" -> {
                val heartPath = Path()
                heartPath.moveTo(cx, cy + size * 0.38f)
                heartPath.cubicTo(cx - size * 0.45f, cy + size * 0.05f, cx - size * 0.45f, cy - size * 0.35f, cx - size * 0.2f, cy - size * 0.35f)
                heartPath.cubicTo(cx - size * 0.05f, cy - size * 0.35f, cx, cy - size * 0.15f, cx, cy - size * 0.15f)
                heartPath.cubicTo(cx, cy - size * 0.15f, cx + size * 0.05f, cy - size * 0.35f, cx + size * 0.2f, cy - size * 0.35f)
                heartPath.cubicTo(cx + size * 0.45f, cy - size * 0.35f, cx + size * 0.45f, cy + size * 0.05f, cx, cy + size * 0.38f)
                heartPath.close()
                val fillPaint = Paint(paint).apply { style = Paint.Style.FILL }
                canvas.drawPath(heartPath, fillPaint)
            }
            "star" -> {
                val starPath = Path()
                val outerR = size * 0.42f
                val innerR = outerR * 0.45f
                for (i in 0 until 10) {
                    val angle = (i * 36 - 90) * Math.PI / 180.0
                    val r = if (i % 2 == 0) outerR else innerR
                    val x = cx + (r * Math.cos(angle)).toFloat()
                    val y = cy + (r * Math.sin(angle)).toFloat()
                    if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
                }
                starPath.close()
                val fillPaint = Paint(paint).apply { style = Paint.Style.FILL }
                canvas.drawPath(starPath, fillPaint)
            }
            "battery" -> {
                val strokePaint = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (size * 0.08f).coerceAtLeast(2f)
                }
                val bodyW = size * 0.7f
                val bodyH = size * 0.38f
                val bodyRect = RectF(cx - bodyW / 2f, cy - bodyH / 2f, cx + bodyW / 2f - size * 0.08f, cy + bodyH / 2f)
                canvas.drawRoundRect(bodyRect, size * 0.06f, size * 0.06f, strokePaint)
                val fillPaint = Paint(paint).apply { style = Paint.Style.FILL }
                val tipRect = RectF(cx + bodyW / 2f - size * 0.06f, cy - bodyH * 0.25f, cx + bodyW / 2f, cy + bodyH * 0.25f)
                canvas.drawRoundRect(tipRect, 2f, 2f, fillPaint)
                val innerRect = RectF(bodyRect.left + size * 0.08f, bodyRect.top + size * 0.08f, bodyRect.centerX(), bodyRect.bottom - size * 0.08f)
                canvas.drawRect(innerRect, fillPaint)
            }
            "music", "music_note" -> {
                val fillPaint = Paint(paint).apply { style = Paint.Style.FILL }
                val strokePaint = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (size * 0.1f).coerceAtLeast(2f)
                }
                canvas.drawOval(RectF(cx - size * 0.35f, cy + size * 0.1f, cx - size * 0.05f, cy + size * 0.35f), fillPaint)
                canvas.drawLine(cx - size * 0.05f, cy + size * 0.2f, cx - size * 0.05f, cy - size * 0.35f, strokePaint)
                val flagPath = Path()
                flagPath.moveTo(cx - size * 0.05f, cy - size * 0.35f)
                flagPath.cubicTo(cx + size * 0.15f, cy - size * 0.35f, cx + size * 0.25f, cy - size * 0.15f, cx + size * 0.2f, cy)
                flagPath.lineTo(cx - size * 0.05f, cy - size * 0.15f)
                flagPath.close()
                canvas.drawPath(flagPath, fillPaint)
            }
            else -> {
                val resId = when (glyphName.lowercase()) {
                    "battery" -> android.R.drawable.ic_lock_idle_charging
                    else -> R.drawable.ic_launcher_foreground
                }
                val drawable = ContextCompat.getDrawable(context, resId)
                if (drawable != null) {
                    val bmp = drawableToBitmap(drawable, rect.width().roundToInt().coerceAtLeast(1), rect.height().roundToInt().coerceAtLeast(1))
                    val tintedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        colorFilter = PorterDuffColorFilter(paint.color, PorterDuff.Mode.SRC_IN)
                    }
                    canvas.drawBitmap(bmp, null, rect, tintedPaint)
                }
            }
        }
    }

    private fun renderButton(doc: CustomWidgetDocument, node: ButtonNode, bridgeId: Int?, intents: WidgetActionIntents, ctx: VariableContext, scaleX: Float = 1f): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.layout_widget_node_button)

        val isConditionalActive = node.conditionalEnabled &&
            NodeConditionEvaluator.isVisible(node.condition, ctx, engine)

        val effectiveLabel = if (isConditionalActive) (node.conditionalLabel ?: node.label) else node.label
        val rawLabel = resolveString(node, BindableProperty.BUTTON_LABEL, effectiveLabel, ctx).orEmpty()
        val resolvedLabel = engine.resolve(rawLabel, ctx)

        val effectiveIcon = if (isConditionalActive) (node.conditionalIcon ?: node.icon) else node.icon
        val rawIcon = resolveString(node, BindableProperty.BUTTON_ICON, effectiveIcon, ctx)
        val resolvedIcon = rawIcon?.let { engine.resolve(it, ctx).ifBlank { null } }

        val effectiveSubIcon = if (isConditionalActive) (node.conditionalSubIcon ?: node.subIcon) else node.subIcon
        val rawSubIcon = resolveString(node, BindableProperty.BUTTON_SUB_ICON, effectiveSubIcon, ctx)
        val resolvedSubIcon = rawSubIcon?.let { engine.resolve(it, ctx).ifBlank { null } }

        val defaultBgColor = ctx.themePrimary?.let { try { it.toColorInt() } catch (_: Exception) { null } }
            ?: 0xFF3DDA82.toInt()
        val effectiveBgHex = if (isConditionalActive) (node.conditionalBackgroundHex ?: node.backgroundHex) else node.backgroundHex
        val bgColor = resolveColor(node, BindableProperty.BUTTON_BACKGROUND, effectiveBgHex, ctx) ?: defaultBgColor

        val effectiveTextHex = if (isConditionalActive) (node.conditionalTextColorHex ?: node.textColorHex) else node.textColorHex
        val textColor = resolveColor(node, BindableProperty.BUTTON_TEXT_COLOR, effectiveTextHex, ctx) ?: Color.WHITE

        val effectiveShapeId = if (isConditionalActive) (node.conditionalShapeId ?: node.shapeId) else node.shapeId
        val resolvedShapeId = resolveString(node, BindableProperty.BUTTON_SHAPE, effectiveShapeId, ctx) ?: effectiveShapeId

        val effectiveCornerRadius = if (isConditionalActive) (node.conditionalCornerRadiusDp ?: node.cornerRadiusDp) else node.cornerRadiusDp
        val resolvedCornerRadius = resolveInt(node, BindableProperty.BUTTON_CORNER_RADIUS, effectiveCornerRadius, ctx) ?: effectiveCornerRadius

        val effectiveStrokeWidth = if (isConditionalActive) (node.conditionalStrokeWidthDp ?: node.strokeWidthDp) else node.strokeWidthDp
        val resolvedStrokeWidth = resolveInt(node, BindableProperty.BUTTON_STROKE_WIDTH, effectiveStrokeWidth, ctx) ?: effectiveStrokeWidth

        val effectiveStrokeColorHex = if (isConditionalActive) (node.conditionalStrokeColorHex ?: node.strokeColorHex) else node.strokeColorHex
        val strokeColor = resolveColor(node, BindableProperty.BUTTON_STROKE_COLOR, effectiveStrokeColorHex, ctx)

        val effectiveAction = if (isConditionalActive) (node.conditionalAction ?: node.action) else node.action
        val pendingIntent: PendingIntent? = resolveAction(requestCode(doc, node, bridgeId), effectiveAction, bridgeId, intents, ctx)
        val finalPendingIntent = pendingIntent ?: noopPendingIntent(requestCode(doc, node, bridgeId))

        val rawWidthDp = node.bounds.widthDp?.takeIf { it > 0 } ?: 80
        val widthDp = (rawWidthDp * scaleX).roundToInt()
        val heightDp = node.bounds.heightDp?.takeIf { it > 0 } ?: 36
        val widthPx = dpToPx(widthDp).coerceAtLeast(24)
        val heightPx = dpToPx(heightDp).coerceAtLeast(24)

        val bitmap = createBitmapWithDensity(widthPx, heightPx)
        val canvas = Canvas(bitmap)

        val rect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }

        val shapePath = Path()
        when (resolvedShapeId.lowercase()) {
            "rectangle", "square", "rect" -> {
                shapePath.addRect(rect, Path.Direction.CW)
            }
            "circle", "ellipse" -> {
                shapePath.addOval(rect, Path.Direction.CW)
            }
            "pill", "stadium" -> {
                val r = heightPx / 2f
                shapePath.addRoundRect(rect, r, r, Path.Direction.CW)
            }
            "rounded", "rounded_rect" -> {
                val r = dpToPx(resolvedCornerRadius).toFloat().coerceIn(0f, minOf(widthPx, heightPx) / 2f)
                shapePath.addRoundRect(rect, r, r, Path.Direction.CW)
            }
            else -> {
                val polygon = getShapeFromId(resolvedShapeId)
                val p = polygon.toPath()
                val bounds = RectF()
                p.computeBounds(bounds, true)
                val matrix = Matrix()
                matrix.setRectToRect(bounds, rect, Matrix.ScaleToFit.FILL)
                p.transform(matrix, shapePath)
            }
        }
        canvas.drawPath(shapePath, bgPaint)

        if (resolvedStrokeWidth > 0 && strokeColor != null) {
            val strokeWidthPx = dpToPx(resolvedStrokeWidth).toFloat().coerceAtLeast(1f)
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = strokeColor
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidthPx
            }
            canvas.drawPath(shapePath, strokePaint)
        }

        val resolvedIconSize = resolveInt(node, BindableProperty.BUTTON_ICON_SIZE, node.iconSizeDp, ctx) ?: node.iconSizeDp
        val iconSizePx = dpToPx(resolvedIconSize.coerceIn(8, 48)).coerceAtMost(heightPx - 4)
        val hasIcon = !resolvedIcon.isNullOrBlank()
        val hasText = resolvedLabel.isNotBlank() && node.iconPosition != ButtonIconPosition.ICON_ONLY

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = dpToPx(13).toFloat()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
        }

        if (hasIcon && !hasText) {
            val iconRect = RectF(
                (widthPx - iconSizePx) / 2f,
                (heightPx - iconSizePx) / 2f,
                (widthPx + iconSizePx) / 2f,
                (heightPx + iconSizePx) / 2f
            )
            drawGlyph(canvas, resolvedIcon!!, iconRect, iconPaint)
        } else if (hasText && !hasIcon) {
            val textY = (heightPx / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText(resolvedLabel, widthPx / 2f, textY, textPaint)
        } else if (hasIcon && hasText) {
            val gapPx = dpToPx(4)
            val textWidth = textPaint.measureText(resolvedLabel)
            val textY = (heightPx / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)

            when (node.iconPosition) {
                ButtonIconPosition.LEADING -> {
                    val totalContentW = iconSizePx + gapPx + textWidth
                    val startX = ((widthPx - totalContentW) / 2f).coerceAtLeast(4f)
                    val iconRect = RectF(startX, (heightPx - iconSizePx) / 2f, startX + iconSizePx, (heightPx + iconSizePx) / 2f)
                    drawGlyph(canvas, resolvedIcon!!, iconRect, iconPaint)
                    val textCenterX = startX + iconSizePx + gapPx + (textWidth / 2f)
                    canvas.drawText(resolvedLabel, textCenterX, textY, textPaint)
                }
                ButtonIconPosition.TRAILING -> {
                    val totalContentW = textWidth + gapPx + iconSizePx
                    val startX = ((widthPx - totalContentW) / 2f).coerceAtLeast(4f)
                    val textCenterX = startX + (textWidth / 2f)
                    canvas.drawText(resolvedLabel, textCenterX, textY, textPaint)
                    val iconRect = RectF(startX + textWidth + gapPx, (heightPx - iconSizePx) / 2f, startX + textWidth + gapPx + iconSizePx, (heightPx + iconSizePx) / 2f)
                    drawGlyph(canvas, resolvedIcon!!, iconRect, iconPaint)
                }
                ButtonIconPosition.TOP -> {
                    val smallIconSize = (iconSizePx * 0.75f).toInt()
                    val totalContentH = smallIconSize + gapPx + dpToPx(12)
                    val startY = ((heightPx - totalContentH) / 2f).coerceAtLeast(2f)
                    val iconRect = RectF((widthPx - smallIconSize) / 2f, startY, (widthPx + smallIconSize) / 2f, startY + smallIconSize)
                    drawGlyph(canvas, resolvedIcon!!, iconRect, iconPaint)
                    val labelY = startY + smallIconSize + gapPx + dpToPx(10)
                    canvas.drawText(resolvedLabel, widthPx / 2f, labelY, textPaint)
                }
                ButtonIconPosition.ICON_ONLY -> {
                    val iconRect = RectF((widthPx - iconSizePx) / 2f, (heightPx - iconSizePx) / 2f, (widthPx + iconSizePx) / 2f, (heightPx + iconSizePx) / 2f)
                    drawGlyph(canvas, resolvedIcon!!, iconRect, iconPaint)
                }
            }
        }

        if (!resolvedSubIcon.isNullOrBlank()) {
            val badgeSize = (iconSizePx * 0.65f).coerceAtLeast(dpToPx(10).toFloat())
            val badgeRect = RectF(widthPx - badgeSize - dpToPx(2), heightPx - badgeSize - dpToPx(2), widthPx.toFloat() - dpToPx(2), heightPx.toFloat() - dpToPx(2))
            val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = bgColor
                style = Paint.Style.FILL
            }
            canvas.drawCircle(badgeRect.centerX(), badgeRect.centerY(), badgeSize / 2f, badgeBgPaint)
            drawGlyph(canvas, resolvedSubIcon, badgeRect, iconPaint)
        }

        rv.setImageViewBitmap(R.id.node_button, bitmap)
        rv.setOnClickPendingIntent(R.id.node_button, finalPendingIntent)
        return rv
    }

    /** Turns a [ButtonAction] into something tappable, or null when there is nothing to fire. */
    /**
     * PendingIntents are matched without their extras, so a request code of just the node id let
     * two islands built from the same design (or two designs sharing a node id) overwrite each
     * other's intent under FLAG_UPDATE_CURRENT: Dismiss on one island closed the other.
     */
    private fun requestCode(doc: CustomWidgetDocument, node: CustomWidgetNode, bridgeId: Int?): Int =
        "${doc.id}/${node.id}/${bridgeId ?: 0}".hashCode()

    private fun resolveAction(
        requestCode: Int,
        action: ButtonAction,
        bridgeId: Int?,
        intents: WidgetActionIntents,
        ctx: VariableContext
    ): PendingIntent? = when (action) {
        is ButtonAction.OpenApp -> {
            val targetPkg = action.packageName.ifBlank { ctx.notifPackage.orEmpty() }
            if (targetPkg.isNotBlank()) {
                context.packageManager.getLaunchIntentForPackage(targetPkg)?.let {
                    PendingIntent.getActivity(context, requestCode, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                }
            } else null
        }

        is ButtonAction.DeepLink -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.uri))
            PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        is ButtonAction.Dismiss -> {
            val intent = Intent(context, WidgetActionReceiver::class.java).apply {
                setAction(WidgetActionReceiver.ACTION_DISMISS)
                putExtra(WidgetActionReceiver.EXTRA_BRIDGE_ID, bridgeId ?: -1)
            }
            PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        // The notification's own button, handed to us by the translator.
        is ButtonAction.NotificationAction -> intents.notificationActions.getOrNull(action.index)

        is ButtonAction.SmartAction -> intents.smartActions.entries
            .firstOrNull { it.key.equals(action.type, ignoreCase = true) }?.value

        is ButtonAction.Broadcast -> {
            val intent = Intent(action.action).apply {
                action.packageName?.takeIf { it.isNotBlank() }?.let { setPackage(it) }
                if (!action.extraKey.isNullOrBlank()) putExtra(action.extraKey, action.extraValue.orEmpty())
            }
            PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        is ButtonAction.MediaControl -> {
            val intent = Intent(context, WidgetActionReceiver::class.java).apply {
                setAction(WidgetActionReceiver.ACTION_MEDIA_CONTROL)
                putExtra(WidgetActionReceiver.EXTRA_MEDIA_COMMAND, action.command)
            }
            PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        // Inline reply fires the notification's own reply action when it has one; a hand-drawn
        // button still cannot open the keyboard by itself.
        is ButtonAction.InlineReply -> intents.inlineReply
    }

    private fun noopPendingIntent(requestCode: Int): PendingIntent {
        val intent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = WidgetActionReceiver.ACTION_NOOP
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()
    }

    private fun createBitmapWithDensity(w: Int, h: Int): Bitmap {
        val bmp = createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1))
        bmp.density = context.resources.displayMetrics.densityDpi
        return bmp
    }

    companion object {
        const val BASE_CANVAS_WIDTH_DP = 350

        fun getIslandWidthDp(context: Context): Float {
            val displayMetrics = context.resources.displayMetrics
            val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
            return (screenWidthDp - 24f).coerceIn(320f, 600f)
        }

        fun computeImageBounds(
            srcW: Float,
            srcH: Float,
            widthPx: Float,
            heightPx: Float,
            scaleType: ImageScaleType,
            isGlyph: Boolean = false,
            minGlyphSize: Float = 12f
        ): ImageBounds {
            val baseSizePx = minOf(widthPx, heightPx)
            val (targetW, targetH) = when (scaleType) {
                ImageScaleType.FIT_WIDTH -> {
                    val w = widthPx
                    val h = if (isGlyph) (widthPx * 0.75f).coerceAtLeast(minGlyphSize) else (srcH * (widthPx / srcW.coerceAtLeast(1f)))
                    Pair(w, h)
                }
                ImageScaleType.FIT_HEIGHT -> {
                    val h = heightPx
                    val w = if (isGlyph) (heightPx * 0.75f).coerceAtLeast(minGlyphSize) else (srcW * (heightPx / srcH.coerceAtLeast(1f)))
                    Pair(w, h)
                }
                ImageScaleType.FIT_CENTER -> {
                    if (isGlyph) {
                        val glyphSize = (baseSizePx * 0.65f).coerceAtLeast(minGlyphSize)
                        Pair(glyphSize, glyphSize)
                    } else {
                        val scale = minOf(widthPx / srcW.coerceAtLeast(1f), heightPx / srcH.coerceAtLeast(1f))
                        Pair(srcW * scale, srcH * scale)
                    }
                }
                ImageScaleType.CENTER_CROP -> {
                    if (isGlyph) {
                        val cropSize = maxOf(widthPx, heightPx)
                        Pair(cropSize, cropSize)
                    } else {
                        val scale = maxOf(widthPx / srcW.coerceAtLeast(1f), heightPx / srcH.coerceAtLeast(1f))
                        Pair(srcW * scale, srcH * scale)
                    }
                }
            }

            val left = (widthPx - targetW) / 2f
            val top = (heightPx - targetH) / 2f
            return ImageBounds(left, top, targetW, targetH)
        }
    }
}

data class ImageBounds(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
    fun toRectF(): RectF = RectF(left, top, right, bottom)
}
