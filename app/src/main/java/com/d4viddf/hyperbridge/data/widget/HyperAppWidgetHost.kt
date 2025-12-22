package com.d4viddf.hyperbridge.data.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.widget.RemoteViews

class HyperAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return HyperAppWidgetHostView(context)
    }
}

class HyperAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

    companion object {
        val cachedRemoteViews = mutableMapOf<Int, RemoteViews>()
    }

    override fun updateAppWidget(remoteViews: RemoteViews?) {
        super.updateAppWidget(remoteViews)

        if (remoteViews != null) {
            cachedRemoteViews[appWidgetId] = remoteViews
        }

        // [FIX] Calls the public method in WidgetManager
        WidgetManager.notifyWidgetUpdated(appWidgetId)
    }
}