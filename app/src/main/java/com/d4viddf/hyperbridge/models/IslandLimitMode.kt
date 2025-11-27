package com.d4viddf.hyperbridge.models

import androidx.annotation.StringRes
import com.d4viddf.hyperbridge.R

enum class IslandLimitMode(@StringRes val titleRes: Int, @StringRes val descRes: Int) {
    FIRST_COME(R.string.mode_fcfs_title, R.string.mode_fcfs_desc),
    MOST_RECENT(R.string.mode_recent_title, R.string.mode_recent_desc),
    PRIORITY(R.string.mode_priority_title, R.string.mode_priority_desc)
}