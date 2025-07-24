package com.aksara.notes.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aksara.notes.R

class DayEventsAdapter(
    private val onEventClick: (CalendarEvent) -> Unit
) : RecyclerView.Adapter<DayEventsAdapter.EventViewHolder>() {

    private var events = listOf<CalendarEvent>()

    fun updateEvents(newEvents: List<CalendarEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.calendar_event_item, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount() = events.size

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorView: View = itemView.findViewById(R.id.view_event_color)
        private val titleView: TextView = itemView.findViewById(R.id.tv_event_title)

        fun bind(event: CalendarEvent) {
            // Set event title with table icon and recurring indicator
            titleView.text = when {
                event.type == EventType.TABLE_RECURRING && !event.tableIcon.isNullOrEmpty() -> {
                    "${event.tableIcon} ${event.title} (predicted)"
                }
                event.type == EventType.TABLE_RECURRING -> {
                    "${event.title} (predicted)"
                }
                !event.tableIcon.isNullOrEmpty() -> {
                    "${event.tableIcon} ${event.title}"
                }
                else -> {
                    event.title
                }
            }

            // Set color indicator
            colorView.setBackgroundColor(getEventColor(event.type))

            // Make recurring events slightly transparent
            itemView.alpha = if (event.type == EventType.TABLE_RECURRING) 0.7f else 1.0f

            itemView.setOnClickListener {
                onEventClick(event)
            }
        }

        private fun getEventColor(eventType: EventType): Int {
            return when (eventType) {
                // Legacy event colors
                EventType.SUBSCRIPTION_OVERDUE -> ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
                EventType.SUBSCRIPTION_DUE_TODAY -> ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark)
                EventType.SUBSCRIPTION_UPCOMING -> ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                EventType.CUSTOM_EVENT -> ContextCompat.getColor(itemView.context, R.color.primary_blue)
                EventType.NOTE_REMINDER -> ContextCompat.getColor(itemView.context, android.R.color.holo_purple)

                // New table-based event colors
                EventType.TABLE_SUBSCRIPTION -> ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark)
                EventType.TABLE_INVESTMENT -> ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                EventType.TABLE_MEETING -> ContextCompat.getColor(itemView.context, R.color.primary_blue)
                EventType.TABLE_TASK -> ContextCompat.getColor(itemView.context, android.R.color.holo_purple)
                EventType.TABLE_DATE -> ContextCompat.getColor(itemView.context, android.R.color.darker_gray)
                EventType.TABLE_RECURRING -> ContextCompat.getColor(itemView.context, android.R.color.darker_gray)
            }
        }

        private fun getEventTypeDescription(eventType: EventType): String {
            return when (eventType) {
                EventType.SUBSCRIPTION_OVERDUE -> "Overdue subscription"
                EventType.SUBSCRIPTION_DUE_TODAY -> "Due today"
                EventType.SUBSCRIPTION_UPCOMING -> "Upcoming subscription"
                EventType.CUSTOM_EVENT -> "Custom event"
                EventType.NOTE_REMINDER -> "Note reminder"
                EventType.TABLE_SUBSCRIPTION -> "Subscription due"
                EventType.TABLE_INVESTMENT -> "Investment event"
                EventType.TABLE_MEETING -> "Meeting or appointment"
                EventType.TABLE_TASK -> "Task due"
                EventType.TABLE_DATE -> "Important date"
                EventType.TABLE_RECURRING -> "Recurring event (predicted)"
            }
        }
    }
}