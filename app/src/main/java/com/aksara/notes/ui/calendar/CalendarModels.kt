package com.aksara.notes.ui.calendar

import java.util.*

data class CalendarDay(
    val dayNumber: String,
    val isToday: Boolean,
    val date: Calendar,
    val events: List<CalendarEvent>
)

data class CalendarEvent(
    val id: String,
    val title: String,
    val type: EventType,
    val description: String? = null,
    val relatedId: String? = null,
    val tableId: String? = null,
    val tableName: String? = null,
    val tableIcon: String? = null
)

enum class EventType {
    // Legacy subscription events (for backwards compatibility)
    SUBSCRIPTION_OVERDUE,
    SUBSCRIPTION_DUE_TODAY,
    SUBSCRIPTION_UPCOMING,
    CUSTOM_EVENT,
    NOTE_REMINDER,

    // New table-based events
    TABLE_SUBSCRIPTION,    // 💰 Subscription tables with due dates
    TABLE_INVESTMENT,      // 🧮 Bond/investment maturity dates
    TABLE_MEETING,         // 📞 Meeting/appointment dates
    TABLE_TASK,           // ✅ Task/todo due dates
    TABLE_DATE,           // 📅 Generic date fields from any table
    TABLE_RECURRING       // 🔄 Predicted recurring events (shown lighter)
}