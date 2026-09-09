package com.example.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.TrackaaApplication
import com.example.data.model.FocusEngineStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class FocusTimerService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var collectorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as TrackaaApplication
        val initial = app.focusEngine.sessionState.value
        if (initial.status != FocusEngineStatus.IDLE && initial.status != FocusEngineStatus.REVIEW_PENDING) {
            startForeground(FocusNotificationManager.NOTIFICATION_ID_ACTIVE, app.notificationManager.buildActiveFocusNotification(initial))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as TrackaaApplication
        val initial = app.focusEngine.sessionState.value
        if (initial.status == FocusEngineStatus.IDLE || initial.status == FocusEngineStatus.REVIEW_PENDING) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(FocusNotificationManager.NOTIFICATION_ID_ACTIVE, app.notificationManager.buildActiveFocusNotification(initial))
        collectorJob?.cancel()
        collectorJob = serviceScope.launch {
            app.focusEngine.sessionState.collectLatest { state ->
                if (state.status == FocusEngineStatus.IDLE || state.status == FocusEngineStatus.REVIEW_PENDING) {
                    ServiceCompat.stopForeground(this@FocusTimerService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    startForeground(FocusNotificationManager.NOTIFICATION_ID_ACTIVE, app.notificationManager.buildActiveFocusNotification(state))
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        collectorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
