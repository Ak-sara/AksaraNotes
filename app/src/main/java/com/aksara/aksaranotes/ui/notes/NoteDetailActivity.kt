package com.aksara.aksaranotes.ui.notes

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.aksara.aksaranotes.MainActivity
import com.aksara.aksaranotes.R
import com.aksara.aksaranotes.databinding.ActivityNoteDetailBinding
import com.aksara.aksaranotes.data.database.entities.Note
import com.aksara.aksaranotes.utils.BiometricHelper

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
        setupFab()
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
        biometricHelper.authenticateUser(
            onSuccess = {
                hasAccessToNote = true
                Toast.makeText(this, "🔓 Note unlocked", Toast.LENGTH_SHORT).show()
                onAccessGranted()
            },
            onError = { error ->
                Toast.makeText(this, "Access denied: $error", Toast.LENGTH_SHORT).show()
                // Stay in access denied state
            },
            onPasswordFallback = {
                biometricHelper.showPasswordDialog(
                    onSuccess = {
                        hasAccessToNote = true
                        Toast.makeText(this, "🔓 Note unlocked", Toast.LENGTH_SHORT).show()
                        onAccessGranted()
                    },
                    onError = { error ->
                        Toast.makeText(this, "Access denied: $error", Toast.LENGTH_SHORT).show()
                        // Stay in access denied state
                    },
                    onCancel = {
                        Toast.makeText(this, "Authentication cancelled", Toast.LENGTH_SHORT).show()
                        finish() // Close activity if user cancels
                    }
                )
            }
        )
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
        binding.fabEdit.visibility = android.view.View.VISIBLE
    }

    private fun showAccessDeniedState() {
        // Hide note content and show access denied message
        binding.etNoteTitle.visibility = android.view.View.GONE
        binding.etNoteContent.visibility = android.view.View.GONE
        binding.fabEdit.visibility = android.view.View.GONE

        supportActionBar?.title = "🔒 Protected Note"
    }

    private fun setupFab() {
        binding.fabEdit.setOnClickListener {
            // If we can see the FAB, we already have access to the note
            if (isEditMode) {
                saveNote()
            } else {
                enterEditMode()
            }
        }
    }

    private fun enterEditMode() {
        isEditMode = true
        binding.etNoteTitle.isEnabled = true
        binding.etNoteContent.isEnabled = true
        binding.etNoteTitle.requestFocus()
        binding.fabEdit.setImageResource(R.drawable.ic_save)
    }

    private fun exitEditMode() {
        isEditMode = false
        binding.etNoteTitle.isEnabled = false
        binding.etNoteContent.isEnabled = false
        binding.fabEdit.setImageResource(R.drawable.ic_edit)
    }

    private fun saveNote() {
        val title = binding.etNoteTitle.text.toString().trim()
        val content = binding.etNoteContent.text.toString().trim()

        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Cannot save empty note", Toast.LENGTH_SHORT).show()
            return
        }

        val note = currentNote?.copy(
            title = title.ifEmpty { "Untitled" },
            content = content,
            updatedAt = System.currentTimeMillis()
        ) ?: Note(
            title = title.ifEmpty { "Untitled" },
            content = content
        )

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
            R.id.action_favorite -> {
                toggleFavorite()
                true
            }
            R.id.action_pin_protect -> {
                togglePinProtection()
                true
            }
            R.id.action_share -> {
                shareNote()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleFavorite() {
        currentNote?.let { note ->
            val updatedNote = note.copy(
                isFavorite = !note.isFavorite,
                updatedAt = System.currentTimeMillis()
            )
            notesViewModel.updateNote(updatedNote)
            currentNote = updatedNote

            val message = if (updatedNote.isFavorite) "⭐ Added to favorites" else "☆ Removed from favorites"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePinProtection() {
        currentNote?.let { note ->
            if (!note.requiresPin) {
                // Enabling PIN protection - ask for authentication to confirm
                biometricHelper.authenticateUser(
                    onSuccess = {
                        updateNotePinProtection(note, true)
                    },
                    onError = { error ->
                        Toast.makeText(this, "Cannot enable PIN protection: $error", Toast.LENGTH_SHORT).show()
                    },
                    onPasswordFallback = {
                        biometricHelper.showPasswordDialog(
                            onSuccess = {
                                updateNotePinProtection(note, true)
                            },
                            onError = { error ->
                                Toast.makeText(this, "Cannot enable PIN protection: $error", Toast.LENGTH_SHORT).show()
                            },
                            onCancel = {
                                Toast.makeText(this, "Authentication cancelled", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
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
        val updatedNote = note.copy(
            requiresPin = requiresPin,
            updatedAt = System.currentTimeMillis()
        )
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