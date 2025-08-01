package com.aksara.notes.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aksara.notes.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class BiometricHelper(private val context: Context) {

    companion object {
        private const val TAG = "BiometricHelper"
        private const val PREF_APP_SETUP = "app_setup"
        private const val PREF_SETUP_COMPLETE = "is_setup"
        private const val PREF_MASTER_PASSWORD = "master_password"
        private const val PREF_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val PREF_AUTO_LOCK_ENABLED = "auto_lock_enabled"
        private const val PREF_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        private const val PREF_NOTE_PIN = "note_pin"
        private const val PREF_PIN_ENABLED = "pin_enabled"

        // Default test password for development
//        private const val DEFAULT_TEST_PASSWORD = "test123"
    }

    private val prefs = context.getSharedPreferences(PREF_APP_SETUP, Context.MODE_PRIVATE)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Check if app is set up
    fun isAppSetUp(): Boolean {
        return prefs.getBoolean(PREF_SETUP_COMPLETE, false)
    }

    // Setup master password (called from SetupActivity)
    fun setupMasterPassword(password: String): Boolean {
        try {
            prefs.edit()
                .putString(PREF_MASTER_PASSWORD, password)
                .putBoolean(PREF_SETUP_COMPLETE, true)
                .apply()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup master password", e)
            return false
        }
    }

    // Setup biometric authentication
    fun setupBiometric(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_BIOMETRIC_ENABLED, enabled).apply()
    }

    // Check if biometric is available on device
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Main authentication method
    fun authenticateUser(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onPasswordFallback: () -> Unit
    ) {
        Log.d(TAG, "authenticateUser called")

        // Check if biometric is enabled and available
        if (isBiometricEnabled() && isBiometricAvailable() && context is FragmentActivity) {
            Log.d(TAG, "Starting biometric authentication")
            performBiometricAuthentication(
                activity = context,
                onSuccess = {
                    Log.d(TAG, "Biometric authentication succeeded")
                    mainHandler.post { onSuccess() }
                },
                onError = { error ->
                    Log.e(TAG, "Biometric authentication failed: $error")
                    mainHandler.post { onError(error) }
                },
                onFallback = {
                    Log.d(TAG, "Biometric authentication fallback to password")
                    mainHandler.post { onPasswordFallback() }
                }
            )
        } else {
            Log.d(TAG, "Biometric not available, falling back to password")
            // Immediate fallback to password if biometric not available
            mainHandler.post { onPasswordFallback() }
        }
    }

    // Perform biometric authentication
    private fun performBiometricAuthentication(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFallback: () -> Unit
    ) {
        try {
            val executor = ContextCompat.getMainExecutor(activity)

            val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d(TAG, "BiometricPrompt.onAuthenticationSucceeded called")
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, "BiometricPrompt.onAuthenticationError: $errorCode - $errString")

                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED -> {
                            // User chose to use password instead
                            onFallback()
                        }
                        BiometricPrompt.ERROR_NO_BIOMETRICS,
                        BiometricPrompt.ERROR_HW_NOT_PRESENT,
                        BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                            // Hardware issues - fallback to password
                            onFallback()
                        }
                        else -> {
                            // Other errors - try password fallback
                            onFallback()
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "BiometricPrompt.onAuthenticationFailed called")
                    // Don't call onError here - let user try again or use password
                }
            })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate to Aksara Notes")
                .setSubtitle("Use your fingerprint or face to access your secure data")
                .setDescription("Place your finger on the sensor or look at the camera")
                .setNegativeButtonText("Use Password")
                .setConfirmationRequired(false)
                .build()

            Log.d(TAG, "Launching biometric prompt")
            biometricPrompt.authenticate(promptInfo)

        } catch (e: Exception) {
            Log.e(TAG, "Exception in performBiometricAuthentication", e)
            onFallback()
        }
    }

    // Show password dialog
    fun showPasswordDialog(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        Log.d(TAG, "showPasswordDialog called")

        mainHandler.post {
            try {
                val dialogView = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(48, 24, 48, 24)
                }

                val passwordLayout = TextInputLayout(context).apply {
                    hint = "Master Password"
                    boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                    
                    // Fix hint text color for better contrast in dark mode
                    val textColor = androidx.core.content.ContextCompat.getColor(context, R.color.text_primary)
                    val hintColor = androidx.core.content.ContextCompat.getColor(context, R.color.text_hint)
                    setHintTextColor(android.content.res.ColorStateList.valueOf(hintColor))
                    boxStrokeColor = textColor
                }

                val passwordEdit = TextInputEditText(context).apply {
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                    
                    // Set text color for better contrast in dark mode
                    val textColor = androidx.core.content.ContextCompat.getColor(context, R.color.text_primary)
                    setTextColor(textColor)
                }

                passwordLayout.addView(passwordEdit)
                dialogView.addView(passwordLayout)

                AlertDialog.Builder(context)
                    .setTitle("Enter Master Password")
                    .setMessage("Please enter your master password to continue")
                    .setView(dialogView)
                    .setPositiveButton("Unlock") { _, _ ->
                        val password = passwordEdit.text?.toString() ?: ""
                        if (verifyMasterPassword(password)) {
                            Log.d(TAG, "Password verification succeeded")
                            onSuccess()
                        } else {
                            Log.w(TAG, "Password verification failed")
                            onError("Incorrect password")
                        }
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        Log.d(TAG, "Password dialog cancelled")
                        onCancel()
                    }
                    .setCancelable(false)
                    .show()

            } catch (e: Exception) {
                Log.e(TAG, "Exception in showPasswordDialog", e)
                onError("Failed to show password dialog")
            }
        }
    }

    // Verify master password
    fun verifyMasterPassword(password: String): Boolean {
        val storedPassword = prefs.getString(PREF_MASTER_PASSWORD, "")

        // Check stored password first
        if (password == storedPassword) {
            return true
        }

        // Fallback to default test password for testing
//        if (password == DEFAULT_TEST_PASSWORD)  return true

        return false
    }

    // Change master password
    fun changeMasterPassword(currentPassword: String, newPassword: String): Boolean {
        if (!verifyMasterPassword(currentPassword)) {
            return false
        }
        prefs.edit().putString(PREF_MASTER_PASSWORD, newPassword).apply()
        return true
    }

    // Biometric settings
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(PREF_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_BIOMETRIC_ENABLED, enabled).apply()
    }

    // Auto-lock settings
    fun isAutoLockEnabled(): Boolean {
        return prefs.getBoolean(PREF_AUTO_LOCK_ENABLED, true)
    }

    fun setAutoLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_LOCK_ENABLED, enabled).apply()
    }

    fun getAutoLockTimeout(): Long {
        return prefs.getLong(PREF_AUTO_LOCK_TIMEOUT, 300000L) // 5 minutes default
    }

    fun setAutoLockTimeout(timeout: Long) {
        prefs.edit().putLong(PREF_AUTO_LOCK_TIMEOUT, timeout).apply()
    }

    // PIN management methods
    fun isPinEnabled(): Boolean {
        return prefs.getBoolean(PREF_PIN_ENABLED, false)
    }

    fun setPinEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_PIN_ENABLED, enabled).apply()
    }

    fun setupNotePin(pin: String): Boolean {
        try {
            if (pin.length != 4 || !pin.all { it.isDigit() }) {
                return false
            }
            prefs.edit()
                .putString(PREF_NOTE_PIN, pin)
                .putBoolean(PREF_PIN_ENABLED, true)
                .apply()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup PIN", e)
            return false
        }
    }

    fun verifyNotePin(pin: String): Boolean {
        val storedPin = prefs.getString(PREF_NOTE_PIN, "")
        return pin == storedPin && pin.isNotEmpty()
    }

    fun changeNotePin(currentPin: String, newPin: String): Boolean {
        if (!verifyNotePin(currentPin)) {
            return false
        }
        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            return false
        }
        prefs.edit().putString(PREF_NOTE_PIN, newPin).apply()
        return true
    }

    fun clearNotePin() {
        prefs.edit()
            .remove(PREF_NOTE_PIN)
            .putBoolean(PREF_PIN_ENABLED, false)
            .apply()
    }

    fun getNotePin(): String? {
        return prefs.getString(PREF_NOTE_PIN, null)
    }

    // Show PIN setup dialog for note protection
    fun showPinSetupDialog(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        Log.d(TAG, "showPinSetupDialog called")

        mainHandler.post {
            try {
                val dialogView = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(48, 24, 48, 24)
                }

                // New PIN
                val pinLayout = TextInputLayout(context).apply {
                    hint = "Enter PIN"
                    boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                    
                    val textColor = androidx.core.content.ContextCompat.getColor(context, R.color.text_primary)
                    val hintColor = androidx.core.content.ContextCompat.getColor(context, R.color.text_hint)
                    setHintTextColor(android.content.res.ColorStateList.valueOf(hintColor))
                    boxStrokeColor = textColor
                }

                val pinEdit = TextInputEditText(context).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                    filters = arrayOf(android.text.InputFilter.LengthFilter(4))
                    
                    val textColor = androidx.core.content.ContextCompat.getColor(context, R.color.text_primary)
                    setTextColor(textColor)
                }

                // Confirm PIN
                val confirmLayout = TextInputLayout(context).apply {
                    hint = "Confirm PIN"
                    boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                    
                    val textColor = androidx.core.content.ContextCompat.getColor(context, R.color.text_primary)
                    val hintColor = androidx.core.content.ContextCompat.getColor(context, R.color.text_hint)
                    setHintTextColor(android.content.res.ColorStateList.valueOf(hintColor))
                    boxStrokeColor = textColor
                }

                val confirmEdit = TextInputEditText(context).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                    filters = arrayOf(android.text.InputFilter.LengthFilter(4))
                    
                    val textColor = androidx.core.content.ContextCompat.getColor(context, R.color.text_primary)
                    setTextColor(textColor)
                }

                pinLayout.addView(pinEdit)
                confirmLayout.addView(confirmEdit)
                dialogView.addView(pinLayout)
                dialogView.addView(confirmLayout)

                AlertDialog.Builder(context)
                    .setTitle("Set up PIN")
                    .setMessage("Set up a 4-digit PIN to protect this note")
                    .setView(dialogView)
                    .setPositiveButton("Set PIN") { _, _ ->
                        val pin = pinEdit.text?.toString() ?: ""
                        val confirm = confirmEdit.text?.toString() ?: ""
                        
                        if (pin.length != 4 || !pin.all { it.isDigit() }) {
                            onError("PIN must be exactly 4 digits")
                            return@setPositiveButton
                        }
                        
                        if (pin != confirm) {
                            onError("PINs do not match")
                            return@setPositiveButton
                        }
                        
                        if (setupNotePin(pin)) {
                            Log.d(TAG, "PIN setup succeeded")
                            onSuccess()
                        } else {
                            Log.w(TAG, "PIN setup failed")
                            onError("Failed to set up PIN")
                        }
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        Log.d(TAG, "PIN setup cancelled")
                        onCancel()
                    }
                    .setCancelable(false)
                    .show()

            } catch (e: Exception) {
                Log.e(TAG, "Exception in showPinSetupDialog", e)
                onError("Failed to show PIN setup dialog")
            }
        }
    }

    // Show PIN input dialog for note access
    fun showPinDialog(
        title: String = "Enter PIN",
        message: String = "Enter your PIN to access this note",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        Log.d(TAG, "showPinDialog called")

        mainHandler.post {
            try {
                val dialogView = android.widget.LinearLayout(context).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(48, 24, 48, 24)
                }

                val pinLayout = TextInputLayout(context).apply {
                    hint = "PIN"
                    boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
                    
                    // Fix hint text color for better contrast in dark mode
                    val textColor = androidx.core.content.ContextCompat.getColor(context, R.color.text_primary)
                    val hintColor = androidx.core.content.ContextCompat.getColor(context, R.color.text_hint)
                    setHintTextColor(android.content.res.ColorStateList.valueOf(hintColor))
                    boxStrokeColor = textColor
                }

                val pinEdit = TextInputEditText(context).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                    filters = arrayOf(android.text.InputFilter.LengthFilter(4))
                    
                    // Set text color for better contrast in dark mode
                    val textColor = androidx.core.content.ContextCompat.getColor(context, R.color.text_primary)
                    setTextColor(textColor)
                }

                pinLayout.addView(pinEdit)
                dialogView.addView(pinLayout)

                AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setView(dialogView)
                    .setPositiveButton("Unlock") { _, _ ->
                        val pin = pinEdit.text?.toString() ?: ""
                        if (verifyNotePin(pin)) {
                            Log.d(TAG, "PIN verification succeeded")
                            onSuccess()
                        } else {
                            Log.w(TAG, "PIN verification failed")
                            onError("Incorrect PIN")
                        }
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        Log.d(TAG, "PIN dialog cancelled")
                        onCancel()
                    }
                    .setCancelable(false)
                    .show()

            } catch (e: Exception) {
                Log.e(TAG, "Exception in showPinDialog", e)
                onError("Failed to show PIN dialog")
            }
        }
    }

    // Timeout options for spinner
    fun getTimeoutOptions(): List<Pair<String, Long>> {
        return listOf(
            "Immediately" to 0L,
            "1 minute" to 60000L,
            "5 minutes" to 300000L,
            "15 minutes" to 900000L,
            "1 hour" to 3600000L
        )
    }

    fun getTimeoutDisplayText(timeout: Long): String {
        return getTimeoutOptions().find { it.second == timeout }?.first ?: "5 minutes"
    }

    // Session management helpers
    fun clearSession() {
        prefs.edit().clear().apply()
    }

    // Password validation
    fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }

    // Biometric availability check with detailed status
    fun getBiometricStatus(): String {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "Available"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No biometric hardware"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Hardware unavailable"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometrics enrolled"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Security update required"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "Unsupported"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "Unknown status"
            else -> "Not available"
        }
    }

    // Additional helper methods for completeness
    fun completeSetup(): Boolean {
        prefs.edit().putBoolean(PREF_SETUP_COMPLETE, true).apply()
        return true
    }

    fun initialize() {
        // Initialization logic if needed
    }

    fun createMasterPasswordHash(password: String): String {
        return password // Simple implementation for now
    }

    fun validatePasswordStrength(password: String): Pair<Boolean, String> {
        return when {
            password.length < 8 -> false to "Password must be at least 8 characters"
            password.length >= 8 -> true to "Password is valid"
            else -> false to "Invalid password"
        }
    }

    fun requiresAuthentication(): Boolean = isAppSetUp()
    fun isSessionValid(): Boolean = isAppSetUp()
    fun lockApp() {}
    fun unlockApp() {}
    fun resetSecurity() = clearSession()
    fun getMasterPassword(): String? = prefs.getString(PREF_MASTER_PASSWORD, null)
    fun setMasterPassword(password: String) {
        prefs.edit().putString(PREF_MASTER_PASSWORD, password).apply()
    }
    fun authenticateWithPassword(password: String): Boolean = verifyMasterPassword(password)
    fun authenticateWithBiometric(onResult: (Boolean, String?) -> Unit) {
        onResult(false, "Use main authenticateUser method")
    }
    fun isSetupComplete(): Boolean = prefs.getBoolean(PREF_SETUP_COMPLETE, false)
    fun markSetupComplete() {
        prefs.edit().putBoolean(PREF_SETUP_COMPLETE, true).apply()
    }
    fun canUseBiometric(): Boolean = isBiometricAvailable() && isBiometricEnabled()
    fun createSession() {}
}