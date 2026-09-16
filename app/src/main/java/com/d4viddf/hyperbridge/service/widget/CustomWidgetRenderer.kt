package com.d4viddf.hyperbridge.service.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import com.d4viddf.hyperbridge.models.widget.ButtonAction
import com.d4viddf.hyperbridge.models.widget.ButtonNode
import com.d4viddf.hyperbridge.models.widget.ContainerLayout
import com.d4viddf.hyperbridge.models.widget.CustomWidgetDocument
import com.d4viddf.hyperbridge.models.widget.CustomWidgetNode
import com.d4viddf.hyperbridge.models.widget.ImageNode
import com.d4viddf.hyperbridge.models.widget.ImageSource
import com.d4viddf.hyperbridge.models.widget.LayoutContainer
import com.d4viddf.hyperbridge.models.widget.ProgressNode
import com.d4viddf.hyperbridge.models.widget.ProgressStyle
import com.d4viddf.hyperbridge.models.widget.TextNode
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

    /** [bridgeId], when known, lets a Dismiss button cancel the actual posted notification. */
    fun render(doc: CustomWidgetDocument, ctx: VariableContext, bridgeId: Int? = null): RemoteViews {
        val root = RemoteViews(context.packageName, R.layout.layout_widget_canvas_root)
        val built = renderNode(doc, doc.root, ctx, bridgeId)
        root.removeAllViews(R.id.widget_canvas_insertion_point)
        root.addView(R.id.widget_canvas_insertion_point, built)
        return root
    }

    private fun renderNode(doc: CustomWidgetDocument, node: CustomWidgetNode, ctx: VariableContext, bridgeId: Int?): RemoteViews {
        val rv = when (node) {
            is LayoutContainer -> renderContainer(doc, node, ctx, bridgeId)
            is TextNode -> renderText(node, ctx)
            is ImageNode -> renderImage(doc, node, ctx)
            is ProgressNode -> renderProgress(node, ctx)
            is ButtonNode -> renderButton(node, bridgeId)
        }
        applySize(rv, rootViewId(node), node.bounds.widthDp, node.bounds.heightDp)
        return rv
    }

    private fun rootViewId(node: CustomWidgetNode): Int = when (node) {
        is LayoutContainer -> R.id.widget_container_root
        is TextNode -> R.id.node_text
        is ImageNode -> R.id.node_image
        is ProgressNode -> R.id.node_progress
        is ButtonNode -> R.id.node_button
    }

    private fun applySize(rv: RemoteViews, viewId: Int, widthDp: Int?, heightDp: Int?) {
        if (widthDp != null) rv.setViewLayoutWidth(viewId, widthDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
        if (heightDp != null) rv.setViewLayoutHeight(viewId, heightDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
    }

    private fun renderContainer(doc: CustomWidgetDocument, node: LayoutContainer, ctx: VariableContext, bridgeId: Int?): RemoteViews {
        val layoutRes = when (node.layout) {
            ContainerLayout.ROW -> R.layout.layout_widget_container_row
            ContainerLayout.COLUMN -> R.layout.layout_widget_container_column
            ContainerLayout.BOX, ContainerLayout.ABSOLUTE -> R.layout.layout_widget_container_box
        }
        val rv = RemoteViews(context.packageName, layoutRes)
        if (node.backgroundHex != null) {
            try {
                rv.setInt(R.id.widget_container_root, "setBackgroundColor", node.backgroundHex.toColorInt())
            } catch (_: Exception) { /* ignore invalid color */ }
        }
        rv.setViewPadding(
            R.id.widget_container_root,
            dpToPx(node.paddingDp), dpToPx(node.paddingDp), dpToPx(node.paddingDp), dpToPx(node.paddingDp)
        )

        node.children.forEachIndexed { index, child ->
            val childRv = renderNode(doc, child, ctx, bridgeId)
            if (node.layout == ContainerLayout.ABSOLUTE) {
                rv.setViewLayoutMargin(rootViewId(child), RemoteViews.MARGIN_LEFT, child.bounds.x.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
                rv.setViewLayoutMargin(rootViewId(child), RemoteViews.MARGIN_TOP, child.bounds.y.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
            } else if (index > 0 && node.gapDp > 0) {
                val marginSide = if (node.layout == ContainerLayout.ROW) RemoteViews.MARGIN_LEFT else RemoteViews.MARGIN_TOP
                rv.setViewLayoutMargin(rootViewId(child), marginSide, node.gapDp.toFloat(), TypedValue.COMPLEX_UNIT_DIP)
            }
            rv.addView(R.id.widget_container_root, childRv)
        }
        return rv
    }

    private fun renderText(node: TextNode, ctx: VariableContext): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.layout_widget_node_text)
        val resolved = engine.resolve(node.template, ctx)
        rv.setTextViewText(R.id.node_text, resolved)
        rv.setTextViewTextSize(R.id.node_text, TypedValue.COMPLEX_UNIT_SP, node.fontSizeSp.toFloat())
        try {
            rv.setTextColor(R.id.node_text, node.colorHex.toColorInt())
        } catch (_: Exception) { /* keep default */ }
        rv.setInt(R.id.node_text, "setMaxLines", node.maxLines)
        rv.setBoolean(R.id.node_text, "setSingleLine", node.maxLines == 1)
        // Marquee support inside a RemoteViews tree hosted by another process (the island shell)
        // is unreliable - it generally needs `isSelected`/focus that a hosted view never gets - so
        // rather than risk a reflective RemoteViews call that throws on some OEMs, [node.marquee]
        // always falls back to the layout's static `android:ellipsize="end"` truncation (see #273
        // scope notes: "marquee is a best-effort flag that may no-op").
        return rv
    }

    private fun renderImage(doc: CustomWidgetDocument, node: ImageNode, ctx: VariableContext): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.layout_widget_node_image)
        val bitmap = resolveImageBitmap(doc, node, ctx)
        if (bitmap != null) {
            val shaped = applyShape(bitmap, node.shapeId)
            rv.setImageViewBitmap(R.id.node_image, shaped)
        }
        return rv
    }

    private fun resolveImageBitmap(doc: CustomWidgetDocument, node: ImageNode, ctx: VariableContext): Bitmap? {
        return try {
            when (val source = node.source) {
                is ImageSource.AppIconOf -> {
                    val pkg = engine.resolve(source.packageTemplate, ctx)
                    if (pkg.isBlank()) null else drawableToBitmap(context.packageManager.getApplicationIcon(pkg))
                }
                is ImageSource.ContactAvatarOf -> null // Descoped: requires READ_CONTACTS (see #273 scope notes).
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
        return if (node.style == ProgressStyle.LINEAR) {
            val rv = RemoteViews(context.packageName, R.layout.layout_widget_node_progress_linear)
            val resolved = engine.resolve(node.valueTemplate, ctx).toIntOrNull() ?: 0
            rv.setProgressBar(R.id.node_progress, node.maxValue, resolved.coerceIn(0, node.maxValue), false)
            try {
                rv.setColorStateList(R.id.node_progress, "setProgressTintList", android.content.res.ColorStateList.valueOf(node.progressColorHex.toColorInt()))
            } catch (_: Exception) { /* best-effort tint only */ }
            rv
        } else {
            // RING style with no other siblings is meant to be routed through the structured
            // setBigIslandInfo(progressInfo = CircularProgressInfo(...)) path by the translator
            // instead of reaching this renderer (see #273 scope notes); as a node inside a larger
            // canvas there is no native RemoteViews ring view, so we fall back to a plain bar.
            val rv = RemoteViews(context.packageName, R.layout.layout_widget_node_progress_linear)
            val resolved = engine.resolve(node.valueTemplate, ctx).toIntOrNull() ?: 0
            rv.setProgressBar(R.id.node_progress, node.maxValue, resolved.coerceIn(0, node.maxValue), false)
            rv
        }
    }

    private fun renderButton(node: ButtonNode, bridgeId: Int?): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.layout_widget_node_button)
        rv.setTextViewText(R.id.node_button, node.label)
        try {
            rv.setTextColor(R.id.node_button, node.textColorHex.toColorInt())
        } catch (_: Exception) { /* keep default */ }
        node.backgroundHex?.let {
            try {
                rv.setInt(R.id.node_button, "setBackgroundColor", it.toColorInt())
            } catch (_: Exception) { /* ignore */ }
        }

        val pendingIntent: PendingIntent? = when (val buttonAction = node.action) {
            is ButtonAction.OpenApp -> {
                context.packageManager.getLaunchIntentForPackage(buttonAction.packageName)?.let {
                    PendingIntent.getActivity(context, node.id.hashCode(), it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                }
            }
            is ButtonAction.DeepLink -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(buttonAction.uri))
                PendingIntent.getActivity(context, node.id.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            }
            is ButtonAction.Dismiss -> {
                val intent = Intent(context, WidgetActionReceiver::class.java).apply {
                    setAction(WidgetActionReceiver.ACTION_DISMISS)
                    putExtra(WidgetActionReceiver.EXTRA_BRIDGE_ID, bridgeId ?: -1)
                }
                PendingIntent.getBroadcast(context, node.id.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            }
            // Inline reply needs a RemoteInput target tied to the *original* notification's own
            // reply action, which a hand-drawn widget button doesn't have - descoped for this
            // vertical slice (see #273 scope notes); tapping it currently does nothing.
            is ButtonAction.InlineReply -> null
        }
        if (pendingIntent != null) {
            rv.setOnClickPendingIntent(R.id.node_button, pendingIntent)
        }
        return rv
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()
    }
}
