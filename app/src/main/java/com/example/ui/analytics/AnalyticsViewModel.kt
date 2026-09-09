package com.example.ui.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TrackaaApplication
import com.example.data.entity.FocusSessionEntity
import com.example.data.entity.InterruptionEntity
import com.example.data.entity.WorkItemEntity
import com.example.domain.calculations.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class AnalyticsTimeframe {
    DAILY,
    WEEKLY,
    MONTHLY
}

data class HourlyDistribution(
    val morningMinutes: Long,   // 05:00 - 12:00
    val afternoonMinutes: Long, // 12:00 - 17:00
    val eveningMinutes: Long,   // 17:00 - 22:00
    val nightMinutes: Long      // 22:00 - 05:00
)

data class AnalyticsUiState(
    val selectedTimeframe: AnalyticsTimeframe = AnalyticsTimeframe.WEEKLY,
    val selectedDate: LocalDate = LocalDate.now(),
    val totalFocusMinutes: Long = 0,
    val dailyAverageMinutes: Long = 0,
    val longestSessionMinutes: Long = 0,
    val totalPauseMinutes: Long = 0,
    val pauseRatioPercent: Double = 0.0,
    val interruptionCount: Int = 0,
    val averageQuality: Double = 0.0,
    val averageEnergy: Double = 0.0,
    val hourlyDistribution: HourlyDistribution = HourlyDistribution(0, 0, 0, 0),
    val workItemDistribution: Map<String, Long> = emptyMap(), // Name -> minutes
    val sessionsInPeriod: List<FocusSessionEntity> = emptyList(),
    val focusDebtResult: FocusDebtResult? = null,
    val recoveryPlanResult: RecoveryPlanResult? = null,
    val forecastResult: ForecastResult? = null,
    val whatIfPaceResult: WhatIfPaceToDateResult? = null,
    val whatIfDeadlineResult: WhatIfDeadlineResult? = null,
    val isLoading: Boolean = true
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = getApplication<TrackaaApplication>()
    private val repository = app.repository

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    fun setTimeframe(timeframe: AnalyticsTimeframe) {
        _uiState.update { it.copy(selectedTimeframe = timeframe) }
        loadAnalytics()
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            repository.getAllFocusSessions().collect { allSessions ->
                val state = _uiState.value
                val zoneId = ZoneId.systemDefault()
                val selectedDate = state.selectedDate

                // Determine timeframe range
                val (startDate, endDate) = when (state.selectedTimeframe) {
                    AnalyticsTimeframe.DAILY -> Pair(selectedDate, selectedDate)
                    AnalyticsTimeframe.WEEKLY -> {
                        val monday = selectedDate.with(DayOfWeek.MONDAY)
                        val sunday = selectedDate.with(DayOfWeek.SUNDAY)
                        Pair(monday, sunday)
                    }
                    AnalyticsTimeframe.MONTHLY -> {
                        val first = selectedDate.withDayOfMonth(1)
                        val last = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth())
                        Pair(first, last)
                    }
                }

                // Filter and split sessions by calendar day
                val dailyMinutesMap = mutableMapOf<LocalDate, Long>()
                val periodSessions = mutableListOf<FocusSessionEntity>()

                var morningMin = 0L
                var afternoonMin = 0L
                var eveningMin = 0L
                var nightMin = 0L

                for (session in allSessions) {
                    val split = DurationCalculator.splitIntervalByCalendarDays(session.startEpochMs, session.endEpochMs, zoneId)
                    var inPeriod = false

                    for ((date, mins) in split) {
                        if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                            dailyMinutesMap[date] = (dailyMinutesMap[date] ?: 0L) + mins
                            inPeriod = true
                        }
                    }

                    if (inPeriod) {
                        periodSessions.add(session)
                        val startLdt = LocalDateTime.ofInstant(Instant.ofEpochMilli(session.startEpochMs), zoneId)
                        when (startLdt.hour) {
                            in 5..11 -> morningMin += session.totalFocusMinutes
                            in 12..16 -> afternoonMin += session.totalFocusMinutes
                            in 17..21 -> eveningMin += session.totalFocusMinutes
                            else -> nightMin += session.totalFocusMinutes
                        }
                    }
                }

                val totalFocus = periodSessions.sumOf { it.totalFocusMinutes }
                val totalPause = periodSessions.sumOf { it.totalPauseMinutes }
                val dayCount = Math.max(1, ChronoUnit.DAYS.between(startDate, endDate) + 1)
                val dailyAvg = totalFocus / dayCount
                val longest = periodSessions.maxOfOrNull { it.totalFocusMinutes } ?: 0L

                val totalWall = totalFocus + totalPause
                val pauseRatio = if (totalWall > 0) (totalPause.toDouble() / totalWall.toDouble()) * 100.0 else 0.0

                val avgQual = if (periodSessions.isNotEmpty()) periodSessions.map { it.focusQuality }.average() else 0.0
                val avgEnergy = if (periodSessions.isNotEmpty()) periodSessions.map { it.energyLevel }.average() else 0.0

                // Interruption count
                val allInterr = repository.getAllInterruptions().first()
                val sessionIds = periodSessions.map { it.id }.toSet()
                val periodInterr = allInterr.filter { sessionIds.contains(it.sessionId) }

                // Expected targets and debt calculation
                val expectedTargetMinutes = dayCount * 240L // assuming 4h / day target
                val debtResult = TargetEngine.calculateDebtOrCredit(expectedTargetMinutes, totalFocus)

                // Recovery Plan if debt exists
                val recoveryPlan = if (debtResult.isDebt && debtResult.differenceMinutes > 0) {
                    val upcomingDays = (1..7).map { i ->
                        val d = LocalDate.now().plusDays(i.toLong())
                        DayCapacity(
                            date = d,
                            dayOfWeek = d.dayOfWeek.value,
                            isAvailable = d.dayOfWeek != DayOfWeek.SUNDAY,
                            capacityMinutes = 360L // 6h available daily capacity
                        )
                    }
                    RecoveryPlanEngine.generateRecoveryPlan(debtResult.differenceMinutes, upcomingDays)
                } else null

                // Forecast
                val forecast = ForecastEngine.calculateForecast(
                    historicalDailyMinutes = dailyMinutesMap,
                    eligibleWorkingDaysCount = dayCount.toInt(),
                    remainingWorkMinutes = Math.max(0L, expectedTargetMinutes - totalFocus),
                    deadlineDate = endDate,
                    currentDate = LocalDate.now()
                )

                _uiState.update {
                    it.copy(
                        totalFocusMinutes = totalFocus,
                        dailyAverageMinutes = dailyAvg,
                        longestSessionMinutes = longest,
                        totalPauseMinutes = totalPause,
                        pauseRatioPercent = pauseRatio,
                        interruptionCount = periodInterr.size,
                        averageQuality = avgQual,
                        averageEnergy = avgEnergy,
                        hourlyDistribution = HourlyDistribution(morningMin, afternoonMin, eveningMin, nightMin),
                        sessionsInPeriod = periodSessions,
                        focusDebtResult = debtResult,
                        recoveryPlanResult = recoveryPlan,
                        forecastResult = forecast,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun runWhatIfPaceSimulation(dailyHours: Double, remainingHours: Double) {
        val result = WhatIfSimulator.simulateFinishDate(
            dailyFocusMinutes = (dailyHours * 60).toLong(),
            remainingWorkMinutes = (remainingHours * 60).toLong()
        )
        _uiState.update { it.copy(whatIfPaceResult = result) }
    }

    fun runWhatIfDeadlineSimulation(targetDaysAhead: Long, remainingHours: Double) {
        val deadline = LocalDate.now().plusDays(targetDaysAhead)
        val result = WhatIfSimulator.simulateRequiredPaceForDeadline(
            deadline = deadline,
            remainingWorkMinutes = (remainingHours * 60).toLong()
        )
        _uiState.update { it.copy(whatIfDeadlineResult = result) }
    }
}
