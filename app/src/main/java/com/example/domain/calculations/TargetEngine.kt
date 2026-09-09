package com.example.domain.calculations

enum class TargetTier {
    NONE,
    MINIMUM,
    GOAL,
    STRETCH
}

data class TargetProgressResult(
    val targetMinutes: Long,
    val actualMinutes: Long,
    val remainingMinutes: Long,
    val rawProgressPercent: Double,
    val tier: TargetTier,
    val minMinutes: Long,
    val goalMinutes: Long,
    val stretchMinutes: Long,
    val isGoalAchieved: Boolean,
    val isStretchAchieved: Boolean
)

data class FocusDebtResult(
    val expectedMinutes: Long,
    val actualMinutes: Long,
    val isDebt: Boolean, // true if behind (Debt), false if ahead (Credit)
    val differenceMinutes: Long
)

object TargetEngine {

    /**
     * Calculates target progress with support for overachievement (> 100%) and tier classification
     */
    fun calculateProgress(
        actualMinutes: Long,
        minMinutes: Long,
        goalMinutes: Long,
        stretchMinutes: Long
    ): TargetProgressResult {
        val target = if (goalMinutes > 0) goalMinutes else if (minMinutes > 0) minMinutes else 1L
        val remaining = Math.max(0L, target - actualMinutes)
        val rawPercent = (actualMinutes.toDouble() / target.toDouble()) * 100.0

        val tier = when {
            stretchMinutes > 0 && actualMinutes >= stretchMinutes -> TargetTier.STRETCH
            goalMinutes > 0 && actualMinutes >= goalMinutes -> TargetTier.GOAL
            minMinutes > 0 && actualMinutes >= minMinutes -> TargetTier.MINIMUM
            else -> TargetTier.NONE
        }

        return TargetProgressResult(
            targetMinutes = target,
            actualMinutes = actualMinutes,
            remainingMinutes = remaining,
            rawProgressPercent = rawPercent,
            tier = tier,
            minMinutes = minMinutes,
            goalMinutes = goalMinutes,
            stretchMinutes = stretchMinutes,
            isGoalAchieved = goalMinutes > 0 && actualMinutes >= goalMinutes,
            isStretchAchieved = stretchMinutes > 0 && actualMinutes >= stretchMinutes
        )
    }

    /**
     * Calculates Focus Debt or Focus Credit based on expected vs actual progress
     */
    fun calculateDebtOrCredit(
        expectedMinutesToDate: Long,
        actualMinutesToDate: Long
    ): FocusDebtResult {
        val diff = actualMinutesToDate - expectedMinutesToDate
        return if (diff < 0) {
            // Expected > Actual: Debt
            FocusDebtResult(
                expectedMinutes = expectedMinutesToDate,
                actualMinutes = actualMinutesToDate,
                isDebt = true,
                differenceMinutes = Math.abs(diff)
            )
        } else {
            // Actual >= Expected: Credit
            FocusDebtResult(
                expectedMinutes = expectedMinutesToDate,
                actualMinutes = actualMinutesToDate,
                isDebt = false,
                differenceMinutes = diff
            )
        }
    }
}
