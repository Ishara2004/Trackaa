package com.example.domain.focus

import android.content.Context
import android.content.SharedPreferences
import com.example.data.entity.*
import com.example.data.model.*
import com.example.data.repository.TrackaaRepository
import com.example.services.FocusNotificationManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

class FocusEngine(
    private val context: Context,
    private val repository: TrackaaRepository,
    private val notificationManager: FocusNotificationManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("trackaa_focus_checkpoint", Context.MODE_PRIVATE)
    private val _sessionState = MutableStateFlow(ActiveSessionState())
    val sessionState: StateFlow<ActiveSessionState> = _sessionState.asStateFlow()

    private var tickerJob: Job? = null
    private var countdownAlertFired = false
    private var pomodoroAlertFired = false
    private var tickCounter = 0

    init { recoverSessionIfAvailable() }

    fun startFocus(
        mode: SessionMode = SessionMode.STOPWATCH,
        plannedDurationMinutes: Long = 0,
        pomodoroFocusMinutes: Long = 25,
        pomodoroBreakMinutes: Long = 5,
        taskId: Long? = null,
        taskTitle: String = "Quick Focus",
        workItemId: Long? = null,
        workItemName: String = "General",
        intent: String? = null
    ) {
        if (_sessionState.value.status != FocusEngineStatus.IDLE) return
        val now = System.currentTimeMillis()
        countdownAlertFired = false
        pomodoroAlertFired = false
        _sessionState.value = ActiveSessionState(
            status = FocusEngineStatus.FOCUSING,
            mode = mode,
            startEpochMs = now,
            lastVerifiedEpochMs = now,
            plannedDurationMinutes = plannedDurationMinutes.coerceAtLeast(0),
            pomodoroFocusMinutes = pomodoroFocusMinutes.coerceAtLeast(1),
            pomodoroBreakMinutes = pomodoroBreakMinutes.coerceAtLeast(1),
            pomodoroState = PomodoroState.FOCUS,
            activeTaskId = taskId,
            activeTaskTitle = taskTitle,
            activeWorkItemId = workItemId,
            activeWorkItemName = workItemName,
            intent = intent,
            currentSegmentStartEpochMs = now
        )
        saveCheckpoint()
        startTicker()
        notificationManager.showActiveFocusNotification(_sessionState.value)
    }

    fun pauseFocus() {
        val current = _sessionState.value
        if (current.status != FocusEngineStatus.FOCUSING) return
        val now = System.currentTimeMillis()
        val segment = focusSegment(current, current.currentSegmentStartEpochMs, now)
        _sessionState.value = current.copy(
            status = FocusEngineStatus.PAUSED,
            currentPauseStartEpochMs = now,
            currentSegmentStartEpochMs = 0L,
            currentCycleFocusSeconds = current.currentCycleFocusSeconds + segment.durationSeconds,
            completedFocusSegments = current.completedFocusSegments + segment,
            lastVerifiedEpochMs = now
        )
        saveCheckpoint()
        notificationManager.showActiveFocusNotification(_sessionState.value)
    }

    fun resumeFocus() {
        val current = _sessionState.value
        if (current.status != FocusEngineStatus.PAUSED) return
        val now = System.currentTimeMillis()
        val pause = pauseSegment(current.currentPauseStartEpochMs, now)
        _sessionState.value = current.copy(
            status = FocusEngineStatus.FOCUSING,
            currentSegmentStartEpochMs = now,
            currentPauseStartEpochMs = 0L,
            completedPauseSegments = current.completedPauseSegments + pause,
            lastVerifiedEpochMs = now
        )
        saveCheckpoint()
        startTicker()
        notificationManager.showActiveFocusNotification(_sessionState.value)
    }

    fun startBreak() {
        val current = _sessionState.value
        if (current.status != FocusEngineStatus.FOCUSING && current.status != FocusEngineStatus.PAUSED) return
        val now = System.currentTimeMillis()
        var focusSegments = current.completedFocusSegments
        var pauseSegments = current.completedPauseSegments
        var cycleFocus = current.currentCycleFocusSeconds
        if (current.status == FocusEngineStatus.FOCUSING && current.currentSegmentStartEpochMs > 0L) {
            val seg = focusSegment(current, current.currentSegmentStartEpochMs, now)
            focusSegments = focusSegments + seg
            cycleFocus += seg.durationSeconds
        }
        if (current.status == FocusEngineStatus.PAUSED && current.currentPauseStartEpochMs > 0L) {
            pauseSegments = pauseSegments + pauseSegment(current.currentPauseStartEpochMs, now)
        }
        pomodoroAlertFired = false
        _sessionState.value = current.copy(
            status = FocusEngineStatus.ON_BREAK,
            pomodoroState = PomodoroState.BREAK,
            currentBreakStartEpochMs = now,
            currentSegmentStartEpochMs = 0L,
            currentPauseStartEpochMs = 0L,
            currentCycleFocusSeconds = cycleFocus,
            currentCycleBreakSeconds = 0L,
            completedFocusSegments = focusSegments,
            completedPauseSegments = pauseSegments,
            lastVerifiedEpochMs = now
        )
        saveCheckpoint()
        notificationManager.showActiveFocusNotification(_sessionState.value)
    }

    fun endBreakAndResumeFocus() {
        val current = _sessionState.value
        if (current.status != FocusEngineStatus.ON_BREAK && current.status != FocusEngineStatus.BREAK_COMPLETE) return
        val now = System.currentTimeMillis()
        var breaks = current.completedBreakSegments
        var cycleBreak = current.currentCycleBreakSeconds
        if (current.status == FocusEngineStatus.ON_BREAK && current.currentBreakStartEpochMs > 0L) {
            val seg = breakSegment(current.currentBreakStartEpochMs, now)
            breaks = breaks + seg
            cycleBreak += seg.durationSeconds
        }
        pomodoroAlertFired = false
        _sessionState.value = current.copy(
            status = FocusEngineStatus.FOCUSING,
            pomodoroState = PomodoroState.FOCUS,
            currentSegmentStartEpochMs = now,
            currentBreakStartEpochMs = 0L,
            currentCycleFocusSeconds = 0L,
            currentCycleBreakSeconds = 0L,
            completedBreakSegments = breaks,
            lastVerifiedEpochMs = now
        )
        saveCheckpoint()
        startTicker()
        notificationManager.showActiveFocusNotification(_sessionState.value)
    }

    fun switchTask(newTaskId: Long, newTaskTitle: String, newWorkItemId: Long, newWorkItemName: String) {
        val current = _sessionState.value
        val now = System.currentTimeMillis()
        var segments = current.completedFocusSegments
        var cycleFocus = current.currentCycleFocusSeconds
        if (current.status == FocusEngineStatus.FOCUSING && current.currentSegmentStartEpochMs > 0L) {
            val seg = focusSegment(current, current.currentSegmentStartEpochMs, now)
            segments = segments + seg
            cycleFocus += seg.durationSeconds
        }
        _sessionState.value = current.copy(
            activeTaskId = newTaskId.takeIf { it > 0 },
            activeTaskTitle = newTaskTitle,
            activeWorkItemId = newWorkItemId.takeIf { it > 0 },
            activeWorkItemName = newWorkItemName,
            currentSegmentStartEpochMs = if (current.status == FocusEngineStatus.FOCUSING) now else current.currentSegmentStartEpochMs,
            currentCycleFocusSeconds = cycleFocus,
            completedFocusSegments = segments,
            lastVerifiedEpochMs = now
        )
        saveCheckpoint()
        notificationManager.showActiveFocusNotification(_sessionState.value)
    }

    fun addInterruption(reason: String, note: String? = null) {
        val current = _sessionState.value
        if (current.status == FocusEngineStatus.IDLE || current.status == FocusEngineStatus.REVIEW_PENDING) return
        val now = System.currentTimeMillis()
        _sessionState.value = current.copy(
            interruptions = current.interruptions + InterruptionEntity(0, 0, now, reason, note),
            lastVerifiedEpochMs = now
        )
        saveCheckpoint()
    }

    fun requestStopSession() {
        val current = _sessionState.value
        if (current.status == FocusEngineStatus.IDLE || current.status == FocusEngineStatus.REVIEW_PENDING) return
        val now = System.currentTimeMillis()
        val finalized = finalizeOpenInterval(current, now).copy(
            status = FocusEngineStatus.REVIEW_PENDING,
            stoppedAtEpochMs = now,
            currentSegmentStartEpochMs = 0L,
            currentPauseStartEpochMs = 0L,
            currentBreakStartEpochMs = 0L,
            lastVerifiedEpochMs = now
        )
        stopTicker()
        notificationManager.cancelActiveFocusNotification()
        _sessionState.value = finalized
        saveCheckpoint()
    }

    fun completeSessionReview(
        focusQuality: Int,
        energyLevel: Int,
        outcomeStatus: OutcomeStatus?,
        outcomeNotes: String?,
        notes: String?,
        onSaved: () -> Unit = {}
    ) {
        val current = _sessionState.value
        if (current.status != FocusEngineStatus.REVIEW_PENDING || focusQuality !in 1..5 || energyLevel !in 1..5) return
        scope.launch(Dispatchers.IO) {
            val focusSec = current.completedFocusSegments.sumOf { it.durationSeconds.coerceAtLeast((it.endEpochMs - it.startEpochMs).coerceAtLeast(0L) / 1000L) }
            val pauseSec = current.completedPauseSegments.sumOf { it.durationSeconds.coerceAtLeast((it.endEpochMs - it.startEpochMs).coerceAtLeast(0L) / 1000L) }
            val breakSec = current.completedBreakSegments.sumOf { it.durationSeconds.coerceAtLeast((it.endEpochMs - it.startEpochMs).coerceAtLeast(0L) / 1000L) }
            val focusMin = focusSec / 60L
            val pauseMin = pauseSec / 60L
            val breakMin = breakSec / 60L
            val plannedSec = current.plannedDurationMinutes * 60L
            val overtimeSec = if (current.mode == SessionMode.COUNTDOWN) (focusSec - plannedSec).coerceAtLeast(0L) else 0L
            val end = current.stoppedAtEpochMs.takeIf { it > 0L } ?: current.lastVerifiedEpochMs.takeIf { it > 0L } ?: System.currentTimeMillis()

            val sessionId = repository.insertFocusSession(
                FocusSessionEntity(
                    startEpochMs = current.startEpochMs,
                    endEpochMs = end,
                    mode = current.mode,
                    plannedDurationMinutes = current.plannedDurationMinutes,
                    overtimeMinutes = overtimeSec / 60L,
                    totalFocusMinutes = focusMin,
                    totalPauseMinutes = pauseMin,
                    totalBreakMinutes = breakMin,
                    totalFocusSeconds = focusSec,
                    totalPauseSeconds = pauseSec,
                    totalBreakSeconds = breakSec,
                    focusQuality = focusQuality,
                    energyLevel = energyLevel,
                    intent = current.intent,
                    outcomeStatus = outcomeStatus,
                    outcomeNotes = outcomeNotes,
                    notes = notes,
                    isRecovered = current.isRecovered
                )
            )

            val focusSegments = current.completedFocusSegments.map { it.copy(sessionId = sessionId) }
            repository.insertFocusSegments(focusSegments)
            repository.insertPauseSegments(current.completedPauseSegments.map { it.copy(sessionId = sessionId) })
            repository.insertBreakSegments(current.completedBreakSegments.map { it.copy(sessionId = sessionId) })
            current.interruptions.forEach { repository.insertInterruption(it.copy(sessionId = sessionId)) }

            focusSegments.groupBy { it.taskId }.forEach { (taskId, segs) ->
                if (taskId > 0L) repository.getTaskById(taskId)?.let { task ->
                    val exactAddedMinutes = segs.sumOf { it.durationSeconds } / 60L
                    repository.updateTask(task.copy(actualFocusMinutes = task.actualFocusMinutes + exactAddedMinutes))
                }
            }

            repository.insertXpEvent(XpEventEntity(source="FOCUS_SESSION", amount=focusMin.toInt(), description="Focused for ${focusMin}m ($focusQuality/5 quality)"))
            evaluateAchievements(current, focusSec, overtimeSec)
            clearCheckpoint()
            _sessionState.value = ActiveSessionState()
            withContext(Dispatchers.Main) { onSaved() }
        }
    }

    fun discardSession() {
        stopTicker()
        clearCheckpoint()
        notificationManager.cancelActiveFocusNotification()
        _sessionState.value = ActiveSessionState()
    }

    private fun focusSegment(state: ActiveSessionState, start: Long, end: Long): FocusSegmentEntity {
        val seconds = ((end - start).coerceAtLeast(0L) / 1000L)
        return FocusSegmentEntity(
            sessionId = 0,
            taskId = state.activeTaskId ?: 0L,
            workItemId = state.activeWorkItemId ?: 0L,
            startEpochMs = start,
            endEpochMs = end,
            durationMinutes = seconds / 60L,
            durationSeconds = seconds
        )
    }

    private fun pauseSegment(start: Long, end: Long): PauseSegmentEntity {
        val seconds = ((end - start).coerceAtLeast(0L) / 1000L)
        return PauseSegmentEntity(0, 0, start, end, seconds / 60L, seconds)
    }

    private fun breakSegment(start: Long, end: Long): BreakSegmentEntity {
        val seconds = ((end - start).coerceAtLeast(0L) / 1000L)
        return BreakSegmentEntity(0, 0, start, end, seconds / 60L, seconds)
    }

    private fun finalizeOpenInterval(current: ActiveSessionState, end: Long): ActiveSessionState = when (current.status) {
        FocusEngineStatus.FOCUSING -> {
            val seg = focusSegment(current, current.currentSegmentStartEpochMs, end)
            current.copy(completedFocusSegments = current.completedFocusSegments + seg, currentCycleFocusSeconds = current.currentCycleFocusSeconds + seg.durationSeconds)
        }
        FocusEngineStatus.PAUSED -> current.copy(completedPauseSegments = current.completedPauseSegments + pauseSegment(current.currentPauseStartEpochMs, end))
        FocusEngineStatus.ON_BREAK -> {
            val seg = breakSegment(current.currentBreakStartEpochMs, end)
            current.copy(completedBreakSegments = current.completedBreakSegments + seg, currentCycleBreakSeconds = current.currentCycleBreakSeconds + seg.durationSeconds)
        }
        else -> current
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) { delay(1000); updateTick() }
        }
    }

    private fun stopTicker() { tickerJob?.cancel(); tickerJob = null }

    private fun updateTick() {
        val current = _sessionState.value
        if (current.status in setOf(FocusEngineStatus.IDLE, FocusEngineStatus.REVIEW_PENDING, FocusEngineStatus.BREAK_COMPLETE)) return
        val now = System.currentTimeMillis()
        val activeFocus = if (current.status == FocusEngineStatus.FOCUSING && current.currentSegmentStartEpochMs > 0L) (now-current.currentSegmentStartEpochMs).coerceAtLeast(0L)/1000L else 0L
        val activePause = if (current.status == FocusEngineStatus.PAUSED && current.currentPauseStartEpochMs > 0L) (now-current.currentPauseStartEpochMs).coerceAtLeast(0L)/1000L else 0L
        val activeBreak = if (current.status == FocusEngineStatus.ON_BREAK && current.currentBreakStartEpochMs > 0L) (now-current.currentBreakStartEpochMs).coerceAtLeast(0L)/1000L else 0L
        val completedFocus = current.completedFocusSegments.sumOf { it.durationSeconds }
        val completedPause = current.completedPauseSegments.sumOf { it.durationSeconds }
        val completedBreak = current.completedBreakSegments.sumOf { it.durationSeconds }
        val totalFocus = completedFocus + activeFocus
        val totalPause = completedPause + activePause
        val totalBreak = completedBreak + activeBreak
        val planned = current.plannedDurationMinutes * 60L
        val overtime = if (current.mode == SessionMode.COUNTDOWN) (totalFocus-planned).coerceAtLeast(0L) else 0L

        if (current.mode == SessionMode.COUNTDOWN && planned > 0 && totalFocus >= planned && !countdownAlertFired) {
            countdownAlertFired = true
            notificationManager.showTimerAlertNotification("Countdown reached planned time", "Continuing in overtime until you stop the session.")
        }

        if (current.mode == SessionMode.POMODORO) {
            val cycleFocus = current.currentCycleFocusSeconds + activeFocus
            if (current.status == FocusEngineStatus.FOCUSING && cycleFocus >= current.pomodoroFocusMinutes * 60L && !pomodoroAlertFired) {
                pomodoroAlertFired = true
                notificationManager.showTimerAlertNotification("Focus interval complete", "Start your ${current.pomodoroBreakMinutes}m break.")
                startBreak()
                return
            }
            val cycleBreak = current.currentCycleBreakSeconds + activeBreak
            if (current.status == FocusEngineStatus.ON_BREAK && cycleBreak >= current.pomodoroBreakMinutes * 60L && !pomodoroAlertFired) {
                pomodoroAlertFired = true
                val seg = breakSegment(current.currentBreakStartEpochMs, now)
                stopTicker()
                _sessionState.value = current.copy(
                    status = FocusEngineStatus.BREAK_COMPLETE,
                    currentBreakStartEpochMs = 0L,
                    currentCycleBreakSeconds = current.currentCycleBreakSeconds + seg.durationSeconds,
                    completedBreakSegments = current.completedBreakSegments + seg,
                    elapsedBreakSeconds = completedBreak + seg.durationSeconds,
                    lastVerifiedEpochMs = now
                )
                saveCheckpoint()
                notificationManager.showTimerAlertNotification("Break complete", "Ready for the next focus interval? Open Trackaa to continue.")
                notificationManager.showActiveFocusNotification(_sessionState.value)
                return
            }
        }

        _sessionState.value = current.copy(
            elapsedFocusSeconds = totalFocus,
            elapsedPauseSeconds = totalPause,
            elapsedBreakSeconds = totalBreak,
            totalElapsedWallSeconds = (now-current.startEpochMs).coerceAtLeast(0L)/1000L,
            isOvertime = overtime > 0,
            overtimeSeconds = overtime,
            lastVerifiedEpochMs = now
        )
        tickCounter++
        if (tickCounter % 10 == 0) saveCheckpoint()
        notificationManager.showActiveFocusNotification(_sessionState.value)
    }

    private suspend fun evaluateAchievements(state: ActiveSessionState, focusSec: Long, overtimeSec: Long) {
        unlockAchievement("FIRST_FOCUS")
        if (overtimeSec >= 20*60L) unlockAchievement("OVERTIME_WARRIOR")
        val all = repository.getAllFocusSessions().first()
        val totalSec = all.sumOf { if (it.totalFocusSeconds > 0) it.totalFocusSeconds else it.totalFocusMinutes*60L }
        if (totalSec >= 10*3600L) unlockAchievement("FIRST_10_HOURS")
        if (totalSec >= 100*3600L) unlockAchievement("CENTURY_CLUB")
        val hour = Instant.ofEpochMilli(state.startEpochMs).atZone(ZoneId.systemDefault()).hour
        if (hour < 6) unlockAchievement("EARLY_BIRD")
        if (hour >= 23) unlockAchievement("NIGHT_OWL")
    }

    private suspend fun unlockAchievement(code: String) {
        repository.getAchievementByCode(code)?.takeIf { !it.isUnlocked }?.let { achievement ->
            repository.updateAchievement(achievement.copy(isUnlocked=true, unlockedAt=System.currentTimeMillis()))
            repository.insertXpEvent(XpEventEntity(source="ACHIEVEMENT", amount=100, description="Achievement Unlocked: ${achievement.title}"))
            notificationManager.showAchievementNotification(achievement.title, achievement.description)
        }
    }

    private fun saveCheckpoint() {
        val state = _sessionState.value
        if (state.status == FocusEngineStatus.IDLE) { clearCheckpoint(); return }
        val json = JSONObject().apply {
            put("status", state.status.name); put("mode", state.mode.name); put("startEpochMs", state.startEpochMs)
            put("stoppedAtEpochMs", state.stoppedAtEpochMs); put("lastVerifiedEpochMs", state.lastVerifiedEpochMs)
            put("plannedDurationMinutes", state.plannedDurationMinutes); put("pomodoroFocusMinutes", state.pomodoroFocusMinutes)
            put("pomodoroBreakMinutes", state.pomodoroBreakMinutes); put("pomodoroState", state.pomodoroState.name)
            put("currentCycleFocusSeconds", state.currentCycleFocusSeconds); put("currentCycleBreakSeconds", state.currentCycleBreakSeconds)
            put("activeTaskId", state.activeTaskId ?: -1L); put("activeTaskTitle", state.activeTaskTitle)
            put("activeWorkItemId", state.activeWorkItemId ?: -1L); put("activeWorkItemName", state.activeWorkItemName)
            put("intent", state.intent ?: ""); put("currentSegmentStartEpochMs", state.currentSegmentStartEpochMs)
            put("currentPauseStartEpochMs", state.currentPauseStartEpochMs); put("currentBreakStartEpochMs", state.currentBreakStartEpochMs)
            put("focusSegments", JSONArray().also { a -> state.completedFocusSegments.forEach { s -> a.put(JSONObject().apply { put("taskId",s.taskId);put("workItemId",s.workItemId);put("start",s.startEpochMs);put("end",s.endEpochMs);put("sec",s.durationSeconds) }) } })
            put("pauseSegments", JSONArray().also { a -> state.completedPauseSegments.forEach { s -> a.put(JSONObject().apply { put("start",s.startEpochMs);put("end",s.endEpochMs);put("sec",s.durationSeconds) }) } })
            put("breakSegments", JSONArray().also { a -> state.completedBreakSegments.forEach { s -> a.put(JSONObject().apply { put("start",s.startEpochMs);put("end",s.endEpochMs);put("sec",s.durationSeconds) }) } })
            put("interruptions", JSONArray().also { a -> state.interruptions.forEach { i -> a.put(JSONObject().apply { put("ts",i.timestampEpochMs);put("reason",i.reason);put("note",i.note ?: "") }) } })
        }
        prefs.edit().putString("active_session_json", json.toString()).commit()
    }

    private fun recoverSessionIfAvailable() {
        val raw = prefs.getString("active_session_json", null) ?: return
        runCatching {
            val j = JSONObject(raw)
            val status = FocusEngineStatus.valueOf(j.optString("status", FocusEngineStatus.IDLE.name))
            if (status == FocusEngineStatus.IDLE) return
            val taskId = j.optLong("activeTaskId", -1L).takeIf { it >= 0 }
            val workId = j.optLong("activeWorkItemId", -1L).takeIf { it >= 0 }
            fun focusList(): List<FocusSegmentEntity> = buildList {
                val a=j.optJSONArray("focusSegments") ?: JSONArray(); for (i in 0 until a.length()) { val x=a.getJSONObject(i); val sec=x.optLong("sec",(x.getLong("end")-x.getLong("start"))/1000L); add(FocusSegmentEntity(0,0,x.optLong("taskId",0),x.optLong("workItemId",0),x.getLong("start"),x.getLong("end"),sec/60L,sec)) }
            }
            fun pauseList(): List<PauseSegmentEntity> = buildList {
                val a=j.optJSONArray("pauseSegments") ?: JSONArray(); for (i in 0 until a.length()) { val x=a.getJSONObject(i); val sec=x.optLong("sec",(x.getLong("end")-x.getLong("start"))/1000L); add(PauseSegmentEntity(0,0,x.getLong("start"),x.getLong("end"),sec/60L,sec)) }
            }
            fun breakList(): List<BreakSegmentEntity> = buildList {
                val a=j.optJSONArray("breakSegments") ?: JSONArray(); for (i in 0 until a.length()) { val x=a.getJSONObject(i); val sec=x.optLong("sec",(x.getLong("end")-x.getLong("start"))/1000L); add(BreakSegmentEntity(0,0,x.getLong("start"),x.getLong("end"),sec/60L,sec)) }
            }
            val ints = buildList {
                val a=j.optJSONArray("interruptions") ?: JSONArray(); for (i in 0 until a.length()) { val x=a.getJSONObject(i); add(InterruptionEntity(0,0,x.getLong("ts"),x.getString("reason"),x.optString("note").ifBlank { null })) }
            }
            var recovered = ActiveSessionState(
                status=status, mode=SessionMode.valueOf(j.optString("mode",SessionMode.STOPWATCH.name)), startEpochMs=j.optLong("startEpochMs"),
                stoppedAtEpochMs=j.optLong("stoppedAtEpochMs"), lastVerifiedEpochMs=j.optLong("lastVerifiedEpochMs",j.optLong("startEpochMs")),
                plannedDurationMinutes=j.optLong("plannedDurationMinutes"), pomodoroFocusMinutes=j.optLong("pomodoroFocusMinutes",25), pomodoroBreakMinutes=j.optLong("pomodoroBreakMinutes",5),
                pomodoroState=PomodoroState.valueOf(j.optString("pomodoroState",PomodoroState.FOCUS.name)), currentCycleFocusSeconds=j.optLong("currentCycleFocusSeconds"), currentCycleBreakSeconds=j.optLong("currentCycleBreakSeconds"),
                activeTaskId=taskId, activeTaskTitle=j.optString("activeTaskTitle","Quick Focus"), activeWorkItemId=workId, activeWorkItemName=j.optString("activeWorkItemName","General"), intent=j.optString("intent").ifBlank { null },
                currentSegmentStartEpochMs=j.optLong("currentSegmentStartEpochMs"), currentPauseStartEpochMs=j.optLong("currentPauseStartEpochMs"), currentBreakStartEpochMs=j.optLong("currentBreakStartEpochMs"),
                completedFocusSegments=focusList(), completedPauseSegments=pauseList(), completedBreakSegments=breakList(), interruptions=ints, isRecovered=true
            )
            if (status != FocusEngineStatus.REVIEW_PENDING && status != FocusEngineStatus.BREAK_COMPLETE) {
                val verifiedEnd = recovered.lastVerifiedEpochMs.coerceAtLeast(recovered.startEpochMs)
                recovered = finalizeOpenInterval(recovered, verifiedEnd).copy(
                    status=FocusEngineStatus.REVIEW_PENDING, stoppedAtEpochMs=verifiedEnd,
                    currentSegmentStartEpochMs=0, currentPauseStartEpochMs=0, currentBreakStartEpochMs=0, isRecovered=true
                )
            }
            _sessionState.value = recovered
            notificationManager.cancelActiveFocusNotification()
            saveCheckpoint()
        }.onFailure { clearCheckpoint() }
    }

    private fun clearCheckpoint() { prefs.edit().remove("active_session_json").commit() }
}
