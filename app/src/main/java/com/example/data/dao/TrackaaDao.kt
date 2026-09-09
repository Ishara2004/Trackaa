package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.*
import com.example.data.model.GoalStatus
import com.example.data.model.TargetPeriod
import com.example.data.model.TargetScope
import com.example.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackaaDao {

    // --- GOALS ---
    @Query("SELECT * FROM goals WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE isDeleted = 0 AND status = :status ORDER BY createdAt DESC")
    fun getGoalsByStatus(status: GoalStatus): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id AND isDeleted = 0")
    suspend fun getGoalById(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("UPDATE goals SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteGoal(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE goals SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreGoal(id: Long)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun permanentlyDeleteGoal(id: Long)

    // --- WORK ITEMS ---
    @Query("SELECT * FROM work_items WHERE isDeleted = 0 AND isArchived = 0 ORDER BY name ASC")
    fun getAllActiveWorkItems(): Flow<List<WorkItemEntity>>

    @Query("SELECT * FROM work_items WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllWorkItems(): Flow<List<WorkItemEntity>>

    @Query("SELECT * FROM work_items WHERE goalId = :goalId AND isDeleted = 0")
    fun getWorkItemsByGoal(goalId: Long): Flow<List<WorkItemEntity>>

    @Query("SELECT * FROM work_items WHERE id = :id AND isDeleted = 0")
    suspend fun getWorkItemById(id: Long): WorkItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkItem(item: WorkItemEntity): Long

    @Update
    suspend fun updateWorkItem(item: WorkItemEntity)

    @Query("UPDATE work_items SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteWorkItem(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE work_items SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreWorkItem(id: Long)

    @Query("DELETE FROM work_items WHERE id = :id")
    suspend fun permanentlyDeleteWorkItem(id: Long)

    // --- WORK ITEM TYPES ---
    @Query("SELECT * FROM work_item_types ORDER BY id ASC")
    fun getAllWorkItemTypes(): Flow<List<WorkItemTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWorkItemType(type: WorkItemTypeEntity): Long

    // --- TOPICS ---
    @Query("SELECT * FROM topics WHERE workItemId = :workItemId AND isDeleted = 0 ORDER BY sortOrder ASC, createdAt ASC")
    fun getTopicsByWorkItem(workItemId: Long): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE isDeleted = 0")
    fun getAllTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE id = :id AND isDeleted = 0")
    suspend fun getTopicById(id: Long): TopicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Query("UPDATE topics SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteTopic(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE topics SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreTopic(id: Long)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun permanentlyDeleteTopic(id: Long)

    // --- TASKS ---
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE workItemId = :workItemId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getTasksByWorkItem(workItemId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE topicId = :topicId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getTasksByTopic(topicId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isFavorite = 1 AND isDeleted = 0")
    fun getFavoriteTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id AND isDeleted = 0")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteTask(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreTask(id: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun permanentlyDeleteTask(id: Long)

    // --- TASK DEPENDENCIES ---
    @Query("SELECT dependsOnTaskId FROM task_dependencies WHERE taskId = :taskId")
    suspend fun getDependenciesForTask(taskId: Long): List<Long>

    @Query("SELECT * FROM tasks WHERE id IN (SELECT dependsOnTaskId FROM task_dependencies WHERE taskId = :taskId) AND isDeleted = 0")
    suspend fun getDependencyTasks(taskId: Long): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskDependency(dependency: TaskDependencyEntity)

    @Query("DELETE FROM task_dependencies WHERE taskId = :taskId AND dependsOnTaskId = :dependsOnTaskId")
    suspend fun removeTaskDependency(taskId: Long, dependsOnTaskId: Long)

    // --- FOCUS SESSIONS ---
    @Query("SELECT * FROM focus_sessions WHERE isDeleted = 0 ORDER BY startEpochMs DESC")
    fun getAllFocusSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE isDeleted = 0 AND startEpochMs >= :startEpochMs AND endEpochMs <= :endEpochMs ORDER BY startEpochMs ASC")
    fun getFocusSessionsInRange(startEpochMs: Long, endEpochMs: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE isDeleted = 0 AND startEpochMs >= :startEpochMs AND endEpochMs <= :endEpochMs ORDER BY startEpochMs ASC")
    suspend fun getFocusSessionsInRangeOnce(startEpochMs: Long, endEpochMs: Long): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions WHERE id = :id")
    suspend fun getFocusSessionById(id: Long): FocusSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSessionEntity): Long

    @Update
    suspend fun updateFocusSession(session: FocusSessionEntity)

    @Query("UPDATE focus_sessions SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteFocusSession(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE focus_sessions SET isDeleted = 0, deletedAt = NULL WHERE id = :id")
    suspend fun restoreFocusSession(id: Long)

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    suspend fun permanentlyDeleteFocusSession(id: Long)

    // Check overlapping sessions
    @Query("SELECT * FROM focus_sessions WHERE isDeleted = 0 AND id != :excludeId AND ((startEpochMs <= :startEpochMs AND endEpochMs > :startEpochMs) OR (startEpochMs < :endEpochMs AND endEpochMs >= :endEpochMs) OR (startEpochMs >= :startEpochMs AND endEpochMs <= :endEpochMs))")
    suspend fun getOverlappingSessions(excludeId: Long, startEpochMs: Long, endEpochMs: Long): List<FocusSessionEntity>

    // --- FOCUS SEGMENTS ---
    @Query("SELECT * FROM focus_segments WHERE sessionId = :sessionId ORDER BY startEpochMs ASC")
    fun getFocusSegmentsForSession(sessionId: Long): Flow<List<FocusSegmentEntity>>

    @Query("SELECT * FROM focus_segments WHERE sessionId = :sessionId ORDER BY startEpochMs ASC")
    suspend fun getFocusSegmentsForSessionOnce(sessionId: Long): List<FocusSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSegment(segment: FocusSegmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSegments(segments: List<FocusSegmentEntity>)

    @Query("DELETE FROM focus_segments WHERE sessionId = :sessionId")
    suspend fun deleteFocusSegmentsForSession(sessionId: Long)

    // --- PAUSE SEGMENTS ---
    @Query("SELECT * FROM pause_segments WHERE sessionId = :sessionId ORDER BY startEpochMs ASC")
    suspend fun getPauseSegmentsForSession(sessionId: Long): List<PauseSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPauseSegment(segment: PauseSegmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPauseSegments(segments: List<PauseSegmentEntity>)

    @Query("DELETE FROM pause_segments WHERE sessionId = :sessionId")
    suspend fun deletePauseSegmentsForSession(sessionId: Long)

    // --- BREAK SEGMENTS ---
    @Query("SELECT * FROM break_segments WHERE sessionId = :sessionId ORDER BY startEpochMs ASC")
    suspend fun getBreakSegmentsForSession(sessionId: Long): List<BreakSegmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreakSegment(segment: BreakSegmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBreakSegments(segments: List<BreakSegmentEntity>)

    @Query("DELETE FROM break_segments WHERE sessionId = :sessionId")
    suspend fun deleteBreakSegmentsForSession(sessionId: Long)

    // --- INTERRUPTIONS ---
    @Query("SELECT * FROM interruptions WHERE sessionId = :sessionId ORDER BY timestampEpochMs ASC")
    fun getInterruptionsForSession(sessionId: Long): Flow<List<InterruptionEntity>>

    @Query("SELECT * FROM interruptions WHERE sessionId = :sessionId ORDER BY timestampEpochMs ASC")
    suspend fun getInterruptionsForSessionOnce(sessionId: Long): List<InterruptionEntity>

    @Query("SELECT * FROM interruptions ORDER BY timestampEpochMs DESC")
    fun getAllInterruptions(): Flow<List<InterruptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterruption(interruption: InterruptionEntity): Long

    @Query("DELETE FROM interruptions WHERE sessionId = :sessionId")
    suspend fun deleteInterruptionsForSession(sessionId: Long)

    // --- INTERRUPTION REASONS ---
    @Query("SELECT * FROM interruption_reasons ORDER BY id ASC")
    fun getAllInterruptionReasons(): Flow<List<InterruptionReasonEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInterruptionReason(reason: InterruptionReasonEntity): Long

    // --- TARGETS ---
    @Query("SELECT * FROM targets ORDER BY createdAt DESC")
    fun getAllTargets(): Flow<List<TargetEntity>>

    @Query("SELECT * FROM targets WHERE scopeType = :scopeType AND periodType = :periodType")
    fun getTargetsByScopeAndPeriod(scopeType: TargetScope, periodType: TargetPeriod): Flow<List<TargetEntity>>

    @Query("SELECT * FROM targets WHERE scopeType = 'GLOBAL' AND periodType = :periodType LIMIT 1")
    fun getGlobalTarget(periodType: TargetPeriod): Flow<TargetEntity?>

    @Query("SELECT * FROM targets WHERE id = :id")
    suspend fun getTargetById(id: Long): TargetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: TargetEntity): Long

    @Update
    suspend fun updateTarget(target: TargetEntity)

    @Delete
    suspend fun deleteTarget(target: TargetEntity)

    // --- TARGET REVISIONS ---
    @Query("SELECT * FROM target_revisions WHERE targetId = :targetId ORDER BY revisedAt DESC")
    fun getRevisionsForTarget(targetId: Long): Flow<List<TargetRevisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargetRevision(revision: TargetRevisionEntity): Long

    // --- AVAILABILITY ---
    @Query("SELECT * FROM availability ORDER BY dayOfWeek ASC")
    fun getAllAvailability(): Flow<List<AvailabilityEntity>>

    @Query("SELECT * FROM availability ORDER BY dayOfWeek ASC")
    suspend fun getAllAvailabilityOnce(): List<AvailabilityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvailability(availability: List<AvailabilityEntity>)

    @Update
    suspend fun updateAvailability(availability: AvailabilityEntity)

    // --- AUDIT EVENTS ---
    @Query("SELECT * FROM audit_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAuditEvents(limit: Int = 100): Flow<List<AuditEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditEvent(event: AuditEventEntity): Long

    // --- XP & ACHIEVEMENTS ---
    @Query("SELECT * FROM xp_events ORDER BY timestamp DESC")
    fun getAllXpEvents(): Flow<List<XpEventEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_events")
    fun getTotalXp(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertXpEvent(event: XpEventEntity): Long

    @Query("SELECT * FROM achievements ORDER BY id ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE code = :code LIMIT 1")
    suspend fun getAchievementByCode(code: String): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    // --- SCHEDULED FOCUS ---
    @Query("SELECT * FROM scheduled_focus WHERE isCompleted = 0 AND scheduledEpochMs >= :fromEpochMs ORDER BY scheduledEpochMs ASC")
    fun getUpcomingScheduledFocus(fromEpochMs: Long): Flow<List<ScheduledFocusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledFocus(scheduled: ScheduledFocusEntity): Long

    @Update
    suspend fun updateScheduledFocus(scheduled: ScheduledFocusEntity)

    @Delete
    suspend fun deleteScheduledFocus(scheduled: ScheduledFocusEntity)

    // --- REPORTING PERIODS ---
    @Query("SELECT * FROM reporting_periods WHERE isArchived = 0 ORDER BY startEpochMs DESC")
    fun getAllReportingPeriods(): Flow<List<ReportingPeriodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReportingPeriod(period: ReportingPeriodEntity): Long

    @Update
    suspend fun updateReportingPeriod(period: ReportingPeriodEntity)

    // --- TRASH & RECOVERY ---
    @Query("SELECT * FROM goals WHERE isDeleted = 1")
    suspend fun getDeletedGoals(): List<GoalEntity>

    @Query("SELECT * FROM work_items WHERE isDeleted = 1")
    suspend fun getDeletedWorkItems(): List<WorkItemEntity>

    @Query("SELECT * FROM topics WHERE isDeleted = 1")
    suspend fun getDeletedTopics(): List<TopicEntity>

    @Query("SELECT * FROM tasks WHERE isDeleted = 1")
    suspend fun getDeletedTasks(): List<TaskEntity>

    @Query("SELECT * FROM focus_sessions WHERE isDeleted = 1")
    suspend fun getDeletedFocusSessions(): List<FocusSessionEntity>

    @Query("DELETE FROM goals WHERE isDeleted = 1 AND deletedAt < :olderThanEpochMs")
    suspend fun purgeOldDeletedGoals(olderThanEpochMs: Long)

    @Query("DELETE FROM work_items WHERE isDeleted = 1 AND deletedAt < :olderThanEpochMs")
    suspend fun purgeOldDeletedWorkItems(olderThanEpochMs: Long)

    @Query("DELETE FROM topics WHERE isDeleted = 1 AND deletedAt < :olderThanEpochMs")
    suspend fun purgeOldDeletedTopics(olderThanEpochMs: Long)

    @Query("DELETE FROM tasks WHERE isDeleted = 1 AND deletedAt < :olderThanEpochMs")
    suspend fun purgeOldDeletedTasks(olderThanEpochMs: Long)

    @Query("DELETE FROM focus_sessions WHERE isDeleted = 1 AND deletedAt < :olderThanEpochMs")
    suspend fun purgeOldDeletedFocusSessions(olderThanEpochMs: Long)

    // --- RECENT TASKS FOR QUICK START ---
    @Query("SELECT t.* FROM tasks t INNER JOIN focus_segments fs ON t.id = fs.taskId WHERE t.isDeleted = 0 GROUP BY t.id ORDER BY MAX(fs.endEpochMs) DESC LIMIT :limit")
    fun getRecentTasks(limit: Int = 5): Flow<List<TaskEntity>>
}
