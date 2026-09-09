package com.example.domain.focus

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
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

class FocusEngine(
    private val context: Context,
    private val repository: TrackaaRepository,
    private val notificationManager: FocusNotificationManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("trackaa_focus_checkpoint", Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow(ActiveSessionState())
    val sessionState: StateFlow<ActiveSessionState> = _sessionState.asStateFlow()

    private var tickerJob: Job? = null
    private var countdownAlertFired = false
    private var pomodoroAlertFired = false

    init {
        // Attempt recovery of any previous active session upon startup
        recoverSessionIfAvailable()
    }

    /**
     * Start a new Focus Session
     */
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
        val now = System.currentTimeMillis()
        countdownAlertFired = false
        pomodoroAlertFired = false

        _sessionState.value = ActiveSessionState(
            status = FocusEngineStatus.FOCUSING,
            mode = mode,
            startEpochMs = now,
            plannedDurationMinutes = plannedDurationMinutes,
            pomodoroFocusMinutes = pomodoroFocusMinutes,
            pomodoroBreakMinutes = pomodoroBreakMinutes,
            pomodoroState = PomodoroState.FOCUS,
            activeTaskId = taskId,
            activeTaskTitle = taskTitle,
            activeWorkItemId = workItemId,
            activeWorkItemName = workItemName,
            intent = intent,
            currentSegmentStartEpochMs = now,
            currentPauseStartEpochMs = 0L,
            currentBreakStartEpochMs = 0L,
            completedFocusSegments = emptyList(),
            completedPauseSegments = emptyList(),
            completedBreakSegments = emptyList(),
            interruptions = emptyList()
        )

        saveCheckpoint()
        startTicker()
        notificationManager.showActiveFocusNotification(_sessionState.value)
    }

    /**
     * Pause the active focus session
     */
    fun pauseFocus() {
        val current = _sessionState.value
        if (current.status != FocusEngineStatus.FOCUSING) return

        val now = System.currentTimeMillis()
        // Finalize current focus segment
        val segDuration = ((now - current.currentSegmentStartEpochMs) / 1000)
        val newFocusSegment = FocusSegmentEntity(
            sessionId = 0,
            taskId = current.activeTaskId ?: 0L,
            workItemId = current.activeWorkItemId ?: 0L,
            startEpochMs = current.currentSegmentStartEpochMs,
            endEpochMs = now,
            durationMinutes = Math.max(1, segDuration / 60)
        )

        _sessionState.value = current.copy(
            status = FocusEngineStatus.PAUSED,
            currentPauseStartEpochMs = now,
            currentSegmentStartEpochMs = 0L,
            completedFocusSegments = current.completedFocusSegments + newFocusSegment
        )

        saveCheckpoint()
        notificationManager.showActiveFocusNotification(_sessionState.value)
    }

    /**
     * Resume from paused state
     */
    fun resumeFocus() {
        val current = _sessionState.value
        if (current.status != FocusEngineStatus.PAUSED) return

        val now = System.currentTimeMillis()
        // Finalize pause segment
        val pauseDuration = ((now - current.currentPauseStartEpochMs) / 1000)
        val newPauseSegment = PauseSegmentEntity(
            sessionId = 0,
            startEpochMs = current.currentPauseStartEpochMs,
            endEpochMs = now,
            durationMinutes = Math.max(0, pauseDuration / 60)
        )

        _sessionState.value = current.copy(
            status = FocusEngineStatus.FOCUSING,
            currentSegmentStartEpochMs = now,
            currentPauseStartEpochMs = 0L,
            completedPauseSegments = current.completedPauseSegments + newPauseSegment
        )

        saveCheckpoint()
        notificationManager.showActiveFocusNotification(_sessionState.value)
    }

    /**
     * Start a break (manual or pomodoro transition)
     */
    fun startBreak() {
        val current = _sessionState.value
        val now = System.currentTimeMillis()

        // If focusing, close focus segment
        val completedFocus = if (current.status == FocusEngineStatus.FOCUSING) {
            val segDuration = ((now - current.currentSegmentStartEpochMs) / 1000)
            val seg = FocusSegmentEntity(
                sessionId = 0,
                taskId = current.activeTaskId ?: 0L,
                workItemId = current.activeWorkItemId ?: 0L,
                startEpochMs = current.currentSegmentStartEpochMs,
                endEpochMs = now,
                durationMinutes = Math.max(1, segDuration / 60)
            )
            current.completedFocusSegments + seg
        } else current.completedFocusSegments

        _sessionState.value = current.copy(
            status = FocusEngineStatus.ON_BREAK,
            pomodoroState = PomodoroState.BREAK,
            currentBreakStartEpochMs = now,
            currentSegmentStartEpochMs = 0L,
            currentPauseStartEpochMs = 0L,
            completedFocusSegments = completedFocus
        )

        saveCheckpoint()
        notificationManager.showActiveFocusNotification(_sessionState.value)
    }

    /**
     * Resume focus from break (e.g. Pomodoro start next focus)
     */
    fun endBreakAndResumeFocus() {
        val current = _sessionState.value
        if (current.status != FocusEngineStatus.ON_BREAK) return

        val now = System.currentTimeMillis()
        val breakDuration = ((now - current.currentBreakStartEpochMs) / 1000)
        val newBreakSegment = BreakSegmentEntity(
            sessionId = 0,
            startEpochMs = current.currentBreakStartEpochMs,
            endEpochMs = now,
            durationMinutes = Math.max(0, breakDuration / 60)
        )

        _sessionState.value = current.copy(
            status = FocusEngineStatus.FOCUSING,
            pomodoroState = PomodoroState.FOCUS,
            currentSegmentStartEpochMs = now,
            currentBreakStartEpochMs = 0L,
            completedBreakSegments = current.completedBreakSegments + newBreakSegment
        )
        pomodoroAlertFired = false

        saveCheckpoint()
        notificationManager.showActiveFocusNotification(_sessionState.value)
    }

    /**
     * Switch active task during a session without ending the session
     */
    fun switchTask(newTaskId: Long, newTaskTitle: String, newWorkItemId: Long, newWorkItemName: String) {
        val current = _sessionState.value
        val now = System.currentTimeMillis()

        if (current.status == FocusEngineStatus.FOCUSING) {
            val segDuration = ((now - current.currentSegmentStartEpochMs) / 1000)
            val seg = FocusSegmentEntity(
                sessionId = 0,
                taskId = current.activeTaskId ?: 0L,
                workItemId = current.activeWorkItemId ?: 0L,
                startEpochMs = current.currentSegmentStartEpochMs,
                endEpochMs = now,
                durationMinutes = Math.max(1, segDuration / 60)
            )
            _sessionState.value = current.copy(
                activeTaskId = newTaskId,
                activeTaskTitle = newTaskTitle,
                activeWorkItemId = newWorkItemId,
                activeWorkItemName = newWorkItemName,
                currentSegmentStartEpochMs = now,
                completedFocusSegments = current.completedFocusSegments + seg
            )
        } else {
            _sessionState.value = current.copy(
                activeTaskId = newTaskId,
                activeTaskTitle = newTaskTitle,
                activeWorkItemId = newWorkItemId,
                activeWorkItemName = newWorkItemName
            )
        }

        saveCheckpoint()
        notificationManager.showActiveFocusNotification(_sessionState.value)
    }

    /**
     * Record an interruption during active focus
     */
    fun addInterruption(reason: String, note: String? = null) {
        val current = _sessionState.value
        val now = System.currentTimeMillis()
        val interruption = InterruptionEntity(
            sessionId = 0,
            timestampEpochMs = now,
            reason = reason,
            note = note
        )
        _sessionState.value = current.copy(
            interruptions = current.interruptions + interruption
        )
        saveCheckpoint()
    }

    /**
     * Request stop session: transitions into REVIEW_PENDING state
     */
    fun requestStopSession() {
        val current = _sessionState.value
        if (current.status == FocusEngineStatus.IDLE) return

        val now = System.currentTimeMillis()
        var focusSegments = current.completedFocusSegments
        var pauseSegments = current.completedPauseSegments
        var breakSegments = current.completedBreakSegments

        if (current.status == FocusEngineStatus.FOCUSING) {
            val segDuration = ((now - current.currentSegmentStartEpochMs) / 1000)
            val seg = FocusSegmentEntity(
                sessionId = 0,
                taskId = current.activeTaskId ?: 0L,
                workItemId = current.activeWorkItemId ?: 0L,
                startEpochMs = current.currentSegmentStartEpochMs,
                endEpochMs = now,
                durationMinutes = Math.max(1, segDuration / 60)
            )
            focusSegments = focusSegments + seg
        } else if (current.status == FocusEngineStatus.PAUSED) {
            val pDuration = ((now - current.currentPauseStartEpochMs) / 1000)
            val p = PauseSegmentEntity(
                sessionId = 0,
                startEpochMs = current.currentPauseStartEpochMs,
                endEpochMs = now,
                durationMinutes = Math.max(0, pDuration / 60)
            )
            pauseSegments = pauseSegments + p
        } else if (current.status == FocusEngineStatus.ON_BREAK) {
            val bDuration = ((now - current.currentBreakStartEpochMs) / 1000)
            val b = BreakSegmentEntity(
                sessionId = 0,
                startEpochMs = current.currentBreakStartEpochMs,
                endEpochMs = now,
                durationMinutes = Math.max(0, bDuration / 60)
            )
            breakSegments = breakSegments + b
        }

        stopTicker()
        notificationManager.cancelActiveFocusNotification()

        _sessionState.value = current.copy(
            status = FocusEngineStatus.REVIEW_PENDING,
            currentSegmentStartEpochMs = 0L,
            currentPauseStartEpochMs = 0L,
            currentBreakStartEpochMs = 0L,
            completedFocusSegments = focusSegments,
            completedPauseSegments = pauseSegments,
            completedBreakSegments = breakSegments
        )
        saveCheckpoint()
    }

    /**
     * Complete and save session review (mandatory quality and energy)
     */
    fun completeSessionReview(
        focusQuality: Int,
        energyLevel: Int,
        outcomeStatus: OutcomeStatus?,
        outcomeNotes: String?,
        notes: String?,
        onSaved: () -> Unit = {}
    ) {
        val current = _sessionState.value
        val now = System.currentTimeMillis()

        scope.launch {
            val totalFocusSec = current.completedFocusSegments.sumOf { (it.endEpochMs - it.startEpochMs) / 1000 }
            val totalPauseSec = current.completedPauseSegments.sumOf { (it.endEpochMs - it.startEpochMs) / 1000 }
            val totalBreakSec = current.completedBreakSegments.sumOf { (it.endEpochMs - it.startEpochMs) / 1000 }

            val totalFocusMin = Math.max(1, totalFocusSec / 60)
            val totalPauseMin = totalPauseSec / 60
            val totalBreakMin = totalBreakSec / 60

            val plannedMin = current.plannedDurationMinutes
            val overtimeMin = if (current.mode == SessionMode.COUNTDOWN && totalFocusMin > plannedMin) {
                totalFocusMin - plannedMin
            } else 0L

            val sessionEntity = FocusSessionEntity(
                startEpochMs = current.startEpochMs,
                endEpochMs = now,
                mode = current.mode,
                plannedDurationMinutes = plannedMin,
                overtimeMinutes = overtimeMin,
                totalFocusMinutes = totalFocusMin,
                totalPauseMinutes = totalPauseMin,
                totalBreakMinutes = totalBreakMin,
                focusQuality = focusQuality.coerceIn(1, 5),
                energyLevel = energyLevel.coerceIn(1, 5),
                intent = current.intent,
                outcomeStatus = outcomeStatus,
                outcomeNotes = outcomeNotes,
                notes = notes,
                isEdited = false,
                isRecovered = false
            )

            val sessionId = repository.insertFocusSession(sessionEntity)

            // Save focus segments linked to real session ID
            val segmentsWithSession = current.completedFocusSegments.map { it.copy(sessionId = sessionId) }
            repository.insertFocusSegments(segmentsWithSession)

            // Save pause segments
            val pausesWithSession = current.completedPauseSegments.map { it.copy(sessionId = sessionId) }
            repository.insertPauseSegments(pausesWithSession)

            // Save break segments
            val breaksWithSession = current.completedBreakSegments.map { it.copy(sessionId = sessionId) }
            repository.insertBreakSegments(breaksWithSession)

            // Save interruptions
            for (interruption in current.interruptions) {
                repository.insertInterruption(interruption.copy(sessionId = sessionId))
            }

            // Update tasks actual focus minutes
            val tasksAllocated = segmentsWithSession.groupBy { it.taskId }
            for ((taskId, segs) in tasksAllocated) {
                if (taskId > 0L) {
                    val task = repository.getTaskById(taskId)
                    if (task != null) {
                        val addedMinutes = segs.sumOf { it.durationMinutes }
                        repository.updateTask(task.copy(actualFocusMinutes = task.actualFocusMinutes + addedMinutes))
                    }
                }
            }

            // Gamification XP calculation:
            // 1 XP per minute of verified focus time + bonus for high energy/streak
            val baseXP = totalFocusMin.toInt()
            repository.insertXpEvent(
                XpEventEntity(
                    source = "FOCUS_SESSION",
                    amount = baseXP,
                    description = "Focused for $totalFocusMin min ($focusQuality/5 quality)"
                )
            )

            // Evaluate achievements
            evaluateAchievements(totalFocusMin, overtimeMin)

            // Clear state and checkpoint
            clearCheckpoint()
            _sessionState.value = ActiveSessionState(status = FocusEngineStatus.IDLE)

            withContext(Dispatchers.Main) {
                onSaved()
            }
        }
    }

    /**
     * Discard an active or pending review session
     */
    fun discardSession() {
        stopTicker()
        clearCheckpoint()
        notificationManager.cancelActiveFocusNotification()
        _sessionState.value = ActiveSessionState(status = FocusEngineStatus.IDLE)
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000)
                updateTick()
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updateTick() {
        val current = _sessionState.value
        if (current.status == FocusEngineStatus.IDLE || current.status == FocusEngineStatus.REVIEW_PENDING) return

        val now = System.currentTimeMillis()
        var activeSegmentSec = 0L
        if (current.status == FocusEngineStatus.FOCUSING && current.currentSegmentStartEpochMs > 0) {
            activeSegmentSec = (now - current.currentSegmentStartEpochMs) / 1000
        }

        var activePauseSec = 0L
        if (current.status == FocusEngineStatus.PAUSED && current.currentPauseStartEpochMs > 0) {
            activePauseSec = (now - current.currentPauseStartEpochMs) / 1000
        }

        var activeBreakSec = 0L
        if (current.status == FocusEngineStatus.ON_BREAK && current.currentBreakStartEpochMs > 0) {
            activeBreakSec = (now - current.currentBreakStartEpochMs) / 1000
        }

        val completedFocusSec = current.completedFocusSegments.sumOf { (it.endEpochMs - it.startEpochMs) / 1000 }
        val completedPauseSec = current.completedPauseSegments.sumOf { (it.endEpochMs - it.startEpochMs) / 1000 }
        val completedBreakSec = current.completedBreakSegments.sumOf { (it.endEpochMs - it.startEpochMs) / 1000 }

        val totalFocusSec = completedFocusSec + activeSegmentSec
        val totalPauseSec = completedPauseSec + activePauseSec
        val totalBreakSec = completedBreakSec + activeBreakSec
        val wallSec = (now - current.startEpochMs) / 1000

        // Countdown Mode check
        var isOvertime = false
        var overtimeSec = 0L
        if (current.mode == SessionMode.COUNTDOWN && current.plannedDurationMinutes > 0) {
            val plannedSec = current.plannedDurationMinutes * 60
            if (totalFocusSec >= plannedSec) {
                isOvertime = true
                overtimeSec = totalFocusSec - plannedSec
                if (!countdownAlertFired) {
                    countdownAlertFired = true
                    notificationManager.showTimerAlertNotification(
                        title = "Countdown Reached Planned Time!",
                        message = "Target of ${current.plannedDurationMinutes}m reached. Entering overtime."
                    )
                }
            }
        }

        // Pomodoro Mode check
        if (current.mode == SessionMode.POMODORO) {
            if (current.pomodoroState == PomodoroState.FOCUS) {
                val pomodoroFocusSec = current.pomodoroFocusMinutes * 60
                if (totalFocusSec >= pomodoroFocusSec && !pomodoroAlertFired) {
                    pomodoroAlertFired = true
                    notificationManager.showTimerAlertNotification(
                        title = "Focus Interval Complete!",
                        message = "Time for a ${current.pomodoroBreakMinutes}m break."
                    )
                    // Automatically transition to break
                    startBreak()
                    return
                }
            } else if (current.pomodoroState == PomodoroState.BREAK) {
                val pomodoroBreakSec = current.pomodoroBreakMinutes * 60
                if (totalBreakSec >= pomodoroBreakSec && !pomodoroAlertFired) {
                    pomodoroAlertFired = true
                    notificationManager.showTimerAlertNotification(
                        title = "Break Complete!",
                        message = "Ready for the next focus interval? Tap to resume."
                    )
                    // Pause break so user must explicitly click Start Next Focus
                    pauseFocus()
                    return
                }
            }
        }

        val updated = current.copy(
            elapsedFocusSeconds = totalFocusSec,
            elapsedPauseSeconds = totalPauseSec,
            elapsedBreakSeconds = totalBreakSec,
            totalElapsedWallSeconds = wallSec,
            isOvertime = isOvertime,
            overtimeSeconds = overtimeSec
        )
        _sessionState.value = updated

        // Update ongoing notification
        notificationManager.showActiveFocusNotification(updated)
    }

    private suspend fun evaluateAchievements(totalFocusMin: Long, overtimeMin: Long) {
        // Unlock First Focus
        unlockAchievement("FIRST_FOCUS")
        if (overtimeMin >= 20) {
            unlockAchievement("OVERTIME_WARRIOR")
        }
        // Additional achievements evaluated based on total focus in database
    }

    private suspend fun unlockAchievement(code: String) {
        val achievement = repository.getAchievementByCode(code)
        if (achievement != null && !achievement.isUnlocked) {
            repository.updateAchievement(
                achievement.copy(
                    isUnlocked = true,
                    unlockedAt = System.currentTimeMillis()
                )
            )
            repository.insertXpEvent(
                XpEventEntity(
                    source = "ACHIEVEMENT",
                    amount = 100,
                    description = "Achievement Unlocked: ${achievement.title}"
                )
            )
            notificationManager.showAchievementNotification(achievement.title, achievement.description)
        }
    }

    // --- CRASH & REBOOT RECOVERY CHECKPOINTING ---
    private fun saveCheckpoint() {
        val state = _sessionState.value
        if (state.status == FocusEngineStatus.IDLE) {
            clearCheckpoint()
            return
        }

        val json = JSONObject().apply {
            put("status", state.status.name)
            put("mode", state.mode.name)
            put("startEpochMs", state.startEpochMs)
            put("plannedDurationMinutes", state.plannedDurationMinutes)
            put("pomodoroFocusMinutes", state.pomodoroFocusMinutes)
            put("pomodoroBreakMinutes", state.pomodoroBreakMinutes)
            put("pomodoroState", state.pomodoroState.name)
            put("activeTaskId", state.activeTaskId ?: -1L)
            put("activeTaskTitle", state.activeTaskTitle)
            put("activeWorkItemId", state.activeWorkItemId ?: -1L)
            put("activeWorkItemName", state.activeWorkItemName)
            put("intent", state.intent ?: "")
            put("currentSegmentStartEpochMs", state.currentSegmentStartEpochMs)
            put("currentPauseStartEpochMs", state.currentPauseStartEpochMs)
            put("currentBreakStartEpochMs", state.currentBreakStartEpochMs)

            // Focus segments
            val segArray = JSONArray()
            state.completedFocusSegments.forEach {
                segArray.put(JSONObject().apply {
                    put("taskId", it.taskId)
                    put("workItemId", it.workItemId)
                    put("start", it.startEpochMs)
                    put("end", it.endEpochMs)
                    put("dur", it.durationMinutes)
                })
            }
            put("focusSegments", segArray)

            // Pause segments
            val pauseArray = JSONArray()
            state.completedPauseSegments.forEach {
                pauseArray.put(JSONObject().apply {
                    put("start", it.startEpochMs)
                    put("end", it.endEpochMs)
                    put("dur", it.durationMinutes)
                })
            }
            put("pauseSegments", pauseArray)

            // Break segments
            val breakArray = JSONArray()
            state.completedBreakSegments.forEach {
                breakArray.put(JSONObject().apply {
                    put("start", it.startEpochMs)
                    put("end", it.endEpochMs)
                    put("dur", it.durationMinutes)
                })
            }
            put("breakSegments", breakArray)

            // Interruptions
            val intArr = JSONArray()
            state.interruptions.forEach {
                intArr.put(JSONObject().apply {
                    put("ts", it.timestampEpochMs)
                    put("reason", it.reason)
                    put("note", it.note ?: "")
                })
            }
            put("interruptions", intArr)
        }

        prefs.edit().putString("active_session_json", json.toString()).apply()
    }

    private fun recoverSessionIfAvailable() {
        val savedJson = prefs.getString("active_session_json", null) ?: return
        runCatching {
            val json = JSONObject(savedJson)
            val statusStr = json.optString("status", FocusEngineStatus.IDLE.name)
            val status = FocusEngineStatus.valueOf(statusStr)
            if (status == FocusEngineStatus.IDLE) return

            val mode = SessionMode.valueOf(json.optString("mode", SessionMode.STOPWATCH.name))
            val startEpochMs = json.optLong("startEpochMs", System.currentTimeMillis())
            val plannedDur = json.optLong("plannedDurationMinutes", 0L)
            val pomoFocus = json.optLong("pomodoroFocusMinutes", 25L)
            val pomoBreak = json.optLong("pomodoroBreakMinutes", 5L)
            val pomoState = PomodoroState.valueOf(json.optString("pomodoroState", PomodoroState.FOCUS.name))
            val activeTaskId = json.optLong("activeTaskId", -1L).let { if (it == -1L) null else it }
            val activeTaskTitle = json.optString("activeTaskTitle", "Focus")
            val activeWorkItemId = json.optLong("activeWorkItemId", -1L).let { if (it == -1L) null else it }
            val activeWorkItemName = json.optString("activeWorkItemName", "General")
            val intent = json.optString("intent").ifEmpty { null }
            val curSegStart = json.optLong("currentSegmentStartEpochMs", 0L)
            val curPauseStart = json.optLong("currentPauseStartEpochMs", 0L)
            val curBreakStart = json.optLong("currentBreakStartEpochMs", 0L)

            val focusSegs = mutableListOf<FocusSegmentEntity>()
            val segArray = json.optJSONArray("focusSegments")
            if (segArray != null) {
                for (i in 0 until segArray.length()) {
                    val item = segArray.getJSONObject(i)
                    focusSegs.add(
                        FocusSegmentEntity(
                            sessionId = 0,
                            taskId = item.getLong("taskId"),
                            workItemId = item.getLong("workItemId"),
                            startEpochMs = item.getLong("start"),
                            endEpochMs = item.getLong("end"),
                            durationMinutes = item.getLong("dur")
                        )
                    )
                }
            }

            val pauseSegs = mutableListOf<PauseSegmentEntity>()
            val pauseArray = json.optJSONArray("pauseSegments")
            if (pauseArray != null) {
                for (i in 0 until pauseArray.length()) {
                    val item = pauseArray.getJSONObject(i)
                    pauseSegs.add(
                        PauseSegmentEntity(
                            sessionId = 0,
                            startEpochMs = item.getLong("start"),
                            endEpochMs = item.getLong("end"),
                            durationMinutes = item.getLong("dur")
                        )
                    )
                }
            }

            val breakSegs = mutableListOf<BreakSegmentEntity>()
            val breakArray = json.optJSONArray("breakSegments")
            if (breakArray != null) {
                for (i in 0 until breakArray.length()) {
                    val item = breakArray.getJSONObject(i)
                    breakSegs.add(
                        BreakSegmentEntity(
                            sessionId = 0,
                            startEpochMs = item.getLong("start"),
                            endEpochMs = item.getLong("end"),
                            durationMinutes = item.getLong("dur")
                        )
                    )
                }
            }

            val ints = mutableListOf<InterruptionEntity>()
            val intArr = json.optJSONArray("interruptions")
            if (intArr != null) {
                for (i in 0 until intArr.length()) {
                    val item = intArr.getJSONObject(i)
                    ints.add(
                        InterruptionEntity(
                            sessionId = 0,
                            timestampEpochMs = item.getLong("ts"),
                            reason = item.getString("reason"),
                            note = item.optString("note").ifEmpty { null }
                        )
                    )
                }
            }

            _sessionState.value = ActiveSessionState(
                status = status,
                mode = mode,
                startEpochMs = startEpochMs,
                plannedDurationMinutes = plannedDur,
                pomodoroFocusMinutes = pomoFocus,
                pomodoroBreakMinutes = pomoBreak,
                pomodoroState = pomoState,
                activeTaskId = activeTaskId,
                activeTaskTitle = activeTaskTitle,
                activeWorkItemId = activeWorkItemId,
                activeWorkItemName = activeWorkItemName,
                intent = intent,
                currentSegmentStartEpochMs = curSegStart,
                currentPauseStartEpochMs = curPauseStart,
                currentBreakStartEpochMs = curBreakStart,
                completedFocusSegments = focusSegs,
                completedPauseSegments = pauseSegs,
                completedBreakSegments = breakSegs,
                interruptions = ints
            )

            if (status == FocusEngineStatus.FOCUSING || status == FocusEngineStatus.PAUSED || status == FocusEngineStatus.ON_BREAK) {
                startTicker()
                notificationManager.showActiveFocusNotification(_sessionState.value)
            }
        }
    }

    private fun clearCheckpoint() {
        prefs.edit().remove("active_session_json").apply()
    }
}
