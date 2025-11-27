package com.d4viddf.hyperbridge.models

import androidx.annotation.StringRes
import com.d4viddf.hyperbridge.R

enum class NotificationType(@StringRes val labelRes: Int) {
    STANDARD(R.string.type_standard),
    PROGRESS(R.string.type_progress),
    MEDIA(R.string.type_media),
    NAVIGATION(R.string.type_nav),
    CALL(R.string.type_call),
    TIMER(R.string.type_timer)
}