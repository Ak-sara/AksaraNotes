package com.aksara.aksaranotes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.aksara.aksaranotes.databinding.ActivityMainBinding
import com.aksara.aksaranotes.ui.auth.SetupActivity
import com.aksara.aksaranotes.ui.calendar.CalendarFragment
import com.aksara.aksaranotes.ui.notes.NotesFragment
import com.aksara.aksaranotes.ui.database.DatabaseFragment
import com.aksara.aksaranotes.utils.BiometricHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        biometricHelper = BiometricHelper(this)

        // Check if app is set up
        if (!isAppSetUp()) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        // Hide UI initially until authenticated
        binding.bottomNavigation.visibility = View.GONE

        // Authenticate user
        authenticateUser()
    }

    private fun isAppSetUp(): Boolean {
        val prefs = getSharedPreferences("app_setup", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_setup", false)
    }

    private fun authenticateUser() {
        biometricHelper.authenticateWithBiometric(
            onSuccess = {
                showToast("Authentication successful!")
                enableUI()
            },
            onError = { error ->
                showToast("Authentication failed: $error")
                // For now, let them continue (remove this in production)
                enableUI()
            },
            onUserCancel = {
                showToast("Authentication cancelled")
                // For now, let them continue (remove this in production)
                enableUI()
            }
        )
    }

    private fun enableUI() {
        binding.bottomNavigation.visibility = View.VISIBLE
        setupBottomNavigation()

        // Load default fragment
        loadFragment(NotesFragment())
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_notes -> {
                    loadFragment(NotesFragment())
                    true
                }
                R.id.nav_calendar -> {
                    loadFragment(CalendarFragment())
                    true
                }
                R.id.nav_database -> {
                    loadFragment(DatabaseFragment())
                    true
                }
                else -> false
            }
        }

        // Set default selected item
        binding.bottomNavigation.selectedItemId = R.id.nav_notes
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}