package com.example.ui.focus

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TrackaaApplication
import com.example.data.entity.InterruptionReasonEntity
import com.example.data.entity.TaskEntity
import com.example.data.entity.WorkItemEntity
import com.example.data.model.ActiveSessionState
import com.example.data.model.OutcomeStatus
import com.example.data.model.SessionMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FocusUiExtra(
    val availableWorkItems: List<WorkItemEntity> = emptyList(),
    val availableTasks: List<TaskEntity> = emptyList(),
    val interruptionReasons: List<InterruptionReasonEntity> = emptyList(),
    val showReviewDialog: Boolean = false,
    val showSwitchTaskDialog: Boolean = false,
    val showInterruptionDialog: Boolean = false
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
            combine(
                repository.getAllActiveWorkItems(),
                repository.getAllTasks(),
                repository.getAllInterruptionReasons()
            ) { items, tasks, reasons ->
                _extraState.update {
                    it.copy(
                        availableWorkItems = items,
                        availableTasks = tasks,
                        interruptionReasons = reasons
                    )
                }
            }.collect()
        }
    }

    fun startSession(
        mode: SessionMode,
        plannedDurationMinutes: Long,
        taskId: Long?,
        taskTitle: String,
        workItemId: Long?,
        workItemName: String,
        intent: String?
    ) {
        viewModelScope.launch {
            val dndEnabled = userPrefs.isDndEnabledFlow.first()
            dndManager.enableFocusProtection(dndEnabled)

            focusEngine.startFocus(
                mode = mode,
                plannedDurationMinutes = plannedDurationMinutes,
                taskId = taskId,
                taskTitle = taskTitle,
                workItemId = workItemId,
                workItemName = workItemName,
                intent = intent
            )
        }
    }

    fun pauseSession() {
        viewModelScope.launch {
            val pauseBehavior = userPrefs.dndPauseBehaviorFlow.first()
            dndManager.handlePause(pauseBehavior)
            focusEngine.pauseFocus()
        }
    }

    fun resumeSession() {
        viewModelScope.launch {
            val pauseBehavior = userPrefs.dndPauseBehaviorFlow.first()
            dndManager.handleResume(pauseBehavior)
            focusEngine.resumeFocus()
        }
    }

    fun startBreak() {
        focusEngine.startBreak()
    }

    fun endBreakAndResumeFocus() {
        focusEngine.endBreakAndResumeFocus()
    }

    fun requestStop() {
        dndManager.disableFocusProtection()
        focusEngine.requestStopSession()
    }

    fun switchTask(newTaskId: Long, newTaskTitle: String, newWorkItemId: Long, newWorkItemName: String) {
        focusEngine.switchTask(newTaskId, newTaskTitle, newWorkItemId, newWorkItemName)
        _extraState.update { it.copy(showSwitchTaskDialog = false) }
    }

    fun addInterruption(reason: String, note: String?) {
        focusEngine.addInterruption(reason, note)
        _extraState.update { it.copy(showInterruptionDialog = false) }
    }

    fun completeReview(
        focusQuality: Int,
        energyLevel: Int,
        outcomeStatus: OutcomeStatus?,
        outcomeNotes: String?,
        notes: String?,
        onSaved: () -> Unit
    ) {
        focusEngine.completeSessionReview(
            focusQuality = focusQuality,
            energyLevel = energyLevel,
            outcomeStatus = outcomeStatus,
            outcomeNotes = outcomeNotes,
            notes = notes,
            onSaved = onSaved
        )
    }

    fun discardSession() {
        dndManager.disableFocusProtection()
        focusEngine.discardSession()
    }

    fun setSwitchTaskDialogVisible(visible: Boolean) {
        _extraState.update { it.copy(showSwitchTaskDialog = visible) }
    }

    fun setInterruptionDialogVisible(visible: Boolean) {
        _extraState.update { it.copy(showInterruptionDialog = visible) }
    }
}
