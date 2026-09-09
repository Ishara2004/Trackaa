package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TrackaaApplication
import com.example.data.entity.FocusSessionEntity
import com.example.data.entity.GoalEntity
import com.example.data.entity.TaskEntity
import com.example.data.entity.TargetEntity
import com.example.data.entity.WorkItemEntity
import com.example.data.model.TargetPeriod
import com.example.domain.calculations.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val todayFocusMinutes: Long = 0,
    val todayGoalMinutes: Long = 240, // 4 hours
    val todayMinMinutes: Long = 120,  // 2 hours
    val todayStretchMinutes: Long = 360, // 6 hours
    val todayTier: TargetTier = TargetTier.NONE,
    val currentStreakDays: Int = 0,
    val isStreakDoneToday: Boolean = false,
    val momentumScore: MomentumScoreBreakdown? = null,
    val rollingForecast: ForecastResult? = null,
    val activeGoals: List<GoalEntity> = emptyList(),
    val activeWorkItems: List<WorkItemEntity> = emptyList(),
    val recentTasks: List<TaskEntity> = emptyList(),
    val totalXp: Int = 0,
    val levelProgress: LevelProgress = LevelProgress(1, 0, 0, 500, 0.0),
    val isLoading: Boolean = true
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = getApplication<TrackaaApplication>()
    private val repository = app.repository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadHomeData() {
        viewModelScope.launch {
            combine(
                repository.getAllFocusSessions(),
                repository.getAllGoals(),
                repository.getAllActiveWorkItems(),
                repository.getRecentTasks(5),
                repository.getTotalXp(),
                repository.getGlobalTarget(TargetPeriod.DAILY)
            ) { args: Array<Any?> ->
                val sessions = args[0] as List<FocusSessionEntity>
                val goals = args[1] as List<GoalEntity>
                val workItems = args[2] as List<WorkItemEntity>
                val tasks = args[3] as List<TaskEntity>
                val xp = args[4] as Int
                val dailyTarget = args[5] as TargetEntity?

                val today = LocalDate.now()
                val zoneId = ZoneId.systemDefault()

                // Calculate daily contributions using DurationCalculator (splitting across midnight boundaries)
                val dailyMinutesMap = mutableMapOf<LocalDate, Long>()
                for (session in sessions) {
                    val split = DurationCalculator.splitIntervalByCalendarDays(session.startEpochMs, session.endEpochMs, zoneId)
                    for ((date, mins) in split) {
                        dailyMinutesMap[date] = (dailyMinutesMap[date] ?: 0L) + mins
                    }
                }

                val todayMinutes = dailyMinutesMap[today] ?: 0L
                val streakThreshold = 60L // 1 hour minimum for streak
                val streak = StreakAndGamification.calculateStreak(dailyMinutesMap, streakThreshold, today)

                // Target calculations
                val minMin = dailyTarget?.minMinutes ?: 120L
                val goalMin = dailyTarget?.goalMinutes ?: 240L
                val stretchMin = dailyTarget?.stretchMinutes ?: 360L
                val progressResult = TargetEngine.calculateProgress(todayMinutes, minMin, goalMin, stretchMin)

                // Momentum calculations: evaluate recent 14 days
                var eligibleDaysInWindow = 14
                var daysMeetingThreshold = 0
                var recent7Sum = 0L
                var prev7Sum = 0L

                for (i in 0 until 7) {
                    val d = today.minusDays(i.toLong())
                    val m = dailyMinutesMap[d] ?: 0L
                    recent7Sum += m
                    if (m >= streakThreshold) daysMeetingThreshold++
                }
                for (i in 7 until 14) {
                    val d = today.minusDays(i.toLong())
                    prev7Sum += (dailyMinutesMap[d] ?: 0L)
                }

                val momentum = MomentumEngine.calculateMomentumScore(
                    actualMinutesToDate = recent7Sum + prev7Sum,
                    expectedMinutesToDate = eligibleDaysInWindow * goalMin,
                    eligibleDaysMeetingThreshold = daysMeetingThreshold,
                    totalEligibleDaysInWindow = eligibleDaysInWindow,
                    recent7DayDailyAverage = recent7Sum.toDouble() / 7.0,
                    previous7DayDailyAverage = prev7Sum.toDouble() / 7.0
                )

                val level = StreakAndGamification.calculateLevel(xp)

                HomeUiState(
                    todayFocusMinutes = todayMinutes,
                    todayGoalMinutes = goalMin,
                    todayMinMinutes = minMin,
                    todayStretchMinutes = stretchMin,
                    todayTier = progressResult.tier,
                    currentStreakDays = streak.currentStreakDays,
                    isStreakDoneToday = streak.isStreakActiveToday,
                    momentumScore = momentum,
                    activeGoals = goals,
                    activeWorkItems = workItems,
                    recentTasks = tasks,
                    totalXp = xp,
                    levelProgress = level,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun insertDemoWorkspace() {
        viewModelScope.launch {
            repository.insertDemoWorkspace()
        }
    }
}
