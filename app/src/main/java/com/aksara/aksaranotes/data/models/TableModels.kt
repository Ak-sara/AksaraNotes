package com.aksara.aksaranotes.data.models

import java.util.UUID

// Column definition for table schema
data class TableColumn(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: ColumnType,
    val required: Boolean = false,
    val defaultValue: String = "",
    val options: Map<String, Any> = emptyMap()
)

enum class ColumnType(val displayName: String, val icon: String) {
    TEXT("Text", "📝"),
    NUMBER("Number", "🔢"),
    CURRENCY("Currency", "💰"),
    DATE("Date", "📅"),
    TIME("Time", "⏰"),
    DATETIME("Date & Time", "🕐"),
    BOOLEAN("Yes/No", "✅"),
    SELECT("Select", "📋"),
    EMAIL("Email", "📧"),
    PHONE("Phone", "📞"),
    URL("URL", "🔗"),
    RATING("Rating", "⭐"),
    COLOR("Color", "🎨"),
    FORMULA("Formula", "🧮")
}

// Table settings
data class TableSettings(
    val primaryColor: String = "#2196F3",
    val showInCalendar: Boolean = false,
    val calendarDateField: String = "",
    val reminderDays: Int = 3
)