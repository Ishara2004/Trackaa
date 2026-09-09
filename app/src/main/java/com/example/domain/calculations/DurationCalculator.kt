package com.example.domain.calculations

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object DurationCalculator {

    /**
     * Splits a time interval [startEpochMs, endEpochMs] across midnight boundaries into separate
     * calendar day contributions.
     * Example: 23:30 to 01:30 -> Day 1 gets 30m, Day 2 gets 90m.
     * Returns Map<LocalDate, Long> where value is verified focus minutes on that date.
     */
    fun splitIntervalByCalendarDays(
        startEpochMs: Long,
        endEpochMs: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Map<LocalDate, Long> {
        if (startEpochMs >= endEpochMs) return emptyMap()

        val result = mutableMapOf<LocalDate, Long>()
        var currentStart = LocalDateTime.ofInstant(Instant.ofEpochMilli(startEpochMs), zoneId)
        val endDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endEpochMs), zoneId)

        while (currentStart.toLocalDate().isBefore(endDateTime.toLocalDate())) {
            val endOfDay = currentStart.toLocalDate().atTime(23, 59, 59, 999_000_000)
            val minutesInDay = ChronoUnit.MINUTES.between(currentStart, endOfDay) + 1
            val date = currentStart.toLocalDate()
            result[date] = (result[date] ?: 0L) + Math.max(0, minutesInDay)
            currentStart = currentStart.toLocalDate().plusDays(1).atStartOfDay()
        }

        // Final day segment
        val remainingMinutes = ChronoUnit.MINUTES.between(currentStart, endDateTime)
        val finalDate = currentStart.toLocalDate()
        result[finalDate] = (result[finalDate] ?: 0L) + Math.max(0, remainingMinutes)

        return result
    }

    /**
     * Format minutes into readable human string: e.g. "5h 26m" or "47m"
     */
    fun formatMinutesHuman(totalMinutes: Long): String {
        if (totalMinutes <= 0) return "0m"
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    /**
     * Format seconds into digital timer display: "HH:MM:SS" or "MM:SS"
     */
    fun formatSecondsTimer(totalSeconds: Long): String {
        val sec = Math.max(0L, totalSeconds)
        val hours = sec / 3600
        val minutes = (sec % 3600) / 60
        val seconds = sec % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Format time range: e.g. "05:00–06:10"
     */
    fun formatTimeRange(startEpochMs: Long, endEpochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(startEpochMs), zoneId)
        val end = LocalDateTime.ofInstant(Instant.ofEpochMilli(endEpochMs), zoneId)
        return String.format("%02d:%02d–%02d:%02d", start.hour, start.minute, end.hour, end.minute)
    }
}
