package com.aksara.aksaranotes.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aksara.aksaranotes.MainActivity
import com.aksara.aksaranotes.databinding.ActivitySetupBinding

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.btnSetPassword.setOnClickListener {
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (validatePassword(password, confirmPassword)) {
                setupApp(password)
            }
        }
    }

    private fun validatePassword(password: String, confirmPassword: String): Boolean {
        // Clear previous errors
        binding.tvError.visibility = View.GONE

        when {
            password.length < 8 -> {
                showError("Password must be at least 8 characters")
                return false
            }
            password != confirmPassword -> {
                showError("Passwords do not match")
                return false
            }
            password.isBlank() -> {
                showError("Password cannot be empty")
                return false
            }
        }

        return true
    }

    private fun setupApp(password: String) {
        try {
            // Save setup completion status
            val prefs = getSharedPreferences("app_setup", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("is_setup", true)
                .putString("setup_date", System.currentTimeMillis().toString())
                .apply()

            // TODO: In next step, we'll add proper encryption here
            // For now, just save a basic hash
            savePasswordHash(password)

            showToast("Setup complete! Welcome to Aksara Notes")

            // Navigate to main app
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

        } catch (e: Exception) {
            showError("Setup failed: ${e.message}")
        }
    }

    private fun savePasswordHash(password: String) {
        // Simple hash for now - we'll improve this with proper encryption later
        val hash = password.hashCode().toString()
        val prefs = getSharedPreferences("app_security", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("password_hash", hash)
            .apply()
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}