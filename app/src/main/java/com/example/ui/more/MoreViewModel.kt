package com.example.ui.more

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TrackaaApplication
import com.example.data.entity.*
import com.example.data.model.DndPauseBehavior
import com.example.data.model.ThemeSetting
import com.example.domain.calculations.LevelProgress
import com.example.domain.calculations.StreakAndGamification
import com.example.domain.pdf.PdfReportGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class TrashItems(
    val goals: List<GoalEntity> = emptyList(),
    val workItems: List<WorkItemEntity> = emptyList(),
    val topics: List<TopicEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val sessions: List<FocusSessionEntity> = emptyList()
)

data class MoreUiState(
    val themeSetting: ThemeSetting = ThemeSetting.SYSTEM,
    val isDndEnabled: Boolean = false,
    val dndPauseBehavior: DndPauseBehavior = DndPauseBehavior.SUSPEND_WHILE_PAUSED,
    val totalXp: Int = 0,
    val levelProgress: LevelProgress = LevelProgress(1, 0, 0, 500, 0.0),
    val achievements: List<AchievementEntity> = emptyList(),
    val auditEvents: List<AuditEventEntity> = emptyList(),
    val trashItems: TrashItems = TrashItems(),
    val lastGeneratedPdf: File? = null,
    val backupSuccessMessage: String? = null,
    val backupErrorMessage: String? = null
)

class MoreViewModel(application: Application) : AndroidViewModel(application) {

    private val app = getApplication<TrackaaApplication>()
    private val repository = app.repository
    private val userPrefs = app.userPreferencesRepository
    private val backupManager = app.backupManager

    private val _uiState = MutableStateFlow(MoreUiState())
    val uiState: StateFlow<MoreUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            @Suppress("UNCHECKED_CAST")
            combine(
                userPrefs.themeSettingFlow,
                userPrefs.isDndEnabledFlow,
                userPrefs.dndPauseBehaviorFlow,
                repository.getTotalXp(),
                repository.getAllAchievements(),
                repository.getRecentAuditEvents(50)
            ) { args: Array<Any?> ->
                val theme = args[0] as ThemeSetting
                val dnd = args[1] as Boolean
                val pauseBeh = args[2] as DndPauseBehavior
                val xp = args[3] as Int
                val achievements = args[4] as List<AchievementEntity>
                val audits = args[5] as List<AuditEventEntity>

                val level = StreakAndGamification.calculateLevel(xp)
                MoreUiState(
                    themeSetting = theme,
                    isDndEnabled = dnd,
                    dndPauseBehavior = pauseBeh,
                    totalXp = xp,
                    levelProgress = level,
                    achievements = achievements,
                    auditEvents = audits,
                    trashItems = _uiState.value.trashItems,
                    lastGeneratedPdf = _uiState.value.lastGeneratedPdf,
                    backupSuccessMessage = _uiState.value.backupSuccessMessage,
                    backupErrorMessage = _uiState.value.backupErrorMessage
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
        loadTrash()
    }

    fun setTheme(theme: ThemeSetting) {
        viewModelScope.launch { userPrefs.setThemeSetting(theme) }
    }

    fun setDndEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setDndEnabled(enabled) }
    }

    fun setDndPauseBehavior(behavior: DndPauseBehavior) {
        viewModelScope.launch { userPrefs.setDndPauseBehavior(behavior) }
    }

    fun generatePdfReport(reportTitle: String, dateRange: String) {
        viewModelScope.launch {
            val sessions = repository.getAllFocusSessions().first()
            val pdfFile = PdfReportGenerator.generateReportPdf(
                context = app,
                reportTitle = reportTitle,
                dateRangeLabel = dateRange,
                sessions = sessions,
                targetMinutes = 1200,
                streakDays = 5,
                momentumScore = 78
            )
            _uiState.update { it.copy(lastGeneratedPdf = pdfFile) }
        }
    }

    fun createEncryptedBackup(passphrase: String, onBackupCreated: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val encrypted = backupManager.createEncryptedBackup(passphrase.toCharArray())
                _uiState.update { it.copy(backupSuccessMessage = "Encrypted backup created successfully.") }
                onBackupCreated(encrypted)
            }.onFailure { err ->
                _uiState.update { it.copy(backupErrorMessage = "Backup failed: ${err.message}") }
            }
        }
    }

    fun restoreFromBackup(backupContent: String, passphrase: String, onRestored: (Int) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.restoreFromEncryptedBackup(backupContent, passphrase.toCharArray())
            result.onSuccess { count ->
                _uiState.update { it.copy(backupSuccessMessage = "Successfully restored $count items!") }
                loadTrash()
                onRestored(count)
            }.onFailure { err ->
                _uiState.update { it.copy(backupErrorMessage = "Restore failed: ${err.message}") }
            }
        }
    }

    fun loadTrash() {
        viewModelScope.launch {
            val goals = repository.getDeletedGoals()
            val workItems = repository.getDeletedWorkItems()
            val topics = repository.getDeletedTopics()
            val tasks = repository.getDeletedTasks()
            val sessions = repository.getDeletedFocusSessions()
            _uiState.update {
                it.copy(
                    trashItems = TrashItems(goals, workItems, topics, tasks, sessions)
                )
            }
        }
    }

    fun restoreGoal(id: Long) {
        viewModelScope.launch {
            repository.restoreGoal(id)
            loadTrash()
        }
    }

    fun restoreWorkItem(id: Long) {
        viewModelScope.launch {
            repository.restoreWorkItem(id)
            loadTrash()
        }
    }

    fun restoreTask(id: Long) {
        viewModelScope.launch {
            repository.restoreTask(id)
            loadTrash()
        }
    }

    fun purgeTrash() {
        viewModelScope.launch {
            // Purge items immediately
            repository.purgeOldTrash(System.currentTimeMillis() + 1000L)
            loadTrash()
        }
    }

    fun insertDemoWorkspace() {
        viewModelScope.launch {
            repository.insertDemoWorkspace()
        }
    }
}
