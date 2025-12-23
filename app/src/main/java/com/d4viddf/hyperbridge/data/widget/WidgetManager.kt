package com.d4viddf.hyperbridge.data.widget

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object WidgetManager {

    private const val HOST_ID = 1024
    private var appWidgetManager: AppWidgetManager? = null
    private var appWidgetHost: HyperAppWidgetHost? = null
    private var context: Context? = null

    private val _widgetUpdates = MutableSharedFlow<Int>(replay = 0, extraBufferCapacity = 10)
    val widgetUpdates: SharedFlow<Int> = _widgetUpdates.asSharedFlow()

    fun init(ctx: Context) {
        if (context != null) return
        context = ctx.applicationContext
        appWidgetManager = AppWidgetManager.getInstance(context)

        appWidgetHost = HyperAppWidgetHost(context!!, HOST_ID)
        appWidgetHost?.startListening()
    }

    // --- ID MANAGEMENT ---

    fun allocateId(ctx: Context): Int {
        init(ctx)
        return appWidgetHost?.allocateAppWidgetId() ?: -1
    }

    fun deleteId(ctx: Context, widgetId: Int) {
        init(ctx)
        appWidgetHost?.deleteAppWidgetId(widgetId)
    }

    // [FIX] Return Boolean so UI knows if binding succeeded or needs permission intent
    fun bindWidget(ctx: Context, widgetId: Int, provider: ComponentName): Boolean {
        init(ctx)
        return try {
            appWidgetManager?.bindAppWidgetIdIfAllowed(widgetId, provider) ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- UPDATE NOTIFICATION ---

    fun notifyWidgetUpdated(widgetId: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            _widgetUpdates.emit(widgetId)
        }
    }

    // --- CORE FUNCTIONS ---

    fun getWidgetInfo(ctx: Context, widgetId: Int): AppWidgetProviderInfo? {
        init(ctx)
        return appWidgetManager?.getAppWidgetInfo(widgetId)
    }

    fun getConfigurationActivity(ctx: Context, widgetId: Int): ComponentName? {
        init(ctx)
        return getWidgetInfo(ctx, widgetId)?.configure
    }

    fun createPreview(ctx: Context, widgetId: Int): AppWidgetHostView? {
        init(ctx)
        val info = getWidgetInfo(ctx, widgetId) ?: return null
        return appWidgetHost?.createView(ctx.applicationContext, widgetId, info)
    }

    fun getWidgetBitmap(ctx: Context, widgetId: Int, width: Int, height: Int): Bitmap? {
        init(ctx)
        val hostView = createPreview(ctx, widgetId) ?: return null
        val info = getWidgetInfo(ctx, widgetId) ?: return null
        hostView.setAppWidget(widgetId, info)

        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        hostView.measure(widthSpec, heightSpec)
        hostView.layout(0, 0, hostView.measuredWidth, hostView.measuredHeight)

        try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            hostView.draw(canvas)
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getLatestRemoteViews(widgetId: Int): RemoteViews? {
        return HyperAppWidgetHostView.cachedRemoteViews[widgetId]
    }
}