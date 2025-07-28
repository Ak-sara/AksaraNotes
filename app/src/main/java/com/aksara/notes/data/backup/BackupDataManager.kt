package com.aksara.notes.data.backup

import android.util.Log
import com.aksara.notes.data.database.entities.*
import com.aksara.notes.ui.database.DatabaseViewModel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages backup and restore operations for app data
 * Handles serialization/deserialization of all app entities
 */
class BackupDataManager(private val databaseViewModel: DatabaseViewModel) {
    
    companion object {
        private const val TAG = "BackupDataManager"
        private const val BACKUP_VERSION = "1.0"
    }
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()
    
    /**
     * Complete backup data structure
     */
    data class BackupData(
        val version: String,
        val createdAt: String,
        val deviceInfo: DeviceInfo,
        val notes: List<NoteBackup>,
        val datasets: List<DatasetBackup>,
        val forms: List<FormBackup>
    )
    
    data class DeviceInfo(
        val appVersion: String = "1.0",
        val backupType: String = "FULL",
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Backup-specific data classes that mirror the Realm entities
     * but are plain Kotlin classes suitable for JSON serialization
     */
    data class NoteBackup(
        val id: String,
        val title: String,
        val content: String,
        val createdAt: Long,
        val updatedAt: Long,
        val requiresPin: Boolean,
        val isEncrypted: Boolean,
        val isFavorite: Boolean,
        val tags: String
    )
    
    data class DatasetBackup(
        val id: String,
        val name: String,
        val description: String,
        val icon: String,
        val datasetType: String,
        val createdAt: Long,
        val updatedAt: Long,
        val columns: List<TableColumnBackup>,
        val settings: TableSettingsBackup?
    )
    
    data class TableColumnBackup(
        val id: String,
        val name: String,
        val type: String,
        val displayName: String,
        val icon: String,
        val required: Boolean,
        val defaultValue: String,
        val options: String
    )
    
    data class TableSettingsBackup(
        val primaryColor: String,
        val showInCalendar: Boolean,
        val calendarDateField: String,
        val reminderDays: Int
    )
    
    data class FormBackup(
        val id: String,
        val datasetId: String,
        val data: String,
        val createdAt: Long,
        val updatedAt: Long
    )
    
    /**
     * Creates a complete backup of all app data
     * @return JSON string containing all app data
     */
    suspend fun createBackup(): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting backup creation")
            
            // Collect all data from database
            val notes = databaseViewModel.getAllNotesForBackup()
            val datasets = databaseViewModel.getAllDatasetsForBackup()
            val forms = databaseViewModel.getAllFormsForBackup()
            
            Log.d(TAG, "Collected data: ${notes.size} notes, ${datasets.size} datasets, ${forms.size} forms")
            
            // Convert to backup format
            val notesBackup = notes.map { convertNoteToBackup(it) }
            val datasetsBackup = datasets.map { convertDatasetToBackup(it) }
            val formsBackup = forms.map { convertFormToBackup(it) }
            
            // Create backup data structure
            val backupData = BackupData(
                version = BACKUP_VERSION,
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
                deviceInfo = DeviceInfo(),
                notes = notesBackup,
                datasets = datasetsBackup,
                forms = formsBackup
            )
            
            // Serialize to JSON
            val jsonData = gson.toJson(backupData)
            Log.d(TAG, "Backup creation completed. JSON size: ${jsonData.length} characters")
            
            return@withContext jsonData
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup", e)
            throw RuntimeException("Failed to create backup: ${e.message}", e)
        }
    }
    
    /**
     * Restores app data from backup JSON
     * @param backupJson The JSON string containing backup data
     * @param replaceExisting Whether to replace existing data or merge
     * @return RestoreResult with details about the operation
     */
    suspend fun restoreFromBackup(backupJson: String, replaceExisting: Boolean = false): RestoreResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting restore from backup")
            
            // Parse JSON
            val backupData = gson.fromJson(backupJson, BackupData::class.java)
            
            // Validate backup version
            if (backupData.version != BACKUP_VERSION) {
                Log.w(TAG, "Backup version mismatch: ${backupData.version} vs $BACKUP_VERSION")
                // You might want to handle version migration here
            }
            
            Log.d(TAG, "Restoring backup from ${backupData.createdAt}")
            Log.d(TAG, "Data to restore: ${backupData.notes.size} notes, ${backupData.datasets.size} datasets, ${backupData.forms.size} forms")
            
            var notesRestored = 0
            var datasetsRestored = 0
            var formsRestored = 0
            val errors = mutableListOf<String>()
            
            // Clear existing data if requested
            if (replaceExisting) {
                Log.d(TAG, "Clearing existing data as requested")
                databaseViewModel.clearAllData()
            }
            
            // Restore datasets first (forms depend on datasets)
            for (datasetBackup in backupData.datasets) {
                try {
                    val dataset = convertBackupToDataset(datasetBackup)
                    databaseViewModel.insertDatasetFromBackup(dataset)
                    datasetsRestored++
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring dataset ${datasetBackup.name}", e)
                    errors.add("Dataset '${datasetBackup.name}': ${e.message}")
                }
            }
            
            // Restore notes
            for (noteBackup in backupData.notes) {
                try {
                    val note = convertBackupToNote(noteBackup)
                    databaseViewModel.insertNoteFromBackup(note)
                    notesRestored++
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring note ${noteBackup.title}", e)
                    errors.add("Note '${noteBackup.title}': ${e.message}")
                }
            }
            
            // Restore forms
            for (formBackup in backupData.forms) {
                try {
                    val form = convertBackupToForm(formBackup)
                    databaseViewModel.insertFormFromBackup(form)
                    formsRestored++
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring form ${formBackup.id}", e)
                    errors.add("Form '${formBackup.id}': ${e.message}")
                }
            }
            
            val result = RestoreResult(
                success = true,
                notesRestored = notesRestored,
                datasetsRestored = datasetsRestored,
                formsRestored = formsRestored,
                errors = errors,
                backupDate = backupData.createdAt
            )
            
            Log.d(TAG, "Restore completed: $result")
            return@withContext result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from backup", e)
            return@withContext RestoreResult(
                success = false,
                notesRestored = 0,
                datasetsRestored = 0,
                formsRestored = 0,
                errors = listOf("Failed to restore backup: ${e.message}"),
                backupDate = "Unknown"
            )
        }
    }
    
    data class RestoreResult(
        val success: Boolean,
        val notesRestored: Int,
        val datasetsRestored: Int,
        val formsRestored: Int,
        val errors: List<String>,
        val backupDate: String
    )
    
    // Conversion functions from Realm entities to backup data classes
    private fun convertNoteToBackup(note: Note): NoteBackup {
        return NoteBackup(
            id = note.id,
            title = note.title,
            content = note.content,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
            requiresPin = note.requiresPin,
            isEncrypted = note.isEncrypted,
            isFavorite = note.isFavorite,
            tags = note.tags
        )
    }
    
    private fun convertDatasetToBackup(dataset: Dataset): DatasetBackup {
        return DatasetBackup(
            id = dataset.id,
            name = dataset.name,
            description = dataset.description,
            icon = dataset.icon,
            datasetType = dataset.datasetType,
            createdAt = dataset.createdAt,
            updatedAt = dataset.updatedAt,
            columns = dataset.columns.map { convertColumnToBackup(it) },
            settings = dataset.settings?.let { convertSettingsToBackup(it) }
        )
    }
    
    private fun convertColumnToBackup(column: TableColumn): TableColumnBackup {
        return TableColumnBackup(
            id = column.id,
            name = column.name,
            type = column.type,
            displayName = column.displayName,
            icon = column.icon,
            required = column.required,
            defaultValue = column.defaultValue,
            options = column.options
        )
    }
    
    private fun convertSettingsToBackup(settings: TableSettings): TableSettingsBackup {
        return TableSettingsBackup(
            primaryColor = settings.primaryColor,
            showInCalendar = settings.showInCalendar,
            calendarDateField = settings.calendarDateField,
            reminderDays = settings.reminderDays
        )
    }
    
    private fun convertFormToBackup(form: Form): FormBackup {
        return FormBackup(
            id = form.id,
            datasetId = form.datasetId,
            data = form.data,
            createdAt = form.createdAt,
            updatedAt = form.updatedAt
        )
    }
    
    // Conversion functions from backup data classes to Realm entities
    private fun convertBackupToNote(backup: NoteBackup): Note {
        return Note().apply {
            id = backup.id
            title = backup.title
            content = backup.content
            createdAt = backup.createdAt
            updatedAt = backup.updatedAt
            requiresPin = backup.requiresPin
            isEncrypted = backup.isEncrypted
            isFavorite = backup.isFavorite
            tags = backup.tags
        }
    }
    
    private fun convertBackupToDataset(backup: DatasetBackup): Dataset {
        return Dataset().apply {
            id = backup.id
            name = backup.name
            description = backup.description
            icon = backup.icon
            datasetType = backup.datasetType
            createdAt = backup.createdAt
            updatedAt = backup.updatedAt
            columns.addAll(backup.columns.map { convertBackupToColumn(it) })
            settings = backup.settings?.let { convertBackupToSettings(it) }
        }
    }
    
    private fun convertBackupToColumn(backup: TableColumnBackup): TableColumn {
        return TableColumn().apply {
            id = backup.id
            name = backup.name
            type = backup.type
            displayName = backup.displayName
            icon = backup.icon
            required = backup.required
            defaultValue = backup.defaultValue
            options = backup.options
        }
    }
    
    private fun convertBackupToSettings(backup: TableSettingsBackup): TableSettings {
        return TableSettings().apply {
            primaryColor = backup.primaryColor
            showInCalendar = backup.showInCalendar
            calendarDateField = backup.calendarDateField
            reminderDays = backup.reminderDays
        }
    }
    
    private fun convertBackupToForm(backup: FormBackup): Form {
        return Form().apply {
            id = backup.id
            datasetId = backup.datasetId
            data = backup.data
            createdAt = backup.createdAt
            updatedAt = backup.updatedAt
        }
    }
}