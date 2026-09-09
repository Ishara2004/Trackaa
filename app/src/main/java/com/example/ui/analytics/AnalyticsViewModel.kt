package com.example.ui.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TrackaaApplication
import com.example.data.entity.*
import com.example.data.model.TargetPeriod
import com.example.domain.calculations.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.temporal.ChronoUnit

enum class AnalyticsTimeframe { DAILY, WEEKLY, MONTHLY }
data class HourlyDistribution(val morningMinutes:Long,val afternoonMinutes:Long,val eveningMinutes:Long,val nightMinutes:Long)
data class AnalyticsUiState(
    val selectedTimeframe: AnalyticsTimeframe=AnalyticsTimeframe.WEEKLY,
    val selectedDate: LocalDate=LocalDate.now(),
    val totalFocusMinutes:Long=0,val dailyAverageMinutes:Long=0,val longestSessionMinutes:Long=0,
    val totalPauseMinutes:Long=0,val pauseRatioPercent:Double=0.0,val interruptionCount:Int=0,
    val averageQuality:Double=0.0,val averageEnergy:Double=0.0,
    val hourlyDistribution:HourlyDistribution=HourlyDistribution(0,0,0,0),
    val workItemDistribution:Map<String,Long> = emptyMap(),
    val sessionsInPeriod:List<FocusSessionEntity> = emptyList(),
    val focusDebtResult:FocusDebtResult?=null,val recoveryPlanResult:RecoveryPlanResult?=null,val forecastResult:ForecastResult?=null,
    val whatIfPaceResult:WhatIfPaceToDateResult?=null,val whatIfDeadlineResult:WhatIfDeadlineResult?=null,
    val isLoading:Boolean=true
)

class AnalyticsViewModel(application: Application): AndroidViewModel(application) {
    private val app=getApplication<TrackaaApplication>(); private val repository=app.repository
    private val _uiState=MutableStateFlow(AnalyticsUiState()); val uiState:StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    init { loadAnalytics() }
    fun setTimeframe(t:AnalyticsTimeframe){ _uiState.update{it.copy(selectedTimeframe=t)}; loadAnalytics() }
    fun selectDate(d:LocalDate){ _uiState.update{it.copy(selectedDate=d)}; loadAnalytics() }

    @Suppress("UNCHECKED_CAST")
    private fun loadAnalytics(){
        viewModelScope.launch {
            combine(repository.getAllFocusSessions(),repository.getAllInterruptions(),repository.getAllTargets(),repository.getAllAvailability(),repository.getAllActiveWorkItems()) { a:Array<Any?> -> a }
                .collect { a ->
                    val allSessions=a[0] as List<FocusSessionEntity>; val interruptions=a[1] as List<InterruptionEntity>; val targets=a[2] as List<TargetEntity>
                    val availability=a[3] as List<AvailabilityEntity>; val workItems=a[4] as List<WorkItemEntity>
                    val zone=ZoneId.systemDefault(); val selected=_uiState.value.selectedDate
                    val (start,end)=when(_uiState.value.selectedTimeframe){
                        AnalyticsTimeframe.DAILY -> selected to selected
                        AnalyticsTimeframe.WEEKLY -> selected.with(DayOfWeek.MONDAY) to selected.with(DayOfWeek.SUNDAY)
                        AnalyticsTimeframe.MONTHLY -> selected.withDayOfMonth(1) to selected.withDayOfMonth(selected.lengthOfMonth())
                    }
                    val startMs=start.atStartOfDay(zone).toInstant().toEpochMilli(); val endMs=end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                    val periodSessions=allSessions.filter { it.endEpochMs>startMs && it.startEpochMs<endMs }
                    val dailySec=mutableMapOf<LocalDate,Long>(); val itemSec=mutableMapOf<Long,Long>()
                    var morning=0L;var afternoon=0L;var evening=0L;var night=0L; var longestSec=0L
                    for(session in periodSessions){
                        var sessionSec=0L
                        for(seg in repository.getFocusSegmentsForSessionOnce(session.id)){
                            val s=maxOf(seg.startEpochMs,startMs); val e=minOf(seg.endEpochMs,endMs); if(s>=e) continue
                            val sec=(e-s)/1000L; sessionSec+=sec; itemSec[seg.workItemId]=(itemSec[seg.workItemId]?:0L)+sec
                            DurationCalculator.splitIntervalByCalendarDaysSeconds(s,e,zone).forEach{(d,v)->dailySec[d]=(dailySec[d]?:0L)+v}
                            val bucket=Instant.ofEpochMilli(s).atZone(zone).hour
                            when(bucket){ in 5..11->morning+=sec; in 12..16->afternoon+=sec; in 17..21->evening+=sec; else->night+=sec }
                        }
                        longestSec=maxOf(longestSec,sessionSec)
                    }
                    val totalSec=dailySec.values.sum(); val totalMin=totalSec/60L
                    val days=ChronoUnit.DAYS.between(start,end)+1; val dailyAvg=if(days>0) totalMin/days else 0
                    val pauseSec=periodSessions.sumOf { if(it.totalPauseSeconds>0) it.totalPauseSeconds else it.totalPauseMinutes*60L }
                    val pauseRatio=if(totalSec+pauseSec>0) pauseSec.toDouble()/(totalSec+pauseSec)*100 else 0.0
                    val ids=periodSessions.map{it.id}.toSet(); val periodInts=interruptions.count{it.sessionId in ids}
                    val avgQ=if(periodSessions.isEmpty())0.0 else periodSessions.map{it.focusQuality}.average(); val avgE=if(periodSessions.isEmpty())0.0 else periodSessions.map{it.energyLevel}.average()
                    val names=workItems.associate{it.id to it.name}; val distribution=itemSec.filterKeys{it>0}.mapKeys{names[it.key]?:"Unknown"}.mapValues{it.value/60L}

                    val availMap=availability.associateBy{it.dayOfWeek}; val dates=generateSequence(start){ if(it<end) it.plusDays(1) else null }.toList()
                    val eligible=dates.filter{availMap[it.dayOfWeek.value]?.isAvailable!=false}; val periodTarget=when(_uiState.value.selectedTimeframe){
                        AnalyticsTimeframe.DAILY->targets.firstOrNull{it.scopeType.name=="GLOBAL"&&it.periodType==TargetPeriod.DAILY}?.goalMinutes ?: 300L
                        AnalyticsTimeframe.WEEKLY->targets.firstOrNull{it.scopeType.name=="GLOBAL"&&it.periodType==TargetPeriod.WEEKLY}?.goalMinutes ?: ((targets.firstOrNull{it.scopeType.name=="GLOBAL"&&it.periodType==TargetPeriod.DAILY}?.goalMinutes?:300L)*eligible.size)
                        AnalyticsTimeframe.MONTHLY->targets.firstOrNull{it.scopeType.name=="GLOBAL"&&it.periodType==TargetPeriod.MONTHLY}?.goalMinutes ?: ((targets.firstOrNull{it.scopeType.name=="GLOBAL"&&it.periodType==TargetPeriod.DAILY}?.goalMinutes?:300L)*eligible.size)
                    }
                    val debt=TargetEngine.calculateDebtOrCredit(periodTarget,totalMin)
                    val upcoming=(1..14).map{ selected.plusDays(it.toLong()) }.map { d -> val av=availMap[d.dayOfWeek.value]; DayCapacity(d,d.dayOfWeek.value,av?.isAvailable?:true,av?.capacityMinutes?:300L) }
                    val recovery=if(debt.isDebt&&debt.differenceMinutes>0) RecoveryPlanEngine.generateRecoveryPlan(debt.differenceMinutes,upcoming) else null
                    val excluded=DayOfWeek.values().filter{availMap[it.value]?.isAvailable==false}.toSet(); val history=dailySec.mapValues{it.value/60L}
                    val remaining=(periodTarget-totalMin).coerceAtLeast(0); val forecast=ForecastEngine.calculateForecast(history,eligible.size,remaining,end,LocalDate.now(),excluded)
                    _uiState.update{it.copy(totalFocusMinutes=totalMin,dailyAverageMinutes=dailyAvg,longestSessionMinutes=longestSec/60L,
                        totalPauseMinutes=pauseSec/60L,pauseRatioPercent=pauseRatio,interruptionCount=periodInts,averageQuality=avgQ,averageEnergy=avgE,
                        hourlyDistribution=HourlyDistribution(morning/60,afternoon/60,evening/60,night/60),workItemDistribution=distribution,
                        sessionsInPeriod=periodSessions,focusDebtResult=debt,recoveryPlanResult=recovery,forecastResult=forecast,isLoading=false)}
                }
        }
    }

    fun runWhatIfPaceSimulation(dailyHours:Double,remainingHours:Double){
        _uiState.update{it.copy(whatIfPaceResult=WhatIfSimulator.simulateFinishDate((dailyHours*60).toLong(),(remainingHours*60).toLong()))}
    }
    fun runWhatIfDeadlineSimulation(targetDaysAhead:Long,remainingHours:Double){
        val deadline=LocalDate.now().plusDays(targetDaysAhead)
        _uiState.update{it.copy(whatIfDeadlineResult=WhatIfSimulator.simulateRequiredPaceForDeadline(deadline,(remainingHours*60).toLong()))}
    }
}
