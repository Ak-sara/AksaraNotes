package com.aksara.aksaranotes.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aksara.aksaranotes.R

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
            titleView.text = event.title
            colorView.setBackgroundColor(getEventColor(event.type))

            itemView.setOnClickListener {
                onEventClick(event)
            }
        }

        private fun getEventColor(eventType: EventType): Int {
            return when (eventType) {
                EventType.SUBSCRIPTION_OVERDUE -> ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
                EventType.SUBSCRIPTION_DUE_TODAY -> ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark)
                EventType.SUBSCRIPTION_UPCOMING -> ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                EventType.CUSTOM_EVENT -> ContextCompat.getColor(itemView.context, R.color.primary_blue)
                EventType.NOTE_REMINDER -> ContextCompat.getColor(itemView.context, android.R.color.holo_purple)
            }
        }
    }
}