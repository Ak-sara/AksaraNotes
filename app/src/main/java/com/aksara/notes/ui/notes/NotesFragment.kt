package com.aksara.notes.ui.notes

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.aksara.notes.MainActivity
import com.aksara.notes.databinding.FragmentNotesBinding
import com.aksara.notes.data.database.entities.Note

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private lateinit var notesViewModel: NotesViewModel
    private lateinit var notesAdapter: NotesAdapter
    private var isGridView = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notesViewModel = ViewModelProvider(this)[NotesViewModel::class.java]

        setupRecyclerView()
        setupSearchView()
        setupFab()
        setupViewToggle()
        observeNotes()
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            onNoteClick = { note ->
                // Always open note detail - let NoteDetailActivity handle authentication
                openNoteDetail(note)
            },
            onNoteLongClick = { note ->
                // Show options without authentication - actions will handle auth if needed
                showNoteOptionsDialog(note)
            }
        )

        binding.rvNotes.apply {
            adapter = notesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSearchView() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString().trim()
            notesViewModel.searchNotes(query)
        }
    }

    private fun setupFab() {
        binding.fabAddNote.setOnClickListener {
            openNoteDetail(null) // null = create new note
        }
    }

    private fun setupViewToggle() {
        binding.btnViewToggle.setOnClickListener {
            isGridView = !isGridView
            updateLayoutManager()
        }
    }

    private fun updateLayoutManager() {
        binding.rvNotes.layoutManager = if (isGridView) {
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        } else {
            LinearLayoutManager(requireContext())
        }

        binding.btnViewToggle.text = if (isGridView) "📋" else "⊞"
    }

    private fun observeNotes() {
        notesViewModel.searchResults.observe(viewLifecycleOwner) { notes ->
            updateUI(notes)
        }
    }

    private fun updateUI(notes: List<Note>) {
        if (notes.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvNotes.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvNotes.visibility = View.VISIBLE
            notesAdapter.submitList(notes)
        }

        // Update notes count
        binding.tvNotesCount.text = "${notes.size} notes"
    }

    private fun openNoteDetail(note: Note?) {
        val intent = Intent(requireContext(), NoteDetailActivity::class.java)
        note?.let {
            intent.putExtra("note_id", it.id)
        }
        startActivity(intent)
    }

    private fun showNoteOptionsDialog(note: Note) {
        val options = arrayOf(
            "Edit",
            "Toggle Favorite",
            "Toggle PIN Protection",
            "Share",
            "Delete"
        )

        AlertDialog.Builder(requireContext())
            .setTitle(note.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openNoteDetail(note) // Edit
                    1 -> toggleFavorite(note)
                    2 -> togglePinProtection(note)
                    3 -> shareNote(note)
                    4 -> confirmDeleteNote(note)
                }
            }
            .show()
    }

    private fun toggleFavorite(note: Note) {
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

        val message = if (updatedNote.isFavorite) "⭐ Added to favorites" else "☆ Removed from favorites"
        showToast(message)
    }

    private fun togglePinProtection(note: Note) {
        if (!note.requiresPin) {
            // Enabling PIN protection - ask for authentication to confirm
            val mainActivity = activity as? MainActivity
            mainActivity?.requestPinAuthentication(
                onSuccess = {
                    updateNotePinProtection(note, true)
                },
                onError = { error ->
                    showToast("Cannot enable PIN protection: $error")
                }
            ) ?: run {
                showToast("Authentication unavailable")
            }
        } else {
            // Disabling PIN protection - just confirm
            AlertDialog.Builder(requireContext())
                .setTitle("Remove PIN Protection")
                .setMessage("Are you sure you want to remove PIN protection from '${note.title}'?")
                .setPositiveButton("Remove") { _, _ ->
                    updateNotePinProtection(note, false)
                }
                .setNegativeButton("Cancel", null)
                .show()
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

        val message = if (requiresPin) "🔒 PIN protection enabled" else "🔓 PIN protection disabled"
        showToast(message)
    }

    private fun shareNote(note: Note) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
            putExtra(Intent.EXTRA_SUBJECT, note.title)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Note"))
    }

    private fun confirmDeleteNote(note: Note) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete '${note.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                notesViewModel.deleteNote(note)
                showToast("🗑️ Note deleted")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh notes when returning from detail activity
        notesViewModel.searchNotes(binding.etSearch.text.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}