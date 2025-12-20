package com.d4viddf.hyperbridge.service.translators

import android.content.Context
import android.widget.RemoteViews
import com.d4viddf.hyperbridge.data.AppPreferences
import com.d4viddf.hyperbridge.data.widget.WidgetManager
import com.d4viddf.hyperbridge.models.HyperIslandData
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import kotlinx.coroutines.flow.first

class WidgetTranslator(context: Context) : BaseTranslator(context) {

    private val preferences = AppPreferences(context)

    // Made suspend to read preferences cleanly
    suspend fun translate(widgetId: Int): HyperIslandData {

        // 1. Fetch the captured RemoteViews (Cached by the Preview)
        val remoteViews: RemoteViews? = WidgetManager.getLatestRemoteViews(widgetId)

        // 2. Fetch User Configuration
        val config = preferences.savedWidgetConfigFlow.first()

        val title = if (remoteViews != null) "Widget Active" else "Loading Widget..."
        val builder = HyperIslandNotification.Builder(context, "widget_channel", title)

        // 3. Set Custom RemoteViews (Direct Pass)
        if (remoteViews != null) {
            builder.setCustomRemoteView(remoteViews)
        }

        // 4. Standard Configuration
        builder.setBigIslandInfo(
            left = ImageTextInfoLeft(
                picInfo = PicInfo(
                    pic = "default_icon"
                )
            )
        )
        builder.setSmallIsland("default_icon")
        builder.addPicture(getTransparentPicture("default_icon"))

        builder.setEnableFloat(true)

        // 5. Apply User Settings
        builder.setTimeout(config.timeout ?: 5000L)
        builder.setShowNotification(config.isShowShade ?: true)

        return HyperIslandData(builder.buildCustomExtras(), builder.buildJsonParam())
    }
}