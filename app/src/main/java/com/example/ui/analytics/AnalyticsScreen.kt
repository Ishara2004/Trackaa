package com.example.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.calculations.DurationCalculator
import com.example.ui.components.MetricCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var whatIfDailyHours by remember { mutableDoubleStateOf(3.0) }
    var whatIfRemainingHours by remember { mutableDoubleStateOf(25.0) }
    var whatIfDaysAhead by remember { mutableLongStateOf(10L) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ANALYTICS & FORECASTING",
                        style = MaterialTheme.typography.titleMedium.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.testTag("analytics_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Timeframe Segmented Control
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.selectedTimeframe == AnalyticsTimeframe.DAILY,
                        onClick = { viewModel.setTimeframe(AnalyticsTimeframe.DAILY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Day")
                    }
                    SegmentedButton(
                        selected = state.selectedTimeframe == AnalyticsTimeframe.WEEKLY,
                        onClick = { viewModel.setTimeframe(AnalyticsTimeframe.WEEKLY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Week")
                    }
                    SegmentedButton(
                        selected = state.selectedTimeframe == AnalyticsTimeframe.MONTHLY,
                        onClick = { viewModel.setTimeframe(AnalyticsTimeframe.MONTHLY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("Month")
                    }
                }
            }

            // High Information Density Metrics Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            label = "Total Focus",
                            value = DurationCalculator.formatMinutesHuman(state.totalFocusMinutes),
                            icon = Icons.Default.PlayArrow,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Daily Average",
                            value = DurationCalculator.formatMinutesHuman(state.dailyAverageMinutes),
                            icon = Icons.Default.DateRange,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            label = "Longest Session",
                            value = DurationCalculator.formatMinutesHuman(state.longestSessionMinutes),
                            icon = Icons.Default.Star,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Pause Ratio",
                            value = "${state.pauseRatioPercent.toInt()}%",
                            subValue = "${DurationCalculator.formatMinutesHuman(state.totalPauseMinutes)} paused",
                            icon = Icons.Default.Refresh,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            label = "Avg Quality",
                            value = if (state.averageQuality > 0) String.format("%.1f ★", state.averageQuality) else "—",
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Avg Energy",
                            value = if (state.averageEnergy > 0) String.format("%.1f ⚡", state.averageEnergy) else "—",
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // AM/PM Hourly Distribution Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "TIME-OF-DAY FOCUS DISTRIBUTION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val dist = state.hourlyDistribution
                        val totalDist = Math.max(1L, dist.morningMinutes + dist.afternoonMinutes + dist.eveningMinutes + dist.nightMinutes)

                        TimeBlockRow("Morning (05:00–12:00)", dist.morningMinutes, totalDist)
                        Spacer(modifier = Modifier.height(8.dp))
                        TimeBlockRow("Afternoon (12:00–17:00)", dist.afternoonMinutes, totalDist)
                        Spacer(modifier = Modifier.height(8.dp))
                        TimeBlockRow("Evening (17:00–22:00)", dist.eveningMinutes, totalDist)
                        Spacer(modifier = Modifier.height(8.dp))
                        TimeBlockRow("Night (22:00–05:00)", dist.nightMinutes, totalDist)
                    }
                }
            }

            // Focus Debt & Recovery Plan Card
            state.focusDebtResult?.let { debt ->
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (debt.isDebt) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f) else Color(0xFF10B981).copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (debt.isDebt) "FOCUS DEBT DETECTED" else "FOCUS CREDIT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (debt.isDebt) MaterialTheme.colorScheme.tertiary else Color(0xFF10B981)
                                )
                                Text(
                                    text = if (debt.isDebt) "-${DurationCalculator.formatMinutesHuman(debt.differenceMinutes)}"
                                    else "+${DurationCalculator.formatMinutesHuman(debt.differenceMinutes)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (debt.isDebt) MaterialTheme.colorScheme.tertiary else Color(0xFF10B981)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (debt.isDebt)
                                    "Actual focus (${DurationCalculator.formatMinutesHuman(debt.actualMinutes)}) is behind expected pace (${DurationCalculator.formatMinutesHuman(debt.expectedMinutes)})."
                                else
                                    "You are ahead of expected target by ${DurationCalculator.formatMinutesHuman(debt.differenceMinutes)}!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Deterministic Recovery Plan schedule if debt
                            state.recoveryPlanResult?.let { plan ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "RECOVERY SCHEDULE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = plan.guidanceMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                plan.dailyRecoveries.filter { it.recoveryAddedMinutes > 0 }.take(4).forEach { day ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${day.dayOfWeekName} (${day.date}):",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "+${DurationCalculator.formatMinutesHuman(day.recoveryAddedMinutes)} (Total ${DurationCalculator.formatMinutesHuman(day.totalMinutes)})",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Rolling Pace & Forecast Projection
            state.forecastResult?.let { forecast ->
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "ROLLING PACE & FORECAST ENGINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = forecast.summaryMessage,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Rolling Pace: ${DurationCalculator.formatMinutesHuman(forecast.rollingAverageDailyMinutes)}/day",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Confidence: ${forecast.confidence.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Interactive What-If Simulator
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "WHAT-IF SCENARIO CALCULATOR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Simulate how changing your daily focus pace impacts your completion date.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = whatIfDailyHours.toString(),
                                onValueChange = { whatIfDailyHours = it.toDoubleOrNull() ?: 3.0 },
                                label = { Text("Daily Pace (hours)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = whatIfRemainingHours.toString(),
                                onValueChange = { whatIfRemainingHours = it.toDoubleOrNull() ?: 25.0 },
                                label = { Text("Remaining Work (hours)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                viewModel.runWhatIfPaceSimulation(whatIfDailyHours, whatIfRemainingHours)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simulate Completion Date")
                        }

                        state.whatIfPaceResult?.let { sim ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Projected Finish: ${sim.projectedFinishDate}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Requires ${sim.eligibleDaysNeeded} working days (${sim.calendarDaysNeeded} calendar days)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeBlockRow(label: String, minutes: Long, totalMinutes: Long) {
    val fraction = (minutes.toDouble() / totalMinutes.toDouble()).toFloat().coerceIn(0f, 1f)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                DurationCalculator.formatMinutesHuman(minutes),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        )
    }
}
