package com.example.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.TaskEntity
import com.example.data.entity.WorkItemEntity
import com.example.data.model.FocusEngineStatus
import com.example.data.model.OutcomeStatus
import com.example.data.model.SessionMode
import com.example.domain.calculations.DurationCalculator
import com.example.ui.components.ChronographDial

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    initialTaskId: Long? = null,
    initialTaskTitle: String? = null,
    initialWorkItemId: Long? = null,
    initialWorkItemName: String? = null,
    modifier: Modifier = Modifier
) {
    val sessionState by viewModel.sessionState.collectAsState()
    val extraState by viewModel.extraState.collectAsState()

    var selectedMode by remember { mutableStateOf(SessionMode.STOPWATCH) }
    var selectedCountdownMinutes by remember { mutableLongStateOf(45L) }
    var selectedPomodoroFocus by remember { mutableLongStateOf(25L) }
    var selectedPomodoroBreak by remember { mutableLongStateOf(5L) }

    var selectedTaskId by remember { mutableStateOf(initialTaskId) }
    var selectedTaskTitle by remember { mutableStateOf(initialTaskTitle ?: "Quick Focus") }
    var selectedWorkItemId by remember { mutableStateOf(initialWorkItemId) }
    var selectedWorkItemName by remember { mutableStateOf(initialWorkItemName ?: "General") }
    var sessionIntent by remember { mutableStateOf("") }

    // Interruption Dialog State
    var showInterruptionModal by remember { mutableStateOf(false) }
    var customInterruptionReason by remember { mutableStateOf("") }
    var interruptionNote by remember { mutableStateOf("") }

    // Task Switch Dialog State
    var showSwitchTaskModal by remember { mutableStateOf(false) }

    val isIdle = sessionState.status == FocusEngineStatus.IDLE
    val isFocusing = sessionState.status == FocusEngineStatus.FOCUSING
    val isPaused = sessionState.status == FocusEngineStatus.PAUSED
    val isBreak = sessionState.status == FocusEngineStatus.ON_BREAK
    val isReviewPending = sessionState.status == FocusEngineStatus.REVIEW_PENDING

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FOCUS ENGINE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    if (!isIdle && !isReviewPending) {
                        FilledTonalButton(
                            onClick = { showInterruptionModal = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Interruption", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.testTag("focus_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Mode Selector (only when IDLE)
            if (isIdle) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = selectedMode == SessionMode.STOPWATCH,
                        onClick = { selectedMode = SessionMode.STOPWATCH },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Stopwatch", style = MaterialTheme.typography.labelSmall)
                    }
                    SegmentedButton(
                        selected = selectedMode == SessionMode.COUNTDOWN,
                        onClick = { selectedMode = SessionMode.COUNTDOWN },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Countdown", style = MaterialTheme.typography.labelSmall)
                    }
                    SegmentedButton(
                        selected = selectedMode == SessionMode.POMODORO,
                        onClick = { selectedMode = SessionMode.POMODORO },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("Pomodoro", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Presets row for Countdown
                if (selectedMode == SessionMode.COUNTDOWN) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(25L, 45L, 60L, 90L).forEach { mins ->
                            FilterChip(
                                selected = selectedCountdownMinutes == mins,
                                onClick = { selectedCountdownMinutes = mins },
                                label = { Text("${mins}m") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Presets row for Pomodoro
                if (selectedMode == SessionMode.POMODORO) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(Pair(25L, 5L), Pair(50L, 10L), Pair(90L, 15L)).forEach { (focus, rest) ->
                            FilterChip(
                                selected = selectedPomodoroFocus == focus && selectedPomodoroBreak == rest,
                                onClick = {
                                    selectedPomodoroFocus = focus
                                    selectedPomodoroBreak = rest
                                },
                                label = { Text("${focus}/${rest}m") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Precision Chronograph Dial
            val elapsedSec = if (isBreak) sessionState.elapsedBreakSeconds else sessionState.elapsedFocusSeconds
            val plannedSec = when (sessionState.mode) {
                SessionMode.COUNTDOWN -> sessionState.plannedDurationMinutes * 60
                SessionMode.POMODORO -> {
                    if (sessionState.status == FocusEngineStatus.ON_BREAK) sessionState.pomodoroBreakMinutes * 60
                    else sessionState.pomodoroFocusMinutes * 60
                }
                else -> 0L
            }

            ChronographDial(
                elapsedSeconds = elapsedSec,
                isOvertime = sessionState.isOvertime,
                overtimeSeconds = sessionState.overtimeSeconds,
                status = sessionState.status,
                mode = sessionState.mode,
                plannedSeconds = plannedSec,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Active Task and Context Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isIdle) "SELECTED TASK" else "CURRENT FOCUS CONTEXT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isIdle) selectedTaskTitle else sessionState.activeTaskTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (isIdle) selectedWorkItemName else sessionState.activeWorkItemName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (!isReviewPending) {
                        OutlinedButton(
                            onClick = { showSwitchTaskModal = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(if (isIdle) "Change" else "Switch")
                        }
                    }
                }
            }

            // Intent field (only visible when IDLE)
            if (isIdle) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = sessionIntent,
                    onValueChange = { sessionIntent = it },
                    label = { Text("Session Intent (e.g. Master AVL tree rotations)") },
                    placeholder = { Text("What specific outcome are you working toward?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Control Buttons
            if (isIdle) {
                Button(
                    onClick = {
                        val plannedDur = when (selectedMode) {
                            SessionMode.COUNTDOWN -> selectedCountdownMinutes
                            SessionMode.POMODORO -> selectedPomodoroFocus
                            SessionMode.STOPWATCH -> 0L
                        }
                        viewModel.startSession(
                            mode = selectedMode,
                            plannedDurationMinutes = plannedDur,
                            taskId = selectedTaskId,
                            taskTitle = selectedTaskTitle,
                            workItemId = selectedWorkItemId,
                            workItemName = selectedWorkItemName,
                            intent = sessionIntent.ifBlank { null }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_focus_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("START DEEP WORK", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            } else if (!isReviewPending) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isFocusing) {
                        Button(
                            onClick = { viewModel.pauseSession() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("pause_focus_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause")
                        }

                        Button(
                            onClick = { viewModel.startBreak() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Break")
                        }
                    } else if (isPaused) {
                        Button(
                            onClick = { viewModel.resumeSession() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("resume_focus_button"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume Focus")
                        }
                    } else if (isBreak) {
                        Button(
                            onClick = { viewModel.endBreakAndResumeFocus() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Next Focus")
                        }
                    }

                    // Stop button
                    Button(
                        onClick = { viewModel.requestStop() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("stop_focus_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("End Session")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // MANDATORY REVIEW MODAL / BOTTOM SHEET
    if (isReviewPending) {
        SessionReviewModal(
            totalFocusMinutes = Math.max(1, sessionState.completedFocusSegments.sumOf { (it.endEpochMs - it.startEpochMs) / 60000 }),
            intent = sessionState.intent,
            onSave = { quality, energy, outcome, outcomeNotes, notes ->
                viewModel.completeReview(quality, energy, outcome, outcomeNotes, notes) {}
            },
            onDiscard = { viewModel.discardSession() }
        )
    }

    // SWITCH TASK DIALOG
    if (showSwitchTaskModal) {
        AlertDialog(
            onDismissRequest = { showSwitchTaskModal = false },
            title = { Text("Select Focus Task") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isIdle) {
                                        selectedTaskId = null
                                        selectedTaskTitle = "Quick Focus"
                                        selectedWorkItemId = null
                                        selectedWorkItemName = "General"
                                    } else {
                                        viewModel.switchTask(0L, "Quick Focus", 0L, "General")
                                    }
                                    showSwitchTaskModal = false
                                }
                                .padding(12.dp)
                        ) {
                            Text("Quick Focus (No specific task)", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(extraState.availableTasks) { task ->
                        val item = extraState.availableWorkItems.find { it.id == task.workItemId }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable {
                                    if (isIdle) {
                                        selectedTaskId = task.id
                                        selectedTaskTitle = task.title
                                        selectedWorkItemId = task.workItemId
                                        selectedWorkItemName = item?.name ?: "Module"
                                    } else {
                                        viewModel.switchTask(task.id, task.title, task.workItemId, item?.name ?: "Module")
                                    }
                                    showSwitchTaskModal = false
                                }
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(task.title, fontWeight = FontWeight.SemiBold)
                                Text(item?.name ?: "Module", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSwitchTaskModal = false }) { Text("Cancel") }
            }
        )
    }

    // ADD INTERRUPTION DIALOG
    if (showInterruptionModal) {
        AlertDialog(
            onDismissRequest = { showInterruptionModal = false },
            title = { Text("Log Interruption") },
            text = {
                Column {
                    Text("Select reason:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    val defaultReasons = listOf("Phone Call", "Urgent Message", "Colleague Request", "Distraction", "Personal")
                    defaultReasons.forEach { reason ->
                        FilterChip(
                            selected = customInterruptionReason == reason,
                            onClick = { customInterruptionReason = reason },
                            label = { Text(reason) },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = interruptionNote,
                        onValueChange = { interruptionNote = it },
                        label = { Text("Short note (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalReason = customInterruptionReason.ifBlank { "Unspecified" }
                        viewModel.addInterruption(finalReason, interruptionNote.ifBlank { null })
                        showInterruptionModal = false
                        customInterruptionReason = ""
                        interruptionNote = ""
                    }
                ) {
                    Text("Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInterruptionModal = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SessionReviewModal(
    totalFocusMinutes: Long,
    intent: String?,
    onSave: (quality: Int, energy: Int, outcome: OutcomeStatus?, outcomeNotes: String?, notes: String?) -> Unit,
    onDiscard: () -> Unit
) {
    var quality by remember { mutableIntStateOf(0) }
    var energy by remember { mutableIntStateOf(0) }
    var outcomeStatus by remember { mutableStateOf<OutcomeStatus?>(null) }
    var outcomeNotes by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {}, // non-cancellable until user decides
        title = {
            Column {
                Text(
                    text = "SESSION REVIEW",
                    style = MaterialTheme.typography.titleMedium.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Completed: ${DurationCalculator.formatMinutesHuman(totalFocusMinutes)} focused",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Focus Quality (Mandatory)
                Text(
                    text = "FOCUS QUALITY (REQUIRED)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (quality == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        Pair(1, "1\nPoor"),
                        Pair(2, "2\nMild"),
                        Pair(3, "3\nGood"),
                        Pair(4, "4\nDeep"),
                        Pair(5, "5\nFlow")
                    ).forEach { (score, label) ->
                        FilterChip(
                            selected = quality == score,
                            onClick = { quality = score },
                            label = { Text(label, fontSize = 10.sp, lineHeight = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Energy Level (Mandatory)
                Text(
                    text = "ENERGY LEVEL (REQUIRED)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (energy == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        Pair(1, "1\nExhausted"),
                        Pair(2, "2\nLow"),
                        Pair(3, "3\nSteady"),
                        Pair(4, "4\nHigh"),
                        Pair(5, "5\nPeak")
                    ).forEach { (score, label) ->
                        FilterChip(
                            selected = energy == score,
                            onClick = { energy = score },
                            label = { Text(label, fontSize = 10.sp, lineHeight = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Outcome Status (Optional)
                Text("OUTCOME STATUS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutcomeStatus.values().forEach { status ->
                        FilterChip(
                            selected = outcomeStatus == status,
                            onClick = { outcomeStatus = if (outcomeStatus == status) null else status },
                            label = { Text(status.name, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = outcomeNotes,
                    onValueChange = { outcomeNotes = it },
                    label = { Text("What did you accomplish?") },
                    placeholder = { Text("Outcome vs original intent") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observations or reflection") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(quality, energy, outcomeStatus, outcomeNotes.ifBlank { null }, notes.ifBlank { null })
                },
                enabled = quality > 0 && energy > 0,
                modifier = Modifier.testTag("save_session_review_button")
            ) {
                Text("Save Session")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDiscard,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Discard")
            }
        }
    )
}
