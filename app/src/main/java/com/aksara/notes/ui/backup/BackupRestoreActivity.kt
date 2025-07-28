package com.aksara.notes.ui.backup

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.aksara.notes.R
import com.aksara.notes.data.backup.RealmBackupManager
import com.aksara.notes.databinding.ActivityBackupRestoreBinding
import com.aksara.notes.ui.database.DatabaseViewModel
import com.aksara.notes.utils.BiometricHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity for creating encrypted backups and restoring data from backups
 */
class BackupRestoreActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "BackupRestoreActivity"
        private const val BACKUP_MIME_TYPE = "application/octet-stream"
        private const val BACKUP_FILE_EXTENSION = ".realm_backup"
    }
    
    private lateinit var binding: ActivityBackupRestoreBinding
    private lateinit var databaseViewModel: DatabaseViewModel
    private lateinit var realmBackupManager: RealmBackupManager
    private lateinit var biometricHelper: BiometricHelper
    
    // File operation launchers
    private val createBackupLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                performBackupToFile(uri)
            }
        }
    }
    
    private val selectRestoreLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                performRestoreFromFile(uri)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityBackupRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize components
        databaseViewModel = ViewModelProvider(this)[DatabaseViewModel::class.java]
        realmBackupManager = RealmBackupManager(this)
        biometricHelper = BiometricHelper(this)
        
        setupToolbar()
        setupUI()
        loadDataCounts()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupUI() {
        binding.btnCreateBackup.setOnClickListener {
            createBackup()
        }
        
        binding.btnRestoreBackup.setOnClickListener {
            selectBackupFile()
        }
    }
    
    private fun loadDataCounts() {
        lifecycleScope.launch {
            try {
                // Observe data counts to update UI
                databaseViewModel.allDatasets.observe(this@BackupRestoreActivity) { datasets ->
                    binding.tvDatasetsCount.text = datasets.size.toString()
                }
                
                databaseViewModel.allForms.observe(this@BackupRestoreActivity) { forms ->
                    binding.tvFormsCount.text = forms.size.toString()
                }
                
                // Get notes count (assuming you have a method for this)
                val notesCount = withContext(Dispatchers.IO) {
                    databaseViewModel.getAllNotesForBackup().size
                }
                binding.tvNotesCount.text = notesCount.toString()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading data counts", e)
                Toast.makeText(this@BackupRestoreActivity, "Error loading data counts", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun createBackup() {
        // Show password confirmation dialog
        showPasswordDialog("Enter Master Password", "Please enter your master password to encrypt the backup:") { password ->
            // Create file picker for backup location
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = BACKUP_MIME_TYPE
                
                // Suggest filename with timestamp
                putExtra(Intent.EXTRA_TITLE, realmBackupManager.generateBackupFilename())
                
                // Set initial directory to Downloads
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, "content://com.android.externalstorage.documents/document/primary%3ADownload")
            }
            
            createBackupLauncher.launch(intent)
        }
    }
    
    private fun performBackupToFile(uri: Uri) {
        setLoading(true, "Creating encrypted backup...")
        
        lifecycleScope.launch {
            try {
                // Create backup using Realm's built-in encryption
                val result = realmBackupManager.createBackup(uri)
                
                setLoading(false)
                
                if (result.success) {
                    // Show success message
                    val sizeInKB = result.backupSize / 1024
                    AlertDialog.Builder(this@BackupRestoreActivity)
                        .setTitle("✅ Backup Created")
                        .setMessage("Your encrypted backup has been created successfully!\n\nSize: ${sizeInKB} KB\n\nThe backup is encrypted with your master password. Keep it safe!")
                        .setPositiveButton("OK", null)
                        .show()
                    
                    Log.d(TAG, "Backup created successfully at: $uri")
                } else {
                    AlertDialog.Builder(this@BackupRestoreActivity)
                        .setTitle("❌ Backup Failed")
                        .setMessage("Failed to create backup: ${result.error}")
                        .setPositiveButton("OK", null)
                        .show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error creating backup", e)
                setLoading(false)
                
                AlertDialog.Builder(this@BackupRestoreActivity)
                    .setTitle("❌ Backup Failed")
                    .setMessage("Failed to create backup: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    private fun selectBackupFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*" // Accept all files since .akb might not be recognized
            
            // Set initial directory to Downloads
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, "content://com.android.externalstorage.documents/document/primary%3ADownload")
        }
        
        selectRestoreLauncher.launch(intent)
    }
    
    private fun performRestoreFromFile(uri: Uri) {
        // Show password dialog
        showPasswordDialog("Enter Master Password", "Please enter your master password to decrypt the backup:") { password ->
            setLoading(true, "Restoring from encrypted backup...")
            
            lifecycleScope.launch {
                try {
                    // Restore using Realm backup manager
                    val result = realmBackupManager.restoreFromBackup(uri, password)
                    
                    setLoading(false)
                    
                    if (result.success) {
                        // Show success message
                        AlertDialog.Builder(this@BackupRestoreActivity)
                            .setTitle("✅ Restore Complete")
                            .setMessage("Backup restored successfully!\n\nAll your data has been restored from the backup. The app will refresh to show the restored data.")
                            .setPositiveButton("OK") { _, _ ->
                                // Refresh data counts and finish
                                loadDataCounts()
                                
                                // Optionally restart the app for full refresh
                                if (result.requiresRestart) {
                                    recreate()
                                }
                            }
                            .show()
                    } else {
                        AlertDialog.Builder(this@BackupRestoreActivity)
                            .setTitle("❌ Restore Failed")
                            .setMessage(result.error ?: "Unknown error occurred during restore")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring backup", e)
                    setLoading(false)
                    
                    val errorMessage = when {
                        e.message?.contains("password") == true || e.message?.contains("decrypt") == true -> 
                            "Incorrect password or corrupted backup file"
                        e.message?.contains("Invalid") == true -> 
                            "Invalid backup file format"
                        else -> 
                            "Failed to restore backup: ${e.message}"
                    }
                    
                    AlertDialog.Builder(this@BackupRestoreActivity)
                        .setTitle("❌ Restore Failed")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }
    
    private fun showPasswordDialog(title: String, message: String, onPassword: (String) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password_input, null)
        val textInputLayout = dialogView.findViewById<TextInputLayout>(R.id.text_input_layout)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.edit_text)
        
        textInputLayout.hint = "Master Password"
        editText.requestFocus()
        
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setView(dialogView)
            .setPositiveButton("Continue") { _, _ ->
                val password = editText.text?.toString()?.trim()
                if (password.isNullOrEmpty()) {
                    Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    onPassword(password)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun setLoading(isLoading: Boolean, message: String = "Processing...") {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.tvProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.tvProgress.text = message
        
        // Disable buttons during loading
        binding.btnCreateBackup.isEnabled = !isLoading
        binding.btnRestoreBackup.isEnabled = !isLoading
    }
}