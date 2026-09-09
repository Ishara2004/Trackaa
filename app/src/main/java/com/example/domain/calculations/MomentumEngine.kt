package com.example.domain.calculations

data class MomentumScoreBreakdown(
    val totalScore: Int, // 0 - 100
    val targetAdherenceScore: Double, // 0 - 100 (50% weight)
    val consistencyScore: Double,       // 0 - 100 (30% weight)
    val recentTrendScore: Double,       // 0 - 100 (20% weight)
    val recent7DayAverageMinutes: Long,
    val previous7DayAverageMinutes: Long,
    val eligibleDaysMeetingThreshold: Int,
    val totalEligibleDaysEvaluated: Int,
    val label: String
)

object MomentumEngine {

    /**
     * Calculates transparent Trackaa Momentum Score (0-100)
     */
    fun calculateMomentumScore(
        actualMinutesToDate: Long,
        expectedMinutesToDate: Long,
        eligibleDaysMeetingThreshold: Int,
        totalEligibleDaysInWindow: Int,
        recent7DayDailyAverage: Double,
        previous7DayDailyAverage: Double
    ): MomentumScoreBreakdown {
        // Component 1: 50% Target Adherence
        val expected = if (expectedMinutesToDate > 0) expectedMinutesToDate.toDouble() else 1.0
        val targetAdherence = Math.min(100.0, Math.max(0.0, (actualMinutesToDate.toDouble() / expected) * 100.0))

        // Component 2: 30% Consistency
        val totalDays = Math.max(1, totalEligibleDaysInWindow)
        val consistency = Math.min(100.0, Math.max(0.0, (eligibleDaysMeetingThreshold.toDouble() / totalDays.toDouble()) * 100.0))

        // Component 3: 20% Recent Trend
        val prevAvg = Math.max(1.0, previous7DayDailyAverage)
        val rawTrend = 50.0 + (100.0 * (recent7DayDailyAverage - previous7DayDailyAverage) / prevAvg)
        val trend = Math.min(100.0, Math.max(0.0, rawTrend))

        val combined = (targetAdherence * 0.50) + (consistency * 0.30) + (trend * 0.20)
        val finalScore = Math.round(combined).toInt().coerceIn(0, 100)

        val label = when {
            finalScore >= 85 -> "Peak Momentum"
            finalScore >= 70 -> "Strong Momentum"
            finalScore >= 50 -> "Steady Pace"
            finalScore >= 30 -> "Building Rhythm"
            else -> "Needs Rebound"
        }

        return MomentumScoreBreakdown(
            totalScore = finalScore,
            targetAdherenceScore = targetAdherence,
            consistencyScore = consistency,
            recentTrendScore = trend,
            recent7DayAverageMinutes = Math.round(recent7DayDailyAverage),
            previous7DayAverageMinutes = Math.round(previous7DayDailyAverage),
            eligibleDaysMeetingThreshold = eligibleDaysMeetingThreshold,
            totalEligibleDaysEvaluated = totalDays,
            label = label
        )
    }
}
