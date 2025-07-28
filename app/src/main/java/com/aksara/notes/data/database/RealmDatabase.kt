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
    
    private var realm: Realm? = null
    private var currentEncryptionKey: ByteArray? = null
    
    /**
     * Initialize Realm with encryption using the master password
     * @param context Application context
     * @param masterPassword The user's master password for encryption
     */
    fun initialize(context: Context, masterPassword: String? = null) {
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
    
    /**
     * Initialize without encryption (fallback for testing or migration)
     */
    fun initializeUnencrypted() {
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
    
    fun getInstance(): Realm {
        return realm ?: run {
            Log.w(TAG, "Realm not initialized, attempting to initialize with default encryption")
            try {
                // Try to initialize with stored master password if available
                // This is a fallback to prevent crashes during app startup
                val context = getCurrentContext()
                if (context != null) {
                    initialize(context)
                    realm ?: throw IllegalStateException("Failed to initialize Realm even with fallback")
                } else {
                    throw IllegalStateException("Realm not initialized and no context available. Call initialize() first.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallback Realm initialization failed", e)
                throw IllegalStateException("Realm not initialized. Call initialize() first.", e)
            }
        }
    }
    
    // Keep a weak reference to context for fallback initialization
    private var contextRef: java.lang.ref.WeakReference<Context>? = null
    
    private fun getCurrentContext(): Context? {
        return contextRef?.get()
    }
    
    fun close() {
        realm?.close()
        realm = null
        currentEncryptionKey = null
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
}