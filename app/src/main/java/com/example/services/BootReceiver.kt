package com.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.TrackaaApplication

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // TrackaaApplication onCreate automatically initializes FocusEngine which recovers any interrupted session
            val app = context.applicationContext as? TrackaaApplication
            // Checkpoint recovery is triggered automatically in FocusEngine.init
        }
    }
}
