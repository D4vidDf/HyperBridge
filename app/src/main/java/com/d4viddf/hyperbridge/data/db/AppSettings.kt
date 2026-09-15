package com.d4viddf.hyperbridge.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)

object SettingsKeys {
    // Migration Flag
    const val MIGRATION_COMPLETE = "migration_to_room_complete"

    // Core Keys
    const val SETUP_COMPLETE = "setup_complete"
    const val LAST_VERSION = "last_version_code"
    const val PRIORITY_EDU = "priority_edu_shown"
    const val ALLOWED_PACKAGES = "allowed_packages"
    const val PRIORITY_ORDER = "priority_app_order"
    const val FEATURED_PERMISSION_WARNING = "featured_permission_warning"
    const val FLOATING_SETUP_NOTICE_PENDING = "floating_setup_notice_pending"
    const val FLOATING_SETUP_CONFIRMED_PACKAGES = "floating_setup_confirmed_packages"
    const val BRIDGE_ALL_APPS_ENABLED = "bridge_all_apps_enabled"
    const val AUTO_ADD_NEW_APPS = "auto_add_new_apps"

    // Global Configs
    const val GLOBAL_FLOAT = "global_float"
    const val GLOBAL_SHADE = "global_shade"
    const val GLOBAL_TIMEOUT = "global_timeout"
    const val GLOBAL_FLOAT_TIMEOUT = "global_float_timeout"
    const val GLOBAL_REMOVE_NOTIF = "global_remove_original_notif"
    const val GLOBAL_DISMISS_WITH_ORIGINAL = "global_dismiss_with_original"
    const val GLOBAL_ENABLE_INLINE_REPLY = "global_enable_inline_reply"
    const val GLOBAL_BLOCKED_TERMS = "global_blocked_terms"

    // Nav
    const val NAV_LEFT = "nav_left_content"
    const val NAV_RIGHT = "nav_right_content"

    // Smart Actions (issue #270) — off by default
    const val SMART_ACTIONS_ENABLED = "smart_actions_enabled"
    const val SMART_ACTIONS_OTP = "smart_actions_otp"
    const val SMART_ACTIONS_URL = "smart_actions_url"
    const val SMART_ACTIONS_PHONE = "smart_actions_phone"
    const val SMART_ACTIONS_TRACKING = "smart_actions_tracking"
    const val SMART_ACTIONS_NAVIGATION = "smart_actions_navigation"
    const val SMART_ACTIONS_EXCLUDED_PACKAGES = "smart_actions_excluded_packages"
    const val SMART_ACTIONS_HIDE_OTP = "smart_actions_hide_otp"

    // System Island
    const val SCREEN_RECORDING_TIMEOUT = "screen_recording_timeout"
    const val SCREEN_RECORDING_LEFT_DESIGN = "screen_recording_left_design"
    const val SCREEN_RECORDING_RIGHT_DESIGN = "screen_recording_right_design"
}