package com.example.domain.calculations

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class WhatIfPaceToDateResult(
    val dailyMinutes: Long,
    val remainingWorkMinutes: Long,
    val eligibleDaysNeeded: Long,
    val calendarDaysNeeded: Long,
    val projectedFinishDate: LocalDate
)

data class WhatIfDeadlineResult(
    val targetDeadline: LocalDate,
    val remainingWorkMinutes: Long,
    val eligibleWorkingDaysCount: Int,
    val requiredMinutesPerDay: Long,
    val isAchievableWithinCapacity: Boolean,
    val dailyCapacityLimitMinutes: Long
)

object WhatIfSimulator {

    /**
     * Scenario A: If I focus X minutes per day on eligible days, when will I finish?
     */
    fun simulateFinishDate(
        dailyFocusMinutes: Long,
        remainingWorkMinutes: Long,
        excludedDaysOfWeek: Set<DayOfWeek> = setOf(DayOfWeek.SUNDAY),
        startDate: LocalDate = LocalDate.now()
    ): WhatIfPaceToDateResult {
        if (dailyFocusMinutes <= 0 || remainingWorkMinutes <= 0) {
            return WhatIfPaceToDateResult(
                dailyMinutes = dailyFocusMinutes,
                remainingWorkMinutes = remainingWorkMinutes,
                eligibleDaysNeeded = 0,
                calendarDaysNeeded = 0,
                projectedFinishDate = startDate
            )
        }

        var remaining = remainingWorkMinutes
        var curDate = startDate
        var eligibleDays = 0L
        var totalCalendarDays = 0L

        while (remaining > 0) {
            curDate = curDate.plusDays(1)
            totalCalendarDays++
            if (!excludedDaysOfWeek.contains(curDate.dayOfWeek)) {
                eligibleDays++
                remaining -= dailyFocusMinutes
            }
        }

        return WhatIfPaceToDateResult(
            dailyMinutes = dailyFocusMinutes,
            remainingWorkMinutes = remainingWorkMinutes,
            eligibleDaysNeeded = eligibleDays,
            calendarDaysNeeded = totalCalendarDays,
            projectedFinishDate = curDate
        )
    }

    /**
     * Scenario B: I need to finish by deadline. How much focus time per day do I need?
     */
    fun simulateRequiredPaceForDeadline(
        deadline: LocalDate,
        remainingWorkMinutes: Long,
        excludedDaysOfWeek: Set<DayOfWeek> = setOf(DayOfWeek.SUNDAY),
        dailyCapacityLimitMinutes: Long = 480, // 8h max
        startDate: LocalDate = LocalDate.now()
    ): WhatIfDeadlineResult {
        if (deadline.isBefore(startDate) || remainingWorkMinutes <= 0) {
            return WhatIfDeadlineResult(
                targetDeadline = deadline,
                remainingWorkMinutes = remainingWorkMinutes,
                eligibleWorkingDaysCount = 0,
                requiredMinutesPerDay = 0,
                isAchievableWithinCapacity = false,
                dailyCapacityLimitMinutes = dailyCapacityLimitMinutes
            )
        }

        var cur = startDate.plusDays(1)
        var eligibleDays = 0
        while (!cur.isAfter(deadline)) {
            if (!excludedDaysOfWeek.contains(cur.dayOfWeek)) {
                eligibleDays++
            }
            cur = cur.plusDays(1)
        }

        if (eligibleDays == 0) {
            return WhatIfDeadlineResult(
                targetDeadline = deadline,
                remainingWorkMinutes = remainingWorkMinutes,
                eligibleWorkingDaysCount = 0,
                requiredMinutesPerDay = remainingWorkMinutes,
                isAchievableWithinCapacity = false,
                dailyCapacityLimitMinutes = dailyCapacityLimitMinutes
            )
        }

        val requiredPerDay = Math.round(remainingWorkMinutes.toDouble() / eligibleDays.toDouble())
        val isAchievable = requiredPerDay <= dailyCapacityLimitMinutes

        return WhatIfDeadlineResult(
            targetDeadline = deadline,
            remainingWorkMinutes = remainingWorkMinutes,
            eligibleWorkingDaysCount = eligibleDays,
            requiredMinutesPerDay = requiredPerDay,
            isAchievableWithinCapacity = isAchievable,
            dailyCapacityLimitMinutes = dailyCapacityLimitMinutes
        )
    }
}
