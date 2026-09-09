package com.example.domain.calculations

import java.time.LocalDate

data class RecoveryDayAllocation(
    val date: LocalDate,
    val dayOfWeekName: String,
    val basePlannedMinutes: Long,
    val recoveryAddedMinutes: Long,
    val totalMinutes: Long,
    val maxCapacityMinutes: Long
)

data class RecoveryPlanResult(
    val debtMinutes: Long,
    val isRecoverable: Boolean,
    val totalRecoverableCapacityMinutes: Long,
    val unrecoverableDeficitMinutes: Long,
    val dailyRecoveries: List<RecoveryDayAllocation>,
    val guidanceMessage: String
)

object RecoveryPlanEngine {

    /**
     * Generates a deterministic rules-based recovery schedule for focus debt
     */
    fun generateRecoveryPlan(
        focusDebtMinutes: Long,
        upcomingDays: List<DayCapacity>, // day date, capacity, isAvailable
        currentPlannedUsage: Map<LocalDate, Long> = emptyMap() // already committed planned minutes per day
    ): RecoveryPlanResult {
        if (focusDebtMinutes <= 0) {
            return RecoveryPlanResult(
                debtMinutes = 0,
                isRecoverable = true,
                totalRecoverableCapacityMinutes = 0,
                unrecoverableDeficitMinutes = 0,
                dailyRecoveries = emptyList(),
                guidanceMessage = "You are on track! No focus recovery needed."
            )
        }

        val availableDays = upcomingDays.filter { it.isAvailable && it.capacityMinutes > 0 }
        if (availableDays.isEmpty()) {
            return RecoveryPlanResult(
                debtMinutes = focusDebtMinutes,
                isRecoverable = false,
                totalRecoverableCapacityMinutes = 0,
                unrecoverableDeficitMinutes = focusDebtMinutes,
                dailyRecoveries = emptyList(),
                guidanceMessage = "No eligible working days available to recover focus debt. Please configure working days."
            )
        }

        // Calculate spare headroom for each day: capacityMinutes - currentlyPlanned
        val headroomList = availableDays.map { day ->
            val planned = currentPlannedUsage[day.date] ?: 0L
            val spare = Math.max(0L, day.capacityMinutes - planned)
            Triple(day, planned, spare)
        }

        val totalSpareCapacity = headroomList.sumOf { it.third }
        val isRecoverable = totalSpareCapacity >= focusDebtMinutes
        val unrecoverable = if (isRecoverable) 0L else focusDebtMinutes - totalSpareCapacity

        val recoveries = mutableListOf<RecoveryDayAllocation>()
        var remainingDebtToDistribute = focusDebtMinutes

        for (item in headroomList) {
            val (day, planned, spare) = item
            if (spare <= 0L || remainingDebtToDistribute <= 0L) {
                recoveries.add(
                    RecoveryDayAllocation(
                        date = day.date,
                        dayOfWeekName = day.date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() },
                        basePlannedMinutes = planned,
                        recoveryAddedMinutes = 0,
                        totalMinutes = planned,
                        maxCapacityMinutes = day.capacityMinutes
                    )
                )
                continue
            }

            // Distribute proportionally or fill headroom
            val share = if (isRecoverable && totalSpareCapacity > 0) {
                Math.min(spare, Math.round(focusDebtMinutes * (spare.toDouble() / totalSpareCapacity.toDouble())))
            } else {
                Math.min(spare, remainingDebtToDistribute)
            }

            val allocated = Math.min(share, remainingDebtToDistribute)
            remainingDebtToDistribute -= allocated

            recoveries.add(
                RecoveryDayAllocation(
                    date = day.date,
                    dayOfWeekName = day.date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() },
                    basePlannedMinutes = planned,
                    recoveryAddedMinutes = allocated,
                    totalMinutes = planned + allocated,
                    maxCapacityMinutes = day.capacityMinutes
                )
            )
        }

        // If some debt remains due to rounding, assign to first day with spare capacity
        if (isRecoverable && remainingDebtToDistribute > 0L) {
            for (i in recoveries.indices) {
                val r = recoveries[i]
                val availableSpace = r.maxCapacityMinutes - r.totalMinutes
                if (availableSpace > 0) {
                    val add = Math.min(availableSpace, remainingDebtToDistribute)
                    recoveries[i] = r.copy(
                        recoveryAddedMinutes = r.recoveryAddedMinutes + add,
                        totalMinutes = r.totalMinutes + add
                    )
                    remainingDebtToDistribute -= add
                    if (remainingDebtToDistribute <= 0) break
                }
            }
        }

        val guidance = if (isRecoverable) {
            "Achievable recovery plan: distributed ${DurationCalculator.formatMinutesHuman(focusDebtMinutes)} across ${recoveries.count { it.recoveryAddedMinutes > 0 }} days within capacity."
        } else {
            "Your current capacity cannot satisfy this deadline (deficit of ${DurationCalculator.formatMinutesHuman(unrecoverable)}). You need either more available hours, a lower target, or an extended deadline."
        }

        return RecoveryPlanResult(
            debtMinutes = focusDebtMinutes,
            isRecoverable = isRecoverable,
            totalRecoverableCapacityMinutes = totalSpareCapacity,
            unrecoverableDeficitMinutes = unrecoverable,
            dailyRecoveries = recoveries,
            guidanceMessage = guidance
        )
    }
}
