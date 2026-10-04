package com.d4viddf.hyperbridge.service.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.net.Uri
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
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
        intents: WidgetActionIntents = WidgetActionIntents()
    ): RemoteViews {
        val root = RemoteViews(context.packageName, R.layout.layout_widget_canvas_root)
        // Conditional nodes (#328) are dropped before rendering, so a button bound to an inline
        // reply simply does not exist on a notification that has none.
        val visibleRoot = NodeConditionEvaluator.prune(doc.root, ctx, engine) as? LayoutContainer
            ?: LayoutContainer(id = doc.root.id, layout = doc.root.layout)
        val built = renderNode(doc, visibleRoot, ctx, bridgeId, intents)
        root.removeAllViews(R.id.widget_canvas_insertion_point)
        root.setViewLayoutHeight(R.id.widget_canvas_insertion_point, doc.canvas.heightDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
        root.addView(R.id.widget_canvas_insertion_point, built)
        return root
    }

    private fun renderNode(
        doc: CustomWidgetDocument,
        node: CustomWidgetNode,
        ctx: VariableContext,
        bridgeId: Int?,
        intents: WidgetActionIntents
    ): RemoteViews {
        val rv = when (node) {
            is LayoutContainer -> renderContainer(doc, node, ctx, bridgeId, intents)
            is TextNode -> renderText(doc, node, ctx)
            is ImageNode -> renderImage(doc, node, ctx)
            is ProgressNode -> renderProgress(node, ctx)
            is ButtonNode -> renderButton(doc, node, bridgeId, intents, ctx)
            is ShapeNode -> renderShape(node, ctx)
        }
        val widthDp = if (node is LayoutContainer && node.bounds.widthDp == null && node.id != doc.root.id) {
            node.adaptedContentWidth()
        } else if (node is TextNode && (node.sizingType == TextSizingType.FIXED_WIDTH || node.sizingType == TextSizingType.FIT_BOX) && node.boxWidthDp != null) {
            node.boxWidthDp
        } else {
            resolveInt(node, BindableProperty.BOUNDS_WIDTH, node.bounds.widthDp, ctx)
        }
        val heightDp = if (node is LayoutContainer && node.bounds.heightDp == null && node.id != doc.root.id) {
            node.adaptedContentHeight()
        } else {
            resolveInt(node, BindableProperty.BOUNDS_HEIGHT, node.bounds.heightDp, ctx)
        }
        val effectiveHeightDp = if (node is TextNode && node.maxLines > 1 && heightDp != null) {
            val minNeeded = (node.fontSizeSp * 1.35f * node.maxLines).toInt()
            maxOf(heightDp, minNeeded)
        } else heightDp
        applySize(rv, rootViewId(node), widthDp, effectiveHeightDp)

        val resolvedOpacity = resolveFloat(node, BindableProperty.OPACITY, node.opacity, ctx) ?: 1f
        rv.setFloat(rootViewId(node), "setAlpha", resolvedOpacity.coerceIn(0f, 1f))

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
                try {
                    return resolved.toColorInt()
                } catch (_: Exception) {}
            }
        }
        return staticHex?.let {
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

    private fun renderContainer(
        doc: CustomWidgetDocument,
        node: LayoutContainer,
        ctx: VariableContext,
        bridgeId: Int?,
        intents: WidgetActionIntents
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
            val childRv = renderNode(doc, child, ctx, bridgeId, intents)
            if (node.layout == ContainerLayout.ABSOLUTE || node.layout == ContainerLayout.BOX) {
                childRv.setViewLayoutMargin(rootViewId(child), RemoteViews.MARGIN_LEFT, child.bounds.x.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                childRv.setViewLayoutMargin(rootViewId(child), RemoteViews.MARGIN_TOP, child.bounds.y.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
            } else if (index > 0 && node.gapDp > 0) {
                val marginSide = if (node.layout == ContainerLayout.ROW) RemoteViews.MARGIN_LEFT else RemoteViews.MARGIN_TOP
                childRv.setViewLayoutMargin(rootViewId(child), marginSide, node.gapDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
            }
            rv.addView(R.id.widget_container_root, childRv)
        }
        return rv
    }

    private fun renderText(doc: CustomWidgetDocument, node: TextNode, ctx: VariableContext): RemoteViews {
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

    private fun renderImage(doc: CustomWidgetDocument, node: ImageNode, ctx: VariableContext): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.layout_widget_node_image)
        val bitmap = resolveImageBitmap(doc, node, ctx)
        if (bitmap != null) {
            val shaped = applyShape(bitmap, node.shapeId)
            rv.setImageViewBitmap(R.id.node_image, shaped)
        }
        val tintColor = resolveColor(node, BindableProperty.IMAGE_TINT, node.tintHex, ctx)
        if (tintColor != null) {
            rv.setInt(R.id.node_image, "setColorFilter", tintColor)
        }
        return rv
    }

    private fun renderShape(node: ShapeNode, ctx: VariableContext): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.layout_widget_node_shape)
        val widthDp = resolveInt(node, BindableProperty.BOUNDS_WIDTH, node.bounds.widthDp, ctx) ?: 48
        val heightDp = resolveInt(node, BindableProperty.BOUNDS_HEIGHT, node.bounds.heightDp, ctx) ?: 48
        val widthPx = dpToPx(widthDp).coerceAtLeast(1)
        val heightPx = dpToPx(heightDp).coerceAtLeast(1)

        val fillColor = resolveColor(node, BindableProperty.SHAPE_FILL, node.fillColorHex, ctx)
        val strokeColor = resolveColor(node, BindableProperty.SHAPE_STROKE, node.strokeColorHex, ctx)
        val strokeWidthPx = dpToPx(resolveInt(node, BindableProperty.SHAPE_STROKE_WIDTH, node.strokeWidthDp, ctx) ?: 0)

        val bitmap = createBitmap(widthPx, heightPx)
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

    private fun resolveImageBitmap(doc: CustomWidgetDocument, node: ImageNode, ctx: VariableContext): Bitmap? {
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
        }
    }

    private fun systemGlyphBitmap(glyphName: String): Bitmap? {
        val resId = when (glyphName) {
            "battery" -> android.R.drawable.ic_lock_idle_charging
            "notification" -> R.drawable.ic_launcher_foreground
            else -> R.drawable.ic_launcher_foreground
        }
        val drawable = ContextCompat.getDrawable(context, resId) ?: return null
        return drawableToBitmap(drawable)
    }

    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
        val size = 96
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bitmap
    }

    private fun applyShape(source: Bitmap, shapeId: String): Bitmap {
        val size = 96
        val output = createBitmap(size, size)
        val canvas = Canvas(output)
        val polygon = getShapeFromId(shapeId)
        val path = polygon.toPath()
        val bounds = RectF()
        path.computeBounds(bounds, true)
        val matrix = Matrix()
        matrix.setRectToRect(bounds, RectF(0f, 0f, size.toFloat(), size.toFloat()), Matrix.ScaleToFit.FILL)
        path.transform(matrix)

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawPath(path, maskPaint)
        val srcPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        val scaled = source.let {
            android.graphics.Bitmap.createScaledBitmap(it, size, size, true)
        }
        canvas.drawBitmap(scaled, 0f, 0f, srcPaint)
        return output
    }

    private fun renderProgress(node: ProgressNode, ctx: VariableContext): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.layout_widget_node_progress_linear)
        val valueTemplate = resolveString(node, BindableProperty.PROGRESS_VALUE, node.valueTemplate, ctx).orEmpty()
        val resolved = engine.resolve(valueTemplate, ctx).toIntOrNull() ?: 0
        rv.setProgressBar(R.id.node_progress, node.maxValue, resolved.coerceIn(0, node.maxValue), false)

        val progressColor = resolveColor(node, BindableProperty.PROGRESS_COLOR, node.progressColorHex, ctx)
        if (progressColor != null) {
            try {
                rv.setColorStateList(R.id.node_progress, "setProgressTintList", android.content.res.ColorStateList.valueOf(progressColor))
            } catch (_: Exception) { /* best-effort tint only */ }
        }
        val trackColor = resolveColor(node, BindableProperty.PROGRESS_TRACK_COLOR, node.trackColorHex, ctx)
        if (trackColor != null) {
            try {
                rv.setColorStateList(R.id.node_progress, "setProgressBackgroundTintList", android.content.res.ColorStateList.valueOf(trackColor))
            } catch (_: Exception) { /* best-effort tint only */ }
        }
        return rv
    }

    private fun renderButton(doc: CustomWidgetDocument, node: ButtonNode, bridgeId: Int?, intents: WidgetActionIntents, ctx: VariableContext): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.layout_widget_node_button)
        val rawLabel = resolveString(node, BindableProperty.BUTTON_LABEL, node.label, ctx).orEmpty()
        val resolvedLabel = engine.resolve(rawLabel, ctx)
        rv.setTextViewText(R.id.node_button, resolvedLabel)

        val defaultBgColor = ctx.themePrimary?.let { try { it.toColorInt() } catch (_: Exception) { null } }
            ?: 0xFF3DDA82.toInt()
        val bgColor = resolveColor(node, BindableProperty.BUTTON_BACKGROUND, node.backgroundHex, ctx) ?: defaultBgColor
        try {
            rv.setColorStateList(R.id.node_button, "setBackgroundTintList", ColorStateList.valueOf(bgColor))
        } catch (_: Exception) {
            rv.setInt(R.id.node_button, "setBackgroundColor", bgColor)
        }

        val textColor = resolveColor(node, BindableProperty.BUTTON_TEXT_COLOR, node.textColorHex, ctx) ?: Color.WHITE
        rv.setTextColor(R.id.node_button, textColor)
        rv.setTextViewTextSize(R.id.node_button, TypedValue.COMPLEX_UNIT_SP, 13f)

        val pendingIntent: PendingIntent? = resolveAction(requestCode(doc, node, bridgeId), node.action, bridgeId, intents, ctx)
        val finalPendingIntent = pendingIntent ?: noopPendingIntent(requestCode(doc, node, bridgeId))
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
}
