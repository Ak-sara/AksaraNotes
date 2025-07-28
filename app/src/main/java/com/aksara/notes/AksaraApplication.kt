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
        try {
            // Try to initialize with encryption if app is set up
            val biometricHelper = com.aksara.notes.utils.BiometricHelper(this)
            if (biometricHelper.isAppSetUp()) {
                Log.d("AksaraApplication", "App is set up, initializing encrypted Realm")
                RealmDatabase.initialize(this)
            } else {
                Log.d("AksaraApplication", "App not set up yet, deferring Realm initialization")
                // Don't initialize Realm yet - it will be done in SetupActivity
            }
        } catch (e: Exception) {
            Log.e("AksaraApplication", "Failed to initialize encrypted Realm, using fallback", e)
            // Fallback to unencrypted for now to prevent crashes
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