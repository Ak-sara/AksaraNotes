package com.aksara.aksaranotes.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "custom_tables")
data class CustomTable(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val icon: String = "📄",
    val tableType: String = "data",
    val columns: String = "[]", // JSON string of column definitions
    val settings: String = "{}", // JSON string of table settings
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)