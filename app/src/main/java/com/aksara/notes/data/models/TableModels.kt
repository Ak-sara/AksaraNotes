package com.aksara.notes.data.models

// Column types enum - still needed for UI and type conversion
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

// Note: TableColumn and TableSettings are now Realm entities in data.database.entities
// This file only contains the enum needed for type definitions