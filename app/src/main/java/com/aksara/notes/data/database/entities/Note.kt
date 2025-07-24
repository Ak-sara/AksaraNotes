package com.aksara.notes.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val requiresPin: Boolean = false,
    val isEncrypted: Boolean = false,
    val isFavorite: Boolean = false,
    val tags: String = "" // JSON string of tags
)
