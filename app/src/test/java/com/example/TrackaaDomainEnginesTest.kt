package com.example

import com.example.data.backup.BackupCrypto
import com.example.domain.calculations.*
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class TrackaaDomainEnginesTest {

    @Test
    fun exactSecondSplitDoesNotInflateSubMinuteWork() {
        val zone = ZoneId.of("UTC")
        val start = ZonedDateTime.of(2026,9,8,10,0,0,0,zone).toInstant().toEpochMilli()
        val end = start + 37_000L
        val split = DurationCalculator.splitIntervalByCalendarDaysSeconds(start,end,zone)
        assertEquals(37L, split.values.sum())
        assertEquals(0L, DurationCalculator.splitIntervalByCalendarDays(start,end,zone).values.sum())
    }

    @Test
    fun testDurationCalculatorMidnightSplit() {
        val zoneId = ZoneId.of("UTC")
        val date1 = LocalDate.of(2026, 9, 8)
        val date2 = LocalDate.of(2026, 9, 9)
        val startMs = ZonedDateTime.of(2026, 9, 8, 23, 30, 0, 0, zoneId).toInstant().toEpochMilli()
        val endMs = ZonedDateTime.of(2026, 9, 9, 1, 15, 0, 0, zoneId).toInstant().toEpochMilli()
        val splitSeconds = DurationCalculator.splitIntervalByCalendarDaysSeconds(startMs, endMs, zoneId)
        assertEquals(1800L, splitSeconds[date1])
        assertEquals(4500L, splitSeconds[date2])
        assertEquals(6300L, splitSeconds.values.sum())
    }

    @Test
    fun capacityPlanNeverExceedsDailyCapacityEvenWhenInfeasible() {
        val start = LocalDate.of(2026,9,10)
        val days = listOf(
            DayCapacity(start,4,true,120),
            DayCapacity(start.plusDays(1),5,true,60)
        )
        val plan = CapacityEngine.calculateCapacityPlan(300,days)
        assertFalse(plan.isFeasible)
        assertEquals(120L,plan.capacityDeficitMinutes)
        assertEquals(180L,plan.plannedAllocations.values.sum())
        days.forEach { day -> assertTrue((plan.plannedAllocations[day.date] ?: 0L) <= day.capacityMinutes) }
    }

    @Test
    fun forecastUsesCeilingAndSkipsExcludedDays() {
        val friday = LocalDate.of(2026,9,11)
        val history = (0 until 7).associate { friday.minusDays(it.toLong()) to 60L }
        val result = ForecastEngine.calculateForecast(
            historicalDailyMinutes = history,
            eligibleWorkingDaysCount = 7,
            remainingWorkMinutes = 61,
            currentDate = friday,
            excludedDaysOfWeek = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        )
        assertEquals(60L,result.rollingAverageDailyMinutes)
        assertEquals(LocalDate.of(2026,9,15),result.projectedCompletionDate)
    }

    @Test
    fun testTargetEngineTiers() {
        val min = 120L; val goal = 240L; val stretch = 360L
        assertEquals(TargetTier.NONE, TargetEngine.calculateProgress(60L,min,goal,stretch).tier)
        assertEquals(TargetTier.MINIMUM, TargetEngine.calculateProgress(150L,min,goal,stretch).tier)
        assertEquals(TargetTier.GOAL, TargetEngine.calculateProgress(250L,min,goal,stretch).tier)
        assertEquals(TargetTier.STRETCH, TargetEngine.calculateProgress(400L,min,goal,stretch).tier)
    }

    @Test
    fun testFocusDebtAndRecoveryPlan() {
        val debt = TargetEngine.calculateDebtOrCredit(1200L, 900L)
        assertTrue(debt.isDebt); assertEquals(300L, debt.differenceMinutes)
        val upcomingDays = (1..5).map { i ->
            val d = LocalDate.of(2026, 9, 10).plusDays(i.toLong())
            DayCapacity(d,d.dayOfWeek.value,true,360L)
        }
        val recovery = RecoveryPlanEngine.generateRecoveryPlan(debt.differenceMinutes, upcomingDays)
        assertEquals(300L,recovery.dailyRecoveries.sumOf { it.recoveryAddedMinutes })
        assertTrue(recovery.dailyRecoveries.all { it.totalMinutes <= it.maxCapacityMinutes })
    }

    @Test
    fun testMomentumScoreBounded() {
        val momentum = MomentumEngine.calculateMomentumScore(1500L,1680L,6,7,214.0,190.0)
        assertTrue(momentum.totalScore in 0..100)
        assertTrue(momentum.targetAdherenceScore in 0.0..100.0)
        assertTrue(momentum.consistencyScore in 0.0..100.0)
        assertTrue(momentum.recentTrendScore in 0.0..100.0)
    }

    @Test
    fun testStreakAndGamificationProgression() {
        val today=LocalDate.of(2026,9,8)
        val daily=mapOf(today to 120L,today.minusDays(1) to 180L,today.minusDays(2) to 90L,today.minusDays(3) to 200L)
        val streak=StreakAndGamification.calculateStreak(daily,60L,today)
        assertEquals(4,streak.currentStreakDays); assertTrue(streak.isStreakActiveToday)
        assertEquals(1,StreakAndGamification.calculateLevel(300).currentLevel)
        assertEquals(2,StreakAndGamification.calculateLevel(750).currentLevel)
    }

    @Test
    fun testBackupCryptoRoundTrip() {
        val passphrase="SecureMasterPassword123!".toCharArray()
        val plainJson="""{"version":1,"goals":[{"id":1,"title":"Master Operating Systems"}]}"""
        val encrypted=BackupCrypto.encrypt(plainJson,passphrase)
        assertFalse(encrypted.ciphertextBase64.contains("Master Operating Systems"))
        assertEquals(plainJson,BackupCrypto.decrypt(encrypted,passphrase))
    }
}
