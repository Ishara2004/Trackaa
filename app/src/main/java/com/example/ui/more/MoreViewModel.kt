package com.example.ui.more

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TrackaaApplication
import com.example.data.entity.*
import com.example.data.model.DndPauseBehavior
import com.example.data.model.TargetPeriod
import com.example.data.model.ThemeSetting
import com.example.domain.calculations.*
import com.example.domain.pdf.PdfReportGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

data class TrashItems(
    val goals:List<GoalEntity> = emptyList(), val workItems:List<WorkItemEntity> = emptyList(),
    val topics:List<TopicEntity> = emptyList(), val tasks:List<TaskEntity> = emptyList(),
    val sessions:List<FocusSessionEntity> = emptyList()
)

data class MoreUiState(
    val themeSetting:ThemeSetting=ThemeSetting.SYSTEM,
    val isDndEnabled:Boolean=false,
    val dndPauseBehavior:DndPauseBehavior=DndPauseBehavior.SUSPEND_WHILE_PAUSED,
    val totalXp:Int=0,
    val levelProgress:LevelProgress=LevelProgress(1,0,0,500,0.0),
    val achievements:List<AchievementEntity> = emptyList(),
    val auditEvents:List<AuditEventEntity> = emptyList(),
    val trashItems:TrashItems=TrashItems(),
    val lastGeneratedPdf:File?=null,
    val backupSuccessMessage:String?=null,
    val backupErrorMessage:String?=null
)

class MoreViewModel(application:Application):AndroidViewModel(application){
    private val app=getApplication<TrackaaApplication>(); private val repository=app.repository
    private val prefs=app.userPreferencesRepository; private val backupManager=app.backupManager
    private val _uiState=MutableStateFlow(MoreUiState()); val uiState:StateFlow<MoreUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            @Suppress("UNCHECKED_CAST")
            combine(prefs.themeSettingFlow,prefs.isDndEnabledFlow,prefs.dndPauseBehaviorFlow,repository.getTotalXp(),repository.getAllAchievements(),repository.getRecentAuditEvents(50)) { a:Array<Any?> -> a }
                .collect { a ->
                    val xp=a[3] as Int
                    _uiState.update { it.copy(themeSetting=a[0] as ThemeSetting,isDndEnabled=a[1] as Boolean,dndPauseBehavior=a[2] as DndPauseBehavior,
                        totalXp=xp,levelProgress=StreakAndGamification.calculateLevel(xp),achievements=a[4] as List<AchievementEntity>,auditEvents=a[5] as List<AuditEventEntity>) }
                }
        }
        loadTrash()
    }

    fun setTheme(theme:ThemeSetting){viewModelScope.launch{prefs.setThemeSetting(theme)}}
    fun setDndEnabled(enabled:Boolean){viewModelScope.launch{prefs.setDndEnabled(enabled)}}
    fun setDndPauseBehavior(b:DndPauseBehavior){viewModelScope.launch{prefs.setDndPauseBehavior(b)}}

    fun generatePdfReport(reportTitle:String,dateRange:String){
        viewModelScope.launch {
            runCatching {
                val zone=ZoneId.systemDefault(); val today=LocalDate.now()
                val period=when {
                    reportTitle.contains("Daily",true) -> Triple(today,today,TargetPeriod.DAILY)
                    reportTitle.contains("Monthly",true) -> Triple(today.withDayOfMonth(1),today.withDayOfMonth(today.lengthOfMonth()),TargetPeriod.MONTHLY)
                    else -> Triple(today.with(DayOfWeek.MONDAY),today.with(DayOfWeek.SUNDAY),TargetPeriod.WEEKLY)
                }
                val startMs=period.first.atStartOfDay(zone).toInstant().toEpochMilli(); val endMs=period.second.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val sessions=repository.getAllFocusSessions().first().filter{it.endEpochMs>startMs&&it.startEpochMs<endMs}
                val target=repository.getGlobalTarget(period.third).first()?.goalMinutes ?: if(period.third==TargetPeriod.DAILY) 300L else {
                    val daily=repository.getGlobalTarget(TargetPeriod.DAILY).first()?.goalMinutes ?: 300L
                    val availability=repository.getAllAvailabilityOnce().associateBy{it.dayOfWeek}
                    generateSequence(period.first){if(it<period.second)it.plusDays(1) else null}.count{availability[it.dayOfWeek.value]?.isAvailable!=false}*daily
                }
                val user=prefs.userPreferencesFlow.first(); val daily=mutableMapOf<LocalDate,Long>()
                for(s in repository.getAllFocusSessions().first()) for(seg in repository.getFocusSegmentsForSessionOnce(s.id))
                    DurationCalculator.splitIntervalByCalendarDaysSeconds(seg.startEpochMs,seg.endEpochMs,zone).forEach{(d,v)->daily[d]=(daily[d]?:0L)+v/60L}
                val streak=StreakAndGamification.calculateStreak(daily,user.streakThresholdMinutes,today).currentStreakDays
                val expected=target; val actual=sessions.sumOf{if(it.totalFocusSeconds>0)it.totalFocusSeconds/60L else it.totalFocusMinutes}
                val momentum=MomentumEngine.calculateMomentumScore(actual,expected,
                    eligibleDaysMeetingThreshold=daily.filterKeys{!it.isBefore(period.first)&&!it.isAfter(period.second)}.count{it.value>=user.streakThresholdMinutes},
                    totalEligibleDaysInWindow=maxOf(1,java.time.temporal.ChronoUnit.DAYS.between(period.first,period.second).toInt()+1),
                    recent7DayDailyAverage=0.0,previous7DayDailyAverage=0.0).totalScore
                val pdf=PdfReportGenerator.generateReportPdf(app,reportTitle,dateRange,sessions,target,streak,momentum)
                _uiState.update{it.copy(lastGeneratedPdf=pdf)}
            }.onFailure { e -> _uiState.update{it.copy(backupErrorMessage="Report failed: ${e.message}")} }
        }
    }

    fun createEncryptedBackup(passphrase:String,onBackupCreated:(String)->Unit){
        viewModelScope.launch { runCatching{backupManager.createEncryptedBackup(passphrase.toCharArray())}
            .onSuccess{payload->_uiState.update{it.copy(backupSuccessMessage="Encrypted full-workspace backup created.",backupErrorMessage=null)};onBackupCreated(payload)}
            .onFailure{e->_uiState.update{it.copy(backupErrorMessage="Backup failed: ${e.message}")}} }
    }
    fun restoreFromBackup(content:String,passphrase:String,onRestored:(Int)->Unit){
        viewModelScope.launch { backupManager.restoreFromEncryptedBackup(content,passphrase.toCharArray())
            .onSuccess{count->_uiState.update{it.copy(backupSuccessMessage="Restored $count database rows transactionally.",backupErrorMessage=null)};loadTrash();onRestored(count)}
            .onFailure{e->_uiState.update{it.copy(backupErrorMessage="Restore failed safely: ${e.message}")}} }
    }

    fun loadTrash(){viewModelScope.launch{_uiState.update{it.copy(trashItems=TrashItems(repository.getDeletedGoals(),repository.getDeletedWorkItems(),repository.getDeletedTopics(),repository.getDeletedTasks(),repository.getDeletedFocusSessions()))}}}
    fun restoreGoal(id:Long){viewModelScope.launch{repository.restoreGoal(id);loadTrash()}}
    fun restoreWorkItem(id:Long){viewModelScope.launch{repository.restoreWorkItem(id);loadTrash()}}
    fun restoreTask(id:Long){viewModelScope.launch{repository.restoreTask(id);loadTrash()}}
    fun purgeTrash(){viewModelScope.launch{repository.purgeOldTrash(System.currentTimeMillis()+1000L);loadTrash()}}
    fun insertDemoWorkspace(){viewModelScope.launch{repository.insertDemoWorkspace()}}
}
