package com.d4viddf.hyperbridge.ui.screens.design

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Represents one App (e.g., "Spotify") containing multiple widgets
data class WidgetAppGroup(
    val packageName: String,
    val appName: String,
    val appIcon: Drawable?,
    val widgets: List<AppWidgetProviderInfo>
)

class WidgetPickerViewModel : ViewModel() {

    private val _widgetGroups = MutableStateFlow<List<WidgetAppGroup>>(emptyList())
    val widgetGroups = _widgetGroups.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    fun loadWidgets(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            val groups = withContext(Dispatchers.IO) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val packageManager = context.packageManager

                // 1. Get all providers
                val providers = appWidgetManager.getInstalledProviders()

                // 2. Group by Package Name
                val grouped = providers.groupBy { it.provider.packageName }

                // 3. Map to UI Model
                grouped.mapNotNull { (packageName, providerList) ->
                    try {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        val appIcon = packageManager.getApplicationIcon(appInfo)

                        WidgetAppGroup(
                            packageName = packageName,
                            appName = appName,
                            appIcon = appIcon,
                            widgets = providerList
                        )
                    } catch (e: Exception) {
                        null // Skip apps that can't be resolved
                    }
                }.sortedBy { it.appName }
            }
            _widgetGroups.value = groups
            _isLoading.value = false
        }
    }
}