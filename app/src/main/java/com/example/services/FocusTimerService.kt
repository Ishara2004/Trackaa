package com.example.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.TrackaaApplication
import com.example.data.model.FocusEngineStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FocusTimerService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as? TrackaaApplication
        if (app != null) {
            scope.launch {
                app.focusEngine.sessionState.collect { state ->
                    if (state.status == FocusEngineStatus.IDLE || state.status == FocusEngineStatus.REVIEW_PENDING) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }
        return START_STICKY
    }
}
