package com.aksara.notes.ui.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.switchMap
import kotlinx.coroutines.launch
import com.aksara.notes.data.database.entities.Note
import com.aksara.notes.data.repository.NotesRepository

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NotesRepository = NotesRepository()
    private val _searchQuery = MutableLiveData("")

    val allNotes: LiveData<List<Note>> = repository.getAllNotes().asLiveData()

    val searchResults: LiveData<List<Note>> = _searchQuery.switchMap { query ->
        if (query.isBlank()) {
            repository.getAllNotes().asLiveData()
        } else {
            repository.searchNotes(query).asLiveData()
        }
    }

    fun searchNotes(query: String) {
        _searchQuery.value = query
    }

    fun insertNote(note: Note) = viewModelScope.launch {
        repository.insertNote(note)
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        repository.updateNote(note)
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        repository.deleteNote(note)
    }

    suspend fun getNoteById(id: String): Note? {
        return repository.getNoteById(id)
    }
}