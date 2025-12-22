package com.d4viddf.hyperbridge.models

import androidx.annotation.StringRes
import com.d4viddf.hyperbridge.R

enum class WidgetSize(@StringRes val labelRes: Int, val layoutRes: Int) {
    ORIGINAL(R.string.size_original, -1),
    SMALL(R.string.size_small, R.layout.layout_island_widget_small),
    MEDIUM(R.string.size_medium, R.layout.layout_island_widget_medium),
    LARGE(R.string.size_large, R.layout.layout_island_widget_large),
    XLARGE(R.string.size_xlarge, R.layout.layout_island_widget_xlarge)
}