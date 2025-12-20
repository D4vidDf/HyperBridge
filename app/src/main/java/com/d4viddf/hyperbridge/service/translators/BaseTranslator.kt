package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.app.Person
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.models.BridgeAction
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperPicture
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap

abstract class BaseTranslator(protected val context: Context) {

    enum class ActionDisplayMode { TEXT, ICON, BOTH }

    protected fun getTransparentPicture(key: String): HyperPicture {
        val conf = Bitmap.Config.ARGB_8888
        val transparentBitmap = createBitmap(96, 96, conf)
        return HyperPicture(key, transparentBitmap)
    }

    protected fun getColoredPicture(key: String, resId: Int, colorHex: String): HyperPicture {
        val drawable = ContextCompat.getDrawable(context, resId)?.mutate()
        val color = colorHex.toColorInt()
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
            // 1. Try EXTRA_PICTURE (BigPictureStyle)
            val picture = extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE)
            if (picture != null) return picture

            // 2. Try CallStyle Person Icon
            // Call notifications often store the caller's face in the Person object
            if (sbn.notification.category == Notification.CATEGORY_CALL) {
                val person = extras.getParcelable<Person>(Notification.EXTRA_MESSAGING_PERSON)
                    ?: extras.getParcelableArrayList<Person>(Notification.EXTRA_PEOPLE_LIST)?.firstOrNull()

                if (person != null && person.icon != null) {
                    val bitmap = loadIconBitmap(person.icon!!, pkg)
                    if (bitmap != null) return bitmap
                }
            }

            // 3. Try Large Icon (Bitmap or Icon)
            val largeIcon = sbn.notification.getLargeIcon() // Returns Icon?
            if (largeIcon != null) {
                val bitmap = loadIconBitmap(largeIcon, pkg)
                if (bitmap != null) return bitmap
            }

            // Legacy: Check extras for raw bitmap
            val largeIconBitmap = extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON)
            if (largeIconBitmap != null) return largeIconBitmap

            // 4. Try Small Icon
            if (sbn.notification.smallIcon != null) {
                val bitmap = loadIconBitmap(sbn.notification.smallIcon, pkg)
                if (bitmap != null) return bitmap
            }

            // 5. Fallback to App Icon
            return getAppIconBitmap(pkg)

        } catch (e: Exception) {
            Log.e("BaseTranslator", "Error extracting bitmap", e)
            return getAppIconBitmap(pkg)
        }
    }

    protected fun resolveIcon(sbn: StatusBarNotification, picKey: String): HyperPicture {
        val bitmap = getNotificationBitmap(sbn)
        return if (bitmap != null) {
            HyperPicture(picKey, bitmap)
        } else {
            getPictureFromResource(picKey, R.drawable.ic_launcher_foreground)
        }
    }

    /**
     * Creates a circular icon with a specific background color.
     */
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
            colorFilter = android.graphics.PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    protected fun extractBridgeActions(
        sbn: StatusBarNotification,
        mode: ActionDisplayMode = ActionDisplayMode.BOTH
    ): List<BridgeAction> {
        val bridgeActions = mutableListOf<BridgeAction>()
        val actions = sbn.notification.actions ?: return emptyList()

        actions.forEachIndexed { index, androidAction ->
            val rawTitle = androidAction.title?.toString() ?: ""
            val uniqueKey = "act_${sbn.key.hashCode()}_$index"

            var actionIcon: Icon? = null
            var hyperPic: HyperPicture? = null

            val finalTitle = if (mode == ActionDisplayMode.ICON) "" else rawTitle
            val shouldLoadIcon = (mode == ActionDisplayMode.ICON) ||
                    (mode == ActionDisplayMode.BOTH) ||
                    (mode == ActionDisplayMode.TEXT && rawTitle.isEmpty())

            if (shouldLoadIcon) {
                val originalIcon = androidAction.getIcon()
                if (originalIcon != null) {
                    val bitmap = loadIconBitmap(originalIcon, sbn.packageName)
                    if (bitmap != null) {
                        actionIcon = Icon.createWithBitmap(bitmap)
                        hyperPic = HyperPicture("${uniqueKey}_icon", bitmap)
                    }
                }
            }

            val hyperAction = HyperAction(
                key = uniqueKey,
                title = finalTitle,
                icon = actionIcon,
                pendingIntent = androidAction.actionIntent,
                actionIntentType = 1
            )

            bridgeActions.add(BridgeAction(hyperAction, hyperPic))
        }
        return bridgeActions
    }

    protected fun loadIconBitmap(icon: Icon, packageName: String): Bitmap? {
        return try {
            val drawable = if (icon.type == Icon.TYPE_RESOURCE) {
                try {
                    val targetContext = context.createPackageContext(packageName, 0)
                    icon.loadDrawable(targetContext)
                } catch (e: PackageManager.NameNotFoundException) {
                    icon.loadDrawable(context)
                } catch (e: Resources.NotFoundException) {
                    null
                }
            } else {
                icon.loadDrawable(context)
            }
            drawable?.toBitmap()
        } catch (e: Exception) {
            Log.w("BaseTranslator", "Failed to load icon from $packageName", e)
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