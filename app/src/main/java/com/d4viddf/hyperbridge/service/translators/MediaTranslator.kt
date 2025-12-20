package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.service.notification.StatusBarNotification
import androidx.palette.graphics.Palette
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.IslandConfig
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo
import androidx.core.graphics.createBitmap

class MediaTranslator(context: Context) : BaseTranslator(context) {

    fun translate(sbn: StatusBarNotification, picKey: String, config: IslandConfig): HyperIslandData {
        val extras = sbn.notification.extras

        // --- 1. Metadata ---
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "Unknown Title"
        val artist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "Unknown Artist"

        // Load Album Art
        val largeIcon = sbn.notification.getLargeIcon()
        var albumArt: Bitmap? = if (largeIcon != null) {
            loadIconBitmap(largeIcon, sbn.packageName)
        } else {
            try {
                extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON)
            } catch (e: Exception) {
                null
            }
        }

        if (albumArt == null) {
            albumArt = getAppIcon(sbn.packageName)
        }

        // --- 2. Material Color Extraction ---
        // Defaults
        var containerColorHex = "#2d2d2d"
        var onContainerTitleHex = "#FFFFFF"
        var onContainerBodyHex = "#E0E0E0" // Slightly dimmed

        if (albumArt != null) {
            val palette = Palette.from(albumArt).generate()

            // Material 3 style usually grabs the Dominant color for containers
            // Or Vibrant for high emphasis. Let's try Dominant for better readability.
            val swatch = palette.dominantSwatch ?: palette.vibrantSwatch ?: palette.mutedSwatch

            if (swatch != null) {
                // The Background Color
                containerColorHex = toHex(swatch.rgb)

                // The Palette library calculates these specifically to contrast with 'rgb'
                onContainerTitleHex = toHex(swatch.titleTextColor)
                onContainerBodyHex = toHex(swatch.bodyTextColor)
            }

            // Round the art for the avatar
            albumArt = getRoundedCornerBitmap(albumArt, 32f)
        }

        val builder = HyperIslandNotification.Builder(context, "bridge_${sbn.packageName}", title)

        val finalTimeout = config.timeout ?: 0
        builder.setEnableFloat(config.isFloat ?: false)
        builder.setTimeout(finalTimeout)
        builder.setShowNotification(config.isShowShade ?: true)

        // --- RESOURCES ---
        val artKey = "album_art"

        if (albumArt != null) {
            builder.addPicture(HyperPicture(artKey, albumArt))
            builder.addPicture(HyperPicture(picKey, albumArt))
        } else {
            builder.addPicture(resolveIcon(sbn, picKey))
            val appIcon = getAppIcon(sbn.packageName) ?: createFallbackBitmap()
            val roundedAppIcon = getRoundedCornerBitmap(appIcon, 32f)
            builder.addPicture(HyperPicture(artKey, roundedAppIcon))
        }

        builder.addPicture(getTransparentPicture("hidden_pixel"))

        // --- 3. Actions (Text Buttons with Material Colors) ---
        val actions = sbn.notification.actions ?: emptyArray()
        val actionKeys = mutableListOf<String>()

        actions.take(3).forEachIndexed { index, action ->
            val uniqueKey = "media_act_${sbn.key.hashCode()}_$index"

            val hyperAction = HyperAction(
                key = uniqueKey,
                title = action.title?.toString() ?: "",
                icon = null, // Text-only
                pendingIntent = action.actionIntent,
                actionIntentType = 1,
                actionBgColor = null,
                // Use the Title Color (On-Container) for buttons so they match the theme
                titleColor = onContainerTitleHex
            )

            builder.addAction(hyperAction)
            actionKeys.add(uniqueKey)
        }

        // --- 4. Layout Configuration ---

        // A. Background (Material Container Color)
        builder.setBackground(
            picKey = null,
            color = containerColorHex,
            type = 1
        )

        // B. Chat Info (Using Material On-Container Colors)
        builder.setChatInfo(
            title = title,
            content = artist,
            pictureKey = if (albumArt != null) artKey else picKey,
            appPkg = sbn.packageName,
            actionKeys = actionKeys,
            titleColor = onContainerTitleHex,    // High Emphasis
            contentColor = onContainerBodyHex    // Medium Emphasis
        )

        // --- Island (Collapsed) ---
        builder.setSmallIsland(if (albumArt != null) artKey else picKey)

        builder.setBigIslandInfo(
            left = ImageTextInfoLeft(
                type = 1,
                picInfo = PicInfo(type = 1, pic = if (albumArt != null) artKey else picKey),
                textInfo = TextInfo(title = "", content = "")
            )
        )
        builder.setIslandConfig(highlightColor = containerColorHex)
        builder.setHideDeco(true).setReopen(true).setShowSmallIcon(true)

        return HyperIslandData(builder.buildResourceBundle(), builder.buildJsonParam())
    }

    // --- HELPERS ---

    private fun toHex(color: Int): String {
        // Format to #RRGGBB (ignoring alpha for safety in some UI libs, or keep alpha if needed)
        // Using 0xFFFFFF mask ensures we get clean hex
        return String.format("#%06X", (0xFFFFFF and color))
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, pixels: Float): Bitmap {
        val output = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(output)

        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = RectF(rect)
        val roundPx = pixels

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    private fun getAppIcon(pkg: String): Bitmap? {
        return try {
            val drawable = context.packageManager.getApplicationIcon(pkg)
            drawableToBitmap(drawable)
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }
        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            createBitmap(1, 1)
        } else {
            createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
        }
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}