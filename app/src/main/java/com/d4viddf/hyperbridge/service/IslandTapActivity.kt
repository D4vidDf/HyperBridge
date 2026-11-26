package com.d4viddf.hyperbridge.service

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.d4viddf.hyperbridge.util.sendAllowingBackgroundLaunch

/**
 * Invisible landing point for a tap on a MESSAGE island.
 *
 * The tap used to reach us as a broadcast, which makes it a notification trampoline: on API 31+
 * the system may drop the activity launch we fire from it, and whether the BAL opt-in from #366
 * rescues it depends on the ROM (#371, #382: the island just closes). An activity started straight
 * from the notification is in the foreground, so launching the app's own content intent from here
 * is always allowed. Then we hand the cleanup (cancel original + island) to the listener service.
 */
class IslandTapActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        val originalIntent = intent.getParcelableExtra<PendingIntent>(EXTRA_ORIGINAL_INTENT)
        try {
            originalIntent?.sendAllowingBackgroundLaunch()
        } catch (e: PendingIntent.CanceledException) {
            Log.e("HyperBridge", "PendingIntent canceled", e)
        }

        sendBroadcast(Intent(ACTION_ISLAND_CLICKED).apply {
            setPackage(packageName)
            putExtra(EXTRA_SBN_KEY, intent.getStringExtra(EXTRA_SBN_KEY))
            putExtra(EXTRA_BRIDGE_ID, intent.getIntExtra(EXTRA_BRIDGE_ID, -1))
        })

        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        const val ACTION_ISLAND_CLICKED = "com.d4viddf.hyperbridge.ISLAND_CLICKED"
        const val EXTRA_SBN_KEY = "sbn_key"
        const val EXTRA_BRIDGE_ID = "bridge_id"
        const val EXTRA_ORIGINAL_INTENT = "original_intent"
    }
}
