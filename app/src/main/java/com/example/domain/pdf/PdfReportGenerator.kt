package com.example.domain.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.entity.FocusSessionEntity
import com.example.domain.calculations.DurationCalculator
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

object PdfReportGenerator {

    fun generateReportPdf(
        context: Context,
        reportTitle: String,
        dateRangeLabel: String,
        sessions: List<FocusSessionEntity>,
        targetMinutes: Long = 0,
        streakDays: Int = 0,
        momentumScore: Int = 0
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (72 dpi)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(100, 116, 139) // Slate 500
            textSize = 12f
            isAntiAlias = true
        }

        val sectionPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 11f
            isAntiAlias = true
        }

        val accentPaint = Paint().apply {
            color = Color.rgb(245, 158, 11) // Amber 500
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        var y = 45f

        // Top Accent Bar
        val barPaint = Paint().apply { color = Color.rgb(14, 165, 233) }
        canvas.drawRect(40f, y, 555f, y + 4f, barPaint)
        y += 28f

        // App Branding & Title
        canvas.drawText("TRACKAA — PERFORMANCE REPORT", 40f, y, titlePaint)
        y += 18f
        canvas.drawText("$reportTitle  |  $dateRangeLabel", 40f, y, subtitlePaint)
        y += 20f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 30f

        // Calculate Core Stats
        val totalFocusMin = sessions.sumOf { it.totalFocusMinutes }
        val sessionCount = sessions.size
        val avgSessionMin = if (sessionCount > 0) totalFocusMin / sessionCount else 0L
        val longestSessionMin = sessions.maxOfOrNull { it.totalFocusMinutes } ?: 0L
        val avgQuality = if (sessionCount > 0) String.format("%.1f", sessions.map { it.focusQuality }.average()) else "N/A"
        val avgEnergy = if (sessionCount > 0) String.format("%.1f", sessions.map { it.energyLevel }.average()) else "N/A"

        // Metric summary grid
        canvas.drawText("PERFORMANCE SUMMARY", 40f, y, sectionPaint)
        y += 22f

        canvas.drawText("Total Focus Time:", 40f, y, bodyPaint)
        canvas.drawText(DurationCalculator.formatMinutesHuman(totalFocusMin), 180f, y, accentPaint)

        canvas.drawText("Completed Sessions:", 320f, y, bodyPaint)
        canvas.drawText("$sessionCount", 460f, y, bodyPaint)
        y += 20f

        canvas.drawText("Average Session:", 40f, y, bodyPaint)
        canvas.drawText(DurationCalculator.formatMinutesHuman(avgSessionMin), 180f, y, bodyPaint)

        canvas.drawText("Longest Session:", 320f, y, bodyPaint)
        canvas.drawText(DurationCalculator.formatMinutesHuman(longestSessionMin), 460f, y, bodyPaint)
        y += 20f

        canvas.drawText("Average Focus Quality:", 40f, y, bodyPaint)
        canvas.drawText("$avgQuality / 5.0", 180f, y, bodyPaint)

        canvas.drawText("Average Energy Level:", 320f, y, bodyPaint)
        canvas.drawText("$avgEnergy / 5.0", 460f, y, bodyPaint)
        y += 20f

        canvas.drawText("Current Streak:", 40f, y, bodyPaint)
        canvas.drawText("$streakDays Days", 180f, y, bodyPaint)

        canvas.drawText("Momentum Score:", 320f, y, bodyPaint)
        canvas.drawText("$momentumScore / 100", 460f, y, bodyPaint)
        y += 28f

        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 26f

        // Target Adherence
        if (targetMinutes > 0) {
            canvas.drawText("TARGET ADHERENCE", 40f, y, sectionPaint)
            y += 20f
            val pct = String.format("%.1f%%", (totalFocusMin.toDouble() / targetMinutes.toDouble()) * 100.0)
            canvas.drawText("Target: ${DurationCalculator.formatMinutesHuman(targetMinutes)}", 40f, y, bodyPaint)
            canvas.drawText("Actual: ${DurationCalculator.formatMinutesHuman(totalFocusMin)}", 200f, y, bodyPaint)
            canvas.drawText("Achievement: $pct", 380f, y, accentPaint)
            y += 26f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 26f
        }

        // Detailed Focus Sessions Table
        canvas.drawText("FOCUS LOG (${sessions.size} SESSIONS)", 40f, y, sectionPaint)
        y += 20f

        // Table Header
        canvas.drawText("Start Time", 40f, y, bodyPaint)
        canvas.drawText("Duration", 180f, y, bodyPaint)
        canvas.drawText("Quality", 280f, y, bodyPaint)
        canvas.drawText("Energy", 360f, y, bodyPaint)
        canvas.drawText("Intent / Notes", 440f, y, bodyPaint)
        y += 12f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 16f

        val sessionsToDisplay = sessions.take(15) // fit cleanly on single A4
        for (session in sessionsToDisplay) {
            val rangeStr = DurationCalculator.formatTimeRange(session.startEpochMs, session.endEpochMs)
            val durStr = DurationCalculator.formatMinutesHuman(session.totalFocusMinutes)
            val notePreview = (session.intent ?: session.notes ?: "—").take(22)

            canvas.drawText(rangeStr, 40f, y, bodyPaint)
            canvas.drawText(durStr, 180f, y, bodyPaint)
            canvas.drawText("${session.focusQuality} ★", 280f, y, bodyPaint)
            canvas.drawText("${session.energyLevel} ⚡", 360f, y, bodyPaint)
            canvas.drawText(notePreview, 440f, y, bodyPaint)
            y += 18f
        }

        if (sessions.size > 15) {
            y += 8f
            canvas.drawText("... and ${sessions.size - 15} additional sessions", 40f, y, subtitlePaint)
        }

        // Footer
        y = 800f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 16f
        canvas.drawText("Generated by Trackaa — Offline Deep Work Operating System on ${LocalDate.now()}", 40f, y, subtitlePaint)

        pdfDocument.finishPage(page)

        val reportDir = File(context.cacheDir, "reports").apply { if (!exists()) mkdirs() }
        val outputFile = File(reportDir, "Trackaa_${reportTitle.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")

        FileOutputStream(outputFile).use { pdfDocument.writeTo(it) }
        pdfDocument.close()

        return outputFile
    }
}
