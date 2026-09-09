package com.example.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.GoalEntity
import com.example.data.entity.TaskEntity
import com.example.data.entity.TopicEntity
import com.example.data.model.GoalType
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.domain.calculations.DurationCalculator
import com.example.ui.components.TaskPriorityBadge
import com.example.ui.components.TaskStatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    onNavigateToFocus: (taskId: Long?, taskTitle: String?, workItemId: Long?, workItemName: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var showAddGoalModal by remember { mutableStateOf(false) }
    var showAddWorkItemModal by remember { mutableStateOf(false) }
    var showAddTaskModal by remember { mutableStateOf(false) }
    var selectedWorkItemIdForTask by remember { mutableStateOf<Long?>(null) }
    var expandedWorkItemId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GOALS & WORKSPACE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { showAddGoalModal = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Goal")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedWorkItemIdForTask = state.workItemsWithProgress.firstOrNull()?.workItem?.id
                    showAddTaskModal = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Task") },
                modifier = Modifier.testTag("add_task_fab")
            )
        },
        modifier = modifier.testTag("goals_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Goals Selector Row
            item {
                Column {
                    Text(
                        text = "ACTIVE GOALS & SEMESTERS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.goals.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("No goals defined yet", style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { showAddGoalModal = true }) { Text("+ Add Goal") }
                            }
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.goals) { goal ->
                                FilterChip(
                                    selected = state.selectedGoalId == goal.id,
                                    onClick = { viewModel.selectGoal(goal.id) },
                                    label = { Text(goal.title) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // WorkItems (Modules & Projects)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MODULES & PROJECTS (${state.workItemsWithProgress.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { showAddWorkItemModal = true }) {
                        Text("+ Add Module")
                    }
                }
            }

            items(state.workItemsWithProgress) { itemWithProgress ->
                val item = itemWithProgress.workItem
                val progress = itemWithProgress.progress
                val isExpanded = expandedWorkItemId == item.id

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            runCatching { Color(android.graphics.Color.parseColor(item.colorHex)) }
                                                .getOrDefault(MaterialTheme.colorScheme.primary)
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${item.type}  •  ${item.credits} Credits",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Overall progress circle
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${progress.overallWeightedPercent.toInt()}%",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Three-dimensional Progress Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Time: ${DurationCalculator.formatMinutesHuman(progress.actualFocusMinutes)} / ${DurationCalculator.formatMinutesHuman(progress.targetFocusMinutes)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Tasks: ${progress.completedTasksCount}/${progress.totalTasksCount}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Topics: ${progress.completedTopicsCount}/${progress.totalTopicsCount}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { (progress.overallWeightedPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Expand / Collapse Action
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    expandedWorkItemId = if (isExpanded) null else item.id
                                }
                            ) {
                                Text(if (isExpanded) "Hide Tasks & Topics ▲" else "View Tasks & Topics (${itemWithProgress.tasks.size}) ▼")
                            }

                            FilledTonalButton(
                                onClick = {
                                    onNavigateToFocus(null, "Focus on ${item.name}", item.id, item.name)
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Focus", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Expanded Section: Topics and Tasks
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Tasks list
                            Text(
                                text = "TASKS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            if (itemWithProgress.tasks.isEmpty()) {
                                Text("No tasks in this module yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                itemWithProgress.tasks.forEach { task ->
                                    TaskRowItem(
                                        task = task,
                                        onStatusToggle = {
                                            val next = when (task.status) {
                                                TaskStatus.COMPLETED -> TaskStatus.IN_PROGRESS
                                                else -> TaskStatus.COMPLETED
                                            }
                                            viewModel.updateTaskStatus(task, next)
                                        },
                                        onFocus = {
                                            onNavigateToFocus(task.id, task.title, item.id, item.name)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ADD GOAL MODAL
    if (showAddGoalModal) {
        var goalTitle by remember { mutableStateOf("") }
        var goalDesc by remember { mutableStateOf("") }
        var targetHours by remember { mutableLongStateOf(40L) }

        AlertDialog(
            onDismissRequest = { showAddGoalModal = false },
            title = { Text("New Goal / Semester") },
            text = {
                Column {
                    OutlinedTextField(
                        value = goalTitle,
                        onValueChange = { goalTitle = it },
                        label = { Text("Goal Title (e.g. Semester 3)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = goalDesc,
                        onValueChange = { goalDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetHours.toString(),
                        onValueChange = { targetHours = it.toLongOrNull() ?: 40L },
                        label = { Text("Target Focus Hours") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (goalTitle.isNotBlank()) {
                            viewModel.createGoal(goalTitle, goalDesc, targetHours)
                            showAddGoalModal = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalModal = false }) { Text("Cancel") }
            }
        )
    }

    // ADD WORK ITEM (MODULE) MODAL
    if (showAddWorkItemModal) {
        var itemName by remember { mutableStateOf("") }
        var itemDesc by remember { mutableStateOf("") }
        var itemType by remember { mutableStateOf("Module") }
        var credits by remember { mutableDoubleStateOf(3.0) }
        var targetHours by remember { mutableLongStateOf(20L) }

        AlertDialog(
            onDismissRequest = { showAddWorkItemModal = false },
            title = { Text("New Module / Project") },
            text = {
                Column {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Name (e.g. Distributed Systems)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = itemType,
                        onValueChange = { itemType = it },
                        label = { Text("Type (Module, Certification, Project)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = credits.toString(),
                            onValueChange = { credits = it.toDoubleOrNull() ?: 3.0 },
                            label = { Text("Credits") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = targetHours.toString(),
                            onValueChange = { targetHours = it.toLongOrNull() ?: 20L },
                            label = { Text("Target Hours") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (itemName.isNotBlank()) {
                            viewModel.createWorkItem(
                                name = itemName,
                                description = itemDesc,
                                type = itemType,
                                credits = credits,
                                targetHours = targetHours,
                                colorHex = "#38BDF8",
                                goalId = state.selectedGoalId
                            )
                            showAddWorkItemModal = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddWorkItemModal = false }) { Text("Cancel") }
            }
        )
    }

    // ADD TASK MODAL
    if (showAddTaskModal) {
        var taskTitle by remember { mutableStateOf("") }
        var estimatedMinutes by remember { mutableLongStateOf(60L) }
        var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }

        AlertDialog(
            onDismissRequest = { showAddTaskModal = false },
            title = { Text("New Focus Task") },
            text = {
                Column {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Task Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = estimatedMinutes.toString(),
                        onValueChange = { estimatedMinutes = it.toLongOrNull() ?: 60L },
                        label = { Text("Estimated Focus Minutes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Priority:", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskPriority.values().forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { Text(p.name, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val wid = selectedWorkItemIdForTask ?: state.workItemsWithProgress.firstOrNull()?.workItem?.id
                        if (taskTitle.isNotBlank() && wid != null) {
                            viewModel.createTask(
                                workItemId = wid,
                                topicId = null,
                                title = taskTitle,
                                description = null,
                                priority = priority,
                                estimatedMinutes = estimatedMinutes
                            )
                            showAddTaskModal = false
                        }
                    }
                ) { Text("Add Task") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskModal = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TaskRowItem(
    task: TaskEntity,
    onStatusToggle: () -> Unit,
    onFocus: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Checkbox(
                    checked = task.status == TaskStatus.COMPLETED,
                    onCheckedChange = { onStatusToggle() }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TaskPriorityBadge(priority = task.priority)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${task.actualFocusMinutes}m / ${task.estimatedMinutes}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onFocus) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Focus on task",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
