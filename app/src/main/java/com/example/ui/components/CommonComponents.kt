package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus

@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subValue: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subValue != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
fun ProgressBarWithTiers(
    currentMinutes: Long,
    minMinutes: Long,
    goalMinutes: Long,
    stretchMinutes: Long,
    modifier: Modifier = Modifier
) {
    val maxScale = Math.max(
        currentMinutes.toDouble(),
        if (stretchMinutes > 0) stretchMinutes.toDouble() else if (goalMinutes > 0) goalMinutes.toDouble() else 100.0
    )

    val currentFraction = (currentMinutes.toDouble() / maxScale).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = currentFraction, label = "progress")

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        when {
                            stretchMinutes > 0 && currentMinutes >= stretchMinutes -> MaterialTheme.colorScheme.tertiary
                            goalMinutes > 0 && currentMinutes >= goalMinutes -> MaterialTheme.colorScheme.primary
                            minMinutes > 0 && currentMinutes >= minMinutes -> Color(0xFF10B981) // Green success
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        }
                    )
            )

            // Goal marker tick
            if (goalMinutes > 0 && maxScale > 0) {
                val goalFraction = (goalMinutes.toDouble() / maxScale).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .align(Alignment.CenterStart)
                        .padding(start = (goalFraction * 200).dp) // approximate visual mark
                        .background(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Min: ${minMinutes / 60}h",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Goal: ${goalMinutes / 60}h",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (stretchMinutes > 0) {
                Text(
                    text = "Stretch: ${stretchMinutes / 60}h",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun TaskPriorityBadge(priority: TaskPriority) {
    val (bgColor, textColor) = when (priority) {
        TaskPriority.CRITICAL -> Pair(Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFEF4444))
        TaskPriority.HIGH -> Pair(Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFF59E0B))
        TaskPriority.MEDIUM -> Pair(Color(0xFF38BDF8).copy(alpha = 0.15f), Color(0xFF38BDF8))
        TaskPriority.LOW -> Pair(Color(0xFF94A3B8).copy(alpha = 0.15f), Color(0xFF94A3B8))
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = priority.name,
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun TaskStatusBadge(status: TaskStatus) {
    val (bgColor, textColor) = when (status) {
        TaskStatus.COMPLETED -> Pair(Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF10B981))
        TaskStatus.IN_PROGRESS -> Pair(Color(0xFF38BDF8).copy(alpha = 0.15f), Color(0xFF38BDF8))
        TaskStatus.BLOCKED -> Pair(Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFEF4444))
        TaskStatus.PLANNED -> Pair(Color(0xFF64748B).copy(alpha = 0.15f), Color(0xFF64748B))
        TaskStatus.BACKLOG -> Pair(Color(0xFF475569).copy(alpha = 0.15f), Color(0xFF94A3B8))
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = status.name.replace("_", " "),
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
