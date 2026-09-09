package com.example.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.room.withTransaction
import com.example.data.database.TrackaaDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class BackupManager(private val context: Context, private val database: TrackaaDatabase) {
    companion object {
        private const val MAGIC = "TRACKAA_BACKUP"
        private const val FORMAT_VERSION = 2
        private val TABLES = listOf(
            "goals","work_item_types","work_items","topics","tasks","task_dependencies",
            "focus_sessions","focus_segments","pause_segments","break_segments","interruptions","interruption_reasons",
            "targets","target_revisions","availability","audit_events","xp_events","achievements","scheduled_focus","reporting_periods"
        )
        private val CLEAR_ORDER = TABLES.reversed()
    }

    suspend fun createEncryptedBackup(passphrase: CharArray): String {
        require(passphrase.size >= 8) { "Use a passphrase with at least 8 characters." }
        val raw = createWorkspaceSnapshotJson()
        val encrypted = BackupCrypto.encrypt(raw, passphrase)
        passphrase.fill('\u0000')
        return JSONObject().apply {
            put("magic", MAGIC); put("formatVersion", FORMAT_VERSION); put("cryptoVersion", encrypted.version)
            put("salt", encrypted.saltBase64); put("iv", encrypted.ivBase64); put("ciphertext", encrypted.ciphertextBase64)
        }.toString(2)
    }

    suspend fun restoreFromEncryptedBackup(backupContent: String, passphrase: CharArray): Result<Int> = runCatching {
        val container = JSONObject(backupContent)
        require(container.optString("magic") == MAGIC) { "Invalid Trackaa backup file." }
        require(container.optInt("formatVersion", 0) in 1..FORMAT_VERSION) { "Unsupported backup version." }
        val pkg = BackupCrypto.EncryptedPackage(
            container.getString("salt"), container.getString("iv"), container.getString("ciphertext"), container.optInt("cryptoVersion",1)
        )
        val raw = BackupCrypto.decrypt(pkg, passphrase)
        passphrase.fill('\u0000')
        validateSnapshot(raw)
        createRestoreSafetyBackup()
        restoreWorkspaceSnapshot(raw)
    }.also { passphrase.fill('\u0000') }

    suspend fun createAutomaticLocalBackup(): File {
        val raw = createWorkspaceSnapshotJson()
        val encrypted = DeviceBackupCrypto.encrypt(raw)
        val dir = File(context.filesDir, "auto_backups").apply { mkdirs() }
        val file = File(dir, "trackaa_auto_${System.currentTimeMillis()}.tauto")
        FileOutputStream(file).use { it.write(encrypted.toByteArray(Charsets.UTF_8)) }
        dir.listFiles()?.filter { it.extension == "tauto" }?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { it.delete() }
        return file
    }

    suspend fun restoreAutomaticLocalBackup(file: File): Result<Int> = runCatching {
        require(file.canonicalPath.startsWith(File(context.filesDir,"auto_backups").canonicalPath)) { "Invalid backup location" }
        val raw = DeviceBackupCrypto.decrypt(file.readText())
        validateSnapshot(raw)
        createRestoreSafetyBackup()
        restoreWorkspaceSnapshot(raw)
    }

    private suspend fun createRestoreSafetyBackup() {
        val dir = File(context.filesDir, "restore_safety").apply { mkdirs() }
        val raw = createWorkspaceSnapshotJson()
        val file = File(dir, "before_restore_${System.currentTimeMillis()}.tauto")
        file.writeText(DeviceBackupCrypto.encrypt(raw))
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(3)?.forEach { it.delete() }
    }

    private fun validateSnapshot(raw: String) {
        val root = JSONObject(raw)
        require(root.optString("magic") == "TRACKAA_WORKSPACE") { "Backup payload is not a Trackaa workspace." }
        require(root.optInt("formatVersion",0) in 1..FORMAT_VERSION) { "Unsupported workspace schema." }
        val tables = root.getJSONObject("tables")
        TABLES.forEach { require(tables.has(it)) { "Backup is incomplete: missing table $it" } }
    }

    private suspend fun createWorkspaceSnapshotJson(): String = database.withTransaction {
        val sqlite = database.openHelper.writableDatabase
        val tablesJson = JSONObject()
        TABLES.forEach { table ->
            val rows = JSONArray()
            sqlite.query("SELECT * FROM $table").use { cursor ->
                while (cursor.moveToNext()) rows.put(cursorRow(cursor))
            }
            tablesJson.put(table, rows)
        }
        JSONObject().apply {
            put("magic","TRACKAA_WORKSPACE"); put("formatVersion",FORMAT_VERSION); put("databaseVersion",2)
            put("createdAt",System.currentTimeMillis()); put("tables",tablesJson)
        }.toString()
    }

    private fun cursorRow(cursor: Cursor): JSONObject = JSONObject().apply {
        for (i in 0 until cursor.columnCount) {
            val cell = JSONObject(); cell.put("type", cursor.getType(i))
            when (cursor.getType(i)) {
                Cursor.FIELD_TYPE_NULL -> cell.put("value", JSONObject.NULL)
                Cursor.FIELD_TYPE_INTEGER -> cell.put("value", cursor.getLong(i))
                Cursor.FIELD_TYPE_FLOAT -> cell.put("value", cursor.getDouble(i))
                Cursor.FIELD_TYPE_STRING -> cell.put("value", cursor.getString(i))
                Cursor.FIELD_TYPE_BLOB -> cell.put("value", java.util.Base64.getEncoder().encodeToString(cursor.getBlob(i)))
            }
            put(cursor.getColumnName(i), cell)
        }
    }

    private suspend fun restoreWorkspaceSnapshot(raw: String): Int = database.withTransaction {
        val sqlite = database.openHelper.writableDatabase
        val tables = JSONObject(raw).getJSONObject("tables")
        CLEAR_ORDER.forEach { sqlite.execSQL("DELETE FROM $it") }
        var count = 0
        TABLES.forEach { table ->
            val rows = tables.getJSONArray(table)
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i); val values = ContentValues()
                row.keys().forEach { column ->
                    val cell=row.getJSONObject(column); val type=cell.getInt("type")
                    when(type){
                        Cursor.FIELD_TYPE_NULL -> values.putNull(column)
                        Cursor.FIELD_TYPE_INTEGER -> values.put(column,cell.getLong("value"))
                        Cursor.FIELD_TYPE_FLOAT -> values.put(column,cell.getDouble("value"))
                        Cursor.FIELD_TYPE_STRING -> values.put(column,cell.getString("value"))
                        Cursor.FIELD_TYPE_BLOB -> values.put(column,java.util.Base64.getDecoder().decode(cell.getString("value")))
                    }
                }
                val result=sqlite.insert(table,android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE,values)
                check(result != -1L) { "Restore failed while inserting $table row $i" }
                count++
            }
        }
        count
    }
}
