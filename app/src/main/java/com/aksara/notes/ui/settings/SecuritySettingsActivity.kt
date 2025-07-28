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
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.aksara.notes.R
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

        // Load PIN settings
        updatePinStatus()

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

        // PIN Protection
        binding.cardPinSetup.setOnClickListener {
            showPinManagementDialog()
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

    private fun updatePinStatus() {
        val isSet = biometricHelper.isPinEnabled()
        
        binding.tvPinStatus.text = if (isSet) {
            "✅ PIN is set up - Tap to change or remove PIN"
        } else {
            "⚪ Tap to set up a 4-digit PIN for protecting sensitive notes"
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
                strengthIndicator.setTextColor(getPasswordStrengthColor(strength.second))
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

    private fun checkPasswordStrength(password: String): Triple<String, PasswordStrength, Boolean> {
        when {
            password.length < 8 -> return Triple("Password too short (minimum 8 characters)", PasswordStrength.WEAK, false)
            password.length < 12 -> {
                val hasUpper = password.any { it.isUpperCase() }
                val hasLower = password.any { it.isLowerCase() }
                val hasDigit = password.any { it.isDigit() }
                val hasSpecial = password.any { !it.isLetterOrDigit() }

                val score = listOf(hasUpper, hasLower, hasDigit, hasSpecial).count { it }
                return when {
                    score < 2 -> Triple("Weak password", PasswordStrength.WEAK, false)
                    score < 3 -> Triple("Fair password", PasswordStrength.FAIR, true)
                    else -> Triple("Good password", PasswordStrength.GOOD, true)
                }
            }
            else -> {
                val hasUpper = password.any { it.isUpperCase() }
                val hasLower = password.any { it.isLowerCase() }
                val hasDigit = password.any { it.isDigit() }
                val hasSpecial = password.any { !it.isLetterOrDigit() }

                val score = listOf(hasUpper, hasLower, hasDigit, hasSpecial).count { it }
                return when {
                    score < 3 -> Triple("Good password", PasswordStrength.GOOD, true)
                    else -> Triple("Strong password", PasswordStrength.STRONG, true)
                }
            }
        }
    }

    private fun showPinManagementDialog() {
        val isPinSet = biometricHelper.isPinEnabled()
        
        if (isPinSet) {
            // Show options: Change PIN or Remove PIN
            AlertDialog.Builder(this)
                .setTitle("PIN Management")
                .setMessage("Choose an option for your PIN:")
                .setPositiveButton("Change PIN") { _, _ ->
                    showChangePinDialog()
                }
                .setNeutralButton("Remove PIN") { _, _ ->
                    showRemovePinDialog()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // Direct setup - no master password required
            showPinInputDialog(
                title = "Set Up PIN",
                message = "Enter a 4-digit PIN to protect sensitive notes",
                isSetup = true
            )
        }
    }

    private fun showChangePinDialog() {
        showPinInputDialog(
            title = "Change PIN",
            message = "Enter your current PIN, then set a new one",
            isSetup = false
        )
    }

    private fun showRemovePinDialog() {
        AlertDialog.Builder(this)
            .setTitle("Remove PIN")
            .setMessage("Are you sure you want to remove PIN protection? This will not affect notes that are already marked as protected.")
            .setPositiveButton("Remove") { _, _ ->
                biometricHelper.clearNotePin()
                updatePinStatus()
                showToast("📌 PIN removed successfully")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPinInputDialog(title: String, message: String, isSetup: Boolean) {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        // Current PIN (only for change)
        val currentPinLayout: TextInputLayout?
        val currentPinEdit: TextInputEditText?
        
        if (!isSetup) {
            currentPinLayout = TextInputLayout(this).apply {
                hint = "Current PIN"
                boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            }
            currentPinEdit = TextInputEditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                filters = arrayOf(android.text.InputFilter.LengthFilter(4))
            }
            currentPinLayout.addView(currentPinEdit)
            dialogView.addView(currentPinLayout)
        } else {
            currentPinLayout = null
            currentPinEdit = null
        }

        // New PIN
        val newPinLayout = TextInputLayout(this).apply {
            hint = if (isSetup) "Enter PIN" else "New PIN"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val newPinEdit = TextInputEditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
        }
        newPinLayout.addView(newPinEdit)

        // Confirm PIN
        val confirmPinLayout = TextInputLayout(this).apply {
            hint = "Confirm PIN"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val confirmPinEdit = TextInputEditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
        }
        confirmPinLayout.addView(confirmPinEdit)

        dialogView.addView(newPinLayout)
        dialogView.addView(confirmPinLayout)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setView(dialogView)
            .setPositiveButton(if (isSetup) "Set PIN" else "Change PIN") { _, _ ->
                val currentPin = currentPinEdit?.text?.toString() ?: ""
                val newPin = newPinEdit.text?.toString() ?: ""
                val confirmPin = confirmPinEdit.text?.toString() ?: ""

                if (isSetup) {
                    setupPin(newPin, confirmPin)
                } else {
                    changePin(currentPin, newPin, confirmPin)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupPin(pin: String, confirmPin: String) {
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            showToast("❌ PIN must be exactly 4 digits")
            return
        }

        if (pin != confirmPin) {
            showToast("❌ PINs do not match")
            return
        }

        val success = biometricHelper.setupNotePin(pin)
        if (success) {
            updatePinStatus()
            showToast("✅ PIN set up successfully!")
        } else {
            showToast("❌ Failed to set up PIN")
        }
    }

    private fun changePin(currentPin: String, newPin: String, confirmPin: String) {
        if (currentPin.isEmpty()) {
            showToast("❌ Enter your current PIN")
            return
        }

        if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
            showToast("❌ New PIN must be exactly 4 digits")
            return
        }

        if (newPin != confirmPin) {
            showToast("❌ New PINs do not match")
            return
        }

        val success = biometricHelper.changeNotePin(currentPin, newPin)
        if (success) {
            showToast("✅ PIN changed successfully!")
        } else {
            showToast("❌ Current PIN is incorrect")
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
            appendLine("• Note PIN: 4-digit PIN for individual note protection")
            appendLine("• Auto-lock: Automatically secures app when inactive")
            appendLine()
            appendLine("📌 PIN Protection Features:")
            appendLine("• Content masking in note list")
            appendLine("• PIN required to view protected notes")
            appendLine("• Separate from master password")
            appendLine("• Independent security layer")
            appendLine()
            appendLine("⚠️ Important:")
            appendLine("• Master password cannot be recovered if forgotten")
            appendLine("• PIN can be changed in Security Settings")
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
            .setMessage("This will:\n\n• Reset master password\n• Disable biometric authentication\n• Clear PIN protection\n• Clear all security preferences\n• ERASE ALL DATA\n\nThis action cannot be undone!")
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
    
    private enum class PasswordStrength {
        WEAK, FAIR, GOOD, STRONG
    }
    
    private fun getPasswordStrengthColor(strength: PasswordStrength): Int {
        return when (strength) {
            PasswordStrength.WEAK -> MaterialColors.getColor(this, R.attr.passwordWeakColor, ContextCompat.getColor(this, R.color.password_weak))
            PasswordStrength.FAIR -> MaterialColors.getColor(this, R.attr.passwordFairColor, ContextCompat.getColor(this, R.color.password_fair))
            PasswordStrength.GOOD -> MaterialColors.getColor(this, R.attr.passwordGoodColor, ContextCompat.getColor(this, R.color.password_good))
            PasswordStrength.STRONG -> MaterialColors.getColor(this, R.attr.passwordStrongColor, ContextCompat.getColor(this, R.color.password_strong))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}