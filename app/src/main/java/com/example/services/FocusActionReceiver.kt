package com.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.TrackaaApplication

class FocusActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? TrackaaApplication ?: return
        when (intent.action) {
            FocusNotificationManager.ACTION_PAUSE -> {
                app.focusEngine.pauseFocus()
            }
            FocusNotificationManager.ACTION_RESUME -> {
                app.focusEngine.resumeFocus()
            }
            FocusNotificationManager.ACTION_STOP -> {
                app.focusEngine.requestStopSession()
            }
        }
    }
}
