package com.example

import android.app.Application
import com.example.data.backup.BackupManager
import com.example.data.database.TrackaaDatabase
import com.example.data.repository.TrackaaRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.domain.focus.FocusEngine
import com.example.services.DndManager
import com.example.services.FocusNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TrackaaApplication : Application() {

    val applicationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val database by lazy { TrackaaDatabase.getInstance(this) }
    val repository by lazy { TrackaaRepository(database.trackaaDao()) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val notificationManager by lazy { FocusNotificationManager(this) }
    val dndManager by lazy { DndManager(this) }
    val backupManager by lazy { BackupManager(this, database.trackaaDao()) }

    lateinit var focusEngine: FocusEngine
        private set

    override fun onCreate() {
        super.onCreate()
        focusEngine = FocusEngine(
            context = this,
            repository = repository,
            notificationManager = notificationManager,
            scope = applicationScope
        )

        // Run automatic local maintenance / cleanup of 30-day-old trash in background
        applicationScope.launch(Dispatchers.IO) {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            repository.purgeOldTrash(thirtyDaysAgo)
        }
    }
}
