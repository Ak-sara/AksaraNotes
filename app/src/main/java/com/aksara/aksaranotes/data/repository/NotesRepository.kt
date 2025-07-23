package com.aksara.aksaranotes.data.repository

import kotlinx.coroutines.flow.Flow
import com.aksara.aksaranotes.data.database.dao.NoteDao
import com.aksara.aksaranotes.data.database.entities.Note

class NotesRepository(private val noteDao: NoteDao) {

    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun getNoteById(id: String): Note? = noteDao.getNoteById(id)

    suspend fun insertNote(note: Note) = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    fun getFavoriteNotes(): Flow<List<Note>> = noteDao.getFavoriteNotes()

    fun getProtectedNotes(): Flow<List<Note>> = noteDao.getProtectedNotes()
}