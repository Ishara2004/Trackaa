package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FocusEngineStatus
import com.example.data.model.SessionMode
import com.example.domain.calculations.DurationCalculator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ChronographDial(
    elapsedSeconds: Long,
    isOvertime: Boolean,
    overtimeSeconds: Long,
    status: FocusEngineStatus,
    mode: SessionMode,
    plannedSeconds: Long,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val overtimeColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val dialColor = if (isOvertime) overtimeColor else primaryColor

    Box(
        modifier = modifier
            .size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 16.dp.toPx()

            // Outer subtle track
            drawCircle(
                color = trackColor.copy(alpha = 0.5f),
                radius = radius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )

            // Chronograph 60-second or 60-minute tick marks
            val tickCount = 60
            for (i in 0 until tickCount) {
                val angleRad = (i * 6 - 90) * (PI / 180.0)
                val isMajor = i % 5 == 0
                val tickLen = if (isMajor) 10.dp.toPx() else 4.dp.toPx()
                val tickWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                val tickAlpha = if (isMajor) 0.8f else 0.3f

                val startX = center.x + (radius - tickLen) * cos(angleRad).toFloat()
                val startY = center.y + (radius - tickLen) * sin(angleRad).toFloat()
                val endX = center.x + radius * cos(angleRad).toFloat()
                val endY = center.y + radius * sin(angleRad).toFloat()

                drawLine(
                    color = if (isOvertime) overtimeColor.copy(alpha = tickAlpha) else primaryColor.copy(alpha = tickAlpha),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = tickWidth
                )
            }

            // Progress Arc
            val sweepAngle = when (mode) {
                SessionMode.COUNTDOWN -> {
                    if (plannedSeconds > 0) {
                        val remaining = plannedSeconds - elapsedSeconds
                        if (remaining > 0) {
                            (remaining.toFloat() / plannedSeconds.toFloat()) * 360f
                        } else {
                            // Overtime rotation
                            ((overtimeSeconds % 60) / 60f) * 360f
                        }
                    } else 0f
                }
                else -> {
                    // 60-minute rotation cycle
                    ((elapsedSeconds % 3600) / 3600f) * 360f
                }
            }

            val strokeWidth = 8.dp.toPx()
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        dialColor.copy(alpha = 0.3f),
                        dialColor
                    )
                ),
                startAngle = -90f,
                sweepAngle = if (status == FocusEngineStatus.IDLE) 0f else sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Outer subtle glow pulse when active
            if (status == FocusEngineStatus.FOCUSING) {
                drawCircle(
                    color = dialColor.copy(alpha = 0.05f * pulseAlpha),
                    radius = radius + 8.dp.toPx(),
                    center = center
                )
            }
        }

        // Digital Time Readout inside Dial
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val displayTime = when {
                isOvertime -> DurationCalculator.formatSecondsTimer(overtimeSeconds)
                mode == SessionMode.COUNTDOWN && plannedSeconds > 0 -> {
                    val rem = Math.max(0L, plannedSeconds - elapsedSeconds)
                    DurationCalculator.formatSecondsTimer(rem)
                }
                else -> DurationCalculator.formatSecondsTimer(elapsedSeconds)
            }

            if (isOvertime) {
                Text(
                    text = "OVERTIME",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = overtimeColor
                )
                Spacer(modifier = Modifier.height(2.dp))
            } else {
                Text(
                    text = when (status) {
                        FocusEngineStatus.PAUSED -> "PAUSED"
                        FocusEngineStatus.ON_BREAK -> "ON BREAK"
                        FocusEngineStatus.FOCUSING -> mode.name
                        else -> "READY"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = if (isOvertime) "+$displayTime" else displayTime,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                ),
                color = if (isOvertime) overtimeColor else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    status == FocusEngineStatus.FOCUSING -> "● RECORDING"
                    status == FocusEngineStatus.PAUSED -> "❚❚ PAUSED"
                    status == FocusEngineStatus.ON_BREAK -> "☕ REST INTERVAL"
                    else -> "TAP START TO BEGIN"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = if (status == FocusEngineStatus.FOCUSING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
