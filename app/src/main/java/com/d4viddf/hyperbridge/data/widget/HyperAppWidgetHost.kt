package com.d4viddf.hyperbridge.data.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.widget.RemoteViews

class HyperAppWidgetHost(
    context: Context,
    hostId: Int,
    private val interactionListener: ((Int) -> Unit)? = null
) : AppWidgetHost(context, hostId) {

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return HyperAppWidgetHostView(context, interactionListener).apply {
            setAppWidget(appWidgetId, appWidget)
        }
    }

    class HyperAppWidgetHostView(
        context: Context,
        private val listener: ((Int) -> Unit)?
    ) : AppWidgetHostView(context) {

        // Catch click interactions if needed
        override fun performClick(): Boolean {
            listener?.invoke(appWidgetId)
            return super.performClick()
        }

        // CRITICAL: This is called when the widget provider sends a new layout
        override fun updateAppWidget(remoteViews: RemoteViews?) {
            super.updateAppWidget(remoteViews)
            // Save this RemoteViews so the NotificationService can use it
            WidgetManager.notifyWidgetUpdated(appWidgetId, remoteViews)
        }
    }
}