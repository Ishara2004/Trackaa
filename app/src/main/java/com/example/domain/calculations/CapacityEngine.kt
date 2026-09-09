package com.example.domain.calculations

import java.time.LocalDate

data class DayCapacity(
    val date: LocalDate,
    val dayOfWeek: Int, // 1=Mon, 7=Sun
    val isAvailable: Boolean,
    val capacityMinutes: Long
)

data class CapacityPlanResult(
    val remainingWorkMinutes: Long,
    val remainingEligibleDaysCount: Int,
    val totalAvailableCapacityMinutes: Long,
    val isFeasible: Boolean,
    val capacityDeficitMinutes: Long,
    val requiredMinutesPerEligibleDay: Long,
    val requiredMinutesPerWeek: Long,
    val plannedAllocations: Map<LocalDate, Long> // Date -> assigned minutes
)

object CapacityEngine {

    /**
     * Calculates capacity-aware allocation of remaining workload across remaining eligible days
     */
    fun calculateCapacityPlan(
        remainingWorkMinutes: Long,
        eligibleDays: List<DayCapacity>
    ): CapacityPlanResult {
        if (remainingWorkMinutes <= 0) {
            return CapacityPlanResult(
                remainingWorkMinutes = 0,
                remainingEligibleDaysCount = eligibleDays.count { it.isAvailable && it.capacityMinutes > 0 },
                totalAvailableCapacityMinutes = eligibleDays.filter { it.isAvailable }.sumOf { it.capacityMinutes },
                isFeasible = true,
                capacityDeficitMinutes = 0,
                requiredMinutesPerEligibleDay = 0,
                requiredMinutesPerWeek = 0,
                plannedAllocations = emptyMap()
            )
        }

        val availableDays = eligibleDays.filter { it.isAvailable && it.capacityMinutes > 0 }
        val eligibleDaysCount = availableDays.size
        val totalCapacity = availableDays.sumOf { it.capacityMinutes }

        if (eligibleDaysCount == 0 || totalCapacity == 0L) {
            return CapacityPlanResult(
                remainingWorkMinutes = remainingWorkMinutes,
                remainingEligibleDaysCount = 0,
                totalAvailableCapacityMinutes = 0,
                isFeasible = false,
                capacityDeficitMinutes = remainingWorkMinutes,
                requiredMinutesPerEligibleDay = 0,
                requiredMinutesPerWeek = 0,
                plannedAllocations = emptyMap()
            )
        }

        val isFeasible = totalCapacity >= remainingWorkMinutes
        val deficit = if (isFeasible) 0L else remainingWorkMinutes - totalCapacity
        val requiredPerDay = Math.round(remainingWorkMinutes.toDouble() / eligibleDaysCount.toDouble())
        val requiredPerWeek = requiredPerDay * Math.min(7, eligibleDaysCount)

        // Distribute proportionally to capacity
        val allocations = mutableMapOf<LocalDate, Long>()
        var distributedMinutes = 0L

        for (i in availableDays.indices) {
            val day = availableDays[i]
            if (i == availableDays.size - 1) {
                // Assign remainder safely on last day
                val remainder = Math.max(0L, remainingWorkMinutes - distributedMinutes)
                allocations[day.date] = remainder
            } else {
                val ratio = day.capacityMinutes.toDouble() / totalCapacity.toDouble()
                val dayAlloc = Math.round(remainingWorkMinutes * ratio)
                allocations[day.date] = dayAlloc
                distributedMinutes += dayAlloc
            }
        }

        return CapacityPlanResult(
            remainingWorkMinutes = remainingWorkMinutes,
            remainingEligibleDaysCount = eligibleDaysCount,
            totalAvailableCapacityMinutes = totalCapacity,
            isFeasible = isFeasible,
            capacityDeficitMinutes = deficit,
            requiredMinutesPerEligibleDay = requiredPerDay,
            requiredMinutesPerWeek = requiredPerWeek,
            plannedAllocations = allocations
        )
    }
}
