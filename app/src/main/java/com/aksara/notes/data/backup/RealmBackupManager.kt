package com.aksara.notes.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.aksara.notes.data.database.RealmDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages backup and restore operations using Realm's built-in encryption
 * Backup = Copy encrypted database file directly
 * Restore = Replace database file and restart with correct password
 */
class RealmBackupManager(private val context: Context) {
    
    companion object {
        private const val TAG = "RealmBackupManager"
        private const val BACKUP_FILE_EXTENSION = ".realm_backup"
        private const val BACKUP_METADATA_EXTENSION = ".backup_info"
        
        // Backup metadata format
        private const val BACKUP_VERSION = "1.0"
        private const val BACKUP_TYPE = "REALM_ENCRYPTED"
    }
    
    data class BackupInfo(
        val version: String,
        val type: String,
        val createdAt: String,
        val deviceInfo: String,
        val databaseSize: Long,
        val isEncrypted: Boolean
    )
    
    data class BackupResult(
        val success: Boolean,
        val backupPath: String? = null,
        val backupSize: Long = 0,
        val error: String? = null
    )
    
    data class RestoreResult(
        val success: Boolean,
        val error: String? = null,
        val requiresRestart: Boolean = false
    )
    
    /**
     * Create a backup by copying the encrypted Realm database file
     * @param outputUri The URI where the backup file should be saved
     * @return BackupResult with success status and details
     */
    suspend fun createBackup(outputUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Realm database backup")
            
            // Get the current database file path
            val databasePath = RealmDatabase.getDatabasePath(context)
            val databaseFile = File(databasePath)
            
            if (!databaseFile.exists()) {
                return@withContext BackupResult(
                    success = false,
                    error = "Database file not found: $databasePath"
                )
            }
            
            val databaseSize = databaseFile.length()
            Log.d(TAG, "Database file size: $databaseSize bytes")
            
            // Close current Realm instance to ensure file is not locked
            val wasEncrypted = RealmDatabase.isEncrypted()
            RealmDatabase.close()
            
            // Copy database file to backup location
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                FileInputStream(databaseFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                    outputStream.flush()
                }
            } ?: throw IOException("Could not open output stream for backup")
            
            // Reinitialize Realm after backup
            try {
                if (wasEncrypted) {
                    RealmDatabase.initialize(context)
                } else {
                    RealmDatabase.initializeUnencrypted()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reinitialize Realm after backup", e)
            }
            
            Log.d(TAG, "Backup created successfully")
            
            return@withContext BackupResult(
                success = true,
                backupPath = outputUri.toString(),
                backupSize = databaseSize
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup", e)
            
            // Try to reinitialize Realm even if backup failed
            try {
                RealmDatabase.initialize(context)
            } catch (reinitError: Exception) {
                Log.e(TAG, "Failed to reinitialize Realm after backup error", reinitError)
            }
            
            return@withContext BackupResult(
                success = false,
                error = "Failed to create backup: ${e.message}"
            )
        }
    }
    
    /**
     * Restore from a backup by replacing the current database file
     * @param inputUri The URI of the backup file to restore from
     * @param masterPassword The master password to verify decryption
     * @return RestoreResult with success status and details
     */
    suspend fun restoreFromBackup(inputUri: Uri, masterPassword: String): RestoreResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting restore from backup")
            
            // Get paths
            val databasePath = RealmDatabase.getDatabasePath(context)
            val databaseFile = File(databasePath)
            val backupTempFile = File(context.cacheDir, "temp_restore.realm")
            
            try {
                // Read backup file to temporary location
                context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                    FileOutputStream(backupTempFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                        outputStream.flush()
                    }
                } ?: throw IOException("Could not read backup file")
                
                Log.d(TAG, "Backup file copied to temporary location: ${backupTempFile.absolutePath}")
                
                // Close current Realm instance
                RealmDatabase.close()
                
                // Backup current database (in case restore fails)
                val currentDbBackup = File(context.cacheDir, "current_db_backup.realm")
                if (databaseFile.exists()) {
                    databaseFile.copyTo(currentDbBackup, overwrite = true)
                    Log.d(TAG, "Current database backed up")
                }
                
                // Replace current database with backup file
                backupTempFile.copyTo(databaseFile, overwrite = true)
                Log.d(TAG, "Database file replaced with backup")
                
                // Try to initialize with the provided password to validate
                try {
                    RealmDatabase.initialize(context, masterPassword)
                    
                    // Test that we can actually read from the database
                    val realm = RealmDatabase.getInstance()
                    val testQuery = realm.query(com.aksara.notes.data.database.entities.Note::class).count()
                    Log.d(TAG, "Backup validation successful - found $testQuery notes")
                    
                    // Clean up temporary files
                    backupTempFile.delete()
                    currentDbBackup.delete()
                    
                    return@withContext RestoreResult(
                        success = true,
                        requiresRestart = false
                    )
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize database with backup - incorrect password or corrupted file", e)
                    
                    // Restore original database
                    if (currentDbBackup.exists()) {
                        currentDbBackup.copyTo(databaseFile, overwrite = true)
                        RealmDatabase.initialize(context) // Use stored password
                        Log.d(TAG, "Original database restored after failed backup")
                    }
                    
                    return@withContext RestoreResult(
                        success = false,
                        error = "Invalid backup file or incorrect password"
                    )
                }
                
            } finally {
                // Clean up temporary files
                if (backupTempFile.exists()) {
                    backupTempFile.delete()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from backup", e)
            
            // Try to reinitialize current database
            try {
                RealmDatabase.initialize(context)
            } catch (reinitError: Exception) {
                Log.e(TAG, "Failed to reinitialize Realm after restore error", reinitError)
            }
            
            return@withContext RestoreResult(
                success = false,
                error = "Failed to restore backup: ${e.message}"
            )
        }
    }
    
    /**
     * Get information about a backup file
     */
    suspend fun getBackupInfo(inputUri: Uri): BackupInfo? = withContext(Dispatchers.IO) {
        try {
            // For Realm backups, we can get basic file info
            val fileSize = context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                inputStream.available().toLong()
            } ?: 0L
            
            return@withContext BackupInfo(
                version = BACKUP_VERSION,
                type = BACKUP_TYPE,
                createdAt = "Unknown", // We could store this in filename or metadata
                deviceInfo = "Realm Database Backup",
                databaseSize = fileSize,
                isEncrypted = true
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting backup info", e)
            return@withContext null
        }
    }
    
    /**
     * Validate that a backup file is valid and can be restored
     */
    suspend fun validateBackupFile(inputUri: Uri, masterPassword: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val tempFile = File(context.cacheDir, "validate_backup.realm")
            
            try {
                // Copy backup to temp location
                context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: return@withContext false
                
                // Try to validate the password against the backup
                return@withContext try {
                    RealmDatabase.validatePassword(context, masterPassword)
                } catch (e: Exception) {
                    false
                }
                
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Backup file validation failed", e)
            return@withContext false
        }
    }
    
    /**
     * Get the current database size
     */
    fun getCurrentDatabaseSize(): Long {
        val databasePath = RealmDatabase.getDatabasePath(context)
        val databaseFile = File(databasePath)
        return if (databaseFile.exists()) databaseFile.length() else 0L
    }
    
    /**
     * Generate a suggested backup filename with timestamp
     */
    fun generateBackupFilename(): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        return "aksara_backup_$timestamp$BACKUP_FILE_EXTENSION"
    }
}