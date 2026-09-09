package com.example.services

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.data.model.DndPauseBehavior

/**
 * Owns Trackaa's temporary focus-protection state.
 * On Android versions where setInterruptionFilter is mapped to an app-owned Automatic Zen Rule,
 * Android handles the rule semantics while Trackaa still tracks whether it activated protection.
 */
class DndManager(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val prefs = context.getSharedPreferences("trackaa_dnd_state", Context.MODE_PRIVATE)

    private var previousFilter: Int
        get() = prefs.getInt("previous_filter", NotificationManager.INTERRUPTION_FILTER_ALL)
        set(value) { prefs.edit().putInt("previous_filter", value).apply() }

    private var isTrackaaRuleActive: Boolean
        get() = prefs.getBoolean("trackaa_active", false)
        set(value) { prefs.edit().putBoolean("trackaa_active", value).apply() }

    fun isDndPermissionGranted(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    fun getDndSettingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    fun enableFocusProtection(dndEnabled: Boolean) {
        if (!dndEnabled || !isDndPermissionGranted() || isTrackaaRuleActive) return
        runCatching {
            previousFilter = notificationManager.currentInterruptionFilter
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            isTrackaaRuleActive = true
        }
    }

    fun disableFocusProtection() {
        if (!isTrackaaRuleActive) return
        if (!isDndPermissionGranted()) {
            isTrackaaRuleActive = false
            return
        }
        runCatching { notificationManager.setInterruptionFilter(previousFilter) }
        isTrackaaRuleActive = false
    }

    fun handlePause(behavior: DndPauseBehavior) {
        if (behavior != DndPauseBehavior.SUSPEND_WHILE_PAUSED || !isTrackaaRuleActive || !isDndPermissionGranted()) return
        runCatching { notificationManager.setInterruptionFilter(previousFilter) }
    }

    fun handleResume(behavior: DndPauseBehavior) {
        if (behavior != DndPauseBehavior.SUSPEND_WHILE_PAUSED || !isTrackaaRuleActive || !isDndPermissionGranted()) return
        runCatching { notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY) }
    }

    fun recoverIfNoActiveSession(hasActiveSession: Boolean) {
        if (!hasActiveSession && isTrackaaRuleActive) disableFocusProtection()
    }
}
