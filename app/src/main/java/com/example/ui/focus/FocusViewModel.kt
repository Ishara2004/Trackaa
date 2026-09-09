package com.example.ui.focus

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TrackaaApplication
import com.example.data.entity.InterruptionReasonEntity
import com.example.data.entity.TaskEntity
import com.example.data.entity.WorkItemEntity
import com.example.data.model.ActiveSessionState
import com.example.data.model.OutcomeStatus
import com.example.data.model.SessionMode
import com.example.services.FocusTimerService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FocusUiExtra(
    val availableWorkItems: List<WorkItemEntity> = emptyList(),
    val availableTasks: List<TaskEntity> = emptyList(),
    val interruptionReasons: List<InterruptionReasonEntity> = emptyList()
)

class FocusViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<TrackaaApplication>()
    private val focusEngine = app.focusEngine
    private val repository = app.repository
    private val dndManager = app.dndManager
    private val userPrefs = app.userPreferencesRepository

    val sessionState: StateFlow<ActiveSessionState> = focusEngine.sessionState
    private val _extraState = MutableStateFlow(FocusUiExtra())
    val extraState: StateFlow<FocusUiExtra> = _extraState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.getAllActiveWorkItems(), repository.getAllTasks(), repository.getAllInterruptionReasons()) { items, tasks, reasons ->
                FocusUiExtra(items, tasks, reasons)
            }.collect { _extraState.value = it }
        }
    }

    /** Backward-compatible API used by the current Compose screen. */
    fun startSession(
        mode: SessionMode,
        plannedDurationMinutes: Long,
        taskId: Long?,
        taskTitle: String,
        workItemId: Long?,
        workItemName: String,
        intent: String?
    ) {
        val pomoBreak = when (plannedDurationMinutes) { 50L -> 10L; 90L -> 15L; else -> 5L }
        startSession(mode, plannedDurationMinutes,
            pomodoroFocusMinutes = if (mode == SessionMode.POMODORO) plannedDurationMinutes.coerceAtLeast(25L) else 25L,
            pomodoroBreakMinutes = pomoBreak,
            taskId, taskTitle, workItemId, workItemName, intent)
    }

    fun startSession(
        mode: SessionMode,
        plannedDurationMinutes: Long,
        pomodoroFocusMinutes: Long,
        pomodoroBreakMinutes: Long,
        taskId: Long?,
        taskTitle: String,
        workItemId: Long?,
        workItemName: String,
        intent: String?
    ) {
        viewModelScope.launch {
            dndManager.enableFocusProtection(userPrefs.isDndEnabledFlow.first())
            focusEngine.startFocus(mode, plannedDurationMinutes, pomodoroFocusMinutes, pomodoroBreakMinutes,
                taskId, taskTitle, workItemId, workItemName, intent)
            ContextCompat.startForegroundService(app, Intent(app, FocusTimerService::class.java))
        }
    }

    fun pauseSession() { viewModelScope.launch { dndManager.handlePause(userPrefs.dndPauseBehaviorFlow.first()); focusEngine.pauseFocus() } }
    fun resumeSession() { viewModelScope.launch { dndManager.handleResume(userPrefs.dndPauseBehaviorFlow.first()); focusEngine.resumeFocus(); ContextCompat.startForegroundService(app, Intent(app, FocusTimerService::class.java)) } }
    fun startBreak() = focusEngine.startBreak()
    fun endBreakAndResumeFocus() { focusEngine.endBreakAndResumeFocus(); ContextCompat.startForegroundService(app, Intent(app, FocusTimerService::class.java)) }
    fun requestStop() { dndManager.disableFocusProtection(); focusEngine.requestStopSession() }
    fun switchTask(newTaskId: Long, newTaskTitle: String, newWorkItemId: Long, newWorkItemName: String) = focusEngine.switchTask(newTaskId, newTaskTitle, newWorkItemId, newWorkItemName)
    fun addInterruption(reason: String, note: String?) = focusEngine.addInterruption(reason, note)
    fun completeReview(focusQuality: Int, energyLevel: Int, outcomeStatus: OutcomeStatus?, outcomeNotes: String?, notes: String?, onSaved: () -> Unit) = focusEngine.completeSessionReview(focusQuality, energyLevel, outcomeStatus, outcomeNotes, notes, onSaved)
    fun discardSession() { dndManager.disableFocusProtection(); focusEngine.discardSession() }
}
