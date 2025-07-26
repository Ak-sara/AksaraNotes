package com.aksara.notes.ui.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aksara.notes.R
import com.aksara.notes.data.database.entities.Note
import java.text.SimpleDateFormat
import java.util.*

class NotesAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: (Note) -> Unit
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.tv_note_title)
        private val contentTextView: TextView = itemView.findViewById(R.id.tv_note_content)
        private val dateTextView: TextView = itemView.findViewById(R.id.tv_note_date)
        private val indicatorsTextView: TextView = itemView.findViewById(R.id.tv_note_indicators)

        fun bind(note: Note) {
            // Handle PIN-protected notes
            if (note.requiresPin) {
                titleTextView.text = "🔒 ${note.title.ifEmpty { "Untitled" }}"
                contentTextView.text = "Tap to unlock and view content..."
            } else {
                titleTextView.text = note.title.ifEmpty { "Untitled" }
                contentTextView.text = note.content.take(100) + if (note.content.length > 100) "..." else ""
            }

            // Format date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateTextView.text = dateFormat.format(Date(note.updatedAt))

            // Show indicators
            val indicators = buildString {
                if (note.isFavorite) append("⭐ ")
                if (note.requiresPin) append("🔒 ")
                if (note.isEncrypted) append("🔐 ")
            }
            indicatorsTextView.text = indicators
            indicatorsTextView.visibility = if (indicators.isNotEmpty()) View.VISIBLE else View.GONE

            // Visual styling for PIN-protected notes
            if (note.requiresPin) {
                itemView.alpha = 0.8f
                itemView.setBackgroundColor(itemView.context.getColor(R.color.secondary))
            } else {
                itemView.alpha = 1.0f
//                itemView.setBackgroundColor(itemView.context.getColor(R.color.secondary))
            }

            // Click listeners
            itemView.setOnClickListener { onNoteClick(note) }
            itemView.setOnLongClickListener {
                onNoteLongClick(note)
                true
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}