package com.d4viddf.hyperbridge.service.translators

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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

abstract class BaseTranslator(protected val context: Context) {

    // Options for how actions should be rendered
    enum class ActionDisplayMode {
        TEXT, // Prioritize Text (Hint Info). Icon only loaded if text missing.
        ICON, // Prioritize Icon. Text is cleared to show circular button.
        BOTH  // Show both Text and Icon (if supported by layout).
    }

    protected fun getTransparentPicture(key: String): HyperPicture {
        val conf = Bitmap.Config.ARGB_8888
        val transparentBitmap = Bitmap.createBitmap(96, 96, conf)
        return HyperPicture(key, transparentBitmap)
    }

    protected fun getColoredPicture(key: String, resId: Int, colorHex: String): HyperPicture {
        val drawable = ContextCompat.getDrawable(context, resId)?.mutate()
        val color = Color.parseColor(colorHex)
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
        var bitmap: Bitmap? = null
        val pkg = sbn.packageName
        val extras = sbn.notification.extras

        try {
            val picture = extras.getParcelable<Bitmap>(android.app.Notification.EXTRA_PICTURE)
            if (picture != null) return picture

            val largeIcon = sbn.notification.getLargeIcon()
            if (largeIcon != null) {
                bitmap = loadIconBitmap(largeIcon, pkg)
            }

            if (bitmap == null && sbn.notification.smallIcon != null) {
                bitmap = loadIconBitmap(sbn.notification.smallIcon, pkg)
            }

            if (bitmap == null) {
                bitmap = getAppIconBitmap(pkg)
            }
        } catch (e: Exception) {
            bitmap = getAppIconBitmap(pkg)
        }
        return bitmap
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
     * Extracts actions with configurable display mode.
     * @param mode Determines if we load icons, show text, or both.
     */
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

            // --- 1. Determine Title ---
            // If mode is ICON, we clear the text so it renders as a round button.
            // Otherwise, we keep the text (Hint Info).
            val finalTitle = if (mode == ActionDisplayMode.ICON) "" else rawTitle

            // --- 2. Determine Icon Loading ---
            // We load the icon if:
            // - Mode is ICON or BOTH
            // - Mode is TEXT but the text is empty (fallback to prevent invisible button)
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
            null
        }
    }

    private fun getAppIconBitmap(packageName: String): Bitmap? {
        return try {
            context.packageManager.getApplicationIcon(packageName).toBitmap()
        } catch (e: Exception) {
            null
        }
    }

    protected fun createFallbackBitmap(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    protected fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable && this.bitmap != null) return this.bitmap
        val width = if (intrinsicWidth > 0) intrinsicWidth else 96
        val height = if (intrinsicHeight > 0) intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }
}