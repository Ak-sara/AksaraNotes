package com.aksara.notes.data.database.dao

import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.Sort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.aksara.notes.data.database.entities.Note
import com.aksara.notes.data.database.RealmDatabase

class NoteDao {
    private val realm: Realm get() = RealmDatabase.getInstance()

    fun getAllNotes(): Flow<List<Note>> {
        return realm.query<Note>()
            .sort("updatedAt", Sort.DESCENDING)
            .asFlow()
            .map { it.list }
    }

    suspend fun getNoteById(id: String): Note? {
        return realm.query<Note>("id == $0", id).first().find()
    }

    suspend fun insertNote(note: Note) {
        realm.write {
            copyToRealm(note)
        }
    }

    suspend fun updateNote(note: Note) {
        realm.write {
            val existingNote = query<Note>("id == $0", note.id).first().find()
            existingNote?.let {
                it.title = note.title
                it.content = note.content
                it.updatedAt = note.updatedAt
                it.requiresPin = note.requiresPin
                it.isEncrypted = note.isEncrypted
                it.isFavorite = note.isFavorite
                it.tags = note.tags
            }
        }
    }

    suspend fun deleteNote(note: Note) {
        realm.write {
            val noteToDelete = query<Note>("id == $0", note.id).first().find()
            noteToDelete?.let { delete(it) }
        }
    }

    fun searchNotes(searchQuery: String): Flow<List<Note>> {
        return realm.query<Note>("title CONTAINS[c] $0 OR content CONTAINS[c] $0", searchQuery)
            .sort("updatedAt", Sort.DESCENDING)
            .asFlow()
            .map { it.list }
    }

    fun getFavoriteNotes(): Flow<List<Note>> {
        return realm.query<Note>("isFavorite == true")
            .sort("updatedAt", Sort.DESCENDING)
            .asFlow()
            .map { it.list }
    }

    fun getProtectedNotes(): Flow<List<Note>> {
        return realm.query<Note>("requiresPin == true")
            .sort("updatedAt", Sort.DESCENDING)
            .asFlow()
            .map { it.list }
    }
    
    // Backup-specific methods
    suspend fun getAllNotesForBackup(): List<Note> {
        return realm.query<Note>()
            .sort("updatedAt", Sort.DESCENDING)
            .find()
    }
    
    suspend fun clearAllNotes() {
        realm.write {
            val allNotes = query<Note>().find()
            delete(allNotes)
        }
    }
    
    suspend fun insertNoteFromBackup(note: Note) {
        realm.write {
            copyToRealm(note)
        }
    }
}