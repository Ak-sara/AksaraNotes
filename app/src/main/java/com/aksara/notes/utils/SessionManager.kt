package com.aksara.notes.utils

import android.content.Context

// TEMPORARY STUB - SessionManager for session handling
class SessionManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager().also { INSTANCE = it }
            }
        }
    }

    private var context: Context? = null
    private var isAuthenticated = false
    private var lastActivityTime = System.currentTimeMillis()
    private var isInBackground = false

    // Initialize session manager
    fun initialize(context: Context) {
        this.context = context
    }

    // Check if authentication is required
    fun isAuthenticationRequired(biometricHelper: BiometricHelper): Boolean {
        // For testing - always require authentication on app start
        return !isAuthenticated || shouldLockDueToTimeout(biometricHelper)
    }

    // Check if should lock due to timeout
    private fun shouldLockDueToTimeout(biometricHelper: BiometricHelper): Boolean {
        if (!biometricHelper.isAutoLockEnabled()) {
            return false
        }

        val timeout = biometricHelper.getAutoLockTimeout()
        if (timeout <= 0) {
            return false // Never timeout if set to 0 or negative
        }

        val timeSinceActivity = System.currentTimeMillis() - lastActivityTime
        return isInBackground && timeSinceActivity > timeout
    }

    // Mark as authenticated
    fun markAuthenticated() {
        isAuthenticated = true
        updateActivity()
    }

    // Update activity timestamp
    fun updateActivity() {
        lastActivityTime = System.currentTimeMillis()
    }

    // App lifecycle methods
    fun onAppForegrounded() {
        isInBackground = false
        updateActivity()
    }

    fun onAppBackgrounded() {
        isInBackground = true
        updateActivity()
    }

    fun onAppDestroyed() {
        // Clear authentication when app is destroyed
        isAuthenticated = false
    }

    // Clear session
    fun clearSession() {
        isAuthenticated = false
        lastActivityTime = System.currentTimeMillis()
        isInBackground = false
    }

    // Session state checks
    fun isSessionActive(): Boolean {
        return isAuthenticated
    }

    fun getLastActivityTime(): Long {
        return lastActivityTime
    }

    fun isAppInBackground(): Boolean {
        return isInBackground
    }

    // Force authentication requirement
    fun requireAuthentication() {
        isAuthenticated = false
    }

    // Session timeout handling
    fun checkSessionTimeout(biometricHelper: BiometricHelper): Boolean {
        return shouldLockDueToTimeout(biometricHelper)
    }

    // Reset session
    fun resetSession() {
        clearSession()
    }
}