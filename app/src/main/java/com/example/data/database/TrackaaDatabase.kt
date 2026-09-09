package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.TrackaaDao
import com.example.data.entity.*
import com.example.data.model.TargetPeriod
import com.example.data.model.TargetScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        GoalEntity::class,
        WorkItemTypeEntity::class,
        WorkItemEntity::class,
        TopicEntity::class,
        TaskEntity::class,
        TaskDependencyEntity::class,
        FocusSessionEntity::class,
        FocusSegmentEntity::class,
        PauseSegmentEntity::class,
        BreakSegmentEntity::class,
        InterruptionEntity::class,
        InterruptionReasonEntity::class,
        TargetEntity::class,
        TargetRevisionEntity::class,
        AvailabilityEntity::class,
        AuditEventEntity::class,
        XpEventEntity::class,
        AchievementEntity::class,
        ScheduledFocusEntity::class,
        ReportingPeriodEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TrackaaDatabase : RoomDatabase() {
    abstract fun trackaaDao(): TrackaaDao

    companion object {
        @Volatile
        private var INSTANCE: TrackaaDatabase? = null

        fun getInstance(context: Context): TrackaaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackaaDatabase::class.java,
                    "trackaa_database.db"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed default data in background
                CoroutineScope(Dispatchers.IO).launch {
                    INSTANCE?.let { database ->
                        seedDefaultData(database.trackaaDao())
                    }
                }
            }
        }

        suspend fun seedDefaultData(dao: TrackaaDao) {
            // Default WorkItem Types
            val defaultTypes = listOf(
                WorkItemTypeEntity(name = "Module", isCustom = false, iconName = "school"),
                WorkItemTypeEntity(name = "Project", isCustom = false, iconName = "code"),
                WorkItemTypeEntity(name = "Work", isCustom = false, iconName = "business_center"),
                WorkItemTypeEntity(name = "Personal", isCustom = false, iconName = "person"),
                WorkItemTypeEntity(name = "Other", isCustom = false, iconName = "more_horiz")
            )
            defaultTypes.forEach { dao.insertWorkItemType(it) }

            // Default Interruption Reasons
            val defaultReasons = listOf(
                InterruptionReasonEntity(name = "Phone", isDefault = true),
                InterruptionReasonEntity(name = "Social Media", isDefault = true),
                InterruptionReasonEntity(name = "Call", isDefault = true),
                InterruptionReasonEntity(name = "People", isDefault = true),
                InterruptionReasonEntity(name = "Tired", isDefault = true),
                InterruptionReasonEntity(name = "Food", isDefault = true),
                InterruptionReasonEntity(name = "Other", isDefault = true)
            )
            defaultReasons.forEach { dao.insertInterruptionReason(it) }

            // Default Weekly Availability (1=Mon..7=Sun)
            val defaultAvailability = listOf(
                AvailabilityEntity(dayOfWeek = 1, isAvailable = true, capacityMinutes = 300), // 5h
                AvailabilityEntity(dayOfWeek = 2, isAvailable = true, capacityMinutes = 300),
                AvailabilityEntity(dayOfWeek = 3, isAvailable = true, capacityMinutes = 300),
                AvailabilityEntity(dayOfWeek = 4, isAvailable = true, capacityMinutes = 300),
                AvailabilityEntity(dayOfWeek = 5, isAvailable = true, capacityMinutes = 300),
                AvailabilityEntity(dayOfWeek = 6, isAvailable = true, capacityMinutes = 180), // 3h
                AvailabilityEntity(dayOfWeek = 7, isAvailable = false, capacityMinutes = 0)   // Rest
            )
            dao.insertAvailability(defaultAvailability)

            // Default Achievements
            val defaultAchievements = listOf(
                AchievementEntity(code = "FIRST_FOCUS", title = "First Focus", description = "Completed your first deep work session", iconName = "timer"),
                AchievementEntity(code = "FIRST_10_HOURS", title = "First 10 Hours", description = "Accumulated 10 hours of verified focus", iconName = "hourglass_top"),
                AchievementEntity(code = "CENTURY_CLUB", title = "Century Club", description = "Accumulated 100 hours of deep work", iconName = "workspace_premium"),
                AchievementEntity(code = "STREAK_7", title = "7-Day Streak", description = "Maintained a 7-day focus streak", iconName = "local_fire_department"),
                AchievementEntity(code = "STREAK_30", title = "Iron Focus", description = "Maintained a 30-day focus streak", iconName = "military_tech"),
                AchievementEntity(code = "FORTY_HOUR_WEEK", title = "40-Hour Week", description = "Focused 40 hours in a single week", iconName = "trending_up"),
                AchievementEntity(code = "TARGET_CRUSHER", title = "Target Crusher", description = "Reached your daily stretch target", iconName = "rocket_launch"),
                AchievementEntity(code = "EARLY_BIRD", title = "Early Bird", description = "Completed a session starting before 6:00 AM", iconName = "wb_twilight", isSecret = true),
                AchievementEntity(code = "NIGHT_OWL", title = "Night Owl", description = "Completed a session starting after 11:00 PM", iconName = "nights_stay", isSecret = true),
                AchievementEntity(code = "OVERTIME_WARRIOR", title = "Overtime Warrior", description = "Achieved 20+ minutes of countdown overtime", iconName = "speed", isSecret = true)
            )
            dao.insertAchievements(defaultAchievements)

            // Default Global Daily Target (Min: 3h=180m, Goal: 5h=300m, Stretch: 7h=420m)
            val defaultDailyTarget = TargetEntity(
                scopeType = TargetScope.GLOBAL,
                periodType = TargetPeriod.DAILY,
                minMinutes = 180,
                goalMinutes = 300,
                stretchMinutes = 420,
                startDateEpochMs = System.currentTimeMillis()
            )
            dao.insertTarget(defaultDailyTarget)

            // Default Global Weekly Target (Min: 15h=900m, Goal: 25h=1500m, Stretch: 35h=2100m)
            val defaultWeeklyTarget = TargetEntity(
                scopeType = TargetScope.GLOBAL,
                periodType = TargetPeriod.WEEKLY,
                minMinutes = 900,
                goalMinutes = 1500,
                stretchMinutes = 2100,
                startDateEpochMs = System.currentTimeMillis()
            )
            dao.insertTarget(defaultWeeklyTarget)
        }
    }
}
