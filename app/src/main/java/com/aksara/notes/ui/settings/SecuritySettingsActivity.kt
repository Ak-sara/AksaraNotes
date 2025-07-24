package com.aksara.notes.ui.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import com.aksara.notes.databinding.ActivitySecuritySettingsBinding
import com.aksara.notes.utils.BiometricHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SecuritySettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecuritySettingsBinding
    private lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecuritySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        biometricHelper = BiometricHelper(this)

        setupToolbar()
        setupUI()
        loadCurrentSettings()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Security Settings"
    }

    private fun setupUI() {
        // Setup auto-lock timeout spinner
        val timeoutOptions = biometricHelper.getTimeoutOptions()
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            timeoutOptions.map { it.first }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAutoLockTimeout.adapter = adapter
    }

    private fun loadCurrentSettings() {
        // Load biometric settings
        updateBiometricStatus()

        // Load auto-lock settings
        binding.switchAutoLock.isChecked = biometricHelper.isAutoLockEnabled()
        updateAutoLockUI()

        // Load current timeout setting
        val currentTimeout = biometricHelper.getAutoLockTimeout()
        val timeoutOptions = biometricHelper.getTimeoutOptions()
        val currentIndex = timeoutOptions.indexOfFirst { it.second == currentTimeout }
        if (currentIndex >= 0) {
            binding.spinnerAutoLockTimeout.setSelection(currentIndex)
        }
    }

    private fun setupClickListeners() {
        // Master Password
        binding.cardChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Biometric Authentication
        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isBiometricAvailable()) {
                binding.switchBiometric.isChecked = false
                showBiometricUnavailableDialog()
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                // Enabling biometric - verify current authentication first
                verifyCurrentAuthentication { success ->
                    if (success) {
                        biometricHelper.setBiometricEnabled(true)
                        updateBiometricStatus()
                        showToast("🔐 Biometric authentication enabled")
                    } else {
                        binding.switchBiometric.isChecked = false
                    }
                }
            } else {
                // Disabling biometric
                biometricHelper.setBiometricEnabled(false)
                updateBiometricStatus()
                showToast("🔓 Biometric authentication disabled")
            }
        }

        // Auto-lock
        binding.switchAutoLock.setOnCheckedChangeListener { _, isChecked ->
            biometricHelper.setAutoLockEnabled(isChecked)
            updateAutoLockUI()

            val message = if (isChecked) "🔒 Auto-lock enabled" else "🔓 Auto-lock disabled"
            showToast(message)
        }

        // Auto-lock timeout
        binding.spinnerAutoLockTimeout.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val timeoutOptions = biometricHelper.getTimeoutOptions()
                val selectedTimeout = timeoutOptions[position].second
                biometricHelper.setAutoLockTimeout(selectedTimeout)

                val timeoutText = biometricHelper.getTimeoutDisplayText(selectedTimeout)
                showToast("⏱️ Auto-lock timeout set to $timeoutText")
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Security Info
        binding.cardSecurityInfo.setOnClickListener {
            showSecurityInfoDialog()
        }

        // Reset Security
        binding.btnResetSecurity.setOnClickListener {
            showResetSecurityDialog()
        }
    }

    private fun updateBiometricStatus() {
        val isAvailable = isBiometricAvailable()
        val isEnabled = biometricHelper.isBiometricEnabled()

        binding.switchBiometric.isEnabled = isAvailable
        binding.switchBiometric.isChecked = isEnabled && isAvailable

        binding.tvBiometricStatus.text = when {
            !isAvailable -> "Biometric authentication is not available on this device"
            isEnabled -> "✅ Enabled - Use fingerprint or face unlock"
            else -> "⚪ Disabled - Use master password only"
        }
    }

    private fun updateAutoLockUI() {
        val isEnabled = binding.switchAutoLock.isChecked
        binding.layoutAutoLockTimeout.visibility = if (isEnabled) View.VISIBLE else View.GONE

        binding.tvAutoLockDescription.text = if (isEnabled) {
            "App will automatically lock after the specified time of inactivity"
        } else {
            "App will stay unlocked until manually closed"
        }
    }

    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showChangePasswordDialog() {
        // Create dialog view programmatically
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        // Current password
        val currentPasswordLayout = TextInputLayout(this).apply {
            hint = "Current Master Password"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val currentPasswordEdit = TextInputEditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        currentPasswordLayout.addView(currentPasswordEdit)

        // New password
        val newPasswordLayout = TextInputLayout(this).apply {
            hint = "New Master Password"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val newPasswordEdit = TextInputEditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        newPasswordLayout.addView(newPasswordEdit)

        // Confirm new password
        val confirmPasswordLayout = TextInputLayout(this).apply {
            hint = "Confirm New Password"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val confirmPasswordEdit = TextInputEditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        confirmPasswordLayout.addView(confirmPasswordEdit)

        // Password strength indicator
        val strengthIndicator = android.widget.TextView(this).apply {
            text = "Enter new password"
            textSize = 12f
            setPadding(0, 16, 0, 0)
        }

        dialogView.addView(currentPasswordLayout)
        dialogView.addView(newPasswordLayout)
        dialogView.addView(confirmPasswordLayout)
        dialogView.addView(strengthIndicator)

        // Add password strength validation
        newPasswordEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                val strength = checkPasswordStrength(password)
                strengthIndicator.text = strength.first
                strengthIndicator.setTextColor(getColor(strength.second))
            }
        })

        AlertDialog.Builder(this)
            .setTitle("Change Master Password")
            .setMessage("Enter your current password and choose a new secure password.")
            .setView(dialogView)
            .setPositiveButton("Change Password") { _, _ ->
                val currentPassword = currentPasswordEdit.text?.toString() ?: ""
                val newPassword = newPasswordEdit.text?.toString() ?: ""
                val confirmPassword = confirmPasswordEdit.text?.toString() ?: ""

                changePassword(currentPassword, newPassword, confirmPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        // Validate current password
        if (!biometricHelper.verifyMasterPassword(currentPassword)) {
            showToast("❌ Current password is incorrect")
            return
        }

        // Validate new password
        if (newPassword.length < 8) {
            showToast("❌ New password must be at least 8 characters long")
            return
        }

        if (newPassword != confirmPassword) {
            showToast("❌ New passwords do not match")
            return
        }

        val strength = checkPasswordStrength(newPassword)
        if (!strength.third) {
            showToast("❌ Password is too weak. Please choose a stronger password.")
            return
        }

        // Change password
        val success = biometricHelper.changeMasterPassword(currentPassword, newPassword)
        if (success) {
            showToast("✅ Master password changed successfully!")
        } else {
            showToast("❌ Failed to change password. Please try again.")
        }
    }

    private fun checkPasswordStrength(password: String): Triple<String, Int, Boolean> {
        when {
            password.length < 8 -> return Triple("Password too short (minimum 8 characters)", android.R.color.holo_red_dark, false)
            password.length < 12 -> {
                val hasUpper = password.any { it.isUpperCase() }
                val hasLower = password.any { it.isLowerCase() }
                val hasDigit = password.any { it.isDigit() }
                val hasSpecial = password.any { !it.isLetterOrDigit() }

                val score = listOf(hasUpper, hasLower, hasDigit, hasSpecial).count { it }
                return when {
                    score < 2 -> Triple("Weak password", android.R.color.holo_red_dark, false)
                    score < 3 -> Triple("Fair password", android.R.color.holo_orange_dark, true)
                    else -> Triple("Good password", android.R.color.holo_green_dark, true)
                }
            }
            else -> {
                val hasUpper = password.any { it.isUpperCase() }
                val hasLower = password.any { it.isLowerCase() }
                val hasDigit = password.any { it.isDigit() }
                val hasSpecial = password.any { !it.isLetterOrDigit() }

                val score = listOf(hasUpper, hasLower, hasDigit, hasSpecial).count { it }
                return when {
                    score < 3 -> Triple("Good password", android.R.color.holo_green_dark, true)
                    else -> Triple("Strong password", android.R.color.holo_green_dark, true)
                }
            }
        }
    }

    private fun verifyCurrentAuthentication(onResult: (Boolean) -> Unit) {
        biometricHelper.authenticateUser(
            onSuccess = { onResult(true) },
            onError = {
                showToast("Authentication failed")
                onResult(false)
            },
            onPasswordFallback = {
                biometricHelper.showPasswordDialog(
                    onSuccess = { onResult(true) },
                    onError = {
                        showToast("Authentication failed")
                        onResult(false)
                    },
                    onCancel = { onResult(false) }
                )
            }
        )
    }

    private fun showBiometricUnavailableDialog() {
        val biometricManager = BiometricManager.from(this)
        val message = when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "This device doesn't have biometric hardware."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "Biometric hardware is currently unavailable."
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "No biometric credentials are enrolled. Please set up fingerprint or face unlock in device settings first."
            else ->
                "Biometric authentication is not available."
        }

        AlertDialog.Builder(this)
            .setTitle("Biometric Unavailable")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSecurityInfoDialog() {
        val message = buildString {
            appendLine("🔐 Security Information")
            appendLine()
            appendLine("• Master Password: Encrypts and protects all your data")
            appendLine("• Biometric Auth: Quick access using fingerprint/face")
            appendLine("• Auto-lock: Automatically secures app when inactive")
            appendLine("• PIN Protection: Additional security for sensitive notes")
            appendLine()
            appendLine("⚠️ Important:")
            appendLine("• Master password cannot be recovered if forgotten")
            appendLine("• Biometric data is stored securely on your device")
            appendLine("• All data is encrypted using industry-standard methods")
        }

        AlertDialog.Builder(this)
            .setTitle("Security Information")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showResetSecurityDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Reset Security Settings")
            .setMessage("This will:\n\n• Reset master password\n• Disable biometric authentication\n• Clear all security preferences\n• ERASE ALL DATA\n\nThis action cannot be undone!")
            .setPositiveButton("Reset Everything") { _, _ ->
                confirmResetSecurity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmResetSecurity() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Final Confirmation")
            .setMessage("Are you absolutely sure you want to reset everything?\n\nALL DATA WILL BE LOST!")
            .setPositiveButton("Yes, Reset Everything") { _, _ ->
                resetSecurity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetSecurity() {
        // Clear biometric helper session
        biometricHelper.clearSession()

        // Clear app setup flag
        val prefs = getSharedPreferences("app_setup", MODE_PRIVATE)
        prefs.edit().clear().apply()

        showToast("🔄 Security reset complete. App will restart.")

        // Restart app to setup activity
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}