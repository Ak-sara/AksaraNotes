package com.aksara.aksaranotes.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val organization: String,
    val accountNumber: String,
    val appWebsite: String,
    val username: String,
    val password: String,
    val email: String = "",
    val notes: String = "",
    val category: String = "General", // Banking, Social, Work, etc.
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
