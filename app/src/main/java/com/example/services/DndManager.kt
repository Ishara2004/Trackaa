package com.example.services

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.data.model.DndPauseBehavior

class DndManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var previousFilter: Int = NotificationManager.INTERRUPTION_FILTER_ALL
    private var isTrackaaRuleActive: Boolean = false

    fun isDndPermissionGranted(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun getDndSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun enableFocusProtection(dndEnabled: Boolean) {
        if (!dndEnabled || !isDndPermissionGranted()) return

        runCatching {
            previousFilter = notificationManager.currentInterruptionFilter
            // Set to priority-only interruptions during deep work
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            isTrackaaRuleActive = true
        }
    }

    fun disableFocusProtection() {
        if (!isDndPermissionGranted() || !isTrackaaRuleActive) return

        runCatching {
            notificationManager.setInterruptionFilter(previousFilter)
            isTrackaaRuleActive = false
        }
    }

    fun handlePause(behavior: DndPauseBehavior) {
        if (behavior == DndPauseBehavior.SUSPEND_WHILE_PAUSED && isTrackaaRuleActive) {
            runCatching {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        }
    }

    fun handleResume(behavior: DndPauseBehavior) {
        if (behavior == DndPauseBehavior.SUSPEND_WHILE_PAUSED && isTrackaaRuleActive) {
            runCatching {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }
        }
    }
}
