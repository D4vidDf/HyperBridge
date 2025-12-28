package com.d4viddf.hyperbridge.service.translators

import android.app.Notification
import android.app.Person
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
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
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.theme.ThemeRepository
import com.d4viddf.hyperbridge.models.BridgeAction
import com.d4viddf.hyperbridge.models.theme.HyperTheme
import com.d4viddf.hyperbridge.models.theme.ResourceType
import com.d4viddf.hyperbridge.models.theme.ThemeResource
import io.github.d4viddf.hyperisland_kit.HyperAction
import io.github.d4viddf.hyperisland_kit.HyperPicture

abstract class BaseTranslator(
    protected val context: Context,
    protected val repository: ThemeRepository? = null
) {

    enum class ActionDisplayMode { TEXT, ICON, BOTH }

    // --- HELPER: Fix Deprecation Warnings ---
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

    /**
     * Resolves highlight color: App Override -> Global -> Default
     */
    protected fun resolveColor(theme: HyperTheme?, pkg: String?, defaultHex: String): String {
        if (theme == null) return defaultHex

        if (pkg != null) {
            val override = theme.apps[pkg]
            if (override?.highlightColor != null) {
                return override.highlightColor
            }
        }

        return theme.global.highlightColor ?: defaultHex
    }

    /**
     * Checks if the app has a specific icon for an action keyword.
     */
    protected fun resolveActionIcon(
        theme: HyperTheme?,
        pkg: String,
        actionTitle: String
    ): Bitmap? {
        if (theme == null || repository == null) return null

        val override = theme.apps[pkg] ?: return null
        val actionsMap = override.actions ?: return null

        // Case-insensitive matching for keywords (e.g. "Reply" matches "Reply")
        val matchedConfig = actionsMap.entries.find { (keyword, _) ->
            actionTitle.contains(keyword, ignoreCase = true)
        }?.value

        val resource = matchedConfig?.icon ?: return null

        if (resource.type == ResourceType.LOCAL_FILE) {
            return repository.getResourceBitmap(resource)
        }

        return null
    }

    // --- CORE LOGIC ---

    protected fun extractBridgeActions(
        sbn: StatusBarNotification,
        theme: HyperTheme? = null, // [UPDATED] Added theme parameter
        mode: ActionDisplayMode = ActionDisplayMode.BOTH,
        hideReplies: Boolean = true,
        useAppOpenForReplies: Boolean = false
    ): List<BridgeAction> {
        val bridgeActions = mutableListOf<BridgeAction>()
        val actions = sbn.notification.actions ?: return emptyList()

        actions.forEachIndexed { index, androidAction ->
            val hasRemoteInput = androidAction.remoteInputs != null && androidAction.remoteInputs!!.isNotEmpty()

            if (hasRemoteInput && hideReplies) return@forEachIndexed

            val rawTitle = androidAction.title?.toString() ?: ""
            val uniqueKey = "act_${sbn.key.hashCode()}_$index"

            var actionIcon: Icon? = null
            var hyperPic: HyperPicture? = null

            val finalTitle = if (mode == ActionDisplayMode.ICON) "" else rawTitle
            val shouldLoadIcon = (mode == ActionDisplayMode.ICON) || (mode == ActionDisplayMode.BOTH) || (mode == ActionDisplayMode.TEXT && rawTitle.isEmpty())

            // [NEW] Check for Theme Override First
            val customBitmap = resolveActionIcon(theme, sbn.packageName, rawTitle)

            if (customBitmap != null) {
                actionIcon = Icon.createWithBitmap(customBitmap)
                hyperPic = HyperPicture("${uniqueKey}_icon", customBitmap)
            } else if (shouldLoadIcon) {
                // Fallback to System Icon
                val originalIcon = androidAction.getIcon()
                if (originalIcon != null) {
                    val bitmap = loadIconBitmap(originalIcon, sbn.packageName)
                    if (bitmap != null) {
                        actionIcon = Icon.createWithBitmap(bitmap)
                        hyperPic = HyperPicture("${uniqueKey}_icon", bitmap)
                    }
                }
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

    protected fun resolveIcon(sbn: StatusBarNotification, picKey: String): HyperPicture {
        val bitmap = getNotificationBitmap(sbn)
        return if (bitmap != null) {
            HyperPicture(picKey, bitmap)
        } else {
            getPictureFromResource(picKey, R.drawable.ic_launcher_foreground)
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
            colorFilter = android.graphics.PorterDuffColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
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