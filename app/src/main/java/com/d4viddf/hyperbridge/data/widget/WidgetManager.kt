package com.d4viddf.hyperbridge.data.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object WidgetManager {
    private const val HOST_ID = 1024

    private var appWidgetHost: HyperAppWidgetHost? = null
    private var appWidgetManager: AppWidgetManager? = null
    private val remoteViewsCache = mutableMapOf<Int, RemoteViews>()
    private val _widgetUpdates = MutableSharedFlow<Int>(replay = 0)
    val widgetUpdates: SharedFlow<Int> = _widgetUpdates.asSharedFlow()

    fun init(context: Context) {
        if (appWidgetManager == null) {
            appWidgetManager = AppWidgetManager.getInstance(context)
        }
        if (appWidgetHost == null) {
            appWidgetHost = HyperAppWidgetHost(context.applicationContext, HOST_ID) { widgetId ->
                _widgetUpdates.tryEmit(widgetId)
            }
            appWidgetHost?.startListening()
        }
    }

    // ... allocateId, bindWidget, getWidgetInfo (Keep existing) ...

    fun allocateId(context: Context): Int {
        init(context)
        return try {
            appWidgetHost?.allocateAppWidgetId() ?: -1
        } catch (e: Exception) { -1 }
    }

    fun bindWidget(context: Context, appWidgetId: Int, provider: ComponentName): Boolean {
        init(context)
        return try {
            appWidgetManager?.bindAppWidgetIdIfAllowed(appWidgetId, provider) == true
        } catch (e: Exception) { false }
    }

    fun getWidgetInfo(context: Context, widgetId: Int): AppWidgetProviderInfo? {
        init(context)
        return appWidgetManager?.getAppWidgetInfo(widgetId)
    }

    // --- NEW: Check for Configuration Activity ---
    fun getConfigurationActivity(context: Context, widgetId: Int): ComponentName? {
        val info = getWidgetInfo(context, widgetId)
        return info?.configure
    }

    // --- NEW: Clean up if config cancelled ---
    fun deleteId(context: Context, widgetId: Int) {
        init(context)
        appWidgetHost?.deleteAppWidgetId(widgetId)
    }

    fun createPreview(context: Context, widgetId: Int): AppWidgetHostView? {
        init(context)
        val info = appWidgetManager?.getAppWidgetInfo(widgetId) ?: return null
        val view = appWidgetHost?.createView(context, widgetId, info)
        view?.setPadding(0, 0, 0, 0)
        return view
    }

    fun notifyWidgetUpdated(widgetId: Int, views: RemoteViews?) {
        if (views != null) {
            remoteViewsCache[widgetId] = views
            _widgetUpdates.tryEmit(widgetId)
        }
    }

    fun getLatestRemoteViews(widgetId: Int): RemoteViews? = remoteViewsCache[widgetId]


    /**
     * Renders the current state of a widget into a Bitmap.
     * This solves empty lists by capturing the actual view content.
     */
    fun getWidgetBitmap(context: Context, widgetId: Int, width: Int, height: Int): android.graphics.Bitmap? {
        init(context)

        // 1. Create the host view locally (which has permission to load list data)
        val hostView = createPreview(context, widgetId) ?: return null
        val info = getWidgetInfo(context, widgetId) ?: return null
        hostView.setAppWidget(widgetId, info)

        // 2. Measure and Layout the view explicitly to the target size
        // We use specific specs to force the list to populate its items
        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)

        hostView.measure(widthSpec, heightSpec)
        hostView.layout(0, 0, hostView.measuredWidth, hostView.measuredHeight)

        // 3. Create Bitmap and Draw
        // Config.ARGB_8888 is standard quality
        val bitmap = createBitmap(width, height)
        val canvas = android.graphics.Canvas(bitmap)
        hostView.draw(canvas)

        return bitmap
    }
}