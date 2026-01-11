package com.aksara.notes.data.database

import android.content.Context
import android.util.Log
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import com.aksara.notes.data.database.entities.Note
import com.aksara.notes.data.database.entities.Dataset
import com.aksara.notes.data.database.entities.Form
import com.aksara.notes.data.database.entities.TableColumn
import com.aksara.notes.data.database.entities.TableSettings
import com.aksara.notes.utils.BiometricHelper
import org.mongodb.kbson.ObjectId
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory
import java.io.File

object RealmDatabase {
    
    private const val TAG = "RealmDatabase"
    private const val DATABASE_NAME = "aksara_notes.realm"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 100000
    private const val KEY_SIZE = 512 // Realm requires 64-byte (512-bit) encryption key
    private const val SALT = "AksaraNotes2024Salt" // Fixed salt for consistency
    
    @Volatile
    private var realm: Realm? = null
    private var currentEncryptionKey: ByteArray? = null
    
    // Lock object for thread-safe singleton operations
    private val lock = Any()
    
    /**
     * Initialize Realm with encryption using the master password
     * @param context Application context
     * @param masterPassword The user's master password for encryption
     */
    fun initialize(context: Context, masterPassword: String? = null) {
        synchronized(lock) {
            // Don't reinitialize if already initialized
            if (realm != null && realm?.isClosed() == false) {
                Log.d(TAG, "Realm already initialized and open")
                return
            }
            
            try {
                Log.d(TAG, "Initializing Realm database with encryption")
                
                // Store context reference for potential fallback initialization
                contextRef = java.lang.ref.WeakReference(context)
            
            // Get master password from BiometricHelper if not provided
            val password = masterPassword ?: run {
                val biometricHelper = BiometricHelper(context)
                biometricHelper.getMasterPassword()
            }
            
            if (password == null) {
                throw SecurityException("Master password not available for database encryption")
            }
            
            // Generate encryption key from master password
            val encryptionKey = deriveEncryptionKey(password)
            currentEncryptionKey = encryptionKey
            
            // Configure Realm with encryption
            val config = RealmConfiguration.Builder(
                schema = setOf(
                    Note::class,
                    Dataset::class,
                    Form::class,
                    TableColumn::class,
                    TableSettings::class
                )
            )
                .name(DATABASE_NAME)
                .schemaVersion(1)
                .encryptionKey(encryptionKey) // Enable AES-256 encryption
                .build()
            
            realm = Realm.open(config)
            Log.d(TAG, "Realm database initialized successfully with encryption")
            
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize encrypted Realm database", e)
                throw RuntimeException("Failed to initialize encrypted database: ${e.message}", e)
            }
        }
    }
    
    /**
     * Initialize without encryption (fallback for testing or migration)
     */
    fun initializeUnencrypted() {
        synchronized(lock) {
            // Don't reinitialize if already initialized
            if (realm != null && realm?.isClosed() == false) {
                Log.d(TAG, "Realm already initialized and open")
                return
            }
            
            try {
                Log.d(TAG, "Initializing Realm database without encryption")
            
            val config = RealmConfiguration.Builder(
                schema = setOf(
                    Note::class,
                    Dataset::class,
                    Form::class,
                    TableColumn::class,
                    TableSettings::class
                )
            )
                .name(DATABASE_NAME)
                .schemaVersion(1)
                .build()
            
            realm = Realm.open(config)
            currentEncryptionKey = null
            Log.d(TAG, "Realm database initialized without encryption")
            
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Realm database", e)
                throw RuntimeException("Failed to initialize database: ${e.message}", e)
            }
        }
    }
    
    fun getInstance(): Realm {
        // Fast path: if realm exists and is not closed, return it without synchronization
        realm?.let { currentRealm ->
            if (!currentRealm.isClosed()) {
                return currentRealm
            }
        }
        
        // Slow path: need to initialize or reinitialize
        synchronized(lock) {
            // Double-check pattern: check again inside synchronized block
            return realm?.takeIf { !it.isClosed() } ?: run {
                Log.w(TAG, "Realm not initialized or closed, attempting to initialize with default encryption")
                try {
                    // Try to initialize with stored master password if available
                    // This is a fallback to prevent crashes during app startup
                    val context = getCurrentContext()
                    if (context != null) {
                        initialize(context)
                        realm?.takeIf { !it.isClosed() } ?: throw IllegalStateException("Failed to initialize Realm even with fallback")
                    } else {
                        throw IllegalStateException("Realm not initialized and no context available. Call initialize() first.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Fallback Realm initialization failed", e)
                    throw IllegalStateException("Realm not initialized. Call initialize() first.", e)
                }
            }
        }
    }
    
    // Keep a weak reference to context for fallback initialization
    private var contextRef: java.lang.ref.WeakReference<Context>? = null
    
    private fun getCurrentContext(): Context? {
        return contextRef?.get()
    }
    
    fun close() {
        synchronized(lock) {
            realm?.let { realmInstance ->
                if (!realmInstance.isClosed()) {
                    Log.d(TAG, "Closing Realm database")
                    realmInstance.close()
                }
            }
            realm = null
            currentEncryptionKey = null
        }
    }
    
    /**
     * Get the current encryption key (for backup operations)
     */
    fun getCurrentEncryptionKey(): ByteArray? {
        return currentEncryptionKey?.copyOf()
    }
    
    /**
     * Check if database is currently encrypted
     */
    fun isEncrypted(): Boolean {
        return currentEncryptionKey != null
    }
    
    /**
     * Check if Realm database is initialized and not closed
     */
    val isInitialized: Boolean
        get() = realm != null && realm?.isClosed() == false
    
    /**
     * Get the database file path
     */
    fun getDatabasePath(context: Context): String {
        return File(context.filesDir, DATABASE_NAME).absolutePath
    }
    
    /**
     * Derive encryption key from master password using PBKDF2
     */
    fun deriveEncryptionKey(masterPassword: String): ByteArray {
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val spec = PBEKeySpec(
            masterPassword.toCharArray(),
            SALT.toByteArray(),
            PBKDF2_ITERATIONS,
            KEY_SIZE
        )
        return factory.generateSecret(spec).encoded
    }
    
    /**
     * Validate that the provided password can decrypt the database
     */
    fun validatePassword(context: Context, password: String): Boolean {
        if (!isEncrypted()) {
            return true // No password needed for unencrypted database
        }

        return try {
            val testKey = deriveEncryptionKey(password)
            // Try to open a temporary connection with this key
            val config = RealmConfiguration.Builder(
                schema = setOf(
                    Note::class,
                    Dataset::class,
                    Form::class,
                    TableColumn::class,
                    TableSettings::class
                )
            )
                .name(DATABASE_NAME)
                .schemaVersion(1)
                .encryptionKey(testKey)
                .build()

            val testRealm = Realm.open(config)
            testRealm.close()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Password validation failed", e)
            false
        }
    }

    /**
     * Detect the actual encryption state of the database file
     * @return "encrypted", "unencrypted", "corrupted", or "missing"
     */
    fun detectDatabaseState(context: Context, masterPassword: String? = null): String {
        val dbFile = File(context.filesDir, DATABASE_NAME)

        if (!dbFile.exists()) {
            return "missing"
        }

        // Try to open as unencrypted first
        try {
            val unencryptedConfig = RealmConfiguration.Builder(
                schema = setOf(
                    Note::class,
                    Dataset::class,
                    Form::class,
                    TableColumn::class,
                    TableSettings::class
                )
            )
                .name(DATABASE_NAME)
                .schemaVersion(1)
                .build()

            val testRealm = Realm.open(unencryptedConfig)
            testRealm.close()
            Log.d(TAG, "Database is UNENCRYPTED")
            return "unencrypted"
        } catch (e: Exception) {
            Log.d(TAG, "Database is not unencrypted: ${e.message}")
        }

        // Try to open as encrypted if password provided
        if (masterPassword != null) {
            try {
                val encryptionKey = deriveEncryptionKey(masterPassword)
                val encryptedConfig = RealmConfiguration.Builder(
                    schema = setOf(
                        Note::class,
                        Dataset::class,
                        Form::class,
                        TableColumn::class,
                        TableSettings::class
                    )
                )
                    .name(DATABASE_NAME)
                    .schemaVersion(1)
                    .encryptionKey(encryptionKey)
                    .build()

                val testRealm = Realm.open(encryptedConfig)
                testRealm.close()
                Log.d(TAG, "Database is ENCRYPTED (password works)")
                return "encrypted"
            } catch (e: Exception) {
                Log.e(TAG, "Database cannot be opened with encryption: ${e.message}")
            }
        }

        Log.w(TAG, "Database appears to be CORRUPTED")
        return "corrupted"
    }

    /**
     * Check if unencrypted database file exists with data
     */
    fun hasUnencryptedData(context: Context): Boolean {
        val dbFile = File(context.filesDir, DATABASE_NAME)
        if (!dbFile.exists() || dbFile.length() == 0L) {
            return false
        }

        return try {
            // Try to open as unencrypted
            val config = RealmConfiguration.Builder(
                schema = setOf(
                    Note::class,
                    Dataset::class,
                    Form::class,
                    TableColumn::class,
                    TableSettings::class
                )
            )
                .name(DATABASE_NAME)
                .schemaVersion(1)
                .build()

            val testRealm = Realm.open(config)
            val noteCount = testRealm.query(Note::class).find().size
            val datasetCount = testRealm.query(Dataset::class).find().size
            val hasData = noteCount > 0 || datasetCount > 0
            testRealm.close()
            hasData
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check for unencrypted data", e)
            false
        }
    }

    /**
     * Migrate data from unencrypted database to encrypted database
     * @param context Application context
     * @param masterPassword Password for encryption
     * @return true if migration successful or no data to migrate, false on error
     */
    fun migrateToEncrypted(context: Context, masterPassword: String): Boolean {
        synchronized(lock) {
            try {
                Log.d(TAG, "Starting migration from unencrypted to encrypted database")

                // Close current realm if open
                close()

                val dbFile = File(context.filesDir, DATABASE_NAME)
                if (!dbFile.exists()) {
                    Log.d(TAG, "No existing database to migrate")
                    return true // Nothing to migrate
                }

                // Check if current database is already encrypted
                try {
                    val encryptionKey = deriveEncryptionKey(masterPassword)
                    val testConfig = RealmConfiguration.Builder(
                        schema = setOf(
                            Note::class,
                            Dataset::class,
                            Form::class,
                            TableColumn::class,
                            TableSettings::class
                        )
                    )
                        .name(DATABASE_NAME)
                        .schemaVersion(1)
                        .encryptionKey(encryptionKey)
                        .build()

                    val testRealm = Realm.open(testConfig)
                    testRealm.close()
                    Log.d(TAG, "Database is already encrypted, no migration needed")
                    return true
                } catch (e: Exception) {
                    // Not encrypted, proceed with migration
                    Log.d(TAG, "Database is unencrypted, proceeding with migration")
                }

                // Open unencrypted database
                val unencryptedConfig = RealmConfiguration.Builder(
                    schema = setOf(
                        Note::class,
                        Dataset::class,
                        Form::class,
                        TableColumn::class,
                        TableSettings::class
                    )
                )
                    .name(DATABASE_NAME)
                    .schemaVersion(1)
                    .build()

                val sourceRealm = Realm.open(unencryptedConfig)

                // Create encrypted database with temporary name
                val tempDbName = "aksara_notes_temp.realm"
                val encryptionKey = deriveEncryptionKey(masterPassword)
                val encryptedConfig = RealmConfiguration.Builder(
                    schema = setOf(
                        Note::class,
                        Dataset::class,
                        Form::class,
                        TableColumn::class,
                        TableSettings::class
                    )
                )
                    .name(tempDbName)
                    .schemaVersion(1)
                    .encryptionKey(encryptionKey)
                    .build()

                val destRealm = Realm.open(encryptedConfig)

                // Copy all data
                val notes = sourceRealm.query(Note::class).find()
                val datasets = sourceRealm.query(Dataset::class).find()
                val forms = sourceRealm.query(Form::class).find()
                val columns = sourceRealm.query(TableColumn::class).find()
                val settings = sourceRealm.query(TableSettings::class).find()

                Log.d(TAG, "Migrating ${notes.size} notes, ${datasets.size} datasets")

                destRealm.writeBlocking {
                    // Copy notes - create new instances with properties from source
                    notes.forEach { sourceNote ->
                        val newNote = Note().apply {
                            // Create new ObjectId from hex string to avoid managed object issues
                            _id = ObjectId(sourceNote._id.toHexString())
                            id = sourceNote.id
                            title = sourceNote.title
                            content = sourceNote.content
                            createdAt = sourceNote.createdAt
                            updatedAt = sourceNote.updatedAt
                            requiresPin = sourceNote.requiresPin
                            isEncrypted = sourceNote.isEncrypted
                            isFavorite = sourceNote.isFavorite
                            tags = sourceNote.tags
                        }
                        copyToRealm(newNote)
                    }

                    // Copy datasets
                    datasets.forEach { sourceDataset ->
                        val newDataset = Dataset().apply {
                            _id = ObjectId(sourceDataset._id.toHexString())
                            id = sourceDataset.id
                            name = sourceDataset.name
                            description = sourceDataset.description
                            icon = sourceDataset.icon
                            datasetType = sourceDataset.datasetType
                            createdAt = sourceDataset.createdAt
                            updatedAt = sourceDataset.updatedAt
                        }
                        copyToRealm(newDataset)
                    }

                    // Copy forms
                    forms.forEach { sourceForm ->
                        val newForm = Form().apply {
                            _id = ObjectId(sourceForm._id.toHexString())
                            id = sourceForm.id
                            datasetId = sourceForm.datasetId
                            data = sourceForm.data
                            createdAt = sourceForm.createdAt
                            updatedAt = sourceForm.updatedAt
                        }
                        copyToRealm(newForm)
                    }

                    // Copy columns
                    columns.forEach { sourceColumn ->
                        val newColumn = TableColumn().apply {
                            _id = ObjectId(sourceColumn._id.toHexString())
                            id = sourceColumn.id
                            name = sourceColumn.name
                            type = sourceColumn.type
                            required = sourceColumn.required
                            defaultValue = sourceColumn.defaultValue
                            options = sourceColumn.options
                            displayName = sourceColumn.displayName
                            icon = sourceColumn.icon
                        }
                        copyToRealm(newColumn)
                    }

                    // Copy settings
                    settings.forEach { sourceSetting ->
                        val newSetting = TableSettings().apply {
                            primaryColor = sourceSetting.primaryColor
                            showInCalendar = sourceSetting.showInCalendar
                            calendarDateField = sourceSetting.calendarDateField
                            reminderDays = sourceSetting.reminderDays
                        }
                        copyToRealm(newSetting)
                    }
                }

                // Close both databases
                sourceRealm.close()
                destRealm.close()

                // Backup old database
                val backupFile = File(context.filesDir, "aksara_notes_backup.realm")
                if (backupFile.exists()) {
                    backupFile.delete()
                }
                dbFile.renameTo(backupFile)

                // Rename temp database to main database
                val tempFile = File(context.filesDir, tempDbName)
                tempFile.renameTo(dbFile)

                Log.d(TAG, "Migration completed successfully. Backup saved as aksara_notes_backup.realm")
                return true

            } catch (e: Exception) {
                Log.e(TAG, "Migration failed", e)
                return false
            }
        }
    }

    /**
     * Migrate data from encrypted database to unencrypted database
     * @param context Application context
     * @param masterPassword Current password for decryption
     * @return true if migration successful or no data to migrate, false on error
     */
    fun migrateToUnencrypted(context: Context, masterPassword: String): Boolean {
        synchronized(lock) {
            var sourceRealm: Realm? = null
            var destRealm: Realm? = null

            try {
                Log.d(TAG, "Starting migration from encrypted to unencrypted database")

                // Close current realm if open
                Log.d(TAG, "Closing current Realm instance")
                close()

                // Give it a moment to fully close
                Thread.sleep(500)

                val dbFile = File(context.filesDir, DATABASE_NAME)
                if (!dbFile.exists()) {
                    Log.d(TAG, "No existing database to migrate")
                    return true // Nothing to migrate
                }

                Log.d(TAG, "Database file exists, size: ${dbFile.length()} bytes")

                // Verify the password is correct
                Log.d(TAG, "Deriving encryption key from password")
                val encryptionKey = deriveEncryptionKey(masterPassword)
                val encryptedConfig = RealmConfiguration.Builder(
                    schema = setOf(
                        Note::class,
                        Dataset::class,
                        Form::class,
                        TableColumn::class,
                        TableSettings::class
                    )
                )
                    .name(DATABASE_NAME)
                    .schemaVersion(1)
                    .encryptionKey(encryptionKey)
                    .build()

                // Open encrypted database
                Log.d(TAG, "Attempting to open encrypted database")
                sourceRealm = try {
                    Realm.open(encryptedConfig)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open encrypted database - wrong password?", e)
                    return false
                }

                Log.d(TAG, "Encrypted database opened successfully")

                // Create unencrypted database with temporary name
                val tempDbName = "aksara_notes_temp.realm"
                Log.d(TAG, "Creating temporary unencrypted database: $tempDbName")

                // Delete temp file if it exists from previous failed attempt
                val tempFile = File(context.filesDir, tempDbName)
                if (tempFile.exists()) {
                    Log.d(TAG, "Deleting old temp file")
                    tempFile.delete()
                }

                val unencryptedConfig = RealmConfiguration.Builder(
                    schema = setOf(
                        Note::class,
                        Dataset::class,
                        Form::class,
                        TableColumn::class,
                        TableSettings::class
                    )
                )
                    .name(tempDbName)
                    .schemaVersion(1)
                    .build()

                Log.d(TAG, "Opening temporary unencrypted database")
                destRealm = Realm.open(unencryptedConfig)
                Log.d(TAG, "Temporary database opened successfully")

                // Copy all data
                Log.d(TAG, "Reading data from encrypted database")
                val notes = sourceRealm.query(Note::class).find()
                val datasets = sourceRealm.query(Dataset::class).find()
                val forms = sourceRealm.query(Form::class).find()
                val columns = sourceRealm.query(TableColumn::class).find()
                val settings = sourceRealm.query(TableSettings::class).find()

                Log.d(TAG, "Found: ${notes.size} notes, ${datasets.size} datasets, ${forms.size} forms, ${columns.size} columns, ${settings.size} settings")
                Log.d(TAG, "Writing data to unencrypted database")

                destRealm.writeBlocking {
                    // Copy notes - create new instances with properties from source
                    notes.forEach { sourceNote ->
                        val newNote = Note().apply {
                            // Create new ObjectId from hex string to avoid managed object issues
                            _id = ObjectId(sourceNote._id.toHexString())
                            id = sourceNote.id
                            title = sourceNote.title
                            content = sourceNote.content
                            createdAt = sourceNote.createdAt
                            updatedAt = sourceNote.updatedAt
                            requiresPin = sourceNote.requiresPin
                            isEncrypted = sourceNote.isEncrypted
                            isFavorite = sourceNote.isFavorite
                            tags = sourceNote.tags
                        }
                        copyToRealm(newNote)
                    }

                    // Copy datasets
                    datasets.forEach { sourceDataset ->
                        val newDataset = Dataset().apply {
                            _id = ObjectId(sourceDataset._id.toHexString())
                            id = sourceDataset.id
                            name = sourceDataset.name
                            description = sourceDataset.description
                            icon = sourceDataset.icon
                            datasetType = sourceDataset.datasetType
                            createdAt = sourceDataset.createdAt
                            updatedAt = sourceDataset.updatedAt
                            // Note: columns and settings are copied separately and linked via IDs
                        }
                        copyToRealm(newDataset)
                    }

                    // Copy forms
                    forms.forEach { sourceForm ->
                        val newForm = Form().apply {
                            _id = ObjectId(sourceForm._id.toHexString())
                            id = sourceForm.id
                            datasetId = sourceForm.datasetId
                            data = sourceForm.data
                            createdAt = sourceForm.createdAt
                            updatedAt = sourceForm.updatedAt
                        }
                        copyToRealm(newForm)
                    }

                    // Copy columns
                    columns.forEach { sourceColumn ->
                        val newColumn = TableColumn().apply {
                            _id = ObjectId(sourceColumn._id.toHexString())
                            id = sourceColumn.id
                            name = sourceColumn.name
                            type = sourceColumn.type
                            required = sourceColumn.required
                            defaultValue = sourceColumn.defaultValue
                            options = sourceColumn.options
                            displayName = sourceColumn.displayName
                            icon = sourceColumn.icon
                        }
                        copyToRealm(newColumn)
                    }

                    // Copy settings
                    settings.forEach { sourceSetting ->
                        val newSetting = TableSettings().apply {
                            primaryColor = sourceSetting.primaryColor
                            showInCalendar = sourceSetting.showInCalendar
                            calendarDateField = sourceSetting.calendarDateField
                            reminderDays = sourceSetting.reminderDays
                        }
                        copyToRealm(newSetting)
                    }
                }

                Log.d(TAG, "Data written successfully, closing databases")

                // Close both databases
                sourceRealm.close()
                sourceRealm = null
                destRealm.close()
                destRealm = null

                Log.d(TAG, "Databases closed, performing file operations")

                // Backup old encrypted database
                val backupFile = File(context.filesDir, "aksara_notes_encrypted_backup.realm")
                if (backupFile.exists()) {
                    Log.d(TAG, "Deleting old backup file")
                    backupFile.delete()
                }

                Log.d(TAG, "Renaming encrypted database to backup")
                val renamed = dbFile.renameTo(backupFile)
                if (!renamed) {
                    Log.e(TAG, "Failed to rename encrypted database to backup")
                    return false
                }

                // Rename temp database to main database
                Log.d(TAG, "Renaming temp database to main database")
                val renamed2 = tempFile.renameTo(dbFile)
                if (!renamed2) {
                    Log.e(TAG, "Failed to rename temp database to main")
                    // Try to restore backup
                    backupFile.renameTo(dbFile)
                    return false
                }

                Log.d(TAG, "Migration to unencrypted completed successfully. Backup saved as aksara_notes_encrypted_backup.realm")
                return true

            } catch (e: Exception) {
                Log.e(TAG, "Migration to unencrypted failed with exception", e)
                Log.e(TAG, "Exception type: ${e.javaClass.name}")
                Log.e(TAG, "Exception message: ${e.message}")
                e.printStackTrace()

                // Clean up
                try {
                    sourceRealm?.close()
                    destRealm?.close()
                } catch (closeException: Exception) {
                    Log.e(TAG, "Error closing realms during cleanup", closeException)
                }

                return false
            }
        }
    }
}