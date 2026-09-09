package com.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.TrackaaApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class FocusActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? TrackaaApplication ?: return
        when (intent.action) {
            FocusNotificationManager.ACTION_PAUSE -> runBlocking {
                app.dndManager.handlePause(app.userPreferencesRepository.dndPauseBehaviorFlow.first())
                app.focusEngine.pauseFocus()
            }
            FocusNotificationManager.ACTION_RESUME -> runBlocking {
                app.dndManager.handleResume(app.userPreferencesRepository.dndPauseBehaviorFlow.first())
                val state = app.focusEngine.sessionState.value
                if (state.status == com.example.data.model.FocusEngineStatus.BREAK_COMPLETE) app.focusEngine.endBreakAndResumeFocus()
                else app.focusEngine.resumeFocus()
                ContextCompat.startForegroundService(context, Intent(context, FocusTimerService::class.java))
            }
            FocusNotificationManager.ACTION_STOP -> {
                app.dndManager.disableFocusProtection()
                app.focusEngine.requestStopSession()
            }
        }
    }
}
