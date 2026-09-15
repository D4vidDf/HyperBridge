package com.d4viddf.hyperbridge.service.smartactions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import com.d4viddf.hyperbridge.models.SmartAction
import com.d4viddf.hyperbridge.models.SmartActionType
import com.d4viddf.hyperbridge.R
import com.d4viddf.hyperbridge.receiver.SmartActionReceiver

/**
 * Turns a [SmartAction] into the pieces a button needs: a stable key, a label, an icon and a
 * [PendingIntent]. Shared by the island translators and the Live Update path so both render the
 * same buttons.
 *
 * URL / phone / tracking / navigation buttons launch an Activity intent directly (no receiver in
 * between), which keeps them clear of the Android 12+ notification-trampoline restriction. Only
 * the OTP copy goes through [SmartActionReceiver], because writing to the clipboard needs no
 * Activity.
 */
object SmartActionIntents {

    private const val KEY_PREFIX = "smart_"

    /** Unique per notification and type, so two islands never share a request code. */
    fun actionKey(notificationKey: String, action: SmartAction): String =
        "$KEY_PREFIX${action.type.name.lowercase()}_${notificationKey.hashCode()}"

    fun label(context: Context, action: SmartAction, hideOtpCode: Boolean = false): String = when (action.type) {
        SmartActionType.OTP -> if (hideOtpCode) {
            context.getString(R.string.smart_action_copy_code_hidden)
        } else {
            context.getString(R.string.smart_action_copy_code, action.value)
        }
        SmartActionType.URL -> context.getString(R.string.smart_action_open_link)
        SmartActionType.PHONE -> context.getString(R.string.smart_action_call)
        SmartActionType.TRACKING -> context.getString(R.string.smart_action_track)
        SmartActionType.NAVIGATION -> context.getString(R.string.smart_action_directions)
    }

    @DrawableRes
    fun iconRes(type: SmartActionType): Int = when (type) {
        SmartActionType.OTP -> R.drawable.ic_smart_copy
        SmartActionType.URL -> R.drawable.ic_smart_link
        SmartActionType.PHONE -> R.drawable.ic_smart_call
        SmartActionType.TRACKING -> R.drawable.ic_smart_package
        SmartActionType.NAVIGATION -> R.drawable.ic_smart_navigate
    }

    fun pendingIntent(context: Context, action: SmartAction, key: String): PendingIntent {
        val requestCode = key.hashCode()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return when (action.type) {
            SmartActionType.OTP -> {
                val intent = Intent(context, SmartActionReceiver::class.java).apply {
                    setAction(SmartActionReceiver.ACTION_COPY)
                    // Distinct data per code: Intent.filterEquals ignores extras, so without this
                    // two live OTP islands would collapse onto one PendingIntent.
                    data = Uri.parse("hyperbridge://smart/otp/${action.target}")
                    putExtra(SmartActionReceiver.EXTRA_TEXT, action.target)
                    putExtra(SmartActionReceiver.EXTRA_SENSITIVE, true)
                }
                PendingIntent.getBroadcast(context, requestCode, intent, flags)
            }
            SmartActionType.URL, SmartActionType.TRACKING, SmartActionType.NAVIGATION -> {
                // NAVIGATION targets are either a map link (opened by whichever app owns it) or a
                // geo: search, which lets the system offer every installed maps app.
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(action.target)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                PendingIntent.getActivity(context, requestCode, intent, flags)
            }
            SmartActionType.PHONE -> {
                // target is "+" plus digits only, which is valid as-is in a tel: URI (RFC 3966);
                // encoding would turn the "+" into "%2B".
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${action.target}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                PendingIntent.getActivity(context, requestCode, intent, flags)
            }
        }
    }
}
