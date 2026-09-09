package com.example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.ActiveSessionState
import com.example.data.model.FocusEngineStatus
import com.example.data.model.SessionMode
import com.example.domain.calculations.DurationCalculator

class FocusNotificationManager(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ACTIVE_FOCUS = "trackaa_active_focus"
        const val CHANNEL_TIMER_ALERTS = "trackaa_timer_alerts"
        const val CHANNEL_REMINDERS = "trackaa_reminders"
        const val CHANNEL_ACHIEVEMENTS = "trackaa_achievements"
        const val CHANNEL_REPORTS = "trackaa_reports"
        const val NOTIFICATION_ID_ACTIVE = 1001
        const val NOTIFICATION_ID_ALERT = 1002
        const val NOTIFICATION_ID_ACHIEVEMENT = 1003
        const val NOTIFICATION_ID_REMINDER = 1004
        const val ACTION_PAUSE = "com.example.trackaa.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.trackaa.ACTION_RESUME"
        const val ACTION_STOP = "com.example.trackaa.ACTION_STOP"
    }

    init { createChannels() }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        notificationManager.createNotificationChannels(listOf(
            NotificationChannel(CHANNEL_ACTIVE_FOCUS, "Active Focus Session", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Persistent status and controls for an active deep-work session"
                setShowBadge(false)
            },
            NotificationChannel(CHANNEL_TIMER_ALERTS, "Timer & Break Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Countdown and Pomodoro interval alerts"; enableVibration(true)
            },
            NotificationChannel(CHANNEL_REMINDERS, "Focus Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Scheduled focus and target reminders"
            },
            NotificationChannel(CHANNEL_ACHIEVEMENTS, "Achievements & Records", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CHANNEL_REPORTS, "Reports & Summaries", NotificationManager.IMPORTANCE_DEFAULT)
        ))
    }

    fun buildActiveFocusNotification(state: ActiveSessionState): Notification {
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_focus", true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val isPaused = state.status == FocusEngineStatus.PAUSED
        val isBreak = state.status == FocusEngineStatus.ON_BREAK || state.status == FocusEngineStatus.BREAK_COMPLETE
        val timeText = when {
            state.mode == SessionMode.COUNTDOWN && state.plannedDurationMinutes > 0 -> {
                val remaining = state.plannedDurationMinutes * 60 - state.elapsedFocusSeconds
                if (remaining >= 0) "${DurationCalculator.formatSecondsTimer(remaining)} remaining"
                else "+${DurationCalculator.formatSecondsTimer(state.overtimeSeconds)} overtime"
            }
            isBreak -> "${DurationCalculator.formatSecondsTimer(state.elapsedBreakSeconds)} break"
            else -> DurationCalculator.formatSecondsTimer(state.elapsedFocusSeconds)
        }
        val title = when {
            state.status == FocusEngineStatus.BREAK_COMPLETE -> "Break complete — ready to focus"
            isPaused -> "Focus paused — $timeText"
            isBreak -> "On break — $timeText"
            state.isOvertime -> "Overtime — $timeText"
            else -> "Focusing — $timeText"
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ACTIVE_FOCUS)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText("${state.activeTaskTitle} (${state.activeWorkItemName})")
            .setOngoing(state.status != FocusEngineStatus.BREAK_COMPLETE)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (state.status == FocusEngineStatus.PAUSED || state.status == FocusEngineStatus.BREAK_COMPLETE) {
            val resume = PendingIntent.getBroadcast(context, 1,
                Intent(context, FocusActionReceiver::class.java).apply { action = ACTION_RESUME },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resume)
        } else if (!isBreak) {
            val pause = PendingIntent.getBroadcast(context, 2,
                Intent(context, FocusActionReceiver::class.java).apply { action = ACTION_PAUSE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pause)
        }

        val stop = PendingIntent.getBroadcast(context, 3,
            Intent(context, FocusActionReceiver::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop)
        return builder.build()
    }

    fun showActiveFocusNotification(state: ActiveSessionState) {
        if (state.status == FocusEngineStatus.IDLE || state.status == FocusEngineStatus.REVIEW_PENDING) {
            cancelActiveFocusNotification(); return
        }
        notificationManager.notify(NOTIFICATION_ID_ACTIVE, buildActiveFocusNotification(state))
    }

    fun cancelActiveFocusNotification() = notificationManager.cancel(NOTIFICATION_ID_ACTIVE)

    fun showTimerAlertNotification(title: String, message: String) {
        val pendingIntent = PendingIntent.getActivity(context, 10,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        notificationManager.notify(NOTIFICATION_ID_ALERT,
            NotificationCompat.Builder(context, CHANNEL_TIMER_ALERTS)
                .setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(title).setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(pendingIntent).build())
    }

    fun showAchievementNotification(title: String, description: String) {
        val pendingIntent = PendingIntent.getActivity(context, 11, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        notificationManager.notify(NOTIFICATION_ID_ACHIEVEMENT,
            NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
                .setSmallIcon(android.R.drawable.star_big_on).setContentTitle("Achievement Unlocked: $title")
                .setContentText(description).setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true)
                .setContentIntent(pendingIntent).build())
    }
}
