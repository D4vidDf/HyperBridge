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
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import androidx.graphics.shapes.toPath
import androidx.palette.graphics.Palette
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.BridgeAction
import com.d4viddf.hyperbridge.models.theme.ActionButtonMode
import com.d4viddf.hyperbridge.models.theme.ActionConfig
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.theme.ResourceType
import com.d4viddf.hyperbridge.models.theme.ThemeResource
import com.d4viddf.hyperbridge.ui.screens.theme.getShapeFromId
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperPicture
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

abstract class BaseTranslator(
    protected val context: Context,
    protected val repository: ThemeRepository? = null
) {

    enum class ActionDisplayMode { TEXT, ICON, BOTH }

    private val appColorCache = ConcurrentHashMap<String, String>()

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

        // 1. App Specific Override (Highest Priority)
        if (pkg != null) {
            val override = theme.apps[pkg]
            val overrideColor = override?.highlightColor
            if (!overrideColor.isNullOrEmpty()) {
                return overrideColor
            }
        }

        // 2. Global "Use App Colors" -> Extract from Icon
        if (theme.global.useAppColors && pkg != null) {
            // Check cache first
            val cached = appColorCache[pkg]
            if (cached != null) return cached

            // Extract and Cache
            val extracted = extractColorFromAppIcon(pkg)
            if (extracted != null) {
                appColorCache[pkg] = extracted
                return extracted
            }
        }

        // 3. Global Theme Highlight -> Default Fallback
        return theme.global.highlightColor ?: defaultHex
    }

    private fun extractColorFromAppIcon(pkg: String): String? {
        return try {
            val drawable = context.packageManager.getApplicationIcon(pkg)
            // Use a scaled down bitmap for faster Palette generation
            val bitmap = drawable.toBitmap(width = 128, height = 128)

            // Clear filters to ensure we process the raw colors
            val palette = Palette.from(bitmap).clearFilters().generate()

            // Priority list for "brand" colors
            val swatches = listOf(
                palette.vibrantSwatch,
                palette.darkVibrantSwatch,
                palette.lightVibrantSwatch,
                palette.dominantSwatch,
                palette.mutedSwatch
            )

            // Find first valid swatch that is NOT Grayscale (White/Black/Grey)
            val bestSwatch = swatches.firstOrNull { it != null && !isGrayscale(it.rgb) }
                ?: palette.dominantSwatch // Fallback to dominant if everything is gray

            if (bestSwatch != null) {
                String.format("#%06X", (0xFFFFFF and bestSwatch.rgb))
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Helper to detect if a color is White, Black, or Gray
    private fun isGrayscale(color: Int): Boolean {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF

        // If RGB values are very close to each other, it's gray scale
        val diff = abs(r - g) + abs(g - b) + abs(b - r)
        return diff < 30
    }

    protected fun resolveActionConfig(theme: HyperTheme?, pkg: String, actionTitle: String): ActionConfig? {
        if (theme == null) return null

        val appOverride = theme.apps[pkg]?.actions?.entries?.find { (keyword, _) ->
            actionTitle.contains(keyword, ignoreCase = true)
        }?.value

        if (appOverride != null) return appOverride

        return theme.defaultActions.entries.find { (keyword, _) ->
            actionTitle.contains(keyword, ignoreCase = true)
        }?.value
    }

    protected fun resolveActionIcon(theme: HyperTheme?, pkg: String, actionTitle: String): Bitmap? {
        if (repository == null) return null
        val config = resolveActionConfig(theme, pkg, actionTitle) ?: return null
        val resource = config.icon ?: return null
        return if (resource.type == ResourceType.LOCAL_FILE) repository.getResourceBitmap(resource) else null
    }

    protected fun resolveIcon(sbn: StatusBarNotification, picKey: String): HyperPicture {
        val originalBitmap = getNotificationBitmap(sbn) ?: createFallbackBitmap()
        return HyperPicture(picKey, originalBitmap)
    }

    protected fun applyThemeToActionIcon(source: Bitmap, shapeId: String, paddingPercent: Int, bgColor: Int): Bitmap {
        val size = 96
        val output = createBitmap(size, size)
        val canvas = Canvas(output)

        val polygon = getShapeFromId(shapeId)
        val path = polygon.toPath()
        val bounds = RectF()
        path.computeBounds(bounds, true)

        val matrix = Matrix()
        val destRect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        matrix.setRectToRect(bounds, destRect, Matrix.ScaleToFit.FILL)
        path.transform(matrix)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, bgPaint)

        val paddingPx = (size * (paddingPercent / 100f))
        val iconDestRect = RectF(paddingPx, paddingPx, size - paddingPx, size - paddingPx)

        if (iconDestRect.width() > 0 && iconDestRect.height() > 0) {
            val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            }
            val iconMatrix = Matrix()
            val iconBounds = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())
            iconMatrix.setRectToRect(iconBounds, iconDestRect, Matrix.ScaleToFit.CENTER)
            canvas.drawBitmap(source, iconMatrix, iconPaint)
        }

        return output
    }

    protected fun applyThemeToActionIcon(source: Bitmap, theme: HyperTheme, bgColor: Int): Bitmap {
        return applyThemeToActionIcon(source, theme.global.iconShapeId, theme.global.iconPaddingPercent, bgColor)
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

        val defaultActionBg = if (theme != null) {
            try {
                val hex = theme.global.highlightColor ?: "#007AFF"
                resolveColor(theme, sbn.packageName, hex).toColorInt()
            } catch (e: Exception) { "#007AFF".toColorInt() }
        } else {
            "#007AFF".toColorInt()
        }

        actions.forEachIndexed { index, androidAction ->
            val hasRemoteInput = androidAction.remoteInputs != null && androidAction.remoteInputs!!.isNotEmpty()
            if (hasRemoteInput && hideReplies) return@forEachIndexed

            val rawTitle = androidAction.title?.toString() ?: ""
            val uniqueKey = "act_${sbn.key.hashCode()}_$index"

            val config = resolveActionConfig(theme, sbn.packageName, rawTitle)

            val finalBgColorInt = if (config?.backgroundColor != null) {
                try {
                    config.backgroundColor.toColorInt()
                } catch(e: Exception) { defaultActionBg }
            } else {
                defaultActionBg
            }

            val finalBgColorHex = String.format("#%08X", (0xFFFFFFFF and finalBgColorInt.toLong()))
            val finalTintColorHex = config?.tintColor ?: "#FFFFFF"

            val effectiveMode = when (config?.mode) {
                ActionButtonMode.ICON -> ActionDisplayMode.ICON
                ActionButtonMode.TEXT -> ActionDisplayMode.TEXT
                ActionButtonMode.BOTH -> ActionDisplayMode.BOTH
                null -> mode
            }

            var actionIcon: Icon? = null
            var hyperPic: HyperPicture? = null

            val finalTitle = if (effectiveMode == ActionDisplayMode.ICON) "" else rawTitle
            val shouldLoadIcon = (effectiveMode != ActionDisplayMode.TEXT)

            var bitmapToUse: Bitmap? = null
            val configIconRes = config?.icon
            if (configIconRes != null && configIconRes.type == ResourceType.LOCAL_FILE && repository != null) {
                bitmapToUse = repository.getResourceBitmap(configIconRes)
            }

            if (bitmapToUse == null && shouldLoadIcon) {
                val originalIcon = androidAction.getIcon()
                if (originalIcon != null) {
                    bitmapToUse = loadIconBitmap(originalIcon, sbn.packageName)
                }
            }

            if (bitmapToUse != null) {
                val processedBitmap = if (theme != null) {
                    applyThemeToActionIcon(bitmapToUse, theme, finalBgColorInt)
                } else {
                    createRoundedIconWithBackground(bitmapToUse, finalBgColorInt, 12)
                }

                actionIcon = Icon.createWithBitmap(processedBitmap)
                hyperPic = HyperPicture("${uniqueKey}_icon", processedBitmap)
            }

            val finalIntent = if (hasRemoteInput && useAppOpenForReplies) {
                sbn.notification.contentIntent ?: androidAction.actionIntent
            } else {
                androidAction.actionIntent
            }

            // [FIX] If mode is TEXT, strip the background color so it doesn't look like a solid block
            val appliedBgColor = if (effectiveMode == ActionDisplayMode.TEXT) null else finalBgColorHex

            val hyperAction = HyperAction(
                key = uniqueKey,
                title = finalTitle,
                icon = actionIcon,
                pendingIntent = finalIntent,
                actionIntentType = 1,
                actionBgColor = appliedBgColor, // Use conditional color
                titleColor = finalTintColorHex
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

    protected fun Drawable.toBitmap(width: Int? = null, height: Int? = null): Bitmap {
        if (this is BitmapDrawable && this.bitmap != null) {
            if (width != null && height != null) {
                return this.bitmap.scale(width, height)
            }
            return this.bitmap
        }

        val w = width ?: if (intrinsicWidth > 0) intrinsicWidth else 96
        val h = height ?: if (intrinsicHeight > 0) intrinsicHeight else 96

        val bitmap = createBitmap(w, h)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }
}