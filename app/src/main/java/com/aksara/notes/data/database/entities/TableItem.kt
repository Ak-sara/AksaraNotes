package com.aksara.notes.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.util.UUID

@Entity(
    tableName = "table_items",
    foreignKeys = [
        ForeignKey(
            entity = CustomTable::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TableItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tableId: String, // References CustomTable.id
    val data: String, // JSON string of field values {"field_name": "value", ...}
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)