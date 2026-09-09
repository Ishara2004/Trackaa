package com.example

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.backup.BackupManager
import com.example.data.database.TrackaaDatabase
import com.example.data.model.FocusEngineStatus
import com.example.data.repository.TrackaaRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.domain.focus.FocusEngine
import com.example.services.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class TrackaaApplication : Application() {
    val applicationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val database by lazy { TrackaaDatabase.getInstance(this) }
    val repository by lazy { TrackaaRepository(database.trackaaDao()) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val notificationManager by lazy { FocusNotificationManager(this) }
    val dndManager by lazy { DndManager(this) }
    val reminderScheduler by lazy { FocusReminderScheduler(this) }
    val backupManager by lazy { BackupManager(this, database) }

    lateinit var focusEngine: FocusEngine
        private set

    override fun onCreate() {
        super.onCreate()
        focusEngine = FocusEngine(this, repository, notificationManager, applicationScope)
        val active = focusEngine.sessionState.value.status in setOf(
            FocusEngineStatus.FOCUSING, FocusEngineStatus.PAUSED,
            FocusEngineStatus.ON_BREAK, FocusEngineStatus.BREAK_COMPLETE
        )
        dndManager.recoverIfNoActiveSession(active)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "trackaa_daily_maintenance", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<TrackaaMaintenanceWorker>(24, TimeUnit.HOURS).build()
        )
        applicationScope.launch(Dispatchers.IO) {
            val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L
            repository.purgeOldTrash(thirtyDaysAgo)
        }
    }
}
