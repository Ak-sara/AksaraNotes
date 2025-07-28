package com.aksara.notes.ui.notes

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.aksara.notes.R
import com.aksara.notes.databinding.ActivityNoteDetailBinding
import com.aksara.notes.data.database.entities.Note
import com.aksara.notes.utils.BiometricHelper

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteDetailBinding
    private lateinit var notesViewModel: NotesViewModel
    private lateinit var biometricHelper: BiometricHelper
    private var currentNote: Note? = null
    private var isEditMode = false
    private var hasAccessToNote = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notesViewModel = ViewModelProvider(this)[NotesViewModel::class.java]
        biometricHelper = BiometricHelper(this)

        setupToolbar()
        loadNote()
        setupButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun loadNote() {
        val noteId = intent.getStringExtra("note_id")

        if (noteId != null) {
            // Edit existing note - check if PIN protected
            lifecycleScope.launch {
                currentNote = notesViewModel.getNoteById(noteId)
                currentNote?.let { note ->
                    if (note.requiresPin) {
                        showAccessDeniedState()
                        checkNoteAccess(note) {
                            displayNoteContent(note)
                        }
                    } else {
                        displayNoteContent(note)
                    }
                }
            }
        } else {
            // Create new note
            supportActionBar?.title = "New Note"
            hasAccessToNote = true
            showNormalState()
            enterEditMode()
        }
    }

    private fun checkNoteAccess(note: Note, onAccessGranted: () -> Unit) {
        // Check if PIN is enabled for note protection
        if (biometricHelper.isPinEnabled()) {
            // Use PIN for note authentication
            biometricHelper.showPinDialog(
                title = "Enter PIN",
                message = "Enter your PIN to access this protected note",
                onSuccess = {
                    hasAccessToNote = true
                    Toast.makeText(this, "🔓 Note unlocked", Toast.LENGTH_SHORT).show()
                    onAccessGranted()
                },
                onError = { error ->
                    Toast.makeText(this, "❌ $error", Toast.LENGTH_SHORT).show()
                    // Stay in access denied state
                },
                onCancel = {
                    Toast.makeText(this, "Authentication cancelled", Toast.LENGTH_SHORT).show()
                    finish() // Close activity if user cancels
                }
            )
        } else {
            // Fallback: No PIN set up, request PIN setup
            Toast.makeText(this, "❌ PIN not set up. Please set up a PIN in Security Settings first.", Toast.LENGTH_LONG).show()
            finish() // Close activity since we can't access the note
        }
    }

    private fun displayNoteContent(note: Note) {
        hasAccessToNote = true
        binding.etNoteTitle.setText(note.title)
        binding.etNoteContent.setText(note.content)
        supportActionBar?.title = "Edit Note"
        showNormalState()
        invalidateOptionsMenu() // Show menu options
    }

    private fun showNormalState() {
        // Show all UI elements
        binding.etNoteTitle.visibility = android.view.View.VISIBLE
        binding.etNoteContent.visibility = android.view.View.VISIBLE
        binding.layoutActionButtons.visibility = android.view.View.VISIBLE
    }

    private fun showAccessDeniedState() {
        // Hide note content and show access denied message
        binding.etNoteTitle.visibility = android.view.View.GONE
        binding.etNoteContent.visibility = android.view.View.GONE
        binding.layoutActionButtons.visibility = android.view.View.GONE

        supportActionBar?.title = "🔒 Protected Note"
    }

    private fun setupButtons() {
        binding.btnFavorite.setOnClickListener {
            toggleFavorite()
        }
        
        binding.btnPinProtect.setOnClickListener {
            togglePinProtection()
        }
        
        binding.btnShare.setOnClickListener {
            shareNote()
        }
    }

    private fun enterEditMode() {
        isEditMode = true
        binding.etNoteTitle.isEnabled = true
        binding.etNoteContent.isEnabled = true
        binding.etNoteTitle.requestFocus()
        invalidateOptionsMenu() // Update menu icon
    }

    private fun exitEditMode() {
        isEditMode = false
        binding.etNoteTitle.isEnabled = false
        binding.etNoteContent.isEnabled = false
        invalidateOptionsMenu() // Update menu icon
    }

    private fun saveNote() {
        val title = binding.etNoteTitle.text.toString().trim()
        val content = binding.etNoteContent.text.toString().trim()

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Cannot save empty note", Toast.LENGTH_SHORT).show()
            return
        }

        val note = currentNote?.let { existing ->
            Note().apply {
                id = existing.id
                this.title = title.ifEmpty { "Untitled" }
                this.content = content
                createdAt = existing.createdAt
                updatedAt = System.currentTimeMillis()
                requiresPin = existing.requiresPin
                isEncrypted = existing.isEncrypted
                isFavorite = existing.isFavorite
                tags = existing.tags
            }
        } ?: Note().apply {
            this.title = title.ifEmpty { "Untitled" }
            this.content = content
        }

        if (currentNote == null) {
            notesViewModel.insertNote(note)
            currentNote = note
        } else {
            notesViewModel.updateNote(note)
        }

        exitEditMode()
        Toast.makeText(this, "💾 Note saved", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Only show menu if user has access to note
        if (hasAccessToNote) {
            menuInflater.inflate(R.menu.note_detail_menu, menu)
            // Update edit icon based on current mode
            val editItem = menu.findItem(R.id.action_edit)
            editItem?.setIcon(if (isEditMode) R.drawable.ic_save else R.drawable.ic_edit)
            editItem?.setTitle(if (isEditMode) "Save" else "Edit")
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (isEditMode) saveNote()
                finish()
                true
            }
            R.id.action_edit -> {
                if (isEditMode) {
                    saveNote()
                } else {
                    enterEditMode()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleFavorite() {
        currentNote?.let { note ->
            val updatedNote = Note().apply {
                id = note.id
                title = note.title
                content = note.content
                createdAt = note.createdAt
                updatedAt = System.currentTimeMillis()
                requiresPin = note.requiresPin
                isEncrypted = note.isEncrypted
                isFavorite = !note.isFavorite
                tags = note.tags
            }
            notesViewModel.updateNote(updatedNote)
            currentNote = updatedNote

            val message = if (updatedNote.isFavorite) "⭐ Added to favorites" else "☆ Removed from favorites"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePinProtection() {
        currentNote?.let { note ->
            if (!note.requiresPin) {
                // Enabling PIN protection - check if PIN is set up, if not auto-prompt setup
                if (!biometricHelper.isPinEnabled()) {
                    // PIN not set up, prompt user to set it up
                    biometricHelper.showPinSetupDialog(
                        onSuccess = {
                            Toast.makeText(this, "✅ PIN set up successfully!", Toast.LENGTH_SHORT).show()
                            updateNotePinProtection(note, true)
                        },
                        onError = { error ->
                            Toast.makeText(this, "❌ $error", Toast.LENGTH_SHORT).show()
                        },
                        onCancel = {
                            Toast.makeText(this, "PIN setup cancelled", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    // PIN already set up, directly enable protection
                    updateNotePinProtection(note, true)
                }
            } else {
                // Disabling PIN protection - just confirm
                AlertDialog.Builder(this)
                    .setTitle("Remove PIN Protection")
                    .setMessage("Are you sure you want to remove PIN protection from this note?")
                    .setPositiveButton("Remove") { _, _ ->
                        updateNotePinProtection(note, false)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun updateNotePinProtection(note: Note, requiresPin: Boolean) {
        val updatedNote = Note().apply {
            id = note.id
            title = note.title
            content = note.content
            createdAt = note.createdAt
            updatedAt = System.currentTimeMillis()
            this.requiresPin = requiresPin
            isEncrypted = note.isEncrypted
            isFavorite = note.isFavorite
            tags = note.tags
        }
        notesViewModel.updateNote(updatedNote)
        currentNote = updatedNote

        val message = if (requiresPin) "🔒 PIN protection enabled" else "🔓 PIN protection disabled"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        // Invalidate menu to show/hide options based on new PIN status
        invalidateOptionsMenu()
    }

    private fun shareNote() {
        currentNote?.let { note ->
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
                putExtra(android.content.Intent.EXTRA_SUBJECT, note.title)
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Share Note"))
        }
    }

    override fun onBackPressed() {
        if (isEditMode) {
            saveNote()
        }
        super.onBackPressed()
    }
}