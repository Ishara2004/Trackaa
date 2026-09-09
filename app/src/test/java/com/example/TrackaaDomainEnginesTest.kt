package com.example

import com.example.data.backup.BackupCrypto
import com.example.domain.calculations.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class TrackaaDomainEnginesTest {

    @Test
    fun testDurationCalculatorMidnightSplit() {
        val zoneId = ZoneId.of("UTC")
        val date1 = LocalDate.of(2026, 9, 8)
        val date2 = LocalDate.of(2026, 9, 9)

        // Session starting at 23:30 on Sept 8 and ending at 01:15 on Sept 9 (105 mins total)
        val startMs = ZonedDateTime.of(2026, 9, 8, 23, 30, 0, 0, zoneId).toInstant().toEpochMilli()
        val endMs = ZonedDateTime.of(2026, 9, 9, 1, 15, 0, 0, zoneId).toInstant().toEpochMilli()

        val split = DurationCalculator.splitIntervalByCalendarDays(startMs, endMs, zoneId)

        assertEquals(2, split.size)
        assertEquals(30L, split[date1]) // 23:30 to 00:00 = 30 mins
        assertEquals(75L, split[date2]) // 00:00 to 01:15 = 75 mins
        assertEquals(105L, split.values.sum())
    }

    @Test
    fun testTargetEngineTiers() {
        val min = 120L
        val goal = 240L
        val stretch = 360L

        val resBelowMin = TargetEngine.calculateProgress(60L, min, goal, stretch)
        assertEquals(TargetTier.NONE, resBelowMin.tier)

        val resMin = TargetEngine.calculateProgress(150L, min, goal, stretch)
        assertEquals(TargetTier.MINIMUM, resMin.tier)

        val resGoal = TargetEngine.calculateProgress(250L, min, goal, stretch)
        assertEquals(TargetTier.GOAL, resGoal.tier)

        val resStretch = TargetEngine.calculateProgress(400L, min, goal, stretch)
        assertEquals(TargetTier.STRETCH, resStretch.tier)
    }

    @Test
    fun testFocusDebtAndRecoveryPlan() {
        // Expected 20h (1200 mins), Actual 15h (900 mins) -> 300 mins debt
        val debt = TargetEngine.calculateDebtOrCredit(1200L, 900L)
        assertTrue(debt.isDebt)
        assertEquals(300L, debt.differenceMinutes)

        val upcomingDays = (1..5).map { i ->
            val d = LocalDate.of(2026, 9, 10).plusDays(i.toLong())
            DayCapacity(
                date = d,
                dayOfWeek = d.dayOfWeek.value,
                isAvailable = true,
                capacityMinutes = 360L
            )
        }

        val recovery = RecoveryPlanEngine.generateRecoveryPlan(debt.differenceMinutes, upcomingDays)
        assertTrue(recovery.dailyRecoveries.isNotEmpty())
        val totalRecovered = recovery.dailyRecoveries.sumOf { it.recoveryAddedMinutes }
        assertEquals(300L, totalRecovered)
    }

    @Test
    fun testMomentumScoreBounded() {
        val momentum = MomentumEngine.calculateMomentumScore(
            actualMinutesToDate = 1500L,
            expectedMinutesToDate = 1680L,
            eligibleDaysMeetingThreshold = 6,
            totalEligibleDaysInWindow = 7,
            recent7DayDailyAverage = 214.0,
            previous7DayDailyAverage = 190.0
        )

        assertTrue(momentum.totalScore in 0..100)
        assertTrue(momentum.targetAdherenceScore in 0.0..100.0)
        assertTrue(momentum.consistencyScore in 0.0..100.0)
        assertTrue(momentum.recentTrendScore in 0.0..100.0)
        assertNotNull(momentum.label)
    }

    @Test
    fun testStreakAndGamificationProgression() {
        val today = LocalDate.of(2026, 9, 8)
        val dailyMinutes = mapOf(
            today to 120L,
            today.minusDays(1) to 180L,
            today.minusDays(2) to 90L,
            today.minusDays(3) to 200L
        )

        val streak = StreakAndGamification.calculateStreak(dailyMinutes, 60L, today)
        assertEquals(4, streak.currentStreakDays)
        assertTrue(streak.isStreakActiveToday)

        val level1 = StreakAndGamification.calculateLevel(300)
        assertEquals(1, level1.currentLevel)

        val level2 = StreakAndGamification.calculateLevel(750)
        assertEquals(2, level2.currentLevel)
    }

    @Test
    fun testBackupCryptoRoundTrip() {
        val passphrase = "SecureMasterPassword123!".toCharArray()
        val plainJson = """{"version":1,"goals":[{"id":1,"title":"Master Operating Systems"}]}"""

        val encryptedPayload = BackupCrypto.encrypt(plainJson, passphrase)
        assertNotNull(encryptedPayload)
        assertFalse(encryptedPayload.ciphertextBase64.contains("Master Operating Systems"))

        val decryptedJson = BackupCrypto.decrypt(encryptedPayload, passphrase)
        assertEquals(plainJson, decryptedJson)
    }
}
