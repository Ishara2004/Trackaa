package com.example.domain.calculations

import java.time.LocalDate
import kotlin.math.ceil

data class DayCapacity(val date: LocalDate, val dayOfWeek: Int, val isAvailable: Boolean, val capacityMinutes: Long)

data class CapacityPlanResult(
    val remainingWorkMinutes: Long,
    val remainingEligibleDaysCount: Int,
    val totalAvailableCapacityMinutes: Long,
    val isFeasible: Boolean,
    val capacityDeficitMinutes: Long,
    val requiredMinutesPerEligibleDay: Long,
    val requiredMinutesPerWeek: Long,
    val plannedAllocations: Map<LocalDate, Long>
)

object CapacityEngine {
    fun calculateCapacityPlan(remainingWorkMinutes: Long, eligibleDays: List<DayCapacity>): CapacityPlanResult {
        val remaining = remainingWorkMinutes.coerceAtLeast(0L)
        val days = eligibleDays.filter { it.isAvailable && it.capacityMinutes > 0 }.sortedBy { it.date }
        val totalCapacity = days.sumOf { it.capacityMinutes }
        if (remaining == 0L) return CapacityPlanResult(0, days.size, totalCapacity, true, 0, 0, 0, emptyMap())
        if (days.isEmpty() || totalCapacity == 0L) return CapacityPlanResult(remaining, 0, 0, false, remaining, 0, 0, emptyMap())

        val feasible = totalCapacity >= remaining
        val distributable = minOf(remaining, totalCapacity)
        val allocations = linkedMapOf<LocalDate, Long>()
        var allocated = 0L

        days.forEachIndexed { index, day ->
            val remainingToAllocate = distributable - allocated
            if (remainingToAllocate <= 0) {
                allocations[day.date] = 0L
                return@forEachIndexed
            }
            val proposed = if (index == days.lastIndex) remainingToAllocate else {
                ((distributable.toDouble() * day.capacityMinutes.toDouble()) / totalCapacity.toDouble()).toLong()
            }
            val safe = minOf(day.capacityMinutes, proposed.coerceAtLeast(0L), remainingToAllocate)
            allocations[day.date] = safe
            allocated += safe
        }

        // Deterministically distribute any rounding remainder into remaining headroom.
        var remainder = distributable - allocated
        if (remainder > 0) {
            for (day in days) {
                if (remainder == 0L) break
                val current = allocations[day.date] ?: 0L
                val headroom = (day.capacityMinutes - current).coerceAtLeast(0L)
                val add = minOf(headroom, remainder)
                allocations[day.date] = current + add
                remainder -= add
            }
        }

        val requiredPerDay = ceil(remaining.toDouble() / days.size.toDouble()).toLong()
        val firstSeven = days.take(7)
        val requiredPerWeek = firstSeven.sumOf { allocations[it.date] ?: 0L }
        return CapacityPlanResult(
            remainingWorkMinutes = remaining,
            remainingEligibleDaysCount = days.size,
            totalAvailableCapacityMinutes = totalCapacity,
            isFeasible = feasible,
            capacityDeficitMinutes = (remaining - totalCapacity).coerceAtLeast(0L),
            requiredMinutesPerEligibleDay = requiredPerDay,
            requiredMinutesPerWeek = requiredPerWeek,
            plannedAllocations = allocations
        )
    }
}
