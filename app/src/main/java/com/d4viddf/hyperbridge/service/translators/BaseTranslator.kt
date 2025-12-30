package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.app.Person
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.graphics.shapes.toPath
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.BridgeAction
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.theme.ResourceType
import com.d4viddf.hyperbridge.models.theme.ThemeResource
import com.d4viddf.hyperbridge.ui.screens.theme.getShapeFromId
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperPicture

abstract class BaseTranslator(
    protected val context: Context,
    protected val repository: ThemeRepository? = null
) {

    enum class ActionDisplayMode { TEXT, ICON, BOTH }

    protected inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {
        return if (Build.VERSION.SDK_INT >= 33) {
            getParcelable(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelable(key)
        }
    }

    protected inline fun <reified T : Parcelable> Bundle.getParcelableArrayListCompat(key: String): ArrayList<T>? {
        return if (Build.VERSION.SDK_INT >= 33) {
            getParcelableArrayList(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableArrayList(key)
        }
    }

    // --- THEME HELPERS ---

    protected fun getThemeBitmap(theme: HyperTheme?, resourceKey: String): Bitmap? {
        if (theme == null || repository == null) return null
        val resource = ThemeResource(ResourceType.LOCAL_FILE, "icons/$resourceKey.png")
        return repository.getResourceBitmap(resource)
    }

    protected fun resolveColor(theme: HyperTheme?, pkg: String?, defaultHex: String): String {
        if (theme == null) return defaultHex
        if (pkg != null) {
            val override = theme.apps[pkg]
            if (override?.highlightColor != null) return override.highlightColor
        }
        return theme.global.highlightColor ?: defaultHex
    }

    protected fun resolveActionIcon(theme: HyperTheme?, pkg: String, actionTitle: String): Bitmap? {
        if (theme == null || repository == null) return null
        val override = theme.apps[pkg] ?: return null
        val actionsMap = override.actions ?: return null
        val matchedConfig = actionsMap.entries.find { (keyword, _) -> actionTitle.contains(keyword, ignoreCase = true) }?.value
        val resource = matchedConfig?.icon ?: return null
        return if (resource.type == ResourceType.LOCAL_FILE) repository.getResourceBitmap(resource) else null
    }

    protected fun resolveIcon(sbn: StatusBarNotification, picKey: String): HyperPicture {
        val originalBitmap = getNotificationBitmap(sbn) ?: createFallbackBitmap()
        return HyperPicture(picKey, originalBitmap)
    }

    /**
     * Applies Theme Shape, Color, and Padding to an Action Icon.
     */
    protected fun applyThemeToActionIcon(source: Bitmap, theme: HyperTheme, bgColor: Int): Bitmap {
        val shapeId = theme.global.iconShapeId
        val paddingPercent = theme.global.iconPaddingPercent

        val size = 96
        val output = createBitmap(size, size)
        val canvas = Canvas(output)

        // 1. Generate & Scale Path (The Shape)
        val polygon = getShapeFromId(shapeId)
        val path = polygon.toPath()
        val bounds = RectF()
        path.computeBounds(bounds, true)

        val matrix = Matrix()
        val destRect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        // ScaleToFit.FILL ensures the shape stretches to fill the 96x96 box exactly
        matrix.setRectToRect(bounds, destRect, Matrix.ScaleToFit.FILL)
        path.transform(matrix)

        // 2. Draw the Colored Background Shape
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, bgPaint)

        // 3. Draw the Icon (Centered & Scaled)
        val paddingPx = (size * (paddingPercent / 100f))

        // Define the area where the icon is allowed to be drawn
        val iconDestRect = RectF(
            paddingPx,
            paddingPx,
            size - paddingPx,
            size - paddingPx
        )

        if (iconDestRect.width() > 0 && iconDestRect.height() > 0) {
            // Configure paint to tint the icon white and clip it to the background shape
            val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            }

            // Calculate matrix to CENTER the icon inside the padding rect
            val iconMatrix = Matrix()
            val iconBounds = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
            iconMatrix.setRectToRect(iconBounds, iconDestRect, Matrix.ScaleToFit.CENTER)

            canvas.drawBitmap(source, iconMatrix, iconPaint)
        }

        return output
    }

    // --- CORE LOGIC ---

    protected fun extractBridgeActions(
        sbn: StatusBarNotification,
        theme: HyperTheme? = null,
        mode: ActionDisplayMode = ActionDisplayMode.BOTH,
        hideReplies: Boolean = true,
        useAppOpenForReplies: Boolean = false
    ): List<BridgeAction> {
        val bridgeActions = mutableListOf<BridgeAction>()
        val actions = sbn.notification.actions ?: return emptyList()

        // [FIX] Changed default fallback from Grey to standard Blue (#007AFF)
        val defaultActionBg = if (theme != null) {
            try {
                // If global highlight is missing, use Blue
                val hex = theme.global.highlightColor ?: "#007AFF"
                resolveColor(theme, sbn.packageName, hex).toColorInt()
            } catch (e: Exception) {
                "#007AFF".toColorInt() }
        } else {
            "#007AFF".toColorInt()
        }

        actions.forEachIndexed { index, androidAction ->
            val hasRemoteInput = androidAction.remoteInputs != null && androidAction.remoteInputs!!.isNotEmpty()
            if (hasRemoteInput && hideReplies) return@forEachIndexed

            val rawTitle = androidAction.title?.toString() ?: ""
            val uniqueKey = "act_${sbn.key.hashCode()}_$index"

            var actionIcon: Icon? = null
            var hyperPic: HyperPicture? = null

            val finalTitle = if (mode == ActionDisplayMode.ICON) "" else rawTitle
            val shouldLoadIcon = (mode == ActionDisplayMode.ICON) || (mode == ActionDisplayMode.BOTH) || (mode == ActionDisplayMode.TEXT && rawTitle.isEmpty())

            var bitmapToUse = resolveActionIcon(theme, sbn.packageName, rawTitle)

            if (bitmapToUse == null && shouldLoadIcon) {
                val originalIcon = androidAction.getIcon()
                if (originalIcon != null) {
                    bitmapToUse = loadIconBitmap(originalIcon, sbn.packageName)
                }
            }

            if (bitmapToUse != null) {
                val processedBitmap = if (theme != null) {
                    applyThemeToActionIcon(bitmapToUse, theme, defaultActionBg)
                } else {
                    createRoundedIconWithBackground(bitmapToUse, defaultActionBg, 12)
                }

                actionIcon = Icon.createWithBitmap(processedBitmap)
                hyperPic = HyperPicture("${uniqueKey}_icon", processedBitmap)
            }

            val finalIntent = if (hasRemoteInput && useAppOpenForReplies) {
                sbn.notification.contentIntent ?: androidAction.actionIntent
            } else {
                androidAction.actionIntent
            }

            val hyperAction = HyperAction(
                key = uniqueKey,
                title = finalTitle,
                icon = actionIcon,
                pendingIntent = finalIntent,
                actionIntentType = 1
            )

            bridgeActions.add(BridgeAction(hyperAction, hyperPic))
        }
        return bridgeActions
    }

    // --- UTILS ---

    protected fun getTransparentPicture(key: String): HyperPicture {
        val conf = Bitmap.Config.ARGB_8888
        val transparentBitmap = createBitmap(96, 96, conf)
        return HyperPicture(key, transparentBitmap)
    }

    protected fun getColoredPicture(key: String, resId: Int, colorHex: String): HyperPicture {
        val drawable = ContextCompat.getDrawable(context, resId)?.mutate()
        val color = try { colorHex.toColorInt() } catch (e: Exception) { Color.WHITE }
        drawable?.setTint(color)
        val bitmap = drawable?.toBitmap() ?: createFallbackBitmap()
        return HyperPicture(key, bitmap)
    }

    protected fun getPictureFromResource(key: String, resId: Int): HyperPicture {
        val drawable = ContextCompat.getDrawable(context, resId)
        val bitmap = drawable?.toBitmap() ?: createFallbackBitmap()
        return HyperPicture(key, bitmap)
    }

    protected fun getNotificationBitmap(sbn: StatusBarNotification): Bitmap? {
        val pkg = sbn.packageName
        val extras = sbn.notification.extras

        try {
            val picture = extras.getParcelableCompat<Bitmap>(Notification.EXTRA_PICTURE)
            if (picture != null) return picture

            if (sbn.notification.category == Notification.CATEGORY_CALL) {
                val person = extras.getParcelableCompat<Person>(Notification.EXTRA_MESSAGING_PERSON)
                    ?: extras.getParcelableArrayListCompat<Person>(Notification.EXTRA_PEOPLE_LIST)?.firstOrNull()

                if (person != null && person.icon != null) {
                    val bitmap = loadIconBitmap(person.icon!!, pkg)
                    if (bitmap != null) return bitmap
                }
            }

            val largeIcon = sbn.notification.getLargeIcon()
            if (largeIcon != null) {
                val bitmap = loadIconBitmap(largeIcon, pkg)
                if (bitmap != null) return bitmap
            }

            @Suppress("DEPRECATION")
            val largeIconBitmap = extras.getParcelableCompat<Bitmap>(Notification.EXTRA_LARGE_ICON)
            if (largeIconBitmap != null) return largeIconBitmap

            if (sbn.notification.smallIcon != null) {
                val bitmap = loadIconBitmap(sbn.notification.smallIcon, pkg)
                if (bitmap != null) return bitmap
            }

            return getAppIconBitmap(pkg)

        } catch (e: Exception) {
            Log.e("BaseTranslator", "Error extracting bitmap", e)
            return getAppIconBitmap(pkg)
        }
    }

    // Standard fallback implementation
    protected fun createRoundedIconWithBackground(source: Bitmap, backgroundColor: Int, paddingDp: Int = 8): Bitmap {
        val size = 96
        val output = createBitmap(size, size)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            color = backgroundColor
        }

        val center = size / 2f
        canvas.drawCircle(center, center, center, paint)

        val density = context.resources.displayMetrics.density
        val paddingPx = (paddingDp * density).toInt()

        val targetSize = size - (paddingPx * 2)
        if (targetSize > 0) {
            val whiteSource = tintBitmap(source, Color.WHITE)
            val destRect = Rect(paddingPx, paddingPx, size - paddingPx, size - paddingPx)
            val srcRect = Rect(0, 0, whiteSource.width, whiteSource.height)
            canvas.drawBitmap(whiteSource, srcRect, destRect, null)
        }

        return output
    }

    private fun tintBitmap(source: Bitmap, color: Int): Bitmap {
        val result = createBitmap(source.width, source.height)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            isFilterBitmap = true
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    protected fun loadIconBitmap(icon: Icon, packageName: String): Bitmap? {
        return try {
            val drawable = if (icon.type == Icon.TYPE_RESOURCE) {
                try {
                    val targetContext = context.createPackageContext(packageName, 0)
                    icon.loadDrawable(targetContext)
                } catch (e: Exception) {
                    icon.loadDrawable(context)
                }
            } else {
                icon.loadDrawable(context)
            }
            drawable?.toBitmap()
        } catch (e: Exception) {
            null
        }
    }

    private fun getAppIconBitmap(packageName: String): Bitmap? {
        return try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap()
        } catch (e: Exception) {
            null
        }
    }

    protected fun createFallbackBitmap(): Bitmap = createBitmap(1, 1)

    protected fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable && this.bitmap != null) return this.bitmap
        val width = if (intrinsicWidth > 0) intrinsicWidth else 96
        val height = if (intrinsicHeight > 0) intrinsicHeight else 96
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }
}