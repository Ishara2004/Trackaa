package com.example.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.TrackaaApplication

class TrackaaMaintenanceWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? TrackaaApplication ?: return Result.failure()
        return runCatching {
            app.backupManager.createAutomaticLocalBackup()
            val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L
            app.repository.purgeOldTrash(thirtyDaysAgo)
            Result.success()
        }.getOrElse { Result.retry() }
    }
}
