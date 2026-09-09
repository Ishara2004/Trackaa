package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TrackaaApplication
import com.example.data.entity.*
import com.example.data.model.TargetPeriod
import com.example.domain.calculations.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val todayFocusMinutes: Long = 0,
    val todayGoalMinutes: Long = 240,
    val todayMinMinutes: Long = 120,
    val todayStretchMinutes: Long = 360,
    val todayTier: TargetTier = TargetTier.NONE,
    val currentStreakDays: Int = 0,
    val isStreakDoneToday: Boolean = false,
    val momentumScore: MomentumScoreBreakdown? = null,
    val rollingForecast: ForecastResult? = null,
    val activeGoals: List<GoalEntity> = emptyList(),
    val activeWorkItems: List<WorkItemEntity> = emptyList(),
    val recentTasks: List<TaskEntity> = emptyList(),
    val totalXp: Int = 0,
    val levelProgress: LevelProgress = LevelProgress(1,0,0,500,0.0),
    val isLoading: Boolean = true
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<TrackaaApplication>()
    private val repository = app.repository
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadHomeData() }

    @Suppress("UNCHECKED_CAST")
    private fun loadHomeData() {
        viewModelScope.launch {
            combine(
                repository.getAllFocusSessions(), repository.getAllGoals(), repository.getAllActiveWorkItems(),
                repository.getRecentTasks(5), repository.getTotalXp(), repository.getGlobalTarget(TargetPeriod.DAILY),
                repository.getAllAvailability(), app.userPreferencesRepository.userPreferencesFlow
            ) { args: Array<Any?> -> args }.collect { args ->
                val sessions=args[0] as List<FocusSessionEntity>; val goals=args[1] as List<GoalEntity>
                val workItems=args[2] as List<WorkItemEntity>; val tasks=args[3] as List<TaskEntity>; val xp=args[4] as Int
                val target=args[5] as TargetEntity?; val availability=args[6] as List<AvailabilityEntity>
                val prefs=args[7] as com.example.data.repository.UserPreferences
                val zone=ZoneId.systemDefault(); val today=LocalDate.now()

                val dailySeconds=mutableMapOf<LocalDate,Long>()
                for (session in sessions) {
                    for (segment in repository.getFocusSegmentsForSessionOnce(session.id)) {
                        DurationCalculator.splitIntervalByCalendarDaysSeconds(segment.startEpochMs,segment.endEpochMs,zone).forEach { (d,s) -> dailySeconds[d]=(dailySeconds[d]?:0L)+s }
                    }
                }
                val dailyMinutes=dailySeconds.mapValues { it.value/60L }
                val todayMinutes=(dailySeconds[today]?:0L)/60L
                val min=target?.minMinutes ?: 180L; val goal=target?.goalMinutes ?: 300L; val stretch=target?.stretchMinutes ?: 420L
                val progress=TargetEngine.calculateProgress(todayMinutes,min,goal,stretch)
                val streak=StreakAndGamification.calculateStreak(dailyMinutes,prefs.streakThresholdMinutes,today)

                val availabilityByDay=availability.associateBy { it.dayOfWeek }
                val fourteen=(0 until 14).map { today.minusDays(it.toLong()) }
                val eligible=fourteen.filter { availabilityByDay[it.dayOfWeek.value]?.isAvailable != false }
                val recent7=eligible.filter { !it.isBefore(today.minusDays(6)) }
                val previous7=eligible.filter { it.isBefore(today.minusDays(6)) }
                val daysMeeting=eligible.count { (dailyMinutes[it]?:0L)>=prefs.streakThresholdMinutes }
                val recentAvg=if(recent7.isNotEmpty()) recent7.sumOf { dailyMinutes[it]?:0L }.toDouble()/recent7.size else 0.0
                val previousAvg=if(previous7.isNotEmpty()) previous7.sumOf { dailyMinutes[it]?:0L }.toDouble()/previous7.size else 0.0
                val momentum=MomentumEngine.calculateMomentumScore(
                    actualMinutesToDate=eligible.sumOf { dailyMinutes[it]?:0L }, expectedMinutesToDate=eligible.size*goal,
                    eligibleDaysMeetingThreshold=daysMeeting,totalEligibleDaysInWindow=eligible.size,
                    recent7DayDailyAverage=recentAvg,previous7DayDailyAverage=previousAvg)

                val historyWindow=(0 until 14).map { today.minusDays(it.toLong()) }.filter { availabilityByDay[it.dayOfWeek.value]?.isAvailable != false }
                val history=historyWindow.associateWith { dailyMinutes[it]?:0L }
                val remaining=(goal-todayMinutes).coerceAtLeast(0L)
                val excluded=DayOfWeek.values().filter { availabilityByDay[it.value]?.isAvailable == false }.toSet()
                val forecast=ForecastEngine.calculateForecast(history,historyWindow.size,remaining,null,today,excluded)

                _uiState.value=HomeUiState(todayMinutes,goal,min,stretch,progress.tier,streak.currentStreakDays,streak.isStreakActiveToday,
                    momentum,forecast,goals,workItems,tasks,xp,StreakAndGamification.calculateLevel(xp),false)
            }
        }
    }

    fun insertDemoWorkspace() { viewModelScope.launch { repository.insertDemoWorkspace() } }
}
