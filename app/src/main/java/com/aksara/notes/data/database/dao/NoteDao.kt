package com.aksara.notes.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.aksara.notes.data.database.entities.Note

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :searchQuery || '%' OR content LIKE '%' || :searchQuery || '%' ORDER BY updatedAt DESC")
    fun searchNotes(searchQuery: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE requiresPin = 1 ORDER BY updatedAt DESC")
    fun getProtectedNotes(): Flow<List<Note>>
}