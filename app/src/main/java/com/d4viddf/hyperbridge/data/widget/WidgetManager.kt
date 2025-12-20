package com.d4viddf.hyperbridge.data.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews

object WidgetManager {
    // Arbitrary Host ID
    private const val HOST_ID = 1024

    private var appWidgetHost: HyperWidgetHost? = null
    private var appWidgetManager: AppWidgetManager? = null

    // Cache for capturing the RemoteViews
    private val remoteViewsCache = mutableMapOf<Int, RemoteViews>()

    fun init(context: Context) {
        if (appWidgetManager == null) {
            appWidgetManager = AppWidgetManager.getInstance(context)
        }
        if (appWidgetHost == null) {
            appWidgetHost = HyperWidgetHost(context.applicationContext, HOST_ID)
            // Essential: Start listening to receive updates
            appWidgetHost?.startListening()
        }
    }

    fun allocateId(context: Context): Int {
        init(context)
        return try {
            appWidgetHost?.allocateAppWidgetId() ?: -1
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * Attempts to bind.
     * Returns TRUE if already bound/allowed.
     * Returns FALSE if we need to ask permission (launch Intent).
     */
    fun bindWidget(context: Context, appWidgetId: Int, provider: ComponentName): Boolean {
        init(context)
        return try {
            // For 3rd party apps, this usually returns FALSE initially
            // unless the user granted "Always allow" previously.
            appWidgetManager?.bindAppWidgetIdIfAllowed(appWidgetId, provider) == true
        } catch (e: Exception) {
            false
        }
    }

    fun createPreview(context: Context, widgetId: Int): AppWidgetHostView? {
        init(context)
        val info = appWidgetManager?.getAppWidgetInfo(widgetId) ?: return null
        return appWidgetHost?.createView(context, widgetId, info)
    }

    fun getLatestRemoteViews(widgetId: Int): RemoteViews? {
        return remoteViewsCache[widgetId]
    }

    // --- INTERNAL CLASSES ---

    private class HyperWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
        override fun onCreateView(
            context: Context,
            appWidgetId: Int,
            appWidget: AppWidgetProviderInfo?
        ): AppWidgetHostView {
            return InterceptorHostView(context)
        }
    }

    private class InterceptorHostView(context: Context) : AppWidgetHostView(context) {
        override fun updateAppWidget(remoteViews: RemoteViews?) {
            super.updateAppWidget(remoteViews)
            if (remoteViews != null) {
                // CAPTURE THE VIEW
                remoteViewsCache[appWidgetId] = remoteViews
            }
        }
    }

    fun getWidgetInfo(context: Context, widgetId: Int): AppWidgetProviderInfo? {
        init(context)
        return appWidgetManager?.getAppWidgetInfo(widgetId)
    }
}