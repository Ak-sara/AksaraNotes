package com.aksara.notes

import android.app.Application
import android.util.Log
import com.aksara.notes.utils.AuthenticationManager
import com.aksara.notes.data.database.RealmDatabase
import java.io.File

class AksaraApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize authentication manager first
        AuthenticationManager.getInstance().initialize(this)

        // Initialize Realm database
        // Default to unencrypted, will switch to encrypted when user enables encryption
        try {
            val biometricHelper = com.aksara.notes.utils.BiometricHelper(this)
            if (biometricHelper.isAppSetUp()) {
                // User has enabled encryption
                Log.d("AksaraApplication", "Encryption enabled in settings")

                // Check if we need to migrate existing unencrypted data
                try {
                    val password = biometricHelper.getMasterPassword()
                    Log.d("AksaraApplication", "Master password available: ${password != null}")

                    val dbState = RealmDatabase.detectDatabaseState(this, password)
                    Log.d("AksaraApplication", "Current database state: $dbState")

                    if (dbState == "unencrypted") {
                        // We have unencrypted data but encryption is enabled
                        // This happens after SetupActivity enables encryption
                        Log.d("AksaraApplication", "⚠️ Migrating unencrypted data to encrypted format...")

                        if (password != null) {
                            Log.d("AksaraApplication", "Starting migration with password...")
                            val migrationSuccess = RealmDatabase.migrateToEncrypted(this, password)
                            if (migrationSuccess) {
                                Log.d("AksaraApplication", "✅ Migration to encrypted completed successfully")
                            } else {
                                Log.e("AksaraApplication", "❌ Migration to encrypted FAILED")
                            }
                        } else {
                            Log.e("AksaraApplication", "❌ Cannot migrate - no password available")
                        }
                    } else if (dbState == "encrypted") {
                        Log.d("AksaraApplication", "✅ Database already encrypted, no migration needed")
                    } else if (dbState == "missing") {
                        Log.d("AksaraApplication", "ℹ️ No database file exists yet, will create encrypted")
                    } else if (dbState == "corrupted") {
                        // Database exists but can't be opened with current password
                        // This happens when password changed or database is actually corrupted
                        Log.w("AksaraApplication", "⚠️ Database corrupted or encrypted with different key, deleting and starting fresh")
                        deleteCorruptedDatabase()
                    } else {
                        Log.w("AksaraApplication", "⚠️ Database state is $dbState")
                    }
                } catch (e: Exception) {
                    Log.e("AksaraApplication", "❌ Error during migration check", e)
                }

                // Initialize with encryption
                Log.d("AksaraApplication", "Initializing encrypted Realm")
                RealmDatabase.initialize(this)
            } else {
                // User hasn't enabled encryption yet, use unencrypted database
                Log.d("AksaraApplication", "Encryption not enabled, using unencrypted Realm")

                // Check if there's an incompatible encrypted database
                val dbFile = File(filesDir, "aksara_notes.realm")
                if (dbFile.exists()) {
                    val dbState = RealmDatabase.detectDatabaseState(this, null)
                    if (dbState == "corrupted") {
                        // Database is encrypted but we're trying unencrypted mode
                        Log.w("AksaraApplication", "⚠️ Found encrypted database but encryption disabled, deleting")
                        deleteCorruptedDatabase()
                    }
                }

                RealmDatabase.initializeUnencrypted()
            }
        } catch (e: Exception) {
            Log.e("AksaraApplication", "Failed to initialize Realm", e)

            // Check if error is due to encryption mismatch
            if (e.message?.contains("decryption failed") == true ||
                e.message?.contains("HMAC check") == true ||
                e.message?.contains("invalid mnemonic") == true ||
                e.cause?.message?.contains("decryption failed") == true ||
                e.cause?.message?.contains("HMAC check") == true) {

                Log.w("AksaraApplication", "⚠️ Encryption key mismatch detected, deleting database and retrying")
                deleteCorruptedDatabase()

                // Retry initialization
                try {
                    val biometricHelper = com.aksara.notes.utils.BiometricHelper(this)
                    if (biometricHelper.isAppSetUp()) {
                        RealmDatabase.initialize(this)
                    } else {
                        RealmDatabase.initializeUnencrypted()
                    }
                    Log.d("AksaraApplication", "✅ Realm initialized after deleting corrupted database")
                } catch (retryError: Exception) {
                    Log.e("AksaraApplication", "❌ Retry failed", retryError)
                }
            } else {
                // Fallback to unencrypted to prevent crashes
                try {
                    deleteCorruptedDatabase()
                    RealmDatabase.initializeUnencrypted()
                    Log.w("AksaraApplication", "Using unencrypted Realm as fallback after delete")
                } catch (fallbackError: Exception) {
                    Log.e("AksaraApplication", "Even unencrypted Realm failed", fallbackError)
                    // Don't crash the app - let it try to initialize later
                }
            }
        }

        // Set up global authentication callback
        AuthenticationManager.getInstance().setAuthenticationCallback {
            // This will be triggered when authentication is required
            // The MainActivity will handle the actual authentication flow
        }
    }

    /**
     * Delete corrupted or incompatible database files
     */
    private fun deleteCorruptedDatabase() {
        try {
            val dbFile = File(filesDir, "aksara_notes.realm")
            val dbLockFile = File(filesDir, "aksara_notes.realm.lock")
            val dbManagementDir = File(filesDir, "aksara_notes.realm.management")

            if (dbFile.exists()) {
                val deleted = dbFile.delete()
                Log.d("AksaraApplication", "Deleted database file: $deleted")
            }
            if (dbLockFile.exists()) {
                dbLockFile.delete()
                Log.d("AksaraApplication", "Deleted lock file")
            }
            if (dbManagementDir.exists()) {
                dbManagementDir.deleteRecursively()
                Log.d("AksaraApplication", "Deleted management directory")
            }
        } catch (e: Exception) {
            Log.e("AksaraApplication", "Error deleting database files", e)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        RealmDatabase.close()
    }
}