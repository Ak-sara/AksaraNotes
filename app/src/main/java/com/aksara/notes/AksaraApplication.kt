package com.aksara.notes

import android.app.Application
import android.util.Log
import com.aksara.notes.utils.AuthenticationManager
import com.aksara.notes.data.database.RealmDatabase

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
                RealmDatabase.initializeUnencrypted()
            }
        } catch (e: Exception) {
            Log.e("AksaraApplication", "Failed to initialize Realm, using unencrypted fallback", e)
            // Fallback to unencrypted to prevent crashes
            try {
                RealmDatabase.initializeUnencrypted()
                Log.w("AksaraApplication", "Using unencrypted Realm as fallback")
            } catch (fallbackError: Exception) {
                Log.e("AksaraApplication", "Even unencrypted Realm failed", fallbackError)
                // Don't crash the app - let it try to initialize later
            }
        }

        // Set up global authentication callback
        AuthenticationManager.getInstance().setAuthenticationCallback {
            // This will be triggered when authentication is required
            // The MainActivity will handle the actual authentication flow
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        RealmDatabase.close()
    }
}