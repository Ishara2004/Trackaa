package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.GoalStatus
import com.example.data.model.GoalType
import com.example.data.model.OutcomeStatus
import com.example.data.model.SessionMode
import com.example.data.model.TargetPeriod
import com.example.data.model.TargetScope
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus

@Entity(tableName = "goals", indices = [Index("status"), Index("isDeleted")])
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val startDateEpochMs: Long = System.currentTimeMillis(),
    val deadlineEpochMs: Long? = null,
    val goalType: GoalType = GoalType.TIME,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val targetMinutes: Long = 0,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "work_item_types")
data class WorkItemTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isCustom: Boolean = false,
    val iconName: String = "folder"
)

@Entity(tableName = "work_items", indices = [Index("goalId"), Index("isArchived"), Index("isDeleted")])
data class WorkItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val type: String = "Module",
    val icon: String = "folder",
    val colorHex: String = "#38BDF8",
    val deadlineEpochMs: Long? = null,
    val credits: Double = 0.0,
    val targetFocusMinutes: Long = 0,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val goalId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "topics", indices = [Index("workItemId"), Index("isDeleted")])
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workItemId: Long,
    val title: String,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks", indices = [Index("topicId"), Index("workItemId"), Index("status"), Index("isDeleted")])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long? = null,
    val workItemId: Long,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.PLANNED,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val estimatedMinutes: Long = 0,
    val actualFocusMinutes: Long = 0,
    val dueDateEpochMs: Long? = null,
    val completedAtEpochMs: Long? = null,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "task_dependencies", primaryKeys = ["taskId", "dependsOnTaskId"], indices = [Index("taskId"), Index("dependsOnTaskId")])
data class TaskDependencyEntity(val taskId: Long, val dependsOnTaskId: Long)

@Entity(tableName = "focus_sessions", indices = [Index("startEpochMs"), Index("endEpochMs"), Index("isDeleted")])
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val mode: SessionMode = SessionMode.STOPWATCH,
    val plannedDurationMinutes: Long = 0,
    val overtimeMinutes: Long = 0,
    val totalFocusMinutes: Long = 0,
    val totalPauseMinutes: Long = 0,
    val totalBreakMinutes: Long = 0,
    val totalFocusSeconds: Long = totalFocusMinutes * 60,
    val totalPauseSeconds: Long = totalPauseMinutes * 60,
    val totalBreakSeconds: Long = totalBreakMinutes * 60,
    val focusQuality: Int = 3,
    val energyLevel: Int = 3,
    val intent: String? = null,
    val outcomeStatus: OutcomeStatus? = null,
    val outcomeNotes: String? = null,
    val notes: String? = null,
    val isEdited: Boolean = false,
    val editReason: String? = null,
    val isRecovered: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_segments", indices = [Index("sessionId"), Index("taskId"), Index("workItemId")])
data class FocusSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val taskId: Long,
    val workItemId: Long,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMinutes: Long,
    val durationSeconds: Long = ((endEpochMs - startEpochMs).coerceAtLeast(0L) / 1000L)
)

@Entity(tableName = "pause_segments", indices = [Index("sessionId")])
data class PauseSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMinutes: Long,
    val durationSeconds: Long = ((endEpochMs - startEpochMs).coerceAtLeast(0L) / 1000L)
)

@Entity(tableName = "break_segments", indices = [Index("sessionId")])
data class BreakSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMinutes: Long,
    val durationSeconds: Long = ((endEpochMs - startEpochMs).coerceAtLeast(0L) / 1000L)
)

@Entity(tableName = "interruptions", indices = [Index("sessionId"), Index("timestampEpochMs")])
data class InterruptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestampEpochMs: Long,
    val reason: String,
    val note: String? = null
)

@Entity(tableName = "interruption_reasons")
data class InterruptionReasonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "targets", indices = [Index("scopeType"), Index("scopeId"), Index("periodType")])
data class TargetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scopeType: TargetScope = TargetScope.GLOBAL,
    val scopeId: Long? = null,
    val periodType: TargetPeriod = TargetPeriod.DAILY,
    val minMinutes: Long = 0,
    val goalMinutes: Long = 0,
    val stretchMinutes: Long = 0,
    val startDateEpochMs: Long = 0,
    val deadlineEpochMs: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "target_revisions", indices = [Index("targetId")])
data class TargetRevisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetId: Long,
    val oldMinMinutes: Long,
    val oldGoalMinutes: Long,
    val oldStretchMinutes: Long,
    val newMinMinutes: Long,
    val newGoalMinutes: Long,
    val newStretchMinutes: Long,
    val revisedAt: Long = System.currentTimeMillis(),
    val reason: String? = null
)

@Entity(tableName = "availability")
data class AvailabilityEntity(
    @PrimaryKey val dayOfWeek: Int,
    val isAvailable: Boolean = true,
    val capacityMinutes: Long = 300
)

@Entity(tableName = "audit_events", indices = [Index("entityType"), Index("entityId"), Index("timestamp")])
data class AuditEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: Long,
    val actionType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val oldValue: String? = null,
    val newValue: String? = null,
    val note: String? = null
)

@Entity(tableName = "xp_events")
data class XpEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val amount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String
)

@Entity(tableName = "achievements", indices = [Index("code", unique = true)])
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val isSecret: Boolean = false
)

@Entity(tableName = "scheduled_focus", indices = [Index("scheduledEpochMs")])
data class ScheduledFocusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val taskId: Long? = null,
    val workItemId: Long? = null,
    val scheduledEpochMs: Long,
    val durationMinutes: Long,
    val isCompleted: Boolean = false,
    val notificationSent: Boolean = false
)

@Entity(tableName = "reporting_periods")
data class ReportingPeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val isArchived: Boolean = false
)
