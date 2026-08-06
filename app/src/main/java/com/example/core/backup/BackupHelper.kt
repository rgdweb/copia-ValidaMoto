package com.example.core.backup

import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.core.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupHelper(
    private val context: Context,
    private val getAllAulasCount: () -> Int,
    private val getAllAlunosCount: () -> Int,
    private val auditLog: (String, String) -> Unit
) {

    private fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun countFilesRecursively(dir: File): Int {
        if (!dir.exists() || !dir.isDirectory) return 0
        var count = 0
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                count += countFilesRecursively(file)
            } else if (file.isFile) {
                count++
            }
        }
        return count
    }

    fun generateBackupMetadata(): org.json.JSONObject {
        val meta = org.json.JSONObject()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        meta.put("version", getAppVersion())
        meta.put("date", sdf.format(Date()))
        meta.put("aulas", getAllAulasCount())
        meta.put("alunos", getAllAlunosCount())
        meta.put("fotos", countFilesRecursively(File(context.filesDir, "photos")))
        val aulasDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "aulas")
        meta.put("pdfs", countFilesRecursively(aulasDir))
        return meta
    }

    fun isZipFileValid(zipFile: File): Boolean {
        if (!zipFile.exists() || zipFile.length() == 0L) return false
        var zipInputStream: ZipInputStream? = null
        try {
            zipInputStream = ZipInputStream(BufferedInputStream(FileInputStream(zipFile)))
            var entry = zipInputStream.nextEntry
            if (entry == null) return false
            while (entry != null) {
                val buffer = ByteArray(1024)
                while (zipInputStream.read(buffer) != -1) {
                    // Read file fully to check integrity
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            return true
        } catch (e: Exception) {
            Log.e("BackupHelper", "ZIP integrity check failed for ${zipFile.name}", e)
            return false
        } finally {
            try {
                zipInputStream?.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun rotateLocalBackups() {
        val docDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return
        val files = docDir.listFiles()?.filter { 
            it.isFile && it.name.startsWith("Backup_ValidaMoto_") && it.name.endsWith(".zip")
        }?.sortedByDescending { it.lastModified() } ?: return
        
        if (files.size > 5) {
            val toDelete = files.subList(5, files.size)
            toDelete.forEach { file ->
                Log.d("BackupHelper", "Deleting old local backup: ${file.name}")
                file.delete()
            }
        }
    }

    fun addFileToZip(zipOut: ZipOutputStream, file: File, zipPath: String) {
        val buffer = ByteArray(4096)
        try {
            BufferedInputStream(FileInputStream(file)).use { bis ->
                val entry = ZipEntry(zipPath)
                zipOut.putNextEntry(entry)
                var count: Int
                while (bis.read(buffer).also { count = it } != -1) {
                    zipOut.write(buffer, 0, count)
                }
                zipOut.closeEntry()
            }
        } catch (e: Exception) {
            Log.e("BackupHelper", "Failed adding file $zipPath to zip", e)
        }
    }

    private fun addFolderToZip(zipOut: ZipOutputStream, folder: File, parentPath: String) {
        val files = folder.listFiles() ?: return
        for (file in files) {
            val entryPath = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
            if (file.isDirectory) {
                addFolderToZip(zipOut, file, entryPath)
            } else {
                addFileToZip(zipOut, file, entryPath)
            }
        }
    }

    suspend fun exportDatabaseBackup(targetFile: File? = null): File? = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath("valida_moto_database")
        val dbShm = context.getDatabasePath("valida_moto_database-shm")
        val dbWal = context.getDatabasePath("valida_moto_database-wal")

        val photosDir = File(context.filesDir, "photos")
        val aulasDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "aulas")
        val sharedPrefsFile = File(context.filesDir.parentFile, "shared_prefs/valida_moto_prefs.xml")

        // Create temporary metadata file
        val metadataFile = File(context.cacheDir, "metadata.json")
        try {
            val metaJson = generateBackupMetadata()
            metadataFile.writeText(metaJson.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val zipFile = targetFile ?: File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "Backup_ValidaMoto_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.zip"
        )

        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
                // Add DB
                if (dbFile.exists()) addFileToZip(zipOut, dbFile, "database/valida_moto_database")
                if (dbShm.exists()) addFileToZip(zipOut, dbShm, "database/valida_moto_database-shm")
                if (dbWal.exists()) addFileToZip(zipOut, dbWal, "database/valida_moto_database-wal")

                // Add Photos recursively
                if (photosDir.exists()) {
                    addFolderToZip(zipOut, photosDir, "photos")
                }

                // Add PDFs recursively
                if (aulasDir.exists()) {
                    addFolderToZip(zipOut, aulasDir, "aulas")
                }

                // Add SharedPreferences
                if (sharedPrefsFile.exists()) {
                    addFileToZip(zipOut, sharedPrefsFile, "shared_prefs/valida_moto_prefs.xml")
                }

                // Add Metadata
                if (metadataFile.exists()) {
                    addFileToZip(zipOut, metadataFile, "metadata.json")
                }
            }
            
            // Clean up temporary metadata file
            if (metadataFile.exists()) {
                metadataFile.delete()
            }
            
            // Validate integrity
            if (!isZipFileValid(zipFile)) {
                Log.e("BackupHelper", "Generated ZIP integrity check failed.")
                if (zipFile.exists()) zipFile.delete()
                null
            } else {
                // Perform local rotation if it's a manual/local backup
                if (targetFile == null) {
                    rotateLocalBackups()
                }
                zipFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun deleteRecursive(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        file.delete()
    }

    private fun copyRecursive(src: File, dst: File) {
        if (src.isDirectory) {
            if (!dst.exists()) dst.mkdirs()
            src.listFiles()?.forEach { file ->
                copyRecursive(file, File(dst, file.name))
            }
        } else {
            dst.parentFile?.mkdirs()
            src.inputStream().use { input ->
                dst.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun isSqliteFileValid(file: File): Boolean {
        if (!file.exists()) return false
        var db: android.database.sqlite.SQLiteDatabase? = null
        return try {
            db = android.database.sqlite.SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            )
            db.rawQuery("PRAGMA integrity_check;", null).use { cursor ->
                if (cursor.moveToFirst()) {
                    val result = cursor.getString(0)
                    result.equals("ok", ignoreCase = true)
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("BackupHelper", "SQLite integrity check failed", e)
            false
        } finally {
            try {
                db?.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    suspend fun restoreFromBackupFile(zipFile: File): Boolean = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath("valida_moto_database")
        val dbShm = context.getDatabasePath("valida_moto_database-shm")
        val dbWal = context.getDatabasePath("valida_moto_database-wal")
        val photosDir = File(context.filesDir, "photos")
        val aulasDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "aulas")
        val sharedPrefsFile = File(context.filesDir.parentFile, "shared_prefs/valida_moto_prefs.xml")
        
        val tempDir = File(context.cacheDir, "temp_restore_${UUID.randomUUID()}")
        tempDir.mkdirs()
        
        try {
            // Step 1: Extract all files to a temporary directory in cache
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zipIn ->
                var entry = zipIn.nextEntry
                val buffer = ByteArray(4096)
                
                while (entry != null) {
                    val name = entry.name
                    val tempOutFile = File(tempDir, name)
                    tempOutFile.parentFile?.mkdirs()
                    
                    if (!entry.isDirectory) {
                        BufferedOutputStream(FileOutputStream(tempOutFile)).use { out ->
                            var count: Int
                            while (zipIn.read(buffer).also { count = it } != -1) {
                                out.write(buffer, 0, count)
                            }
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            
            // Step 2: Validate extracted database integrity
            val tempDbFile = File(tempDir, "database/valida_moto_database")
            if (!tempDbFile.exists()) {
                Log.e("BackupHelper", "Database file missing in extracted backup")
                deleteRecursive(tempDir)
                return@withContext false
            }
            
            if (!isSqliteFileValid(tempDbFile)) {
                Log.e("BackupHelper", "Extracted database SQLite integrity check failed")
                deleteRecursive(tempDir)
                return@withContext false
            }
            
            // Step 3: Close database before replacing
            AppDatabase.closeDatabase()
            
            // Step 4: Safe replacement
            // DB Files
            if (dbFile.exists()) dbFile.delete()
            if (dbShm.exists()) dbShm.delete()
            if (dbWal.exists()) dbWal.delete()
            
            val tempDbShm = File(tempDir, "database/valida_moto_database-shm")
            val tempDbWal = File(tempDir, "database/valida_moto_database-wal")
            
            copyRecursive(tempDbFile, dbFile)
            if (tempDbShm.exists()) copyRecursive(tempDbShm, dbShm)
            if (tempDbWal.exists()) copyRecursive(tempDbWal, dbWal)
            
            // Photos
            val tempPhotosDir = File(tempDir, "photos")
            if (tempPhotosDir.exists()) {
                if (photosDir.exists()) {
                    deleteRecursive(photosDir)
                }
                copyRecursive(tempPhotosDir, photosDir)
            }
            
            // Aulas PDFs
            val tempAulasDir = File(tempDir, "aulas")
            if (tempAulasDir.exists()) {
                if (aulasDir.exists()) {
                    deleteRecursive(aulasDir)
                }
                copyRecursive(tempAulasDir, aulasDir)
            }
            
            // Shared Preferences
            val tempPrefsFile = File(tempDir, "shared_prefs/valida_moto_prefs.xml")
            if (tempPrefsFile.exists()) {
                copyRecursive(tempPrefsFile, sharedPrefsFile)
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            // Step 5: Clean up temp extraction area
            if (tempDir.exists()) {
                deleteRecursive(tempDir)
            }
        }
    }
}
