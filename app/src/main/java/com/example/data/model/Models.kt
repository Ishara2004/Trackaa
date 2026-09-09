package com.example.data.model

enum class GoalType {
    TIME,
    COMPLETION,
    DEADLINE
}

enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED
}

enum class TaskStatus {
    BACKLOG,
    PLANNED,
    IN_PROGRESS,
    BLOCKED,
    COMPLETED
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class SessionMode {
    STOPWATCH,
    COUNTDOWN,
    POMODORO
}

enum class PomodoroState {
    FOCUS,
    BREAK
}

enum class OutcomeStatus {
    COMPLETED,
    PARTIALLY_COMPLETED,
    NOT_COMPLETED
}

enum class TargetPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    DEADLINE
}

enum class TargetScope {
    GLOBAL,
    GOAL,
    WORK_ITEM
}

enum class TimeOfDaySlot {
    MORNING,   // 05:00 - 11:59
    AFTERNOON, // 12:00 - 16:59
    EVENING,   // 17:00 - 20:59
    NIGHT      // 21:00 - 04:59
}

enum class ThemeSetting {
    SYSTEM,
    LIGHT,
    DARK
}

enum class DndPauseBehavior {
    KEEP_ACTIVE,
    SUSPEND_WHILE_PAUSED
}
