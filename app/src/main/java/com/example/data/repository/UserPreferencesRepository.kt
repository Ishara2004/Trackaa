package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DndPauseBehavior
import com.example.data.model.ThemeSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "trackaa_user_preferences")

data class UserPreferences(
    val theme: ThemeSetting = ThemeSetting.SYSTEM,
    val streakThresholdMinutes: Long = 180, // 3 hours default threshold for streak
    val timeProgressWeight: Int = 50,
    val taskProgressWeight: Int = 30,
    val topicProgressWeight: Int = 20,
    val dndEnabled: Boolean = true,
    val dndPauseBehavior: DndPauseBehavior = DndPauseBehavior.KEEP_ACTIVE,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursStartMinute: Int = 0,
    val quietHoursEndHour: Int = 7,
    val quietHoursEndMinute: Int = 0,
    val endOfDayReminderEnabled: Boolean = true,
    val endOfDayReviewHour: Int = 21,
    val endOfDayReviewMinute: Int = 30,
    val onboardingCompleted: Boolean = false,
    val demoWorkspaceActive: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val STREAK_THRESHOLD = longPreferencesKey("streak_threshold_minutes")
        val WEIGHT_TIME = intPreferencesKey("weight_time")
        val WEIGHT_TASK = intPreferencesKey("weight_task")
        val WEIGHT_TOPIC = intPreferencesKey("weight_topic")
        val DND_ENABLED = booleanPreferencesKey("dnd_enabled")
        val DND_PAUSE_BEHAVIOR = stringPreferencesKey("dnd_pause_behavior")
        val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
        val QUIET_START_HOUR = intPreferencesKey("quiet_start_hour")
        val QUIET_START_MIN = intPreferencesKey("quiet_start_min")
        val QUIET_END_HOUR = intPreferencesKey("quiet_end_hour")
        val QUIET_END_MIN = intPreferencesKey("quiet_end_min")
        val EOD_REMINDER_ENABLED = booleanPreferencesKey("eod_reminder_enabled")
        val EOD_REVIEW_HOUR = intPreferencesKey("eod_review_hour")
        val EOD_REVIEW_MIN = intPreferencesKey("eod_review_min")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DEMO_WORKSPACE_ACTIVE = booleanPreferencesKey("demo_workspace_active")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                theme = runCatching {
                    ThemeSetting.valueOf(preferences[PreferencesKeys.THEME] ?: ThemeSetting.SYSTEM.name)
                }.getOrDefault(ThemeSetting.SYSTEM),
                streakThresholdMinutes = preferences[PreferencesKeys.STREAK_THRESHOLD] ?: 180,
                timeProgressWeight = preferences[PreferencesKeys.WEIGHT_TIME] ?: 50,
                taskProgressWeight = preferences[PreferencesKeys.WEIGHT_TASK] ?: 30,
                topicProgressWeight = preferences[PreferencesKeys.WEIGHT_TOPIC] ?: 20,
                dndEnabled = preferences[PreferencesKeys.DND_ENABLED] ?: true,
                dndPauseBehavior = runCatching {
                    DndPauseBehavior.valueOf(preferences[PreferencesKeys.DND_PAUSE_BEHAVIOR] ?: DndPauseBehavior.KEEP_ACTIVE.name)
                }.getOrDefault(DndPauseBehavior.KEEP_ACTIVE),
                quietHoursEnabled = preferences[PreferencesKeys.QUIET_HOURS_ENABLED] ?: false,
                quietHoursStartHour = preferences[PreferencesKeys.QUIET_START_HOUR] ?: 22,
                quietHoursStartMinute = preferences[PreferencesKeys.QUIET_START_MIN] ?: 0,
                quietHoursEndHour = preferences[PreferencesKeys.QUIET_END_HOUR] ?: 7,
                quietHoursEndMinute = preferences[PreferencesKeys.QUIET_END_MIN] ?: 0,
                endOfDayReminderEnabled = preferences[PreferencesKeys.EOD_REMINDER_ENABLED] ?: true,
                endOfDayReviewHour = preferences[PreferencesKeys.EOD_REVIEW_HOUR] ?: 21,
                endOfDayReviewMinute = preferences[PreferencesKeys.EOD_REVIEW_MIN] ?: 30,
                onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
                demoWorkspaceActive = preferences[PreferencesKeys.DEMO_WORKSPACE_ACTIVE] ?: false
            )
        }

    val themeSettingFlow: Flow<ThemeSetting> = userPreferencesFlow.map { it.theme }
    val isDndEnabledFlow: Flow<Boolean> = userPreferencesFlow.map { it.dndEnabled }
    val dndPauseBehaviorFlow: Flow<DndPauseBehavior> = userPreferencesFlow.map { it.dndPauseBehavior }

    suspend fun setThemeSetting(theme: ThemeSetting) = setTheme(theme)

    suspend fun setDndEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DND_ENABLED] = enabled
        }
    }

    suspend fun setDndPauseBehavior(behavior: DndPauseBehavior) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DND_PAUSE_BEHAVIOR] = behavior.name
        }
    }

    suspend fun setTheme(theme: ThemeSetting) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun setStreakThresholdMinutes(minutes: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STREAK_THRESHOLD] = minutes
        }
    }

    suspend fun setProgressWeights(timeWeight: Int, taskWeight: Int, topicWeight: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEIGHT_TIME] = timeWeight
            preferences[PreferencesKeys.WEIGHT_TASK] = taskWeight
            preferences[PreferencesKeys.WEIGHT_TOPIC] = topicWeight
        }
    }

    suspend fun setDndSettings(enabled: Boolean, pauseBehavior: DndPauseBehavior) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DND_ENABLED] = enabled
            preferences[PreferencesKeys.DND_PAUSE_BEHAVIOR] = pauseBehavior.name
        }
    }

    suspend fun setQuietHours(enabled: Boolean, startH: Int, startM: Int, endH: Int, endM: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.QUIET_HOURS_ENABLED] = enabled
            preferences[PreferencesKeys.QUIET_START_HOUR] = startH
            preferences[PreferencesKeys.QUIET_START_MIN] = startM
            preferences[PreferencesKeys.QUIET_END_HOUR] = endH
            preferences[PreferencesKeys.QUIET_END_MIN] = endM
        }
    }

    suspend fun setEndOfDayReview(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EOD_REMINDER_ENABLED] = enabled
            preferences[PreferencesKeys.EOD_REVIEW_HOUR] = hour
            preferences[PreferencesKeys.EOD_REVIEW_MIN] = minute
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setDemoWorkspaceActive(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEMO_WORKSPACE_ACTIVE] = active
        }
    }
}
