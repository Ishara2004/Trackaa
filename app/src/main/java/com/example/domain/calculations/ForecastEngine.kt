package com.example.domain.calculations

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.ceil

enum class ForecastConfidence { HIGH, MEDIUM, LOW, INSUFFICIENT_DATA }

data class ForecastResult(
    val rollingAverageDailyMinutes: Long,
    val remainingWorkMinutes: Long,
    val projectedCompletionDate: LocalDate?,
    val targetDeadlineDate: LocalDate?,
    val daysAheadOrBehindDeadline: Long?,
    val requiredDailyMinutesToMeetDeadline: Long?,
    val confidence: ForecastConfidence,
    val isAheadOfTarget: Boolean,
    val summaryMessage: String
)

object ForecastEngine {
    fun calculateForecast(
        historicalDailyMinutes: Map<LocalDate, Long>,
        eligibleWorkingDaysCount: Int,
        remainingWorkMinutes: Long,
        deadlineDate: LocalDate? = null,
        currentDate: LocalDate = LocalDate.now(),
        excludedDaysOfWeek: Set<DayOfWeek> = emptySet()
    ): ForecastResult {
        val remaining = remainingWorkMinutes.coerceAtLeast(0L)
        if (remaining == 0L) return ForecastResult(0,0,currentDate,deadlineDate,
            deadlineDate?.let { java.time.temporal.ChronoUnit.DAYS.between(currentDate,it) },0,
            ForecastConfidence.HIGH,true,"Target completed! All required work is complete.")

        val confidence = when {
            eligibleWorkingDaysCount >= 14 -> ForecastConfidence.HIGH
            eligibleWorkingDaysCount >= 7 -> ForecastConfidence.MEDIUM
            eligibleWorkingDaysCount >= 3 -> ForecastConfidence.LOW
            else -> ForecastConfidence.INSUFFICIENT_DATA
        }

        val deadlineEligibleDays = deadlineDate?.let { countEligibleDays(currentDate, it, excludedDaysOfWeek) } ?: 0
        val requiredDaily = if (deadlineDate != null && deadlineEligibleDays > 0)
            ceil(remaining.toDouble() / deadlineEligibleDays.toDouble()).toLong() else null

        if (eligibleWorkingDaysCount < 3 || historicalDailyMinutes.isEmpty()) {
            return ForecastResult(0, remaining, null, deadlineDate, null, requiredDaily,
                ForecastConfidence.INSUFFICIENT_DATA, false,
                "More focus history is needed before Trackaa can estimate your current pace reliably.")
        }

        val pace = (historicalDailyMinutes.values.sum().toDouble() / eligibleWorkingDaysCount.toDouble()).let { kotlin.math.round(it).toLong() }
        if (pace <= 0L) return ForecastResult(0,remaining,null,deadlineDate,null,requiredDaily,confidence,false,
            "Current pace is 0 min/day. No completion date can be projected yet.")

        val eligibleDaysNeeded = ceil(remaining.toDouble() / pace.toDouble()).toLong()
        val projected = advanceEligibleDays(currentDate, eligibleDaysNeeded, excludedDaysOfWeek)
        val aheadBehind = deadlineDate?.let { java.time.temporal.ChronoUnit.DAYS.between(projected, it) }
        val ahead = aheadBehind?.let { it >= 0 } ?: false
        val msg = if (deadlineDate == null) {
            "At ${DurationCalculator.formatMinutesHuman(pace)}/eligible day, projected completion is $projected."
        } else if (ahead) {
            "At your current pace, projected completion is $projected (${aheadBehind ?: 0} calendar days ahead of deadline)."
        } else {
            "At your current pace, projected completion is $projected (${kotlin.math.abs(aheadBehind ?: 0)} calendar days behind deadline)."
        }
        return ForecastResult(pace,remaining,projected,deadlineDate,aheadBehind,requiredDaily,confidence,ahead,msg)
    }

    private fun countEligibleDays(start: LocalDate, endInclusive: LocalDate, excluded: Set<DayOfWeek>): Int {
        if (!endInclusive.isAfter(start)) return 0
        var d = start.plusDays(1); var count = 0
        while (!d.isAfter(endInclusive)) { if (d.dayOfWeek !in excluded) count++; d=d.plusDays(1) }
        return count
    }

    private fun advanceEligibleDays(start: LocalDate, count: Long, excluded: Set<DayOfWeek>): LocalDate {
        if (count <= 0) return start
        var d=start; var remaining=count
        while (remaining>0) { d=d.plusDays(1); if (d.dayOfWeek !in excluded) remaining-- }
        return d
    }
}
