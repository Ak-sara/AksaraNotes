package com.aksara.aksaranotes.ui.calendar

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
    val relatedId: String? = null
)

enum class EventType {
    SUBSCRIPTION_OVERDUE,
    SUBSCRIPTION_DUE_TODAY,
    SUBSCRIPTION_UPCOMING,
    CUSTOM_EVENT,
    NOTE_REMINDER
}