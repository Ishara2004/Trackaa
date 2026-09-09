package com.example.domain.calculations

import java.time.LocalDate

data class StreakResult(
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val isStreakActiveToday: Boolean,
    val streakDates: Set<LocalDate>
)

data class LevelProgress(
    val currentLevel: Int,
    val totalXp: Int,
    val xpForCurrentLevel: Int,
    val xpForNextLevel: Int,
    val progressPercent: Double
)

object GamificationConstants {
    const val XP_PER_FOCUS_MINUTE = 1
    const val XP_MINIMUM_TARGET_BONUS = 50
    const val XP_GOAL_TARGET_BONUS = 100
    const val XP_STRETCH_TARGET_BONUS = 200
    const val XP_STREAK_DAY_BONUS = 75
    const val XP_ACHIEVEMENT_UNLOCK = 150
}

object StreakAndGamification {

    /**
     * Calculates streak history based on verified focus minutes per calendar date
     */
    fun calculateStreak(
        dailyFocusMinutes: Map<LocalDate, Long>,
        streakThresholdMinutes: Long,
        currentDate: LocalDate = LocalDate.now()
    ): StreakResult {
        if (dailyFocusMinutes.isEmpty() || streakThresholdMinutes <= 0) {
            return StreakResult(0, 0, false, emptySet())
        }

        val streakDates = dailyFocusMinutes.filter { it.value >= streakThresholdMinutes }.keys.toSet()
        val isTodayDone = streakDates.contains(currentDate)

        // Calculate current streak
        var currentStreak = 0
        var checkDate = if (isTodayDone) currentDate else currentDate.minusDays(1)

        while (streakDates.contains(checkDate)) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        // Calculate longest historical streak
        val sortedDates = streakDates.sorted()
        var longestStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null

        for (date in sortedDates) {
            if (prevDate == null || date == prevDate.plusDays(1)) {
                tempStreak++
            } else {
                tempStreak = 1
            }
            if (tempStreak > longestStreak) {
                longestStreak = tempStreak
            }
            prevDate = date
        }

        return StreakResult(
            currentStreakDays = currentStreak,
            longestStreakDays = Math.max(currentStreak, longestStreak),
            isStreakActiveToday = isTodayDone,
            streakDates = streakDates
        )
    }

    /**
     * Calculates level from total accumulated XP (deterministic progression curve)
     */
    fun calculateLevel(totalXp: Int): LevelProgress {
        val safeXp = Math.max(0, totalXp)
        var level = 1
        var accumulatedXp = 0
        var xpForNext = 500

        while (safeXp >= accumulatedXp + xpForNext) {
            accumulatedXp += xpForNext
            level++
            xpForNext = level * 500
        }

        val xpInLevel = safeXp - accumulatedXp
        val progressPct = (xpInLevel.toDouble() / xpForNext.toDouble()) * 100.0

        return LevelProgress(
            currentLevel = level,
            totalXp = safeXp,
            xpForCurrentLevel = accumulatedXp,
            xpForNextLevel = accumulatedXp + xpForNext,
            progressPercent = progressPct.coerceIn(0.0, 100.0)
        )
    }
}
