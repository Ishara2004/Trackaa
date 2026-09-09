package com.example.data.repository

import com.example.data.dao.TrackaaDao
import com.example.data.entity.*
import com.example.data.model.GoalStatus
import com.example.data.model.TargetPeriod
import com.example.data.model.TargetScope
import com.example.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

class TrackaaRepository(private val dao: TrackaaDao) {

    // Goals
    fun getAllGoals(): Flow<List<GoalEntity>> = dao.getAllGoals()
    fun getGoalsByStatus(status: GoalStatus): Flow<List<GoalEntity>> = dao.getGoalsByStatus(status)
    suspend fun getGoalById(id: Long): GoalEntity? = dao.getGoalById(id)
    suspend fun insertGoal(goal: GoalEntity): Long = dao.insertGoal(goal)
    suspend fun updateGoal(goal: GoalEntity) = dao.updateGoal(goal)
    suspend fun softDeleteGoal(id: Long) {
        dao.softDeleteGoal(id)
        dao.insertAuditEvent(
            AuditEventEntity(
                entityType = "GOAL",
                entityId = id,
                actionType = "SOFT_DELETE",
                note = "Goal moved to Trash"
            )
        )
    }
    suspend fun restoreGoal(id: Long) {
        dao.restoreGoal(id)
        dao.insertAuditEvent(
            AuditEventEntity(
                entityType = "GOAL",
                entityId = id,
                actionType = "RESTORE",
                note = "Goal restored from Trash"
            )
        )
    }
    suspend fun permanentlyDeleteGoal(id: Long) = dao.permanentlyDeleteGoal(id)

    // WorkItems
    fun getAllActiveWorkItems(): Flow<List<WorkItemEntity>> = dao.getAllActiveWorkItems()
    fun getAllWorkItems(): Flow<List<WorkItemEntity>> = dao.getAllWorkItems()
    fun getWorkItemsByGoal(goalId: Long): Flow<List<WorkItemEntity>> = dao.getWorkItemsByGoal(goalId)
    suspend fun getWorkItemById(id: Long): WorkItemEntity? = dao.getWorkItemById(id)
    suspend fun insertWorkItem(item: WorkItemEntity): Long = dao.insertWorkItem(item)
    suspend fun updateWorkItem(item: WorkItemEntity) = dao.updateWorkItem(item)
    suspend fun softDeleteWorkItem(id: Long) {
        dao.softDeleteWorkItem(id)
        dao.insertAuditEvent(
            AuditEventEntity(
                entityType = "WORK_ITEM",
                entityId = id,
                actionType = "SOFT_DELETE",
                note = "WorkItem moved to Trash"
            )
        )
    }
    suspend fun restoreWorkItem(id: Long) = dao.restoreWorkItem(id)
    suspend fun permanentlyDeleteWorkItem(id: Long) = dao.permanentlyDeleteWorkItem(id)

    // WorkItem Types
    fun getAllWorkItemTypes(): Flow<List<WorkItemTypeEntity>> = dao.getAllWorkItemTypes()
    suspend fun insertWorkItemType(type: WorkItemTypeEntity): Long = dao.insertWorkItemType(type)

    // Topics
    fun getTopicsByWorkItem(workItemId: Long): Flow<List<TopicEntity>> = dao.getTopicsByWorkItem(workItemId)
    fun getAllTopics(): Flow<List<TopicEntity>> = dao.getAllTopics()
    suspend fun getTopicById(id: Long): TopicEntity? = dao.getTopicById(id)
    suspend fun insertTopic(topic: TopicEntity): Long = dao.insertTopic(topic)
    suspend fun updateTopic(topic: TopicEntity) = dao.updateTopic(topic)
    suspend fun softDeleteTopic(id: Long) = dao.softDeleteTopic(id)
    suspend fun restoreTopic(id: Long) = dao.restoreTopic(id)
    suspend fun permanentlyDeleteTopic(id: Long) = dao.permanentlyDeleteTopic(id)

    // Tasks
    fun getAllTasks(): Flow<List<TaskEntity>> = dao.getAllTasks()
    fun getTasksByWorkItem(workItemId: Long): Flow<List<TaskEntity>> = dao.getTasksByWorkItem(workItemId)
    fun getTasksByTopic(topicId: Long): Flow<List<TaskEntity>> = dao.getTasksByTopic(topicId)
    fun getFavoriteTasks(): Flow<List<TaskEntity>> = dao.getFavoriteTasks()
    fun getRecentTasks(limit: Int = 5): Flow<List<TaskEntity>> = dao.getRecentTasks(limit)
    suspend fun getTaskById(id: Long): TaskEntity? = dao.getTaskById(id)
    suspend fun insertTask(task: TaskEntity): Long = dao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = dao.updateTask(task)
    suspend fun softDeleteTask(id: Long) = dao.softDeleteTask(id)
    suspend fun restoreTask(id: Long) = dao.restoreTask(id)
    suspend fun permanentlyDeleteTask(id: Long) = dao.permanentlyDeleteTask(id)

    // Dependencies
    suspend fun getDependenciesForTask(taskId: Long): List<Long> = dao.getDependenciesForTask(taskId)
    suspend fun getDependencyTasks(taskId: Long): List<TaskEntity> = dao.getDependencyTasks(taskId)
    suspend fun insertTaskDependency(dependency: TaskDependencyEntity) = dao.insertTaskDependency(dependency)
    suspend fun removeTaskDependency(taskId: Long, dependsOnTaskId: Long) = dao.removeTaskDependency(taskId, dependsOnTaskId)

    // Focus Sessions
    fun getAllFocusSessions(): Flow<List<FocusSessionEntity>> = dao.getAllFocusSessions()
    fun getFocusSessionsInRange(startEpochMs: Long, endEpochMs: Long): Flow<List<FocusSessionEntity>> = dao.getFocusSessionsInRange(startEpochMs, endEpochMs)
    suspend fun getFocusSessionsInRangeOnce(startEpochMs: Long, endEpochMs: Long): List<FocusSessionEntity> = dao.getFocusSessionsInRangeOnce(startEpochMs, endEpochMs)
    suspend fun getFocusSessionById(id: Long): FocusSessionEntity? = dao.getFocusSessionById(id)
    suspend fun insertFocusSession(session: FocusSessionEntity): Long = dao.insertFocusSession(session)
    suspend fun updateFocusSession(session: FocusSessionEntity) = dao.updateFocusSession(session)
    suspend fun softDeleteFocusSession(id: Long) {
        dao.softDeleteFocusSession(id)
        dao.insertAuditEvent(
            AuditEventEntity(
                entityType = "FOCUS_SESSION",
                entityId = id,
                actionType = "SOFT_DELETE",
                note = "Focus session moved to Trash"
            )
        )
    }
    suspend fun restoreFocusSession(id: Long) = dao.restoreFocusSession(id)
    suspend fun permanentlyDeleteFocusSession(id: Long) = dao.permanentlyDeleteFocusSession(id)
    suspend fun getOverlappingSessions(excludeId: Long, startEpochMs: Long, endEpochMs: Long): List<FocusSessionEntity> =
        dao.getOverlappingSessions(excludeId, startEpochMs, endEpochMs)

    // Segments
    fun getFocusSegmentsForSession(sessionId: Long): Flow<List<FocusSegmentEntity>> = dao.getFocusSegmentsForSession(sessionId)
    suspend fun getFocusSegmentsForSessionOnce(sessionId: Long): List<FocusSegmentEntity> = dao.getFocusSegmentsForSessionOnce(sessionId)
    suspend fun insertFocusSegments(segments: List<FocusSegmentEntity>) = dao.insertFocusSegments(segments)
    suspend fun deleteFocusSegmentsForSession(sessionId: Long) = dao.deleteFocusSegmentsForSession(sessionId)

    suspend fun getPauseSegmentsForSession(sessionId: Long): List<PauseSegmentEntity> = dao.getPauseSegmentsForSession(sessionId)
    suspend fun insertPauseSegments(segments: List<PauseSegmentEntity>) = dao.insertPauseSegments(segments)
    suspend fun deletePauseSegmentsForSession(sessionId: Long) = dao.deletePauseSegmentsForSession(sessionId)

    suspend fun getBreakSegmentsForSession(sessionId: Long): List<BreakSegmentEntity> = dao.getBreakSegmentsForSession(sessionId)
    suspend fun insertBreakSegments(segments: List<BreakSegmentEntity>) = dao.insertBreakSegments(segments)
    suspend fun deleteBreakSegmentsForSession(sessionId: Long) = dao.deleteBreakSegmentsForSession(sessionId)

    // Interruptions
    fun getInterruptionsForSession(sessionId: Long): Flow<List<InterruptionEntity>> = dao.getInterruptionsForSession(sessionId)
    suspend fun getInterruptionsForSessionOnce(sessionId: Long): List<InterruptionEntity> = dao.getInterruptionsForSessionOnce(sessionId)
    fun getAllInterruptions(): Flow<List<InterruptionEntity>> = dao.getAllInterruptions()
    suspend fun insertInterruption(interruption: InterruptionEntity): Long = dao.insertInterruption(interruption)
    fun getAllInterruptionReasons(): Flow<List<InterruptionReasonEntity>> = dao.getAllInterruptionReasons()
    suspend fun insertInterruptionReason(reason: InterruptionReasonEntity): Long = dao.insertInterruptionReason(reason)

    // Targets & Revisions
    fun getAllTargets(): Flow<List<TargetEntity>> = dao.getAllTargets()
    fun getTargetsByScopeAndPeriod(scopeType: TargetScope, periodType: TargetPeriod): Flow<List<TargetEntity>> = dao.getTargetsByScopeAndPeriod(scopeType, periodType)
    fun getGlobalTarget(periodType: TargetPeriod): Flow<TargetEntity?> = dao.getGlobalTarget(periodType)
    suspend fun getTargetById(id: Long): TargetEntity? = dao.getTargetById(id)
    suspend fun insertTarget(target: TargetEntity): Long = dao.insertTarget(target)
    suspend fun updateTarget(target: TargetEntity) = dao.updateTarget(target)
    suspend fun deleteTarget(target: TargetEntity) = dao.deleteTarget(target)
    fun getRevisionsForTarget(targetId: Long): Flow<List<TargetRevisionEntity>> = dao.getRevisionsForTarget(targetId)
    suspend fun insertTargetRevision(revision: TargetRevisionEntity): Long = dao.insertTargetRevision(revision)

    // Availability
    fun getAllAvailability(): Flow<List<AvailabilityEntity>> = dao.getAllAvailability()
    suspend fun getAllAvailabilityOnce(): List<AvailabilityEntity> = dao.getAllAvailabilityOnce()
    suspend fun insertAvailability(availability: List<AvailabilityEntity>) = dao.insertAvailability(availability)
    suspend fun updateAvailability(availability: AvailabilityEntity) = dao.updateAvailability(availability)

    // Audit Events
    fun getRecentAuditEvents(limit: Int = 100): Flow<List<AuditEventEntity>> = dao.getRecentAuditEvents(limit)
    suspend fun insertAuditEvent(event: AuditEventEntity): Long = dao.insertAuditEvent(event)

    // XP & Achievements
    fun getAllXpEvents(): Flow<List<XpEventEntity>> = dao.getAllXpEvents()
    fun getTotalXp(): Flow<Int> = dao.getTotalXp()
    suspend fun insertXpEvent(event: XpEventEntity): Long = dao.insertXpEvent(event)
    fun getAllAchievements(): Flow<List<AchievementEntity>> = dao.getAllAchievements()
    suspend fun getAchievementByCode(code: String): AchievementEntity? = dao.getAchievementByCode(code)
    suspend fun updateAchievement(achievement: AchievementEntity) = dao.updateAchievement(achievement)

    // Scheduled Focus
    fun getUpcomingScheduledFocus(fromEpochMs: Long): Flow<List<ScheduledFocusEntity>> = dao.getUpcomingScheduledFocus(fromEpochMs)
    suspend fun insertScheduledFocus(scheduled: ScheduledFocusEntity): Long = dao.insertScheduledFocus(scheduled)
    suspend fun updateScheduledFocus(scheduled: ScheduledFocusEntity) = dao.updateScheduledFocus(scheduled)
    suspend fun deleteScheduledFocus(scheduled: ScheduledFocusEntity) = dao.deleteScheduledFocus(scheduled)

    // Reporting Periods
    fun getAllReportingPeriods(): Flow<List<ReportingPeriodEntity>> = dao.getAllReportingPeriods()
    suspend fun insertReportingPeriod(period: ReportingPeriodEntity): Long = dao.insertReportingPeriod(period)
    suspend fun updateReportingPeriod(period: ReportingPeriodEntity) = dao.updateReportingPeriod(period)

    // Trash
    suspend fun getDeletedGoals(): List<GoalEntity> = dao.getDeletedGoals()
    suspend fun getDeletedWorkItems(): List<WorkItemEntity> = dao.getDeletedWorkItems()
    suspend fun getDeletedTopics(): List<TopicEntity> = dao.getDeletedTopics()
    suspend fun getDeletedTasks(): List<TaskEntity> = dao.getDeletedTasks()
    suspend fun getDeletedFocusSessions(): List<FocusSessionEntity> = dao.getDeletedFocusSessions()
    suspend fun purgeOldTrash(olderThanEpochMs: Long) {
        dao.purgeOldDeletedGoals(olderThanEpochMs)
        dao.purgeOldDeletedWorkItems(olderThanEpochMs)
        dao.purgeOldDeletedTopics(olderThanEpochMs)
        dao.purgeOldDeletedTasks(olderThanEpochMs)
        dao.purgeOldDeletedFocusSessions(olderThanEpochMs)
    }

    // Demo Data Seeder & Cleaner
    suspend fun insertDemoWorkspace() {
        // Goal
        val goalId = dao.insertGoal(
            GoalEntity(
                title = "Semester 3 Academic Excellence",
                description = "Master core algorithms and software engineering principles",
                targetMinutes = 2400, // 40 hours
                createdAt = System.currentTimeMillis() - 86400000L * 7
            )
        )
        // WorkItem 1: DSA
        val dsaId = dao.insertWorkItem(
            WorkItemEntity(
                name = "Data Structures & Algorithms",
                description = "Advanced tree structures, graphs, and dynamic programming",
                type = "Module",
                icon = "school",
                colorHex = "#38BDF8",
                credits = 4.0,
                targetFocusMinutes = 1200,
                goalId = goalId
            )
        )
        // Topic 1
        val treeTopicId = dao.insertTopic(
            TopicEntity(
                workItemId = dsaId,
                title = "Self-Balancing Trees",
                notes = "AVL and Red-Black trees",
                isCompleted = false,
                sortOrder = 1
            )
        )
        // Tasks
        val bstTaskId = dao.insertTask(
            TaskEntity(
                topicId = treeTopicId,
                workItemId = dsaId,
                title = "Review Binary Search Trees",
                description = "Recursion vs iteration in tree traversal",
                status = TaskStatus.COMPLETED,
                estimatedMinutes = 120,
                actualFocusMinutes = 115,
                completedAtEpochMs = System.currentTimeMillis() - 86400000L * 2
            )
        )
        val avlTaskId = dao.insertTask(
            TaskEntity(
                topicId = treeTopicId,
                workItemId = dsaId,
                title = "Learn AVL Rotations",
                description = "LL, RR, LR, RL rotation implementations",
                status = TaskStatus.IN_PROGRESS,
                estimatedMinutes = 180,
                actualFocusMinutes = 90
            )
        )
        // Dependency: AVL depends on BST
        dao.insertTaskDependency(TaskDependencyEntity(taskId = avlTaskId, dependsOnTaskId = bstTaskId))

        // WorkItem 2: Database Systems
        val dbId = dao.insertWorkItem(
            WorkItemEntity(
                name = "Database Systems",
                description = "Relational algebra, normalization, query optimization",
                type = "Module",
                icon = "storage",
                colorHex = "#F59E0B",
                credits = 3.0,
                targetFocusMinutes = 900,
                goalId = goalId
            )
        )
        val normalTopicId = dao.insertTopic(
            TopicEntity(
                workItemId = dbId,
                title = "Normalization & Functional Dependencies",
                notes = "1NF through BCNF",
                isCompleted = true,
                sortOrder = 1
            )
        )
        dao.insertTask(
            TaskEntity(
                topicId = normalTopicId,
                workItemId = dbId,
                title = "BCNF Decomposition Practice",
                description = "Solve past exam papers",
                status = TaskStatus.COMPLETED,
                estimatedMinutes = 150,
                actualFocusMinutes = 140,
                completedAtEpochMs = System.currentTimeMillis() - 86400000L
            )
        )

        // Historical focus sessions for realistic analytics
        val now = System.currentTimeMillis()
        val session1Start = now - 86400000L * 2 + 18000000L // 2 days ago morning
        val session1End = session1Start + 75 * 60000L
        val sess1Id = dao.insertFocusSession(
            FocusSessionEntity(
                startEpochMs = session1Start,
                endEpochMs = session1End,
                totalFocusMinutes = 70,
                totalPauseMinutes = 5,
                focusQuality = 5,
                energyLevel = 4,
                intent = "Understand BST deletion edge cases",
                outcomeStatus = com.example.data.model.OutcomeStatus.COMPLETED,
                outcomeNotes = "Solved all corner cases with two children",
                notes = "Strong focus session without phone distractions"
            )
        )
        dao.insertFocusSegment(
            FocusSegmentEntity(
                sessionId = sess1Id,
                taskId = bstTaskId,
                workItemId = dsaId,
                startEpochMs = session1Start,
                endEpochMs = session1End - 5 * 60000L,
                durationMinutes = 70
            )
        )
        dao.insertPauseSegment(
            PauseSegmentEntity(
                sessionId = sess1Id,
                startEpochMs = session1End - 5 * 60000L,
                endEpochMs = session1End,
                durationMinutes = 5
            )
        )

        // Add 50 XP
        dao.insertXpEvent(XpEventEntity(source = "SESSION", amount = 70, description = "Completed 70m focus session"))
    }
}
