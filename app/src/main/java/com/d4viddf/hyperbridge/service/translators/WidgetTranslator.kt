package com.d4viddf.hyperbridge.service.translators

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.widget.RemoteViews
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.models.HyperIslandData
import com.d4viddf.hyperbridge.models.WidgetRenderMode
import com.d4viddf.hyperbridge.models.WidgetSize
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class WidgetTranslator(context: Context) : BaseTranslator(context) {

    private val preferences = AppPreferences(context)

    suspend fun translate(widgetId: Int): HyperIslandData {
        val config = preferences.getWidgetConfigFlow(widgetId).first()

        val targetSize = config.size
        val renderMode = config.renderMode

        // 1. Get Widget Provider Info
        val widgetInfo = WidgetManager.getWidgetInfo(context, widgetId)
        val packageName = widgetInfo?.provider?.packageName
        val label = if (widgetInfo != null) widgetInfo.loadLabel(context.packageManager) else "Widget"

        val title = label.toString()
        val builder = HyperIslandNotification.Builder(context, "widget_channel", title)

        // 2. Load the App Icon
        val iconKey = "widget_icon_$widgetId"
        var iconBitmap: Bitmap? = null

        if (packageName != null) {
            try {
                iconBitmap = context.packageManager.getApplicationIcon(packageName).toBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // --- View Rendering Logic ---
        if (renderMode == WidgetRenderMode.SNAPSHOT) {
            val density = context.resources.displayMetrics.density
            val widthPx = (350 * density).toInt()

            val heightDp = when(targetSize) {
                WidgetSize.SMALL -> 100
                WidgetSize.MEDIUM -> 180
                WidgetSize.LARGE -> 280
                WidgetSize.XLARGE -> 380
                WidgetSize.ORIGINAL -> 180
            }
            val heightPx = (heightDp * density).toInt()

            val bitmap: Bitmap? = withContext(Dispatchers.Main) {
                try {
                    WidgetManager.getWidgetBitmap(context, widgetId, widthPx, heightPx)
                } catch (_: Exception) { null }
            }

            if (bitmap != null) {
                val snapshotView = RemoteViews(context.packageName, R.layout.layout_island_widget_snapshot)
                snapshotView.setImageViewBitmap(R.id.widget_snapshot_view, bitmap)
                builder.setCustomRemoteView(snapshotView)
            }

        } else {
            val remoteViews = WidgetManager.getLatestRemoteViews(widgetId)

            if (remoteViews != null) {
                if (targetSize == WidgetSize.ORIGINAL) {
                    builder.setCustomRemoteView(remoteViews)
                } else {
                    val wrapper = RemoteViews(context.packageName, targetSize.layoutRes)
                    wrapper.removeAllViews(R.id.widget_insertion_point)
                    wrapper.addView(R.id.widget_insertion_point, remoteViews)
                    builder.setCustomRemoteView(wrapper)
                }
            }
        }

        // [FIX] 3. Apply the Icon to the Island
        if (iconBitmap != null) {
            val icon = Icon.createWithBitmap(iconBitmap)

            // Fix: Create the HyperPicture object first
            val picture = HyperPicture(iconKey, icon)
            builder.addPicture(picture)

            builder.setBigIslandInfo(left = ImageTextInfoLeft(picInfo = PicInfo(pic = iconKey)))
            builder.setSmallIsland(iconKey)
        } else {
            builder.setBigIslandInfo(left = ImageTextInfoLeft(picInfo = PicInfo(pic = "default_icon")))
            builder.setSmallIsland("default_icon")
            builder.addPicture(getTransparentPicture("default_icon"))
        }

        builder.setIslandConfig(timeout = config.timeout , dismissible = false)
        builder.setEnableFloat(false)
        builder.setShowNotification(config.isShowShade)
        builder.setIslandFirstFloat(false)
        builder.setReopen(true)
        builder.setHideDeco(true)

        return HyperIslandData(builder.buildCustomExtras(), builder.buildJsonParam())
    }
}