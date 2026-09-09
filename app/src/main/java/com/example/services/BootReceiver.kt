package com.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.TrackaaApplication
import com.example.data.model.FocusEngineStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext as? TrackaaApplication ?: return
        val pendingResult = goAsync()
        app.applicationScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                app.repository.getUpcomingScheduledFocus(now).first().forEach { scheduled ->
                    val taskTitle = scheduled.taskId?.let { app.repository.getTaskById(it)?.title } ?: "Focus Session"
                    app.reminderScheduler.schedule(scheduled, taskTitle)
                }
                val active = app.focusEngine.sessionState.value.status in setOf(
                    FocusEngineStatus.FOCUSING,
                    FocusEngineStatus.PAUSED,
                    FocusEngineStatus.ON_BREAK,
                    FocusEngineStatus.BREAK_COMPLETE
                )
                app.dndManager.recoverIfNoActiveSession(active)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
