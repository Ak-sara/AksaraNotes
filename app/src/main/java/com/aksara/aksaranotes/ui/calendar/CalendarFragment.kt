package com.aksara.aksaranotes.ui.calendar

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.aksara.aksaranotes.databinding.FragmentCalendarBinding
import com.aksara.aksaranotes.ui.database.DatabaseViewModel
import com.aksara.aksaranotes.ui.database.items.ItemEditorActivity
import com.aksara.aksaranotes.ui.database.view.TableViewActivity
import com.aksara.aksaranotes.data.database.entities.CustomTable
import com.aksara.aksaranotes.data.database.entities.TableItem
import com.aksara.aksaranotes.data.models.TableColumn
import com.aksara.aksaranotes.data.models.ColumnType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var eventsAdapter: DayEventsAdapter
    private lateinit var databaseViewModel: DatabaseViewModel

    private val calendar = Calendar.getInstance()
    private var selectedDate: Calendar? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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

        databaseViewModel = ViewModelProvider(this)[DatabaseViewModel::class.java]

        setupCalendar()
        setupEventsList()
        setupNavigation()
        observeData()
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

    private fun observeData() {
        // Observe tables and items to update calendar when data changes
        databaseViewModel.allTables.observe(viewLifecycleOwner) { tables ->
            updateCalendarDisplay()
        }

        databaseViewModel.allItems.observe(viewLifecycleOwner) { items ->
            updateCalendarDisplay()
        }
    }

    private fun updateCalendarDisplay() {
        // Update month/year header
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvMonthYear.text = monthFormat.format(calendar.time)

        // Generate calendar days with events
        generateCalendarDaysWithEvents()

        // Update selected date events
        selectedDate?.let { date ->
            updateSelectedDateEvents(date)
        }
    }

    private fun generateCalendarDaysWithEvents() {
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

        calendarAdapter.updateDays(days)
    }

    private fun getEventsForDate(date: Calendar): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val targetDateString = dateFormat.format(date.time)

        // Get all tables and their items from the database
        val tables = databaseViewModel.allTables.value ?: emptyList()
        val allItems = databaseViewModel.allItems.value ?: emptyList()

        tables.forEach { table ->
            // Parse table columns to find date fields
            val dateColumns = getDateColumnsFromTable(table)

            if (dateColumns.isNotEmpty()) {
                // Get items for this table
                val tableItems = allItems.filter { it.tableId == table.id }

                tableItems.forEach { item ->
                    // Parse item data to check for matching dates
                    val itemEvents = extractEventsFromItem(item, table, dateColumns, targetDateString)
                    events.addAll(itemEvents)
                }
            }
        }

        return events
    }

    private fun getDateColumnsFromTable(table: CustomTable): List<TableColumn> {
        val gson = Gson()
        val type = object : TypeToken<List<TableColumn>>() {}.type
        val columns: List<TableColumn> = try {
            gson.fromJson(table.columns, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        // Filter columns that are date-related
        return columns.filter {
            it.type == ColumnType.DATE ||
                    it.type == ColumnType.DATETIME ||
                    it.type == ColumnType.TIME
        }
    }

    private fun extractEventsFromItem(
        item: TableItem,
        table: CustomTable,
        dateColumns: List<TableColumn>,
        targetDateString: String
    ): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()

        // Parse item data
        val gson = Gson()
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val itemData: Map<String, Any> = try {
            gson.fromJson(item.data, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        dateColumns.forEach { dateColumn ->
            val dateValue = itemData[dateColumn.name]?.toString()
            if (dateValue != null) {
                // Check calendar options for this date field - fix type casting
                val calendarOptionsKey = "${dateColumn.name}_calendarOptions"
                val calendarOptionsValue = itemData[calendarOptionsKey]

                val calendarOptions = when (calendarOptionsValue) {
                    is Map<*, *> -> calendarOptionsValue
                    is String -> {
                        // If it's a JSON string, parse it
                        try {
                            val gson = Gson()
                            val mapType = object : TypeToken<Map<String, Any>>() {}.type
                            gson.fromJson<Map<String, Any>>(calendarOptionsValue, mapType)
                        } catch (e: Exception) {
                            emptyMap<String, Any>()
                        }
                    }
                    else -> emptyMap<String, Any>()
                }

                val showInCalendar = calendarOptions["showInCalendar"]?.toString()?.toBoolean()
                    ?: dateColumn.options["showInCalendar"]?.toString()?.toBoolean()
                    ?: true

                if (!showInCalendar) return@forEach // Skip if user disabled calendar for this field

                val isRecurring = calendarOptions["isRecurring"]?.toString()?.toBoolean()
                    ?: dateColumn.options["isRecurring"]?.toString()?.toBoolean()
                    ?: false

                val recurrenceFrequency = calendarOptions["recurrenceFrequency"]?.toString()
                    ?: dateColumn.options["recurrenceFrequency"]?.toString()
                    ?: "Monthly"

                // Parse the stored date
                val storedDate = try {
                    dateFormat.parse(dateValue)
                } catch (e: Exception) {
                    null
                }

                if (storedDate != null) {
                    val storedCalendar = Calendar.getInstance().apply { time = storedDate }
                    val targetCalendar = Calendar.getInstance().apply { time = dateFormat.parse(targetDateString)!! }

                    // Check if target date matches stored date (exact match)
                    if (dateValue.startsWith(targetDateString)) {
                        events.add(createCalendarEvent(item, table, dateColumn, itemData, EventType.TABLE_DATE))
                    }
                    // Check for recurring events (predicted)
                    else if (isRecurring && isRecurringMatch(storedCalendar, targetCalendar, recurrenceFrequency)) {
                        events.add(createCalendarEvent(item, table, dateColumn, itemData, EventType.TABLE_RECURRING))
                    }
                }
            }
        }

        return events
    }

    private fun isRecurringMatch(storedDate: Calendar, targetDate: Calendar, frequency: String): Boolean {
        // Only generate future recurring events, not past ones
        if (targetDate.before(storedDate)) return false

        return when (frequency) {
            "Monthly" -> {
                storedDate.get(Calendar.DAY_OF_MONTH) == targetDate.get(Calendar.DAY_OF_MONTH)
            }
            "Weekly" -> {
                val daysDiff = ((targetDate.timeInMillis - storedDate.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                daysDiff % 7 == 0
            }
            "Yearly" -> {
                storedDate.get(Calendar.DAY_OF_MONTH) == targetDate.get(Calendar.DAY_OF_MONTH) &&
                        storedDate.get(Calendar.MONTH) == targetDate.get(Calendar.MONTH)
            }
            "Every 2 weeks" -> {
                val daysDiff = ((targetDate.timeInMillis - storedDate.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                daysDiff % 14 == 0
            }
            "Every 3 months" -> {
                storedDate.get(Calendar.DAY_OF_MONTH) == targetDate.get(Calendar.DAY_OF_MONTH) &&
                        (targetDate.get(Calendar.MONTH) - storedDate.get(Calendar.MONTH)) % 3 == 0
            }
            "Every 6 months" -> {
                storedDate.get(Calendar.DAY_OF_MONTH) == targetDate.get(Calendar.DAY_OF_MONTH) &&
                        (targetDate.get(Calendar.MONTH) - storedDate.get(Calendar.MONTH)) % 6 == 0
            }
            else -> false
        }
    }

    private fun createCalendarEvent(
        item: TableItem,
        table: CustomTable,
        dateColumn: TableColumn,
        itemData: Map<String, Any>,
        eventType: EventType
    ): CalendarEvent {
        val eventTitle = generateEventTitle(itemData, table)
        val finalEventType = if (eventType == EventType.TABLE_RECURRING) {
            EventType.TABLE_RECURRING
        } else {
            getEventTypeForTable(table, dateColumn)
        }

        return CalendarEvent(
            id = "${item.id}_${dateColumn.name}",
            title = eventTitle,
            type = finalEventType,
            description = dateColumn.name,
            relatedId = item.id,
            tableId = table.id,
            tableName = table.name,
            tableIcon = table.icon
        )
    }

    private fun generateEventTitle(itemData: Map<String, Any>, table: CustomTable): String {
        // Try to find a good title field from the item data
        val titleFields = listOf("name", "title", "description", "subject", "item", "account")

        for (field in titleFields) {
            val value = itemData[field]?.toString()
            if (!value.isNullOrBlank()) {
                return value
            }
        }

        // Fallback to table name + first field value
        val firstValue = itemData.values.firstOrNull()?.toString()
        return if (!firstValue.isNullOrBlank()) {
            "${table.icon} $firstValue"
        } else {
            "${table.icon} ${table.name}"
        }
    }

    private fun getEventTypeForTable(table: CustomTable, dateColumn: TableColumn): EventType {
        val tableName = table.name.lowercase()
        val columnName = dateColumn.name.lowercase()

        return when {
            tableName.contains("subscription") -> {
                when {
                    columnName.contains("due") || columnName.contains("next") -> EventType.TABLE_SUBSCRIPTION
                    else -> EventType.TABLE_DATE
                }
            }
            tableName.contains("bond") || tableName.contains("investment") -> EventType.TABLE_INVESTMENT
            tableName.contains("meeting") || tableName.contains("appointment") -> EventType.TABLE_MEETING
            tableName.contains("task") || tableName.contains("todo") -> EventType.TABLE_TASK
            else -> EventType.TABLE_DATE
        }
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

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun handleEventClick(event: CalendarEvent) {
        // Navigate based on event type and related data
        when {
            event.relatedId != null && event.tableId != null -> {
                // Navigate to edit the specific table item
                val intent = Intent(requireContext(), ItemEditorActivity::class.java).apply {
                    putExtra("table_id", event.tableId)
                    putExtra("item_id", event.relatedId)
                }
                startActivity(intent)
            }
            event.tableId != null -> {
                // Navigate to the table view
                val intent = Intent(requireContext(), TableViewActivity::class.java).apply {
                    putExtra("table_id", event.tableId)
                }
                startActivity(intent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}