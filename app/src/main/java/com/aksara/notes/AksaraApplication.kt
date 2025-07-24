package com.aksara.notes

import android.app.Application
import com.aksara.notes.utils.AuthenticationManager

class AksaraApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize authentication manager
        AuthenticationManager.getInstance().initialize(this)

        // Set up global authentication callback
        AuthenticationManager.getInstance().setAuthenticationCallback {
            // This will be triggered when authentication is required
            // The MainActivity will handle the actual authentication flow
        }
    }
}