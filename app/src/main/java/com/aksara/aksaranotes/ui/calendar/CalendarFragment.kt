package com.aksara.aksaranotes.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aksara.aksaranotes.R
import com.aksara.aksaranotes.databinding.FragmentCalendarBinding
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var eventsAdapter: DayEventsAdapter

    private val calendar = Calendar.getInstance()
    private var selectedDate: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCalendar()
        setupEventsList()
        setupNavigation()
        updateCalendarDisplay()
    }

    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter { date ->
            selectDate(date)
        }

        binding.rvCalendar.apply {
            layoutManager = GridLayoutManager(requireContext(), 7) // 7 days per week
            adapter = calendarAdapter
        }
    }

    private fun setupEventsList() {
        eventsAdapter = DayEventsAdapter { event ->
            handleEventClick(event)
        }

        binding.rvDayEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventsAdapter
        }
    }

    private fun setupNavigation() {
        binding.btnPreviousMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendarDisplay()
        }

        binding.btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendarDisplay()
        }
    }

    private fun updateCalendarDisplay() {
        // Update month/year header
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvMonthYear.text = monthFormat.format(calendar.time)

        // Generate calendar days
        val days = generateCalendarDays()
        calendarAdapter.updateDays(days)

        // Update selected date events
        selectedDate?.let { date ->
            updateSelectedDateEvents(date)
        }
    }

    private fun generateCalendarDays(): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()

        // Get first day of the month
        val monthCalendar = calendar.clone() as Calendar
        monthCalendar.set(Calendar.DAY_OF_MONTH, 1)

        // Get first day of week (Sunday = 1)
        val firstDayOfWeek = monthCalendar.get(Calendar.DAY_OF_WEEK) - 1

        // Add empty days for previous month
        for (i in 0 until firstDayOfWeek) {
            days.add(CalendarDay("", false, Calendar.getInstance(), emptyList()))
        }

        // Add days of current month
        val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (day in 1..daysInMonth) {
            val dayCalendar = monthCalendar.clone() as Calendar
            dayCalendar.set(Calendar.DAY_OF_MONTH, day)

            val events = getEventsForDate(dayCalendar)
            val isToday = isSameDay(dayCalendar, Calendar.getInstance())

            days.add(CalendarDay(day.toString(), isToday, dayCalendar, events))
        }

        return days
    }

    private fun selectDate(date: Calendar) {
        selectedDate = date
        calendarAdapter.setSelectedDate(date)
        updateSelectedDateEvents(date)

        // Show events panel
        binding.layoutSelectedDayEvents.visibility = View.VISIBLE
    }

    private fun updateSelectedDateEvents(date: Calendar) {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        binding.tvSelectedDate.text = dateFormat.format(date.time)

        val events = getEventsForDate(date)
        eventsAdapter.updateEvents(events)
    }

    private fun getEventsForDate(date: Calendar): List<CalendarEvent> {
        // TODO: Get events from database
        // For now, return sample events
        val events = mutableListOf<CalendarEvent>()

        // Sample subscription due dates
        if (date.get(Calendar.DAY_OF_MONTH) == 15) {
            events.add(CalendarEvent("1", "Netflix Subscription", EventType.SUBSCRIPTION_DUE_TODAY))
        }

        if (date.get(Calendar.DAY_OF_MONTH) == 10) {
            events.add(CalendarEvent("2", "Spotify Premium", EventType.SUBSCRIPTION_UPCOMING))
        }

        return events
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun handleEventClick(event: CalendarEvent) {
        // TODO: Handle event clicks
        // Navigate to subscription details, notes, etc.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}