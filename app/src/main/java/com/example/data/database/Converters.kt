package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.*

class Converters {
    @TypeConverter
    fun fromGoalStatus(value: GoalStatus): String = value.name
    @TypeConverter
    fun toGoalStatus(value: String): GoalStatus = runCatching { GoalStatus.valueOf(value) }.getOrDefault(GoalStatus.ACTIVE)

    @TypeConverter
    fun fromGoalType(value: GoalType): String = value.name
    @TypeConverter
    fun toGoalType(value: String): GoalType = runCatching { GoalType.valueOf(value) }.getOrDefault(GoalType.TIME)

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus): String = value.name
    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = runCatching { TaskStatus.valueOf(value) }.getOrDefault(TaskStatus.PLANNED)

    @TypeConverter
    fun fromTaskPriority(value: TaskPriority): String = value.name
    @TypeConverter
    fun toTaskPriority(value: String): TaskPriority = runCatching { TaskPriority.valueOf(value) }.getOrDefault(TaskPriority.MEDIUM)

    @TypeConverter
    fun fromSessionMode(value: SessionMode): String = value.name
    @TypeConverter
    fun toSessionMode(value: String): SessionMode = runCatching { SessionMode.valueOf(value) }.getOrDefault(SessionMode.STOPWATCH)

    @TypeConverter
    fun fromOutcomeStatus(value: OutcomeStatus?): String? = value?.name
    @TypeConverter
    fun toOutcomeStatus(value: String?): OutcomeStatus? = value?.let { runCatching { OutcomeStatus.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromTargetPeriod(value: TargetPeriod): String = value.name
    @TypeConverter
    fun toTargetPeriod(value: String): TargetPeriod = runCatching { TargetPeriod.valueOf(value) }.getOrDefault(TargetPeriod.DAILY)

    @TypeConverter
    fun fromTargetScope(value: TargetScope): String = value.name
    @TypeConverter
    fun toTargetScope(value: String): TargetScope = runCatching { TargetScope.valueOf(value) }.getOrDefault(TargetScope.GLOBAL)
}
