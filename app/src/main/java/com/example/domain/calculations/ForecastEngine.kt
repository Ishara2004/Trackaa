package com.example.domain.calculations

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class ForecastConfidence {
    HIGH,
    MEDIUM,
    LOW,
    INSUFFICIENT_DATA
}

data class ForecastResult(
    val rollingAverageDailyMinutes: Long,
    val remainingWorkMinutes: Long,
    val projectedCompletionDate: LocalDate?,
    val targetDeadlineDate: LocalDate?,
    val daysAheadOrBehindDeadline: Long?, // positive = early, negative = delayed
    val requiredDailyMinutesToMeetDeadline: Long?,
    val confidence: ForecastConfidence,
    val isAheadOfTarget: Boolean,
    val summaryMessage: String
)

object ForecastEngine {

    /**
     * Calculates rolling pace across historical eligible days (including zero-output days)
     * and projects completion date for remaining work.
     */
    fun calculateForecast(
        historicalDailyMinutes: Map<LocalDate, Long>, // actual tracked minutes for past dates
        eligibleWorkingDaysCount: Int, // total eligible days in window (including zeroes)
        remainingWorkMinutes: Long,
        deadlineDate: LocalDate? = null,
        currentDate: LocalDate = LocalDate.now()
    ): ForecastResult {
        if (remainingWorkMinutes <= 0) {
            return ForecastResult(
                rollingAverageDailyMinutes = 0,
                remainingWorkMinutes = 0,
                projectedCompletionDate = currentDate,
                targetDeadlineDate = deadlineDate,
                daysAheadOrBehindDeadline = deadlineDate?.let { ChronoUnit.DAYS.between(currentDate, it) },
                requiredDailyMinutesToMeetDeadline = 0,
                confidence = ForecastConfidence.HIGH,
                isAheadOfTarget = true,
                summaryMessage = "Target completed! All required work is complete."
            )
        }

        if (eligibleWorkingDaysCount < 3 || historicalDailyMinutes.isEmpty()) {
            val remainingDaysToDeadline = deadlineDate?.let { ChronoUnit.DAYS.between(currentDate, it) }
            val reqDaily = if (remainingDaysToDeadline != null && remainingDaysToDeadline > 0) {
                Math.round(remainingWorkMinutes.toDouble() / remainingDaysToDeadline.toDouble())
            } else null

            return ForecastResult(
                rollingAverageDailyMinutes = 0,
                remainingWorkMinutes = remainingWorkMinutes,
                projectedCompletionDate = null,
                targetDeadlineDate = deadlineDate,
                daysAheadOrBehindDeadline = null,
                requiredDailyMinutesToMeetDeadline = reqDaily,
                confidence = ForecastConfidence.INSUFFICIENT_DATA,
                isAheadOfTarget = false,
                summaryMessage = "More focus history is needed before Trackaa can estimate your current pace reliably."
            )
        }

        val totalFocusedInWindow = historicalDailyMinutes.values.sum()
        // Rolling pace includes zero-output eligible days
        val dailyPace = Math.round(totalFocusedInWindow.toDouble() / eligibleWorkingDaysCount.toDouble())

        val confidence = when {
            eligibleWorkingDaysCount >= 14 -> ForecastConfidence.HIGH
            eligibleWorkingDaysCount >= 7 -> ForecastConfidence.MEDIUM
            else -> ForecastConfidence.LOW
        }

        if (dailyPace <= 0L) {
            return ForecastResult(
                rollingAverageDailyMinutes = 0,
                remainingWorkMinutes = remainingWorkMinutes,
                projectedCompletionDate = null,
                targetDeadlineDate = deadlineDate,
                daysAheadOrBehindDeadline = null,
                requiredDailyMinutesToMeetDeadline = null,
                confidence = confidence,
                isAheadOfTarget = false,
                summaryMessage = "Current pace is 0 min/day. No progress is being made toward target."
            )
        }

        val daysNeeded = Math.round(remainingWorkMinutes.toDouble() / dailyPace.toDouble())
        val projectedFinish = currentDate.plusDays(daysNeeded)

        val daysAheadBehind = deadlineDate?.let {
            ChronoUnit.DAYS.between(projectedFinish, it) // positive = finishing before deadline (early), negative = late
        }

        val remainingDaysToDeadline = deadlineDate?.let { ChronoUnit.DAYS.between(currentDate, it) }
        val requiredDaily = if (remainingDaysToDeadline != null && remainingDaysToDeadline > 0) {
            Math.round(remainingWorkMinutes.toDouble() / remainingDaysToDeadline.toDouble())
        } else null

        val isAhead = (daysAheadBehind ?: 0L) >= 0

        val msg = if (deadlineDate != null) {
            if (isAhead) {
                "At your real pace of ${DurationCalculator.formatMinutesHuman(dailyPace)}/day, you are projected to finish on $projectedFinish, $daysAheadBehind days ahead of deadline."
            } else {
                val delayDays = Math.abs(daysAheadBehind ?: 0L)
                "At your real pace of ${DurationCalculator.formatMinutesHuman(dailyPace)}/day, you are projected to finish on $projectedFinish ($delayDays days behind deadline)."
            }
        } else {
            "At your real pace of ${DurationCalculator.formatMinutesHuman(dailyPace)}/day, you will complete remaining work on $projectedFinish."
        }

        return ForecastResult(
            rollingAverageDailyMinutes = dailyPace,
            remainingWorkMinutes = remainingWorkMinutes,
            projectedCompletionDate = projectedFinish,
            targetDeadlineDate = deadlineDate,
            daysAheadOrBehindDeadline = daysAheadBehind,
            requiredDailyMinutesToMeetDeadline = requiredDaily,
            confidence = confidence,
            isAheadOfTarget = isAhead,
            summaryMessage = msg
        )
    }
}
