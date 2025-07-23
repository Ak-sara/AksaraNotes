package com.aksara.aksaranotes.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.aksara.aksaranotes.data.database.entities.Note
import com.aksara.aksaranotes.data.database.entities.CustomTable
import com.aksara.aksaranotes.data.database.dao.NoteDao
import com.aksara.aksaranotes.data.database.dao.CustomTableDao

@Database(
    entities = [Note::class, CustomTable::class], // Only include existing entities
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun customTableDao(): CustomTableDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aksara_notes_database"
                )
                    .fallbackToDestructiveMigration() // For development
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}