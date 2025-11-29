package com.d4viddf.hyperbridge.service.translators

import android.content.Context
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
import androidx.core.graphics.createBitmap

abstract class BaseTranslator(protected val context: Context) {

    protected fun getTransparentPicture(key: String): HyperPicture {
        val conf = Bitmap.Config.ARGB_8888
        val transparentBitmap = Bitmap.createBitmap(48, 48, conf)
        return HyperPicture(key, transparentBitmap)
    }

    protected fun getColoredPicture(key: String, resId: Int, colorHex: String): HyperPicture {
        val drawable = ContextCompat.getDrawable(context, resId)?.mutate()
        val color = Color.parseColor(colorHex)
        drawable?.setTint(color)
        val bitmap = drawable?.toBitmap() ?: createFallbackBitmap()
        return HyperPicture(key, bitmap)
    }

    // Helper to create HyperPicture from Resource ID safely
    protected fun getPictureFromResource(key: String, resId: Int): HyperPicture {
        val drawable = ContextCompat.getDrawable(context, resId)
        val bitmap = drawable?.toBitmap() ?: createFallbackBitmap()
        return HyperPicture(key, bitmap)
    }

    // This was missing in your file, causing the error
    protected fun getNotificationBitmap(sbn: StatusBarNotification): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            val largeIcon = sbn.notification.getLargeIcon()
            if (largeIcon != null) bitmap = loadIconBitmap(largeIcon)
            if (bitmap == null && sbn.notification.smallIcon != null) bitmap = loadIconBitmap(sbn.notification.smallIcon)
            if (bitmap == null) bitmap = getAppIconBitmap(sbn.packageName)
        } catch (e: Exception) { Log.e("HyperBridge", "Icon error", e) }
        return bitmap
    }

    protected fun resolveIcon(sbn: StatusBarNotification, picKey: String): HyperPicture {
        val bitmap = getNotificationBitmap(sbn)
        return if (bitmap != null) HyperPicture(picKey, bitmap)
        else getPictureFromResource(picKey, R.drawable.ic_launcher_foreground)
    }

    protected fun extractBridgeActions(sbn: StatusBarNotification): List<BridgeAction> {
        val bridgeActions = mutableListOf<BridgeAction>()
        val actions = sbn.notification.actions ?: return emptyList()

        actions.forEachIndexed { index, androidAction ->
            if (!androidAction.title.isNullOrEmpty()) {
                val uniqueKey = "act_${sbn.key.hashCode()}_$index"
                var actionIcon: Icon? = null
                var hyperPic: HyperPicture? = null

                val originalIcon = androidAction.getIcon()
                if (originalIcon != null) {
                    val bitmap = loadIconBitmap(originalIcon)
                    if (bitmap != null) {
                        actionIcon = Icon.createWithBitmap(bitmap)
                        hyperPic = HyperPicture("${uniqueKey}_icon", bitmap)
                    }
                }

                val hyperAction = HyperAction(
                    key = uniqueKey,
                    title = androidAction.title.toString(),
                    icon = actionIcon,
                    pendingIntent = androidAction.actionIntent,
                    actionIntentType = 1
                )

                bridgeActions.add(BridgeAction(hyperAction, hyperPic))
            }
        }
        return bridgeActions
    }

    private fun loadIconBitmap(icon: Icon): Bitmap? = try { icon.loadDrawable(context)?.toBitmap() } catch (e: Exception) { null }
    private fun getAppIconBitmap(packageName: String): Bitmap? = try { context.packageManager.getApplicationIcon(packageName).toBitmap() } catch (e: Exception) { null }
    private fun createFallbackBitmap(): Bitmap = createBitmap(1, 1)

    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable && this.bitmap != null) return this.bitmap
        val width = if (intrinsicWidth > 0) intrinsicWidth else 1
        val height = if (intrinsicHeight > 0) intrinsicHeight else 1
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }
}