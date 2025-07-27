package com.aksara.notes.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import com.google.android.material.color.MaterialColors
import com.aksara.notes.MainActivity
import com.aksara.notes.R
import com.aksara.notes.databinding.ActivitySetupBinding
import com.aksara.notes.utils.BiometricHelper

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        biometricHelper = BiometricHelper(this)

        setupUI()
        setupValidation()
        checkBiometricAvailability()
    }

    private fun setupUI() {
        binding.btnCreatePassword.setOnClickListener {
            createMasterPassword()
        }

        binding.switchEnableBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isBiometricAvailable()) {
                binding.switchEnableBiometric.isChecked = false
                Toast.makeText(this, "Biometric authentication is not available on this device", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupValidation() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePasswords()
            }
        }

        binding.etMasterPassword.addTextChangedListener(textWatcher)
        binding.etConfirmPassword.addTextChangedListener(textWatcher)
    }

    private fun validatePasswords() {
        val password = binding.etMasterPassword.text?.toString() ?: ""
        val confirmPassword = binding.etConfirmPassword.text?.toString() ?: ""

        // Check password strength
        val strengthResult = checkPasswordStrength(password)
        binding.tvPasswordStrength.text = strengthResult.first
        binding.tvPasswordStrength.setTextColor(getPasswordStrengthColor(strengthResult.second))

        // Check if passwords match
        val passwordsMatch = password.isNotEmpty() && password == confirmPassword
        binding.tilConfirmPassword.error = if (confirmPassword.isNotEmpty() && !passwordsMatch) {
            "Passwords do not match"
        } else null

        // Enable button if all validations pass
        binding.btnCreatePassword.isEnabled = strengthResult.third && passwordsMatch && password.length >= 8
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

    private fun checkBiometricAvailability() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                binding.switchEnableBiometric.isEnabled = true
                binding.tvBiometricStatus.text = "Biometric authentication is available"
                binding.switchEnableBiometric.isChecked = true // Default to enabled if available
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                binding.switchEnableBiometric.isEnabled = false
                binding.tvBiometricStatus.text = "No biometric hardware available"
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                binding.switchEnableBiometric.isEnabled = false
                binding.tvBiometricStatus.text = "Biometric hardware is currently unavailable"
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                binding.switchEnableBiometric.isEnabled = false
                binding.tvBiometricStatus.text = "No biometric credentials enrolled. Please set up fingerprint or face unlock in device settings."
            }
            else -> {
                binding.switchEnableBiometric.isEnabled = false
                binding.tvBiometricStatus.text = "Biometric authentication is not available"
            }
        }
    }

    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun createMasterPassword() {
        val password = binding.etMasterPassword.text?.toString() ?: ""
        val confirmPassword = binding.etConfirmPassword.text?.toString() ?: ""

        // Final validation
        if (password.length < 8) {
            Toast.makeText(this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button to prevent double submission
        binding.btnCreatePassword.isEnabled = false

        // Set up master password
        val success = biometricHelper.setupMasterPassword(password)
        if (!success) {
            Toast.makeText(this, "Failed to create master password", Toast.LENGTH_SHORT).show()
            binding.btnCreatePassword.isEnabled = true
            return
        }

        // Set biometric preference
        if (binding.switchEnableBiometric.isChecked && isBiometricAvailable()) {
            biometricHelper.setBiometricEnabled(true)
        }

        // Mark app as set up
        val prefs = getSharedPreferences("app_setup", MODE_PRIVATE)
        prefs.edit().putBoolean("is_setup", true).apply()

        Toast.makeText(this, "Master password created successfully!", Toast.LENGTH_SHORT).show()

        // Navigate to main activity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}