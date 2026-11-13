package com.github.abusalam.gps_coordinates.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class BackupInfo(
    val exists: Boolean = false,
    val file: File? = null,
    val timestamp: Long = 0L,
    val formattedDate: String = "No backup found",
    val count: Int = 0,
    val fileSizeFormatted: String = "0 B",
    val filePath: String = "",
)

data class BackupResult(
    val success: Boolean,
    val message: String,
    val count: Int = 0,
    val file: File? = null
)

data class RestoreResult(
    val success: Boolean,
    val message: String,
    val restoredCount: Int = 0
)

class BackupManager(private val context: Context, private val dao: CoordinateDao) {

    private val prefs = context.getSharedPreferences("gps_backup_prefs", Context.MODE_PRIVATE)

    companion object {
        const val BACKUP_FILE_NAME = "gps_coordinates_backup.json"
        private const val KEY_AUTO_RESTORE = "key_auto_restore"
        private const val KEY_AUTO_BACKUP = "key_auto_backup"
        private const val KEY_LAST_BACKUP_TIME = "key_last_backup_time"
        private const val KEY_LAST_BACKUP_COUNT = "key_last_backup_count"
    }

    var isAutoRestoreEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RESTORE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RESTORE, value).apply()

    var isAutoBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BACKUP, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_BACKUP, value).apply()

    fun getBackupDirectories(): List<File> {
        val list = mutableListOf<File>()
        // 1. External files backup dir
        context.getExternalFilesDir("backups")?.let { list.add(it) }
        // 2. Internal files backup dir
        val internalBackups = File(context.filesDir, "backups")
        if (!internalBackups.exists()) internalBackups.mkdirs()
        list.add(internalBackups)
        // 3. Fallback external root
        context.getExternalFilesDir(null)?.let { list.add(it) }
        // 4. Internal root
        list.add(context.filesDir)
        return list
    }

    fun getLatestBackupFile(): File? {
        val candidates = getBackupDirectories().asSequence().map { File(it, BACKUP_FILE_NAME) }
            .filter { (it.exists() && it.length() > 0) }
        return candidates.maxByOrNull { it.lastModified() }
    }

    suspend fun getBackupInfo(): BackupInfo = withContext(Dispatchers.IO) {
        val file = getLatestBackupFile()
        if (file == null || !file.exists() || file.length() == 0L) {
            return@withContext BackupInfo()
        }

        try {
            val content = file.readText()
            val json = JSONObject(content)
            val timestamp = json.optLong("backupTimestamp", file.lastModified())
            val count = json.optInt("count", json.optJSONArray("coordinates")?.length() ?: 0)
            val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(timestamp))
            val sizeFormatted = formatFileSize(file.length())

            BackupInfo(
                exists = true,
                file = file,
                timestamp = timestamp,
                formattedDate = dateStr,
                count = count,
                fileSizeFormatted = sizeFormatted,
                filePath = file.absolutePath
            )
        } catch (_: Exception) {
            BackupInfo(
                exists = true,
                file = file,
                timestamp = file.lastModified(),
                formattedDate = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(file.lastModified())),
                count = 0,
                fileSizeFormatted = formatFileSize(file.length()),
                filePath = file.absolutePath
            )
        }
    }

    suspend fun createBackup(): BackupResult = withContext(Dispatchers.IO) {
        try {
            val coordinates = dao.getAllList()
            val json = serializeCoordinates(coordinates)
            val jsonString = json.toString(2)

            // Write to primary external backup location
            val primaryDir = context.getExternalFilesDir("backups") ?: File(context.filesDir, "backups")
            if (!primaryDir.exists()) primaryDir.mkdirs()
            val primaryFile = File(primaryDir, BACKUP_FILE_NAME)
            primaryFile.writeText(jsonString)

            // Write backup copy to internal storage as well for resilience
            try {
                val internalDir = File(context.filesDir, "backups")
                if (!internalDir.exists()) internalDir.mkdirs()
                File(internalDir, BACKUP_FILE_NAME).writeText(jsonString)
            } catch (_: Exception) {}

            // On Android 10+, also attempt to save copy to public Documents folder
            saveToDocumentsMediaStore(jsonString)

            prefs.edit()
                .putLong(KEY_LAST_BACKUP_TIME, System.currentTimeMillis())
                .putInt(KEY_LAST_BACKUP_COUNT, coordinates.size)
                .apply()

            BackupResult(
                success = true,
                message = "Backup created successfully with ${coordinates.size} waypoints",
                count = coordinates.size,
                file = primaryFile
            )
        } catch (e: Exception) {
            BackupResult(
                success = false,
                message = "Backup failed: ${e.localizedMessage ?: "Unknown error"}",
                count = 0
            )
        }
    }

    private fun saveToDocumentsMediaStore(jsonContent: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, BACKUP_FILE_NAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/GPSCoordinates")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
                uri?.let { targetUri ->
                    resolver.openOutputStream(targetUri)?.use { os ->
                        os.write(jsonContent.toByteArray(Charsets.UTF_8))
                        os.flush()
                    }
                }
            } catch (_: Exception) {
                // Best-effort export to Documents
            }
        }
    }

    suspend fun checkAndAutoRestore(): BackupResult = withContext(Dispatchers.IO) {
        if (!isAutoRestoreEnabled) {
            return@withContext BackupResult(success = false, message = "Auto-restore is disabled", count = 0)
        }

        val existingCount = dao.getCount()
        if (existingCount > 0) {
            return@withContext BackupResult(success = false, message = "Existing data present ($existingCount waypoints)", count = 0)
        }

        val backupFile = getLatestBackupFile()
        if (backupFile == null || !backupFile.exists() || backupFile.length() == 0L) {
            return@withContext BackupResult(success = false, message = "No backup file found", count = 0)
        }

        val result = restoreFromFile(backupFile, replaceExisting = false)
        BackupResult(
            success = result.success,
            message = result.message,
            count = result.restoredCount,
            file = backupFile
        )
    }

    suspend fun restoreFromFile(file: File, replaceExisting: Boolean = false): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val content = file.readText()
            restoreFromJsonString(content, replaceExisting)
        } catch (e: Exception) {
            RestoreResult(false, "Failed to read backup file: ${e.localizedMessage}", 0)
        }
    }

    suspend fun restoreFromInputStream(inputStream: InputStream, replaceExisting: Boolean = false): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val content = inputStream.bufferedReader().use { it.readText() }
            restoreFromJsonString(content, replaceExisting)
        } catch (e: Exception) {
            RestoreResult(false, "Failed to restore: ${e.localizedMessage}", 0)
        }
    }

    private suspend fun restoreFromJsonString(jsonString: String, replaceExisting: Boolean): RestoreResult {
        val json = JSONObject(jsonString)
        val array = json.optJSONArray("coordinates")
            ?: return RestoreResult(false, "Invalid backup format: missing coordinates list", 0)

        val parsedList = mutableListOf<Coordinate>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val coord = jsonToCoordinate(item)
            parsedList.add(coord)
        }

        if (parsedList.isEmpty()) {
            return RestoreResult(false, "No valid waypoints found in backup", 0)
        }

        if (replaceExisting) {
            dao.deleteAll()
            val cleaned = parsedList.map { it.copy(id = 0) }
            dao.insertAll(cleaned)
            return RestoreResult(true, "Restored ${cleaned.size} waypoints (replaced existing)", cleaned.size)
        } else {
            val currentList = dao.getAllList()
            val toInsert = parsedList.filter { candidate ->
                currentList.none { existing ->
                    abs(existing.latitude - candidate.latitude) < 0.000001 &&
                    abs(existing.longitude - candidate.longitude) < 0.000001 &&
                    existing.label.equals(candidate.label, ignoreCase = true)
                }
            }.map { it.copy(id = 0) }

            if (toInsert.isNotEmpty()) {
                dao.insertAll(toInsert)
            }
            return RestoreResult(
                true,
                "Restored ${toInsert.size} new waypoints (${parsedList.size - toInsert.size} duplicates skipped)",
                toInsert.size
            )
        }
    }

    private fun serializeCoordinates(coordinates: List<Coordinate>): JSONObject {
        val root = JSONObject()
        root.put("version", 1)
        root.put("appName", "GPS Coordinates")
        val now = System.currentTimeMillis()
        root.put("backupTimestamp", now)
        root.put("backupDateFormatted", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(now)))
        root.put("count", coordinates.size)

        val array = JSONArray()
        for (c in coordinates) {
            val item = JSONObject()
            item.put("id", c.id)
            item.put("label", c.label)
            item.put("latitude", c.latitude)
            item.put("longitude", c.longitude)
            item.put("isPinned", c.isPinned)
            c.altitude?.let { item.put("altitude", it) }
            c.accuracy?.let { item.put("accuracy", it.toDouble()) }
            c.tag?.let { item.put("tag", it) }
            item.put("timestamp", c.timestamp)
            array.put(item)
        }
        root.put("coordinates", array)
        return root
    }

    private fun jsonToCoordinate(obj: JSONObject): Coordinate {
        return Coordinate(
            id = 0,
            label = obj.optString("label", "Waypoint"),
            latitude = obj.optDouble("latitude", 0.0),
            longitude = obj.optDouble("longitude", 0.0),
            isPinned = obj.optBoolean("isPinned", false),
            altitude = if (obj.has("altitude") && !obj.isNull("altitude")) obj.optDouble("altitude") else null,
            accuracy = if (obj.has("accuracy") && !obj.isNull("accuracy")) obj.optDouble("accuracy").toFloat() else null,
            tag = if (obj.has("tag") && !obj.isNull("tag")) obj.optString("tag") else null,
            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
        )
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, index.toDouble()), units[index])
    }
}
