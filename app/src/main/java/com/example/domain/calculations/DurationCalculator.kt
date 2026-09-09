package com.example.domain.calculations

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object DurationCalculator {
    /** Splits an exact interval across local calendar days without counting pauses or breaks. */
    fun splitIntervalByCalendarDaysSeconds(
        startEpochMs: Long,
        endEpochMs: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Map<LocalDate, Long> {
        if (startEpochMs >= endEpochMs) return emptyMap()
        val result = linkedMapOf<LocalDate, Long>()
        var cursor = Instant.ofEpochMilli(startEpochMs).atZone(zoneId)
        val end = Instant.ofEpochMilli(endEpochMs).atZone(zoneId)
        while (cursor.toLocalDate().isBefore(end.toLocalDate())) {
            val nextMidnight = cursor.toLocalDate().plusDays(1).atStartOfDay(zoneId)
            val seconds = ChronoUnit.SECONDS.between(cursor, nextMidnight).coerceAtLeast(0L)
            result[cursor.toLocalDate()] = (result[cursor.toLocalDate()] ?: 0L) + seconds
            cursor = nextMidnight
        }
        val seconds = ChronoUnit.SECONDS.between(cursor, end).coerceAtLeast(0L)
        result[cursor.toLocalDate()] = (result[cursor.toLocalDate()] ?: 0L) + seconds
        return result
    }

    /** Compatibility minute view; authoritative calculations should prefer the seconds variant. */
    fun splitIntervalByCalendarDays(startEpochMs: Long, endEpochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): Map<LocalDate, Long> =
        splitIntervalByCalendarDaysSeconds(startEpochMs, endEpochMs, zoneId).mapValues { it.value / 60L }

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

    fun formatSecondsHuman(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val h = safe / 3600
        val m = (safe % 3600) / 60
        val s = safe % 60
        return when {
            h > 0 -> if (s > 0) "${h}h ${m}m ${s}s" else "${h}h ${m}m"
            m > 0 -> if (s > 0) "${m}m ${s}s" else "${m}m"
            else -> "${s}s"
        }
    }

    fun formatSecondsTimer(totalSeconds: Long): String {
        val sec = totalSeconds.coerceAtLeast(0L)
        val hours = sec / 3600
        val minutes = (sec % 3600) / 60
        val seconds = sec % 60
        return if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else String.format("%02d:%02d", minutes, seconds)
    }

    fun formatTimeRange(startEpochMs: Long, endEpochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val start = Instant.ofEpochMilli(startEpochMs).atZone(zoneId)
        val end = Instant.ofEpochMilli(endEpochMs).atZone(zoneId)
        return String.format("%02d:%02d–%02d:%02d", start.hour, start.minute, end.hour, end.minute)
    }
}
