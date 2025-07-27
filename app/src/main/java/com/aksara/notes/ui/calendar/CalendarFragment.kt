package com.aksara.notes.ui.calendar

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.aksara.notes.databinding.FragmentCalendarBinding
import com.aksara.notes.ui.database.DatabaseViewModel
import com.aksara.notes.ui.database.forms.FormEditorActivity
import com.aksara.notes.ui.database.view.DatasetViewActivity
import com.aksara.notes.data.database.entities.Dataset
import com.aksara.notes.data.database.entities.Form
import com.aksara.notes.data.database.entities.TableColumn
import com.aksara.notes.data.models.ColumnType
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
        // Observe datasets and forms to update calendar when data changes
        databaseViewModel.allDatasets.observe(viewLifecycleOwner) { datasets ->
            updateCalendarDisplay()
        }

        databaseViewModel.allForms.observe(viewLifecycleOwner) { forms ->
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

        // Get all datasets and their forms from the database
        val datasets = databaseViewModel.allDatasets.value ?: emptyList()
        val allForms = databaseViewModel.allForms.value ?: emptyList()

        datasets.forEach { dataset ->
            // Parse dataset columns to find date fields
            val dateColumns = getDateColumnsFromDataset(dataset)

            if (dateColumns.isNotEmpty()) {
                // Get forms for this dataset
                val datasetForms = allForms.filter { it.datasetId == dataset.id }

                datasetForms.forEach { form ->
                    // Parse form data to check for matching dates
                    val formEvents = extractEventsFromForm(form, dataset, dateColumns, targetDateString)
                    events.addAll(formEvents)
                }
            }
        }

        return events
    }

    private fun getDateColumnsFromDataset(dataset: Dataset): List<TableColumn> {
        // Access columns directly (no JSON parsing needed!)
        val columns = dataset.columns

        // Filter columns that are date-related
        return columns.filter {
            it.type == "DATE" ||
                    it.type == "DATETIME" ||
                    it.type == "TIME"
        }
    }

    private fun extractEventsFromForm(
        form: Form,
        dataset: Dataset,
        dateColumns: List<TableColumn>,
        targetDateString: String
    ): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()

        // Parse form data
        val gson = Gson()
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val formData: Map<String, Any> = try {
            gson.fromJson(form.data, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        dateColumns.forEach { dateColumn ->
            val dateValue = formData[dateColumn.name]?.toString()
            if (dateValue != null) {
                // Check calendar options for this date field - fix type casting
                val calendarOptionsKey = "${dateColumn.name}_calendarOptions"
                val calendarOptionsValue = formData[calendarOptionsKey]

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

                // Parse column options from JSON string
                val columnOptions = try {
                    if (dateColumn.options.isNotEmpty()) {
                        val gson = Gson()
                        val mapType = object : TypeToken<Map<String, Any>>() {}.type
                        gson.fromJson<Map<String, Any>>(dateColumn.options, mapType)
                    } else {
                        emptyMap<String, Any>()
                    }
                } catch (e: Exception) {
                    emptyMap<String, Any>()
                }

                val showInCalendar = calendarOptions["showInCalendar"]?.toString()?.toBoolean()
                    ?: columnOptions["showInCalendar"]?.toString()?.toBoolean()
                    ?: true

                if (!showInCalendar) return@forEach // Skip if user disabled calendar for this field

                val isRecurring = calendarOptions["isRecurring"]?.toString()?.toBoolean()
                    ?: columnOptions["isRecurring"]?.toString()?.toBoolean()
                    ?: false

                val recurrenceFrequency = calendarOptions["recurrenceFrequency"]?.toString()
                    ?: columnOptions["recurrenceFrequency"]?.toString()
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
                        events.add(createCalendarEvent(form, dataset, dateColumn, formData, EventType.TABLE_DATE))
                    }
                    // Check for recurring events (predicted)
                    else if (isRecurring && isRecurringMatch(storedCalendar, targetCalendar, recurrenceFrequency)) {
                        events.add(createCalendarEvent(form, dataset, dateColumn, formData, EventType.TABLE_RECURRING))
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
        form: Form,
        dataset: Dataset,
        dateColumn: TableColumn,
        formData: Map<String, Any>,
        eventType: EventType
    ): CalendarEvent {
        val eventTitle = generateEventTitle(formData, dataset)
        val finalEventType = if (eventType == EventType.TABLE_RECURRING) {
            EventType.TABLE_RECURRING
        } else {
            getEventTypeForDataset(dataset, dateColumn)
        }

        return CalendarEvent(
            id = "${form.id}_${dateColumn.name}",
            title = eventTitle,
            type = finalEventType,
            description = dateColumn.name,
            relatedId = form.id,
            tableId = dataset.id,
            tableName = dataset.name,
            tableIcon = dataset.icon
        )
    }

    private fun generateEventTitle(formData: Map<String, Any>, dataset: Dataset): String {
        // Try to find a good title field from the form data
        val titleFields = listOf("name", "title", "description", "subject", "item", "account")

        for (field in titleFields) {
            val value = formData[field]?.toString()
            if (!value.isNullOrBlank()) {
                return value
            }
        }

        // Fallback to dataset name + first field value
        val firstValue = formData.values.firstOrNull()?.toString()
        return if (!firstValue.isNullOrBlank()) {
            "${dataset.icon} $firstValue"
        } else {
            "${dataset.icon} ${dataset.name}"
        }
    }

    private fun getEventTypeForDataset(dataset: Dataset, dateColumn: TableColumn): EventType {
        val datasetName = dataset.name.lowercase()
        val columnName = dateColumn.name.lowercase()

        return when {
            datasetName.contains("subscription") -> {
                when {
                    columnName.contains("due") || columnName.contains("next") -> EventType.TABLE_SUBSCRIPTION
                    else -> EventType.TABLE_DATE
                }
            }
            datasetName.contains("bond") || datasetName.contains("investment") -> EventType.TABLE_INVESTMENT
            datasetName.contains("meeting") || datasetName.contains("appointment") -> EventType.TABLE_MEETING
            datasetName.contains("task") || datasetName.contains("todo") -> EventType.TABLE_TASK
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
                // Navigate to edit the specific form
                val intent = Intent(requireContext(), FormEditorActivity::class.java).apply {
                    putExtra("dataset_id", event.tableId)
                    putExtra("form_id", event.relatedId)
                }
                startActivity(intent)
            }
            event.tableId != null -> {
                // Navigate to the dataset view
                val intent = Intent(requireContext(), DatasetViewActivity::class.java).apply {
                    putExtra("dataset_id", event.tableId)
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