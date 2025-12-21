package com.d4viddf.hyperbridge.models

import com.d4viddf.hyperbridge.R

enum class WidgetSize(val label: String, val layoutRes: Int) {
    ORIGINAL("Original", -1), // No wrapper
    SMALL("Small (100dp)", R.layout.layout_island_widget_small),
    MEDIUM("Medium (180dp)", R.layout.layout_island_widget_medium),
    LARGE("Large (280dp)", R.layout.layout_island_widget_large),
    XLARGE("Extra Large (380dp)", R.layout.layout_island_widget_xlarge)
}