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

            // Display events (Mac-style)
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
                    text = "●"
                    textSize = 8f
                    setTextColor(getEventColor(event.type))
                    setPadding(2, 0, 2, 1)
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
                EventType.SUBSCRIPTION_OVERDUE -> Color.RED
                EventType.SUBSCRIPTION_DUE_TODAY -> Color.parseColor("#FFA500") // Orange
                EventType.SUBSCRIPTION_UPCOMING -> Color.GREEN
                EventType.CUSTOM_EVENT -> ContextCompat.getColor(itemView.context, R.color.primary_blue)
                EventType.NOTE_REMINDER -> Color.parseColor("#800080") // Purple
            }
        }

        private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }
    }
}