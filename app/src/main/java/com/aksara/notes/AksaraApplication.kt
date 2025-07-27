package com.aksara.notes

import android.app.Application
import com.aksara.notes.utils.AuthenticationManager
import com.aksara.notes.data.database.RealmDatabase

class AksaraApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Realm database
        RealmDatabase.initialize()

        // Initialize authentication manager
        AuthenticationManager.getInstance().initialize(this)

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