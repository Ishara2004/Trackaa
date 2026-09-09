package com.example.ui.goals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TrackaaApplication
import com.example.data.entity.GoalEntity
import com.example.data.entity.TaskDependencyEntity
import com.example.data.entity.TaskEntity
import com.example.data.entity.TopicEntity
import com.example.data.entity.WorkItemEntity
import com.example.data.model.GoalStatus
import com.example.data.model.GoalType
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.domain.calculations.DetailedProgress
import com.example.domain.calculations.ProgressModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WorkItemWithProgress(
    val workItem: WorkItemEntity,
    val progress: DetailedProgress,
    val tasks: List<TaskEntity>,
    val topics: List<TopicEntity>
)

data class GoalsUiState(
    val goals: List<GoalEntity> = emptyList(),
    val selectedGoalId: Long? = null,
    val workItemsWithProgress: List<WorkItemWithProgress> = emptyList(),
    val allTasks: List<TaskEntity> = emptyList(),
    val allTopics: List<TopicEntity> = emptyList(),
    val isLoading: Boolean = true
)

class GoalsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = getApplication<TrackaaApplication>()
    private val repository = app.repository

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAllGoals(),
                repository.getAllActiveWorkItems(),
                repository.getAllTopics(),
                repository.getAllTasks()
            ) { goals, workItems, topics, tasks ->
                val selectedGoal = _uiState.value.selectedGoalId ?: goals.firstOrNull()?.id

                val filteredItems = if (selectedGoal != null) {
                    workItems.filter { it.goalId == selectedGoal }
                } else workItems

                val itemsWithProgress = filteredItems.map { item ->
                    val itemTasks = tasks.filter { it.workItemId == item.id }
                    val itemTopics = topics.filter { it.workItemId == item.id }

                    val completedTasks = itemTasks.count { it.status == TaskStatus.COMPLETED }
                    val completedTopics = itemTopics.count { it.isCompleted }
                    val actualMinutes = itemTasks.sumOf { it.actualFocusMinutes }

                    val progress = ProgressModel.calculateDetailedProgress(
                        actualFocusMinutes = actualMinutes,
                        targetFocusMinutes = item.targetFocusMinutes,
                        completedTasks = completedTasks,
                        totalTasks = itemTasks.size,
                        completedTopics = completedTopics,
                        totalTopics = itemTopics.size
                    )

                    WorkItemWithProgress(
                        workItem = item,
                        progress = progress,
                        tasks = itemTasks,
                        topics = itemTopics
                    )
                }

                _uiState.update {
                    it.copy(
                        goals = goals,
                        selectedGoalId = selectedGoal,
                        workItemsWithProgress = itemsWithProgress,
                        allTasks = tasks,
                        allTopics = topics,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun selectGoal(goalId: Long) {
        _uiState.update { it.copy(selectedGoalId = goalId) }
        loadData()
    }

    fun createGoal(title: String, description: String, targetHours: Long, goalType: GoalType = GoalType.TIME) {
        viewModelScope.launch {
            repository.insertGoal(
                GoalEntity(
                    title = title,
                    description = description,
                    targetMinutes = targetHours * 60,
                    goalType = goalType
                )
            )
        }
    }

    fun createWorkItem(name: String, description: String, type: String, credits: Double, targetHours: Long, colorHex: String, goalId: Long?) {
        viewModelScope.launch {
            repository.insertWorkItem(
                WorkItemEntity(
                    name = name,
                    description = description,
                    type = type,
                    credits = credits,
                    targetFocusMinutes = targetHours * 60,
                    colorHex = colorHex,
                    goalId = goalId
                )
            )
        }
    }

    fun createTopic(workItemId: Long, title: String, notes: String?) {
        viewModelScope.launch {
            repository.insertTopic(
                TopicEntity(
                    workItemId = workItemId,
                    title = title,
                    notes = notes ?: ""
                )
            )
        }
    }

    fun toggleTopicCompleted(topic: TopicEntity) {
        viewModelScope.launch {
            repository.updateTopic(topic.copy(isCompleted = !topic.isCompleted))
        }
    }

    fun createTask(
        workItemId: Long,
        topicId: Long?,
        title: String,
        description: String?,
        priority: TaskPriority,
        estimatedMinutes: Long,
        dependsOnTaskId: Long? = null
    ) {
        viewModelScope.launch {
            val taskId = repository.insertTask(
                TaskEntity(
                    workItemId = workItemId,
                    topicId = topicId,
                    title = title,
                    description = description ?: "",
                    priority = priority,
                    estimatedMinutes = estimatedMinutes
                )
            )
            if (dependsOnTaskId != null && dependsOnTaskId > 0) {
                repository.insertTaskDependency(
                    TaskDependencyEntity(taskId = taskId, dependsOnTaskId = dependsOnTaskId)
                )
            }
        }
    }

    fun updateTaskStatus(task: TaskEntity, newStatus: TaskStatus) {
        viewModelScope.launch {
            val completedTime = if (newStatus == TaskStatus.COMPLETED) System.currentTimeMillis() else null
            repository.updateTask(task.copy(status = newStatus, completedAtEpochMs = completedTime))
        }
    }

    fun softDeleteGoal(goalId: Long) {
        viewModelScope.launch { repository.softDeleteGoal(goalId) }
    }

    fun softDeleteWorkItem(workItemId: Long) {
        viewModelScope.launch { repository.softDeleteWorkItem(workItemId) }
    }

    fun softDeleteTask(taskId: Long) {
        viewModelScope.launch { repository.softDeleteTask(taskId) }
    }
}
