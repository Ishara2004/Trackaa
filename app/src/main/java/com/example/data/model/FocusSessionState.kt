package com.example.data.model

import com.example.data.entity.BreakSegmentEntity
import com.example.data.entity.FocusSegmentEntity
import com.example.data.entity.InterruptionEntity
import com.example.data.entity.PauseSegmentEntity

enum class FocusEngineStatus {
    IDLE,
    FOCUSING,
    PAUSED,
    ON_BREAK,
    BREAK_COMPLETE,
    REVIEW_PENDING
}

data class ActiveSessionState(
    val status: FocusEngineStatus = FocusEngineStatus.IDLE,
    val mode: SessionMode = SessionMode.STOPWATCH,
    val startEpochMs: Long = 0L,
    val stoppedAtEpochMs: Long = 0L,
    val lastVerifiedEpochMs: Long = 0L,
    val plannedDurationMinutes: Long = 0L,
    val pomodoroFocusMinutes: Long = 25L,
    val pomodoroBreakMinutes: Long = 5L,
    val pomodoroState: PomodoroState = PomodoroState.FOCUS,
    val activeTaskId: Long? = null,
    val activeTaskTitle: String = "Quick Focus",
    val activeWorkItemId: Long? = null,
    val activeWorkItemName: String = "General",
    val intent: String? = null,
    val currentSegmentStartEpochMs: Long = 0L,
    val currentPauseStartEpochMs: Long = 0L,
    val currentBreakStartEpochMs: Long = 0L,
    val completedFocusSegments: List<FocusSegmentEntity> = emptyList(),
    val completedPauseSegments: List<PauseSegmentEntity> = emptyList(),
    val completedBreakSegments: List<BreakSegmentEntity> = emptyList(),
    val interruptions: List<InterruptionEntity> = emptyList(),
    val isOvertime: Boolean = false,
    val overtimeSeconds: Long = 0L,
    val elapsedFocusSeconds: Long = 0L,
    val elapsedPauseSeconds: Long = 0L,
    val elapsedBreakSeconds: Long = 0L,
    val totalElapsedWallSeconds: Long = 0L,
    val isRecovered: Boolean = false
)
