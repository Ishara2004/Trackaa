package com.example.data.backup

import android.content.Context
import com.example.data.dao.TrackaaDao
import com.example.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class BackupManager(
    private val context: Context,
    private val dao: TrackaaDao
) {

    /**
     * Creates an encrypted backup string using user's passphrase
     */
    suspend fun createEncryptedBackup(passphrase: CharArray): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("createdAt", System.currentTimeMillis())
        root.put("appName", "Trackaa")

        // Goals
        val goals = dao.getAllGoals().first()
        val goalsArr = JSONArray()
        goals.forEach { g ->
            goalsArr.put(JSONObject().apply {
                put("id", g.id)
                put("title", g.title)
                put("description", g.description)
                put("targetMinutes", g.targetMinutes)
                put("goalType", g.goalType.name)
                put("status", g.status.name)
                put("createdAt", g.createdAt)
            })
        }
        root.put("goals", goalsArr)

        // WorkItems
        val workItems = dao.getAllWorkItems().first()
        val itemsArr = JSONArray()
        workItems.forEach { w ->
            itemsArr.put(JSONObject().apply {
                put("id", w.id)
                put("name", w.name)
                put("description", w.description)
                put("type", w.type)
                put("colorHex", w.colorHex)
                put("credits", w.credits)
                put("targetFocusMinutes", w.targetFocusMinutes)
                put("goalId", w.goalId ?: -1L)
            })
        }
        root.put("workItems", itemsArr)

        // Tasks
        val tasks = dao.getAllTasks().first()
        val tasksArr = JSONArray()
        tasks.forEach { t ->
            tasksArr.put(JSONObject().apply {
                put("id", t.id)
                put("topicId", t.topicId ?: -1L)
                put("workItemId", t.workItemId)
                put("title", t.title)
                put("status", t.status.name)
                put("priority", t.priority.name)
                put("estimatedMinutes", t.estimatedMinutes)
                put("actualFocusMinutes", t.actualFocusMinutes)
            })
        }
        root.put("tasks", tasksArr)

        // FocusSessions
        val sessions = dao.getAllFocusSessions().first()
        val sessionsArr = JSONArray()
        sessions.forEach { s ->
            sessionsArr.put(JSONObject().apply {
                put("id", s.id)
                put("start", s.startEpochMs)
                put("end", s.endEpochMs)
                put("mode", s.mode.name)
                put("totalFocusMinutes", s.totalFocusMinutes)
                put("totalPauseMinutes", s.totalPauseMinutes)
                put("totalBreakMinutes", s.totalBreakMinutes)
                put("focusQuality", s.focusQuality)
                put("energyLevel", s.energyLevel)
                put("intent", s.intent ?: "")
                put("notes", s.notes ?: "")
            })
        }
        root.put("sessions", sessionsArr)

        val jsonString = root.toString()
        val encryptedPkg = BackupCrypto.encrypt(jsonString, passphrase)

        val exportContainer = JSONObject().apply {
            put("magic", "TRACKAA_BACKUP")
            put("version", encryptedPkg.version)
            put("salt", encryptedPkg.saltBase64)
            put("iv", encryptedPkg.ivBase64)
            put("ciphertext", encryptedPkg.ciphertextBase64)
        }

        exportContainer.toString(2)
    }

    /**
     * Validates and restores data from an encrypted backup string
     */
    suspend fun restoreFromEncryptedBackup(
        backupContent: String,
        passphrase: CharArray
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val container = JSONObject(backupContent)
            if (container.optString("magic") != "TRACKAA_BACKUP") {
                error("Invalid Trackaa backup file format.")
            }

            val pkg = BackupCrypto.EncryptedPackage(
                saltBase64 = container.getString("salt"),
                ivBase64 = container.getString("iv"),
                ciphertextBase64 = container.getString("ciphertext"),
                version = container.optInt("version", 1)
            )

            // Decrypt & authenticate
            val plainJson = BackupCrypto.decrypt(pkg, passphrase)
            val root = JSONObject(plainJson)

            var restoredItemCount = 0

            // Restore Goals
            val goalsArr = root.optJSONArray("goals")
            if (goalsArr != null) {
                for (i in 0 until goalsArr.length()) {
                    val g = goalsArr.getJSONObject(i)
                    dao.insertGoal(
                        GoalEntity(
                            id = g.getLong("id"),
                            title = g.getString("title"),
                            description = g.optString("description", ""),
                            targetMinutes = g.optLong("targetMinutes", 0L)
                        )
                    )
                    restoredItemCount++
                }
            }

            // Restore WorkItems
            val itemsArr = root.optJSONArray("workItems")
            if (itemsArr != null) {
                for (i in 0 until itemsArr.length()) {
                    val w = itemsArr.getJSONObject(i)
                    val gid = w.optLong("goalId", -1L).let { if (it == -1L) null else it }
                    dao.insertWorkItem(
                        WorkItemEntity(
                            id = w.getLong("id"),
                            name = w.getString("name"),
                            description = w.optString("description", ""),
                            type = w.optString("type", "Module"),
                            colorHex = w.optString("colorHex", "#38BDF8"),
                            credits = w.optDouble("credits", 0.0),
                            targetFocusMinutes = w.optLong("targetFocusMinutes", 0L),
                            goalId = gid
                        )
                    )
                    restoredItemCount++
                }
            }

            // Restore Tasks
            val tasksArr = root.optJSONArray("tasks")
            if (tasksArr != null) {
                for (i in 0 until tasksArr.length()) {
                    val t = tasksArr.getJSONObject(i)
                    val tid = t.optLong("topicId", -1L).let { if (it == -1L) null else it }
                    dao.insertTask(
                        TaskEntity(
                            id = t.getLong("id"),
                            topicId = tid,
                            workItemId = t.getLong("workItemId"),
                            title = t.getString("title"),
                            estimatedMinutes = t.optLong("estimatedMinutes", 0L),
                            actualFocusMinutes = t.optLong("actualFocusMinutes", 0L)
                        )
                    )
                    restoredItemCount++
                }
            }

            // Restore Sessions
            val sessionsArr = root.optJSONArray("sessions")
            if (sessionsArr != null) {
                for (i in 0 until sessionsArr.length()) {
                    val s = sessionsArr.getJSONObject(i)
                    dao.insertFocusSession(
                        FocusSessionEntity(
                            id = s.getLong("id"),
                            startEpochMs = s.getLong("start"),
                            endEpochMs = s.getLong("end"),
                            totalFocusMinutes = s.getLong("totalFocusMinutes"),
                            totalPauseMinutes = s.optLong("totalPauseMinutes", 0L),
                            totalBreakMinutes = s.optLong("totalBreakMinutes", 0L),
                            focusQuality = s.optInt("focusQuality", 3),
                            energyLevel = s.optInt("energyLevel", 3),
                            intent = s.optString("intent").ifEmpty { null },
                            notes = s.optString("notes").ifEmpty { null }
                        )
                    )
                    restoredItemCount++
                }
            }

            // Record audit event
            dao.insertAuditEvent(
                AuditEventEntity(
                    entityType = "DATABASE",
                    entityId = 0,
                    actionType = "RESTORE",
                    note = "Restored $restoredItemCount items from authenticated backup"
                )
            )

            restoredItemCount
        }
    }

    /**
     * Automatic local rotating backup saved to private app storage
     */
    suspend fun createAutomaticLocalBackup() = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "auto_backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val timestamp = System.currentTimeMillis()
        val backupFile = File(backupDir, "trackaa_auto_$timestamp.json")

        // Use device-private key / passphrase for auto backups
        val autoPass = "Trackaa_Device_Local_Secret".toCharArray()
        val encryptedContent = createEncryptedBackup(autoPass)

        FileOutputStream(backupFile).use { it.write(encryptedContent.toByteArray()) }

        // Keep maximum 5 rotating backups
        val files = backupDir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()
        if (files.size > 5) {
            for (i in 0 until (files.size - 5)) {
                files[i].delete()
            }
        }
    }
}
