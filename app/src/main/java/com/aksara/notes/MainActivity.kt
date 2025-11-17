package com.aksara.notes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aksara.notes.databinding.ActivityMainBinding
import com.aksara.notes.ui.auth.SetupActivity
import com.aksara.notes.ui.calendar.CalendarFragment
import com.aksara.notes.ui.notes.NotesFragment
import com.aksara.notes.ui.database.DatabaseFragment
import com.aksara.notes.ui.settings.SecuritySettingsActivity
import com.aksara.notes.ui.settings.AboutActivity
import com.aksara.notes.ui.backup.BackupRestoreActivity
import com.aksara.notes.utils.BiometricHelper
import com.aksara.notes.utils.SessionManager
import com.aksara.notes.data.database.RealmDatabase
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var biometricHelper: BiometricHelper
    private lateinit var sessionManager: SessionManager
    private var isAuthenticating = false
    private var authenticationAttempts = 0
    private val maxAuthenticationAttempts = 3

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            biometricHelper = BiometricHelper(this)
            sessionManager = SessionManager.getInstance()
            sessionManager.initialize(this)
            Log.d(TAG, "BiometricHelper and SessionManager initialized")

            // Check if app is set up, if not redirect to setup
            if (!biometricHelper.isAppSetUp()) {
                Log.d(TAG, "App not set up, redirecting to setup")
                startActivity(Intent(this, SetupActivity::class.java))
                finish()
                return
            }

            setupToolbar()
            setupNavigationDrawer()
            setupBottomNavigation()
            setupBackPressHandler()

            // Check if authentication is required
            Log.d(TAG, "Checking if authentication required...")
            val authRequired = try {
                sessionManager.isAuthenticationRequired(biometricHelper)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking authentication requirement", e)
                true // Default to requiring authentication if we can't check
            }

            Log.d(TAG, "Authentication required: $authRequired")

            if (authRequired) {
                startAuthenticationFlow()
            } else {
                Log.d(TAG, "Authentication not required, initializing database and showing UI")
                initializeEncryptedDatabase()
                showUI()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            showToast("Error initializing app: ${e.message}")
            // Show authentication UI anyway for security
            showAuthenticationUI()

            // Try to recover by showing password dialog
            lifecycleScope.launch {
                delay(1000)
                showPasswordAuthenticationDirect()
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Aksara Notes"
    }

    private fun setupNavigationDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_notes -> {
                loadFragment(NotesFragment())
                binding.bottomNavigation.selectedItemId = R.id.nav_notes
                supportActionBar?.title = "Notes"
            }
            R.id.nav_calendar -> {
                loadFragment(CalendarFragment())
                binding.bottomNavigation.selectedItemId = R.id.nav_calendar
                supportActionBar?.title = "Calendar"
            }
            R.id.nav_database -> {
                loadFragment(DatabaseFragment())
                binding.bottomNavigation.selectedItemId = R.id.nav_database
                supportActionBar?.title = "Database"
            }
            R.id.nav_security_settings -> {
                startActivity(Intent(this, SecuritySettingsActivity::class.java))
            }
            R.id.nav_backup_restore -> {
                startActivity(Intent(this, BackupRestoreActivity::class.java))
            }
            R.id.nav_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
            }
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")

        try {
            // Update activity timestamp to show app is actively being used
            sessionManager.updateActivity()
            sessionManager.onAppForegrounded()

            // Check if we need to authenticate when resuming
            if (!isAuthenticating && sessionManager.isAuthenticationRequired(biometricHelper)) {
                Log.d(TAG, "Authentication required on resume, starting authentication flow")
                startAuthenticationFlow()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume", e)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop called")

        try {
            // Only call onAppBackgrounded when we're actually backgrounding the app
            if (isTaskRoot && !isChangingConfigurations) {
                sessionManager.onAppBackgrounded()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStop", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")

        try {
            // Only clear session if this is a real app exit
            if (isFinishing && !isChangingConfigurations && isTaskRoot) {
                sessionManager.onAppDestroyed()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }

    private fun startAuthenticationFlow() {
        Log.d(TAG, "startAuthenticationFlow called")
        if (isAuthenticating) {
            Log.d(TAG, "Already authenticating, skipping")
            return
        }

        isAuthenticating = true
        authenticationAttempts = 0
        showAuthenticationUI()

        // Add a timeout to prevent getting stuck indefinitely
        lifecycleScope.launch {
            delay(30000) // 30 second timeout
            if (isAuthenticating) {
                Log.w(TAG, "Authentication timeout reached")
                showToast("Authentication timeout. Please try again.")
                showPasswordAuthenticationDirect()
            }
        }
        attemptAuthentication()
    }

    private fun attemptAuthentication() {
        Log.d(TAG, "attemptAuthentication called, attempt: ${authenticationAttempts + 1}")
        authenticationAttempts++

        if (authenticationAttempts > maxAuthenticationAttempts) {
            handleMaxAttemptsReached()
            return
        }

        try {
            biometricHelper.authenticateUser(
                onSuccess = {
                    Log.d(TAG, "✅ Authentication SUCCESS callback triggered!")
                    runOnUiThread {
                        handleAuthenticationSuccess()
                    }
                },
                onError = { error ->
                    Log.e(TAG, "❌ Authentication ERROR callback: $error")
                    runOnUiThread {
                        handleAuthenticationError(error)
                    }
                },
                onPasswordFallback = {
                    Log.d(TAG, "🔑 Password FALLBACK callback triggered")
                    runOnUiThread {
                        showPasswordAuthentication()
                    }
                }
            )

            Log.d(TAG, "biometricHelper.authenticateUser() call completed")

        } catch (e: Exception) {
            Log.e(TAG, "💥 Exception during authentication attempt", e)
            // If BiometricHelper completely fails, go directly to password
            showPasswordAuthenticationDirect()
        }
    }

    private fun showPasswordAuthentication() {
        Log.d(TAG, "showPasswordAuthentication called")
        showPasswordAuthenticationDirect()
    }

    private fun showPasswordAuthenticationDirect() {
        Log.d(TAG, "showPasswordAuthenticationDirect called")
        try {
            biometricHelper.showPasswordDialog(
                onSuccess = {
                    Log.d(TAG, "✅ Password authentication SUCCESS")
                    runOnUiThread {
                        handleAuthenticationSuccess()
                    }
                },
                onError = { error ->
                    Log.e(TAG, "❌ Password authentication ERROR: $error")
                    runOnUiThread {
                        handleAuthenticationError(error)
                    }
                },
                onCancel = {
                    Log.d(TAG, "🚫 Password authentication CANCELLED")
                    runOnUiThread {
                        handleAuthenticationCancel()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "💥 Exception during password authentication", e)
            showToast("Authentication system error. Please restart the app.")
            handleMaxAttemptsReached()
        }
    }

    private fun handleAuthenticationSuccess() {
        Log.d(TAG, "handleAuthenticationSuccess called")
        isAuthenticating = false
        authenticationAttempts = 0

        try {
            // Mark authentication successful in session manager
            sessionManager.markAuthenticated()
            Log.d(TAG, "Session marked as authenticated")
            
            // Initialize Realm database with encryption after successful authentication
            initializeEncryptedDatabase()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error marking authentication", e)
        }

        showToast("Welcome back!")
        showUI()
    }
    
    private fun initializeEncryptedDatabase() {
        try {
            Log.d(TAG, "Ensuring Realm database is initialized")
            // Just check if Realm is initialized without closing it
            if (RealmDatabase.isInitialized) {
                Log.d(TAG, "Realm database is already initialized")
            } else {
                Log.d(TAG, "Realm not initialized, attempting initialization")
                RealmDatabase.initialize(this)
                Log.d(TAG, "Realm database initialized successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Realm database", e)
            showToast("Database initialization failed: ${e.message}")
        }
    }

    private fun handleAuthenticationError(error: String) {
        Log.e(TAG, "handleAuthenticationError: $error")
        showToast("Authentication failed: $error")

        if (authenticationAttempts < maxAuthenticationAttempts) {
            // Wait a bit before retrying
            lifecycleScope.launch {
                delay(1500)
                Log.d(TAG, "Retrying authentication...")
                attemptAuthentication()
            }
        } else {
            Log.w(TAG, "Max authentication attempts reached")
            handleMaxAttemptsReached()
        }
    }

    private fun handleAuthenticationCancel() {
        showToast("Authentication cancelled")

        if (authenticationAttempts < maxAuthenticationAttempts) {
            // Give user another chance after a delay
            lifecycleScope.launch {
                delay(2000)
                attemptAuthentication()
            }
        } else {
            handleMaxAttemptsReached()
        }
    }

    private fun handleMaxAttemptsReached() {
        showToast("Too many failed attempts. Please restart the app.")

        // Close the app after too many failed attempts
        lifecycleScope.launch {
            delay(2000)
            finishAndRemoveTask()
        }
    }

    private fun showAuthenticationUI() {
        binding.layoutAuthenticating.visibility = View.VISIBLE
        binding.bottomNavigation.visibility = View.GONE
        binding.fragmentContainer.visibility = View.GONE
        binding.toolbar.visibility = View.GONE
    }

    private fun showUI() {
        binding.layoutAuthenticating.visibility = View.GONE
        binding.bottomNavigation.visibility = View.VISIBLE
        binding.fragmentContainer.visibility = View.VISIBLE
        binding.toolbar.visibility = View.VISIBLE

        // Load default fragment if none is loaded
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            loadFragment(NotesFragment())
            supportActionBar?.title = "Notes"
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_notes -> {
                    loadFragment(NotesFragment())
                    supportActionBar?.title = "Notes"
                    true
                }
                R.id.nav_calendar -> {
                    loadFragment(CalendarFragment())
                    supportActionBar?.title = "Calendar"
                    true
                }
                R.id.nav_database -> {
                    loadFragment(DatabaseFragment())
                    supportActionBar?.title = "Database"
                    true
                }
                else -> false
            }
        }

        // Set default selected item
        binding.bottomNavigation.selectedItemId = R.id.nav_notes
    }

    private fun loadFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading fragment", e)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Handle back press - close drawer first, then allow normal back press
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this) {
            when {
                binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                }
                isAuthenticating -> {
                    showToast("Please complete authentication first")
                }
                else -> {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    // Public method for PIN-protected notes to request PIN authentication
    fun requestPinAuthentication(onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            // Check if PIN is enabled for note protection
            if (biometricHelper.isPinEnabled()) {
                // Use PIN for note authentication
                biometricHelper.showPinDialog(
                    title = "Enter PIN",
                    message = "Enter your PIN to access this protected note",
                    onSuccess = onSuccess,
                    onError = onError,
                    onCancel = {
                        onError("PIN entry cancelled")
                    }
                )
            } else {
                // Fallback: No PIN set up, request PIN setup
                showToast("❌ PIN not set up. Please set up a PIN in Security Settings first.")
                onError("PIN not configured")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in requestPinAuthentication", e)
            onError("Authentication error: ${e.message}")
        }
    }

    // Check if a note requires PIN and handle authentication
    fun checkNoteAccess(note: com.aksara.notes.data.database.entities.Note, onAccessGranted: () -> Unit) {
        if (note.requiresPin) {
            requestPinAuthentication(
                onSuccess = {
                    showToast("🔓 Note unlocked")
                    onAccessGranted()
                },
                onError = { error ->
                    showToast("Access denied: $error")
                }
            )
        } else {
            onAccessGranted()
        }
    }
}