package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.*

@Dao
interface BackupDao {
    @Query("SELECT * FROM goals") suspend fun goals(): List<GoalEntity>
    @Query("SELECT * FROM work_item_types") suspend fun workItemTypes(): List<WorkItemTypeEntity>
    @Query("SELECT * FROM work_items") suspend fun workItems(): List<WorkItemEntity>
    @Query("SELECT * FROM topics") suspend fun topics(): List<TopicEntity>
    @Query("SELECT * FROM tasks") suspend fun tasks(): List<TaskEntity>
    @Query("SELECT * FROM task_dependencies") suspend fun taskDependencies(): List<TaskDependencyEntity>
    @Query("SELECT * FROM focus_sessions") suspend fun focusSessions(): List<FocusSessionEntity>
    @Query("SELECT * FROM focus_segments") suspend fun focusSegments(): List<FocusSegmentEntity>
    @Query("SELECT * FROM pause_segments") suspend fun pauseSegments(): List<PauseSegmentEntity>
    @Query("SELECT * FROM break_segments") suspend fun breakSegments(): List<BreakSegmentEntity>
    @Query("SELECT * FROM interruptions") suspend fun interruptions(): List<InterruptionEntity>
    @Query("SELECT * FROM interruption_reasons") suspend fun interruptionReasons(): List<InterruptionReasonEntity>
    @Query("SELECT * FROM targets") suspend fun targets(): List<TargetEntity>
    @Query("SELECT * FROM target_revisions") suspend fun targetRevisions(): List<TargetRevisionEntity>
    @Query("SELECT * FROM availability") suspend fun availability(): List<AvailabilityEntity>
    @Query("SELECT * FROM audit_events") suspend fun auditEvents(): List<AuditEventEntity>
    @Query("SELECT * FROM xp_events") suspend fun xpEvents(): List<XpEventEntity>
    @Query("SELECT * FROM achievements") suspend fun achievements(): List<AchievementEntity>
    @Query("SELECT * FROM scheduled_focus") suspend fun scheduledFocus(): List<ScheduledFocusEntity>
    @Query("SELECT * FROM reporting_periods") suspend fun reportingPeriods(): List<ReportingPeriodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertGoals(v: List<GoalEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWorkItemTypes(v: List<WorkItemTypeEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWorkItems(v: List<WorkItemEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTopics(v: List<TopicEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTasks(v: List<TaskEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTaskDependencies(v: List<TaskDependencyEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFocusSessions(v: List<FocusSessionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFocusSegments(v: List<FocusSegmentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPauseSegments(v: List<PauseSegmentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBreakSegments(v: List<BreakSegmentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertInterruptions(v: List<InterruptionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertInterruptionReasons(v: List<InterruptionReasonEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTargets(v: List<TargetEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTargetRevisions(v: List<TargetRevisionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAvailability(v: List<AvailabilityEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAuditEvents(v: List<AuditEventEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertXpEvents(v: List<XpEventEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAchievements(v: List<AchievementEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertScheduledFocus(v: List<ScheduledFocusEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReportingPeriods(v: List<ReportingPeriodEntity>)

    @Query("DELETE FROM goals") suspend fun clearGoals()
    @Query("DELETE FROM work_item_types") suspend fun clearWorkItemTypes()
    @Query("DELETE FROM work_items") suspend fun clearWorkItems()
    @Query("DELETE FROM topics") suspend fun clearTopics()
    @Query("DELETE FROM tasks") suspend fun clearTasks()
    @Query("DELETE FROM task_dependencies") suspend fun clearTaskDependencies()
    @Query("DELETE FROM focus_sessions") suspend fun clearFocusSessions()
    @Query("DELETE FROM focus_segments") suspend fun clearFocusSegments()
    @Query("DELETE FROM pause_segments") suspend fun clearPauseSegments()
    @Query("DELETE FROM break_segments") suspend fun clearBreakSegments()
    @Query("DELETE FROM interruptions") suspend fun clearInterruptions()
    @Query("DELETE FROM interruption_reasons") suspend fun clearInterruptionReasons()
    @Query("DELETE FROM targets") suspend fun clearTargets()
    @Query("DELETE FROM target_revisions") suspend fun clearTargetRevisions()
    @Query("DELETE FROM availability") suspend fun clearAvailability()
    @Query("DELETE FROM audit_events") suspend fun clearAuditEvents()
    @Query("DELETE FROM xp_events") suspend fun clearXpEvents()
    @Query("DELETE FROM achievements") suspend fun clearAchievements()
    @Query("DELETE FROM scheduled_focus") suspend fun clearScheduledFocus()
    @Query("DELETE FROM reporting_periods") suspend fun clearReportingPeriods()
}
