package com.aksara.aksaranotes.ui.calendar

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aksara.aksaranotes.R
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
                    dayNumber.setTextColor(Color.WHITE)
                }
                selectedDate != null && isSameDay(day.date, selectedDate!!) -> {
                    itemView.setBackgroundResource(R.drawable.calendar_selected_background)
                    dayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary_blue))
                }
                else -> {
                    itemView.setBackgroundResource(R.drawable.calendar_day_background)
                    dayNumber.setTextColor(Color.BLACK)
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
                    setTextColor(Color.GRAY)
                }
                eventsLayout.addView(moreView)
            }
        }

        private fun getEventColor(eventType: EventType): Int {
            return when (eventType) {
                // Legacy event colors
                EventType.SUBSCRIPTION_OVERDUE -> Color.RED
                EventType.SUBSCRIPTION_DUE_TODAY -> Color.parseColor("#FFA500") // Orange
                EventType.SUBSCRIPTION_UPCOMING -> Color.GREEN
                EventType.CUSTOM_EVENT -> ContextCompat.getColor(itemView.context, R.color.primary_blue)
                EventType.NOTE_REMINDER -> Color.parseColor("#800080") // Purple

                // New table-based event colors
                EventType.TABLE_SUBSCRIPTION -> Color.parseColor("#FF6B35") // Orange-red for subscriptions
                EventType.TABLE_INVESTMENT -> Color.parseColor("#4CAF50") // Green for investments/bonds
                EventType.TABLE_MEETING -> Color.parseColor("#2196F3") // Blue for meetings
                EventType.TABLE_TASK -> Color.parseColor("#9C27B0") // Purple for tasks
                EventType.TABLE_DATE -> Color.parseColor("#607D8B") // Blue-grey for generic dates
                EventType.TABLE_RECURRING -> Color.parseColor("#9E9E9E") // Grey for recurring predictions
            }
        }

        private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }
    }
}