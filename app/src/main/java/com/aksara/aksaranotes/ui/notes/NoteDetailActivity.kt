package com.aksara.aksaranotes.ui.notes

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.aksara.aksaranotes.R
import com.aksara.aksaranotes.databinding.ActivityNoteDetailBinding
import com.aksara.aksaranotes.data.database.entities.Note

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteDetailBinding
    private lateinit var notesViewModel: NotesViewModel
    private var currentNote: Note? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notesViewModel = ViewModelProvider(this)[NotesViewModel::class.java]

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
            // Edit existing note
            lifecycleScope.launch {
                currentNote = notesViewModel.getNoteById(noteId)
                currentNote?.let { note ->
                    binding.etNoteTitle.setText(note.title)
                    binding.etNoteContent.setText(note.content)
                    supportActionBar?.title = "Edit Note"
                }
            }
        } else {
            // Create new note
            supportActionBar?.title = "New Note"
            enterEditMode()
        }
    }

    private fun setupFab() {
        binding.fabEdit.setOnClickListener {
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
        Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.note_detail_menu, menu)
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

            val message = if (updatedNote.isFavorite) "Added to favorites" else "Removed from favorites"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePinProtection() {
        currentNote?.let { note ->
            val updatedNote = note.copy(
                requiresPin = !note.requiresPin,
                updatedAt = System.currentTimeMillis()
            )
            notesViewModel.updateNote(updatedNote)
            currentNote = updatedNote

            val message = if (updatedNote.requiresPin) "PIN protection enabled" else "PIN protection disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
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