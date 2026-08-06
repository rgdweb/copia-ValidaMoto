package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class GoogleDriveService(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleDriveService"
        private const val APP_FOLDER_NAME = "ValidaMotoBackup"
        private const val SESSION_RECOVERY_FILENAME = "session_recovery_temp.kbp"
        private const val BACKUP_FILENAME_PREFIX = "backup_validamoto_"
    }
    
    private var driveService: Drive? = null
    
    fun getSignInClient(): GoogleSignInClient {
        val clientId = context.getString(R.string.google_oauth_client_id)
        Log.d(TAG, "REAL_CALL: getSignInClient with clientId=$clientId")
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(clientId)
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }
    
    fun initializeWithAccount(account: GoogleSignInAccount) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_APPDATA)
            ).apply {
                selectedAccount = account.account
                setBackOff(ExponentialBackOff())
            }
            
            driveService = Drive.Builder(
                com.google.api.client.http.javanet.NetHttpTransport(),
                GsonFactory(),
                credential
            ).setApplicationName("ValidaMoto").build()
            
            Log.d(TAG, "REAL_CALL: initializeWithAccount email=${account.email}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Drive service", e)
        }
    }
    
    fun isInitialized(): Boolean = driveService != null
    
    /**
     * Upload of session recovery file (.kbp) to Drive AppFolder.
     * File is overwritten if it already exists.
     */
    suspend fun uploadSessionRecovery(localFile: File): Boolean = withContext(Dispatchers.IO) {
        if (driveService == null) {
            Log.w(TAG, "Google Drive service not initialized on uploadSessionRecovery")
            return@withContext false
        }
        
        try {
            Log.d(TAG, "REAL_CALL: uploadSessionRecovery filename=$SESSION_RECOVERY_FILENAME size=${localFile.length()}")
            val existingFileId = findFileInAppFolder(SESSION_RECOVERY_FILENAME)
            
            val metadata = DriveFile().apply {
                name = SESSION_RECOVERY_FILENAME
                parents = listOf("appDataFolder")
                mimeType = "application/octet-stream"
            }
            
            val mediaContent = FileContent("application/octet-stream", localFile)
            
            if (existingFileId != null) {
                // Update existing file
                driveService!!.files()
                    .update(existingFileId, metadata, mediaContent)
                    .execute()
                Log.d(TAG, "Session recovery file updated successfully in Drive")
            } else {
                // Create new file
                driveService!!.files()
                    .create(metadata, mediaContent)
                    .execute()
                Log.d(TAG, "Session recovery file created successfully in Drive")
            }
            true
        } catch (e: GoogleJsonResponseException) {
            Log.e(TAG, "Drive API error: ${e.statusCode} - ${e.details?.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload session recovery", e)
            false
        }
    }
    
    /**
     * Download of session recovery file (.kbp) from Drive AppFolder.
     */
    suspend fun downloadSessionRecovery(targetFile: File): Boolean = withContext(Dispatchers.IO) {
        if (driveService == null) {
            Log.w(TAG, "Google Drive service not initialized on downloadSessionRecovery")
            return@withContext false
        }
        
        try {
            Log.d(TAG, "REAL_CALL: downloadSessionRecovery target=${targetFile.absolutePath}")
            val fileId = findFileInAppFolder(SESSION_RECOVERY_FILENAME)
            if (fileId == null) {
                Log.d(TAG, "No session recovery file found in cloud AppFolder")
                return@withContext false
            }
            
            FileOutputStream(targetFile).use { outputStream ->
                driveService!!.files()
                    .get(fileId)
                    .executeMediaAndDownloadTo(outputStream)
            }
            
            Log.d(TAG, "Session recovery file downloaded successfully from Drive")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download session recovery", e)
            false
        }
    }
    
    /**
     * Delete of session recovery file (.kbp) from Drive.
     */
    suspend fun deleteSessionRecovery(): Boolean = withContext(Dispatchers.IO) {
        if (driveService == null) return@withContext false
        
        try {
            Log.d(TAG, "REAL_CALL: deleteSessionRecovery")
            val fileId = findFileInAppFolder(SESSION_RECOVERY_FILENAME) ?: return@withContext true
            driveService!!.files().delete(fileId).execute()
            Log.d(TAG, "Session recovery file deleted from Drive successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete session recovery", e)
            false
        }
    }
    
    /**
     * Check if an active session recovery file exists in the cloud AppFolder.
     */
    suspend fun hasActiveSessionInCloud(): Boolean = withContext(Dispatchers.IO) {
        if (driveService == null) return@withContext false
        try {
            Log.d(TAG, "REAL_CALL: hasActiveSessionInCloud")
            val fileId = findFileInAppFolder(SESSION_RECOVERY_FILENAME)
            fileId != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Upload of a full backup (.zip) file to AppFolder.
     */
    suspend fun uploadBackup(localZipFile: File, descriptionJson: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (driveService == null) {
            Log.w(TAG, "Google Drive service not initialized on uploadBackup")
            return@withContext false
        }
        
        try {
            val backupName = BACKUP_FILENAME_PREFIX + 
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".zip"
            
            Log.d(TAG, "REAL_CALL: uploadBackup filename=$backupName size=${localZipFile.length()}")
            val metadata = DriveFile().apply {
                name = backupName
                parents = listOf("appDataFolder")
                mimeType = "application/zip"
                if (descriptionJson != null) {
                    description = descriptionJson
                }
            }
            
            val mediaContent = FileContent("application/zip", localZipFile)
            driveService!!.files().create(metadata, mediaContent).execute()
            
            Log.d(TAG, "Backup uploaded successfully to Drive: $backupName")
            
            // Execute rotation keeping only 30 latest backups
            rotateBackups()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload backup to Google Drive", e)
            false
        }
    }

    /**
     * Deletes older backups, maintaining only the latest 30.
     */
    suspend fun rotateBackups(): Boolean = withContext(Dispatchers.IO) {
        if (driveService == null) return@withContext false
        try {
            Log.d(TAG, "REAL_CALL: rotateBackups (keeping 30 latest backups)")
            val result = driveService!!.files().list()
                .setSpaces("appDataFolder")
                .setQ("(name contains 'validamoto' or name contains 'ValidaMoto') and trashed = false")
                .setPageSize(100)
                .execute()
            
            val filesList = result.files ?: return@withContext true
            if (filesList.size <= 30) return@withContext true
            
            // Sort by name descending (since filename includes timestamp format)
            val sortedFiles = filesList.sortedByDescending { it.name }
            
            // Keep first 30, delete rest
            val filesToDelete = sortedFiles.subList(30, sortedFiles.size)
            for (file in filesToDelete) {
                Log.d(TAG, "Deleting old backup file from Google Drive: name=${file.name} id=${file.id}")
                driveService!!.files().delete(file.id).execute()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate backups on Google Drive", e)
            false
        }
    }

    /**
     * List all full backups (.zip) with metadata from Drive AppFolder.
     */
    suspend fun listBackups(): List<DriveFile> = withContext(Dispatchers.IO) {
        if (driveService == null) return@withContext emptyList()
        try {
            val result = driveService!!.files().list()
                .setSpaces("appDataFolder")
                .setQ("(name contains 'validamoto' or name contains 'ValidaMoto') and trashed = false")
                .setFields("files(id, name, size, description, createdTime)")
                .setPageSize(100)
                .execute()
            result.files ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list backups from Google Drive", e)
            emptyList()
        }
    }
    
    private suspend fun findFileInAppFolder(filename: String): String? = withContext(Dispatchers.IO) {
        try {
            val result = driveService!!.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$filename' and trashed = false")
                .setPageSize(10)
                .execute()
            
            result.files?.firstOrNull()?.id
        } catch (e: Exception) {
            Log.e(TAG, "Error searching for file $filename in appDataFolder", e)
            null
        }
    }
    
    suspend fun getLatestBackupIdAndName(): Pair<String, String>? = withContext(Dispatchers.IO) {
        if (driveService == null) return@withContext null
        try {
            val result = driveService!!.files().list()
                .setSpaces("appDataFolder")
                .setQ("name contains '$BACKUP_FILENAME_PREFIX' and trashed = false")
                .setPageSize(100)
                .execute()
            
            val filesList = result.files ?: return@withContext null
            val latest = filesList.sortedByDescending { it.name }.firstOrNull()
            if (latest != null) {
                latest.id to latest.name
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get latest backup from Google Drive", e)
            null
        }
    }

    suspend fun downloadFile(fileId: String, targetFile: File): Boolean = withContext(Dispatchers.IO) {
        if (driveService == null) return@withContext false
        try {
            FileOutputStream(targetFile).use { outputStream ->
                driveService!!.files()
                    .get(fileId)
                    .executeMediaAndDownloadTo(outputStream)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download file with id $fileId from Google Drive", e)
            false
        }
    }

    fun signOut() {
        Log.d(TAG, "REAL_CALL: signOut")
        driveService = null
        try {
            getSignInClient().signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call GoogleSignInClient.signOut()", e)
        }
    }
}
