package com.aksara.aksaranotes.utils

import android.content.Context

// TEMPORARY STUB - AuthenticationManager with missing constants
class AuthenticationManager private constructor() {

    companion object {
        // Timeout constants
        const val TIMEOUT_NEVER = -1L
        const val TIMEOUT_IMMEDIATE = 0L
        const val TIMEOUT_1_MINUTE = 60000L
        const val TIMEOUT_5_MINUTES = 300000L
        const val TIMEOUT_15_MINUTES = 900000L
        const val TIMEOUT_1_HOUR = 3600000L

        // Authentication types
        const val AUTH_TYPE_PASSWORD = 1
        const val AUTH_TYPE_BIOMETRIC = 2
        const val AUTH_TYPE_BOTH = 3

        // Authentication states
        const val STATE_AUTHENTICATED = 1
        const val STATE_UNAUTHENTICATED = 0
        const val STATE_LOCKED = -1

        // Singleton instance
        @Volatile
        private var INSTANCE: AuthenticationManager? = null

        fun getInstance(): AuthenticationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthenticationManager().also { INSTANCE = it }
            }
        }
    }

    private var context: Context? = null
    private var authenticationCallback: (() -> Unit)? = null
    private var isInitialized = false

    // Initialize the authentication manager
    fun initialize(context: Context) {
        this.context = context
        this.isInitialized = true
    }

    // Set authentication callback
    fun setAuthenticationCallback(callback: () -> Unit) {
        this.authenticationCallback = callback
    }

    // Check if initialized
    fun isInitialized(): Boolean {
        return isInitialized
    }

    // Authentication methods
    fun requiresAuthentication(): Boolean {
        return true // Default to requiring authentication
    }

    fun authenticate(onSuccess: () -> Unit, onError: (String) -> Unit) {
        // Simplified - just call success for testing
        onSuccess()
    }

    fun markAuthenticated() {
        // Mark as authenticated
    }

    fun clearAuthentication() {
        // Clear authentication state
    }

    // Timeout methods
    fun setAutoLockTimeout(timeout: Long) {
        // Set auto-lock timeout
    }

    fun getAutoLockTimeout(): Long {
        return TIMEOUT_5_MINUTES // Default 5 minutes
    }

    // Session methods
    fun startSession() {
        // Start authentication session
    }

    fun endSession() {
        // End authentication session
    }

    fun isSessionValid(): Boolean {
        return true // Default to valid for testing
    }

    // Lock methods
    fun lockApp() {
        authenticationCallback?.invoke()
    }

    fun unlockApp() {
        // Unlock the app
    }

    // Helper methods for backwards compatibility
    val immediate: Long get() = TIMEOUT_IMMEDIATE
    val never: Long get() = TIMEOUT_NEVER
}