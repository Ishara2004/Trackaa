package com.example.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.entity.ScheduledFocusEntity

class FocusReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(item: ScheduledFocusEntity, taskTitle: String = "Focus Session") {
        if (item.scheduledEpochMs <= System.currentTimeMillis()) return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("scheduleId", item.id)
            putExtra("title", item.title)
            putExtra("taskTitle", taskTitle)
        }
        val pending = PendingIntent.getBroadcast(
            context, requestCode(item.id), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.scheduledEpochMs, pending)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, item.scheduledEpochMs, pending)
        }
    }

    fun cancel(id: Long) {
        val pending = PendingIntent.getBroadcast(
            context, requestCode(id), Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }

    fun exactAlarmSettingsIntent(): Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    } else null

    private fun requestCode(id: Long): Int = (id xor (id ushr 32)).toInt()
}
