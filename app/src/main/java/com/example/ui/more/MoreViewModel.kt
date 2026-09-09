package com.example.ui.more

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TrackaaApplication
import com.example.data.entity.*
import com.example.data.model.*
import com.example.data.repository.UserPreferences
import com.example.domain.calculations.*
import com.example.domain.pdf.PdfReportGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

data class TrashItems(
    val goals: List<GoalEntity> = emptyList(),
    val workItems: List<WorkItemEntity> = emptyList(),
    val topics: List<TopicEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val sessions: List<FocusSessionEntity> = emptyList()
)

data class MoreUiState(
    val preferences: UserPreferences = UserPreferences(),
    val targets: List<TargetEntity> = emptyList(),
    val availability: List<AvailabilityEntity> = emptyList(),
    val workItemTypes: List<WorkItemTypeEntity> = emptyList(),
    val interruptionReasons: List<InterruptionReasonEntity> = emptyList(),
    val scheduledFocus: List<ScheduledFocusEntity> = emptyList(),
    val totalXp: Int = 0,
    val levelProgress: LevelProgress = LevelProgress(1,0,0,500,0.0),
    val achievements: List<AchievementEntity> = emptyList(),
    val auditEvents: List<AuditEventEntity> = emptyList(),
    val trashItems: TrashItems = TrashItems(),
    val lastGeneratedPdf: File? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
) {
    val themeSetting: ThemeSetting get() = preferences.theme
    val isDndEnabled: Boolean get() = preferences.dndEnabled
    val dndPauseBehavior: DndPauseBehavior get() = preferences.dndPauseBehavior
}

class MoreViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<TrackaaApplication>()
    private val repository = app.repository
    private val prefs = app.userPreferencesRepository
    private val backupManager = app.backupManager
    private val _uiState = MutableStateFlow(MoreUiState())
    val uiState: StateFlow<MoreUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { prefs.userPreferencesFlow.collect { p -> _uiState.update { it.copy(preferences=p) } } }
        viewModelScope.launch {
            combine(repository.getTotalXp(), repository.getAllAchievements(), repository.getRecentAuditEvents(100)) { xp, achievements, audits -> Triple(xp,achievements,audits) }
                .collect { (xp,achievements,audits) -> _uiState.update { it.copy(totalXp=xp,levelProgress=StreakAndGamification.calculateLevel(xp),achievements=achievements,auditEvents=audits) } }
        }
        viewModelScope.launch { repository.getAllTargets().collect { value -> _uiState.update { it.copy(targets=value) } } }
        viewModelScope.launch { repository.getAllAvailability().collect { value -> _uiState.update { it.copy(availability=value) } } }
        viewModelScope.launch { repository.getAllWorkItemTypes().collect { value -> _uiState.update { it.copy(workItemTypes=value) } } }
        viewModelScope.launch { repository.getAllInterruptionReasons().collect { value -> _uiState.update { it.copy(interruptionReasons=value) } } }
        viewModelScope.launch { repository.getUpcomingScheduledFocus(System.currentTimeMillis()).collect { value -> _uiState.update { it.copy(scheduledFocus=value) } } }
        loadTrash()
    }

    fun clearMessage() = _uiState.update { it.copy(successMessage=null,errorMessage=null) }
    fun setTheme(theme: ThemeSetting) { viewModelScope.launch { prefs.setThemeSetting(theme) } }
    fun setDndEnabled(enabled: Boolean) { viewModelScope.launch { prefs.setDndEnabled(enabled) } }
    fun setDndPauseBehavior(value: DndPauseBehavior) { viewModelScope.launch { prefs.setDndPauseBehavior(value) } }
    fun setStreakThreshold(minutes: Long) { viewModelScope.launch { prefs.setStreakThresholdMinutes(minutes.coerceAtLeast(1)) } }
    fun setProgressWeights(time: Int, task: Int, topic: Int) {
        val t=time.coerceAtLeast(0); val k=task.coerceAtLeast(0); val p=topic.coerceAtLeast(0)
        if (t+k+p != 100) { _uiState.update { it.copy(errorMessage="Progress weights must total 100%") }; return }
        viewModelScope.launch { prefs.setProgressWeights(t,k,p); _uiState.update { it.copy(successMessage="Progress weights updated") } }
    }
    fun setQuietHours(enabled:Boolean,startH:Int,startM:Int,endH:Int,endM:Int) {
        if (startH !in 0..23 || endH !in 0..23 || startM !in 0..59 || endM !in 0..59) return
        viewModelScope.launch { prefs.setQuietHours(enabled,startH,startM,endH,endM) }
    }
    fun setEndOfDayReview(enabled:Boolean,hour:Int,minute:Int) {
        if (hour !in 0..23 || minute !in 0..59) return
        viewModelScope.launch { prefs.setEndOfDayReview(enabled,hour,minute) }
    }

    fun saveGlobalTarget(period: TargetPeriod, minMinutes:Long, goalMinutes:Long, stretchMinutes:Long, deadlineEpochMs:Long?=null) {
        val min=minMinutes.coerceAtLeast(0); val goal=goalMinutes.coerceAtLeast(min); val stretch=stretchMinutes.coerceAtLeast(goal)
        viewModelScope.launch {
            val existing=_uiState.value.targets.firstOrNull { it.scopeType==TargetScope.GLOBAL && it.periodType==period }
            if(existing==null) {
                repository.insertTarget(TargetEntity(scopeType=TargetScope.GLOBAL,periodType=period,minMinutes=min,goalMinutes=goal,stretchMinutes=stretch,startDateEpochMs=System.currentTimeMillis(),deadlineEpochMs=deadlineEpochMs))
            } else {
                repository.insertTargetRevision(TargetRevisionEntity(targetId=existing.id,oldMinMinutes=existing.minMinutes,oldGoalMinutes=existing.goalMinutes,oldStretchMinutes=existing.stretchMinutes,newMinMinutes=min,newGoalMinutes=goal,newStretchMinutes=stretch,reason="Updated in Settings"))
                repository.updateTarget(existing.copy(minMinutes=min,goalMinutes=goal,stretchMinutes=stretch,deadlineEpochMs=deadlineEpochMs))
            }
            _uiState.update { it.copy(successMessage="${period.name.lowercase().replaceFirstChar(Char::uppercase)} target saved") }
        }
    }

    fun updateAvailability(day:Int,isAvailable:Boolean,capacityMinutes:Long) {
        if(day !in 1..7) return
        viewModelScope.launch { repository.updateAvailability(AvailabilityEntity(day,isAvailable,if(isAvailable) capacityMinutes.coerceAtLeast(1) else 0)) }
    }

    fun addWorkItemType(name:String) {
        val clean=name.trim(); if(clean.isBlank()) return
        viewModelScope.launch { repository.insertWorkItemType(WorkItemTypeEntity(name=clean,isCustom=true)); _uiState.update{it.copy(successMessage="Work type added")} }
    }
    fun addInterruptionReason(name:String) {
        val clean=name.trim(); if(clean.isBlank()) return
        viewModelScope.launch { repository.insertInterruptionReason(InterruptionReasonEntity(name=clean,isDefault=false)); _uiState.update{it.copy(successMessage="Interruption reason added")} }
    }

    fun scheduleFocus(title:String,scheduledEpochMs:Long,durationMinutes:Long,taskId:Long?=null,workItemId:Long?=null) {
        if(scheduledEpochMs <= System.currentTimeMillis() || durationMinutes <= 0) { _uiState.update{it.copy(errorMessage="Choose a future time and positive duration")}; return }
        viewModelScope.launch {
            val base=ScheduledFocusEntity(title=title.ifBlank{"Focus Session"},taskId=taskId,workItemId=workItemId,scheduledEpochMs=scheduledEpochMs,durationMinutes=durationMinutes)
            val id=repository.insertScheduledFocus(base)
            val taskTitle=taskId?.let{repository.getTaskById(it)?.title} ?: "Focus Session"
            app.reminderScheduler.schedule(base.copy(id=id),taskTitle)
            _uiState.update{it.copy(successMessage="Focus reminder scheduled")}
        }
    }
    fun deleteScheduledFocus(item:ScheduledFocusEntity) { viewModelScope.launch { app.reminderScheduler.cancel(item.id); repository.deleteScheduledFocus(item) } }
    fun dndSettingsIntent(): Intent = app.dndManager.getDndSettingsIntent()
    fun exactAlarmSettingsIntent(): Intent? = app.reminderScheduler.exactAlarmSettingsIntent()
    fun isDndPermissionGranted():Boolean = app.dndManager.isDndPermissionGranted()

    fun generatePdfReport(reportTitle:String,dateRange:String) {
        viewModelScope.launch {
            runCatching {
                val zone=ZoneId.systemDefault(); val today=LocalDate.now()
                val period=when {
                    reportTitle.contains("Daily",true) -> Triple(today,today,TargetPeriod.DAILY)
                    reportTitle.contains("Monthly",true) -> Triple(today.withDayOfMonth(1),today.withDayOfMonth(today.lengthOfMonth()),TargetPeriod.MONTHLY)
                    else -> Triple(today.with(DayOfWeek.MONDAY),today.with(DayOfWeek.SUNDAY),TargetPeriod.WEEKLY)
                }
                val startMs=period.first.atStartOfDay(zone).toInstant().toEpochMilli(); val endMs=period.second.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val allSessions=repository.getAllFocusSessions().first(); val sessions=allSessions.filter{it.endEpochMs>startMs&&it.startEpochMs<endMs}
                val availability=repository.getAllAvailabilityOnce().associateBy{it.dayOfWeek}
                val dailyTarget=repository.getGlobalTarget(TargetPeriod.DAILY).first()?.goalMinutes ?: 300L
                val target=repository.getGlobalTarget(period.third).first()?.goalMinutes ?: generateSequence(period.first){if(it<period.second)it.plusDays(1)else null}.count{availability[it.dayOfWeek.value]?.isAvailable!=false}*dailyTarget
                val user=prefs.userPreferencesFlow.first(); val daily=mutableMapOf<LocalDate,Long>()
                for(s in allSessions) for(seg in repository.getFocusSegmentsForSessionOnce(s.id)) DurationCalculator.splitIntervalByCalendarDaysSeconds(seg.startEpochMs,seg.endEpochMs,zone).forEach{(d,v)->daily[d]=(daily[d]?:0L)+v/60L}
                val streak=StreakAndGamification.calculateStreak(daily,user.streakThresholdMinutes,today).currentStreakDays
                val actual=sessions.sumOf{if(it.totalFocusSeconds>0)it.totalFocusSeconds/60L else it.totalFocusMinutes}
                val dates=generateSequence(period.first){if(it<period.second)it.plusDays(1) else null}.toList()
                val current=dates.takeLast(7); val previous=dates.dropLast(7).takeLast(7)
                val recentAvg=if(current.isEmpty())0.0 else current.sumOf{daily[it]?:0L}.toDouble()/current.size
                val previousAvg=if(previous.isEmpty())0.0 else previous.sumOf{daily[it]?:0L}.toDouble()/previous.size
                val momentum=MomentumEngine.calculateMomentumScore(actual,target,dates.count{(daily[it]?:0L)>=user.streakThresholdMinutes},maxOf(1,dates.size),recentAvg,previousAvg).totalScore
                PdfReportGenerator.generateReportPdf(app,reportTitle,dateRange,sessions,target,streak,momentum)
            }.onSuccess { pdf -> _uiState.update{it.copy(lastGeneratedPdf=pdf,successMessage="PDF report generated",errorMessage=null)} }
             .onFailure { e -> _uiState.update{it.copy(errorMessage="Report failed: ${e.message}")} }
        }
    }

    fun createEncryptedBackup(passphrase:String,onBackupCreated:(String)->Unit) {
        viewModelScope.launch { runCatching{backupManager.createEncryptedBackup(passphrase.toCharArray())}
            .onSuccess{payload->_uiState.update{it.copy(successMessage="Encrypted full-workspace backup created",errorMessage=null)};onBackupCreated(payload)}
            .onFailure{e->_uiState.update{it.copy(errorMessage="Backup failed: ${e.message}")}} }
    }
    fun restoreFromBackup(content:String,passphrase:String,onRestored:(Int)->Unit) {
        viewModelScope.launch { backupManager.restoreFromEncryptedBackup(content,passphrase.toCharArray())
            .onSuccess{count->_uiState.update{it.copy(successMessage="Restored $count database rows transactionally",errorMessage=null)};loadTrash();onRestored(count)}
            .onFailure{e->_uiState.update{it.copy(errorMessage="Restore failed safely: ${e.message}")}} }
    }

    fun loadTrash(){viewModelScope.launch{_uiState.update{it.copy(trashItems=TrashItems(repository.getDeletedGoals(),repository.getDeletedWorkItems(),repository.getDeletedTopics(),repository.getDeletedTasks(),repository.getDeletedFocusSessions()))}}}
    fun restoreGoal(id:Long){viewModelScope.launch{repository.restoreGoal(id);loadTrash()}}
    fun restoreWorkItem(id:Long){viewModelScope.launch{repository.restoreWorkItem(id);loadTrash()}}
    fun restoreTask(id:Long){viewModelScope.launch{repository.restoreTask(id);loadTrash()}}
    fun purgeTrash(){viewModelScope.launch{repository.purgeOldTrash(System.currentTimeMillis()+1000L);loadTrash()}}
    fun insertDemoWorkspace(){viewModelScope.launch{repository.insertDemoWorkspace()}}
}
