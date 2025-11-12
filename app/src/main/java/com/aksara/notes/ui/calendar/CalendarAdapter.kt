package com.aksara.notes.ui.calendar

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aksara.notes.R
import java.util.*

class CalendarAdapter(
    private val onDateClick: (Calendar) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {

    private var days = listOf<CalendarDay>()
    private var selectedDate: Calendar? = null

    fun updateDays(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }

    fun setSelectedDate(date: Calendar) {
        selectedDate = date
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.calendar_day_layout, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount() = days.size

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayNumber: TextView = itemView.findViewById(R.id.tv_day_number)
        private val eventsLayout: LinearLayout = itemView.findViewById(R.id.layout_events)

        fun bind(day: CalendarDay) {
            dayNumber.text = day.dayNumber

            if (day.dayNumber.isEmpty()) {
                // Empty day (padding)
                itemView.visibility = View.INVISIBLE
                return
            }

            itemView.visibility = View.VISIBLE

            // Style the day
            when {
                day.isToday -> {
                    itemView.setBackgroundResource(R.drawable.calendar_today_background)
                    dayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.on_primary))
                }
                selectedDate != null && isSameDay(day.date, selectedDate!!) -> {
                    itemView.setBackgroundResource(R.drawable.calendar_selected_background)
                    dayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary))
                }
                else -> {
                    itemView.setBackgroundResource(R.drawable.calendar_day_background)
                    dayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.on_surface_variant))
                }
            }

            // Display events (Mac-style with table icons)
            displayEvents(day.events)

            // Click listener
            itemView.setOnClickListener {
                onDateClick(day.date)
            }
        }

        private fun displayEvents(events: List<CalendarEvent>) {
            eventsLayout.removeAllViews()

            events.take(3).forEach { event ->
                val eventView = TextView(itemView.context).apply {
                    // Use table icon if available, otherwise use colored dot
                    text = if (!event.tableIcon.isNullOrEmpty()) {
                        event.tableIcon
                    } else {
                        "●"
                    }
                    textSize = if (!event.tableIcon.isNullOrEmpty()) 10f else 8f

                    // Set color and alpha based on event type
                    val color = getEventColor(event.type)
                    setTextColor(color)

                    // Make recurring events semi-transparent to show they're predictions
                    alpha = if (event.type == EventType.TABLE_RECURRING) 0.6f else 1.0f

                    setPadding(1, 0, 1, 1)
                }
                eventsLayout.addView(eventView)
            }

            if (events.size > 3) {
                val moreView = TextView(itemView.context).apply {
                    text = "+${events.size - 3}"
                    textSize = 6f
                    setTextColor(ContextCompat.getColor(itemView.context, R.color.text_hint))
                }
                eventsLayout.addView(moreView)
            }
        }

        private fun getEventColor(eventType: EventType): Int {
            return when (eventType) {
                // Legacy event colors - using theme calendar colors
                EventType.SUBSCRIPTION_OVERDUE -> ContextCompat.getColor(itemView.context, R.color.calendar_overdue)
                EventType.SUBSCRIPTION_DUE_TODAY -> ContextCompat.getColor(itemView.context, R.color.calendar_due_today)
                EventType.SUBSCRIPTION_UPCOMING -> ContextCompat.getColor(itemView.context, R.color.calendar_upcoming)
                EventType.CUSTOM_EVENT -> ContextCompat.getColor(itemView.context, R.color.primary)
                EventType.NOTE_REMINDER -> ContextCompat.getColor(itemView.context, R.color.calendar_reminder)

                // New table-based event colors - using theme calendar colors
                EventType.TABLE_SUBSCRIPTION -> ContextCompat.getColor(itemView.context, R.color.calendar_subscription)
                EventType.TABLE_INVESTMENT -> ContextCompat.getColor(itemView.context, R.color.calendar_investment)
                EventType.TABLE_MEETING -> ContextCompat.getColor(itemView.context, R.color.tertiary)
                EventType.TABLE_TASK -> ContextCompat.getColor(itemView.context, R.color.calendar_task)
                EventType.TABLE_DATE -> ContextCompat.getColor(itemView.context, R.color.on_surface_variant)
                EventType.TABLE_RECURRING -> ContextCompat.getColor(itemView.context, R.color.outline)
            }
        }

        private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }
    }
}