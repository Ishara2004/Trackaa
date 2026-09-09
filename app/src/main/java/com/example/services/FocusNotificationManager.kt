package com.example.services

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

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val activeChannel = NotificationChannel(
                CHANNEL_ACTIVE_FOCUS,
                "Active Focus Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing timer status and controls for deep work sessions"
                setShowBadge(false)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_TIMER_ALERTS,
                "Timer & Break Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts when countdown or pomodoro intervals complete"
                enableVibration(true)
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Focus Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Scheduled sessions and daily goal reminders"
            }

            val achievementChannel = NotificationChannel(
                CHANNEL_ACHIEVEMENTS,
                "Achievements & Records",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Unlocks and milestone celebrations"
            }

            val reportChannel = NotificationChannel(
                CHANNEL_REPORTS,
                "Reports & Summaries",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily and weekly performance reviews"
            }

            notificationManager.createNotificationChannels(
                listOf(activeChannel, alertChannel, reminderChannel, achievementChannel, reportChannel)
            )
        }
    }

    fun showActiveFocusNotification(state: ActiveSessionState) {
        if (state.status == FocusEngineStatus.IDLE || state.status == FocusEngineStatus.REVIEW_PENDING) {
            cancelActiveFocusNotification()
            return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPaused = state.status == FocusEngineStatus.PAUSED
        val isBreak = state.status == FocusEngineStatus.ON_BREAK

        val timeStr = when {
            state.mode == SessionMode.COUNTDOWN && state.plannedDurationMinutes > 0 -> {
                val plannedSec = state.plannedDurationMinutes * 60
                val remainingSec = plannedSec - state.elapsedFocusSeconds
                if (remainingSec >= 0) {
                    "${DurationCalculator.formatSecondsTimer(remainingSec)} remaining"
                } else {
                    "+${DurationCalculator.formatSecondsTimer(state.overtimeSeconds)} overtime"
                }
            }
            isBreak -> {
                "${DurationCalculator.formatSecondsTimer(state.elapsedBreakSeconds)} break"
            }
            else -> {
                DurationCalculator.formatSecondsTimer(state.elapsedFocusSeconds)
            }
        }

        val title = when {
            isPaused -> "Focus Paused — $timeStr"
            isBreak -> "On Break — $timeStr"
            state.isOvertime -> "Overtime — $timeStr"
            else -> "Focusing — $timeStr"
        }

        val subtext = "${state.activeTaskTitle} (${state.activeWorkItemName})"

        val builder = NotificationCompat.Builder(context, CHANNEL_ACTIVE_FOCUS)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(subtext)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Actions: Pause / Resume
        if (isPaused) {
            val resumeIntent = Intent(context, FocusActionReceiver::class.java).apply { action = ACTION_RESUME }
            val resumePending = PendingIntent.getBroadcast(
                context, 1, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePending)
        } else {
            val pauseIntent = Intent(context, FocusActionReceiver::class.java).apply { action = ACTION_PAUSE }
            val pausePending = PendingIntent.getBroadcast(
                context, 2, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePending)
        }

        // Action: Stop
        val stopIntent = Intent(context, FocusActionReceiver::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getBroadcast(
            context, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)

        notificationManager.notify(NOTIFICATION_ID_ACTIVE, builder.build())
    }

    fun cancelActiveFocusNotification() {
        notificationManager.cancel(NOTIFICATION_ID_ACTIVE)
    }

    fun showTimerAlertNotification(title: String, message: String) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TIMER_ALERTS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_ALERT, notification)
    }

    fun showAchievementNotification(title: String, description: String) {
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(android.R.drawable.star_big_on)
            .setContentTitle("Achievement Unlocked: $title")
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_ACHIEVEMENT, notification)
    }
}
