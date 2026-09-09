package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BackupDao
import com.example.data.dao.TrackaaDao
import com.example.data.entity.*
import com.example.data.model.TargetPeriod
import com.example.data.model.TargetScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [GoalEntity::class, WorkItemTypeEntity::class, WorkItemEntity::class, TopicEntity::class,
        TaskEntity::class, TaskDependencyEntity::class, FocusSessionEntity::class, FocusSegmentEntity::class,
        PauseSegmentEntity::class, BreakSegmentEntity::class, InterruptionEntity::class,
        InterruptionReasonEntity::class, TargetEntity::class, TargetRevisionEntity::class,
        AvailabilityEntity::class, AuditEventEntity::class, XpEventEntity::class,
        AchievementEntity::class, ScheduledFocusEntity::class, ReportingPeriodEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TrackaaDatabase : RoomDatabase() {
    abstract fun trackaaDao(): TrackaaDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile private var INSTANCE: TrackaaDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN totalFocusSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN totalPauseSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN totalBreakSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE focus_segments ADD COLUMN durationSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pause_segments ADD COLUMN durationSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE break_segments ADD COLUMN durationSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE focus_sessions SET totalFocusSeconds = totalFocusMinutes * 60, totalPauseSeconds = totalPauseMinutes * 60, totalBreakSeconds = totalBreakMinutes * 60")
                db.execSQL("UPDATE focus_segments SET durationSeconds = MAX(0, (endEpochMs - startEpochMs) / 1000)")
                db.execSQL("UPDATE pause_segments SET durationSeconds = MAX(0, (endEpochMs - startEpochMs) / 1000)")
                db.execSQL("UPDATE break_segments SET durationSeconds = MAX(0, (endEpochMs - startEpochMs) / 1000)")
            }
        }

        fun getInstance(context: Context): TrackaaDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, TrackaaDatabase::class.java, "trackaa_database.db")
                .addMigrations(MIGRATION_1_2)
                .addCallback(DatabaseCallback())
                .build()
                .also { INSTANCE = it }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch { INSTANCE?.let { seedDefaultData(it.trackaaDao()) } }
            }
        }

        suspend fun seedDefaultData(dao: TrackaaDao) {
            listOf(
                WorkItemTypeEntity(name = "Module", iconName = "school"),
                WorkItemTypeEntity(name = "Project", iconName = "code"),
                WorkItemTypeEntity(name = "Work", iconName = "business_center"),
                WorkItemTypeEntity(name = "Personal", iconName = "person"),
                WorkItemTypeEntity(name = "Other", iconName = "more_horiz")
            ).forEach { dao.insertWorkItemType(it) }

            listOf("Phone", "Social Media", "Call", "People", "Tired", "Food", "Other")
                .forEach { dao.insertInterruptionReason(InterruptionReasonEntity(name = it, isDefault = true)) }

            dao.insertAvailability(listOf(
                AvailabilityEntity(1, true, 300), AvailabilityEntity(2, true, 300),
                AvailabilityEntity(3, true, 300), AvailabilityEntity(4, true, 300),
                AvailabilityEntity(5, true, 300), AvailabilityEntity(6, true, 180),
                AvailabilityEntity(7, false, 0)
            ))

            dao.insertAchievements(listOf(
                AchievementEntity(code="FIRST_FOCUS", title="First Focus", description="Completed your first deep work session", iconName="timer"),
                AchievementEntity(code="FIRST_10_HOURS", title="First 10 Hours", description="Accumulated 10 hours of verified focus", iconName="hourglass_top"),
                AchievementEntity(code="CENTURY_CLUB", title="Century Club", description="Accumulated 100 hours of deep work", iconName="workspace_premium"),
                AchievementEntity(code="STREAK_7", title="7-Day Streak", description="Maintained a 7-day focus streak", iconName="local_fire_department"),
                AchievementEntity(code="STREAK_30", title="Iron Focus", description="Maintained a 30-day focus streak", iconName="military_tech"),
                AchievementEntity(code="FORTY_HOUR_WEEK", title="40-Hour Week", description="Focused 40 hours in a single week", iconName="trending_up"),
                AchievementEntity(code="TARGET_CRUSHER", title="Target Crusher", description="Reached your daily stretch target", iconName="rocket_launch"),
                AchievementEntity(code="EARLY_BIRD", title="Early Bird", description="Completed a session starting before 6:00 AM", iconName="wb_twilight", isSecret=true),
                AchievementEntity(code="NIGHT_OWL", title="Night Owl", description="Completed a session starting after 11:00 PM", iconName="nights_stay", isSecret=true),
                AchievementEntity(code="OVERTIME_WARRIOR", title="Overtime Warrior", description="Achieved 20+ minutes of countdown overtime", iconName="speed", isSecret=true)
            ))

            dao.insertTarget(TargetEntity(scopeType=TargetScope.GLOBAL, periodType=TargetPeriod.DAILY,
                minMinutes=180, goalMinutes=300, stretchMinutes=420, startDateEpochMs=System.currentTimeMillis()))
            dao.insertTarget(TargetEntity(scopeType=TargetScope.GLOBAL, periodType=TargetPeriod.WEEKLY,
                minMinutes=900, goalMinutes=1500, stretchMinutes=2100, startDateEpochMs=System.currentTimeMillis()))
        }
    }
}
