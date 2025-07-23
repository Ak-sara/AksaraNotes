package com.aksara.aksaranotes.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val amount: Double,
    val currency: String = "USD",
    val frequency: String, // "monthly", "yearly", "weekly", "daily"
    val nextDueDate: Long, // timestamp
    val lastPaidDate: Long = 0,
    val isActive: Boolean = true,
    val category: String = "General", // Entertainment, Utilities, Software, etc.
    val reminderDays: Int = 3, // days before due date to remind
    val autoRenew: Boolean = true,
    val paymentMethod: String = "",
    val website: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)