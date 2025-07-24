package com.aksara.aksaranotes.ui.database.builder

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aksara.aksaranotes.R
import com.aksara.aksaranotes.databinding.ActivityTableBuilderBinding
import com.aksara.aksaranotes.data.models.TableColumn
import com.aksara.aksaranotes.data.models.ColumnType
import com.aksara.aksaranotes.data.database.entities.CustomTable
import com.aksara.aksaranotes.data.templates.TableTemplates
import com.aksara.aksaranotes.ui.database.DatabaseViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.util.UUID

class TableBuilderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTableBuilderBinding
    private lateinit var viewModel: DatabaseViewModel
    private lateinit var columnAdapter: ColumnBuilderAdapter

    private val columns = mutableListOf<TableColumn>()
    private var selectedIcon = "📄"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("TableBuilder", "========== onCreate() called ==========")

        try {
            super.onCreate(savedInstanceState)
            Log.d("TableBuilder", "super.onCreate() completed")

            binding = ActivityTableBuilderBinding.inflate(layoutInflater)
            Log.d("TableBuilder", "Binding inflated")

            setContentView(binding.root)
            Log.d("TableBuilder", "setContentView completed")

            viewModel = ViewModelProvider(this)[DatabaseViewModel::class.java]
            Log.d("TableBuilder", "ViewModel created")

            setupToolbar()
            Log.d("TableBuilder", "Toolbar setup completed")

            setupRecyclerView()
            Log.d("TableBuilder", "RecyclerView setup completed")

            setupUI()
            Log.d("TableBuilder", "UI setup completed")

            loadTable()
            Log.d("TableBuilder", "loadTable() called")

        } catch (e: Exception) {
            Log.e("TableBuilder", "Error in onCreate()", e)
            throw e
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Table Builder"
    }

    private fun setupRecyclerView() {
        columnAdapter = ColumnBuilderAdapter(
            onColumnEdit = { column -> editColumn(column) },
            onColumnDelete = { column -> deleteColumn(column) }
        )

        binding.rvColumns.apply {
            adapter = columnAdapter
            layoutManager = LinearLayoutManager(this@TableBuilderActivity)
        }
    }

    private fun setupUI() {
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnCreateTable.setOnClickListener { createTable() }
        binding.btnAddColumn.setOnClickListener { showAddColumnDialog() }
        binding.btnChooseIcon.setOnClickListener { showIconPicker() }
        binding.tvSelectedIcon.setOnClickListener { showIconPicker() }

        updateColumnsDisplay()
    }

    private fun loadTable() {
        val template = intent.getStringExtra("template")
        val editTableId = intent.getStringExtra("edit_table_id")

        Log.d("TableBuilder", "loadTable called - template: $template, editTableId: $editTableId")

        when {
            editTableId != null -> {
                Log.d("TableBuilder", "Loading existing table with ID: $editTableId")
                supportActionBar?.title = "Edit Table"
                binding.btnCreateTable.text = "Update Table"

                lifecycleScope.launch {
                    try {
                        val existingTable = viewModel.getTableById(editTableId)
                        Log.d("TableBuilder", "Found table: ${existingTable?.name}")

                        existingTable?.let { table ->
                            Log.d("TableBuilder", "Table data - name: ${table.name}, columns: ${table.columns}")

                            // Update UI on main thread
                            runOnUiThread {
                                binding.etTableName.setText(table.name)
                                binding.etTableDescription.setText(table.description)
                                selectedIcon = table.icon
                                binding.tvSelectedIcon.text = selectedIcon
                                Log.d("TableBuilder", "UI updated with table data")
                            }

                            // Parse existing columns
                            val gson = Gson()
                            val type = object : TypeToken<List<TableColumn>>() {}.type
                            val existingColumns: List<TableColumn> = try {
                                gson.fromJson(table.columns, type) ?: emptyList()
                            } catch (e: Exception) {
                                Log.e("TableBuilder", "Error parsing columns: ${e.message}")
                                emptyList()
                            }

                            Log.d("TableBuilder", "Parsed ${existingColumns.size} columns")

                            // Update columns on main thread
                            runOnUiThread {
                                columns.clear()
                                columns.addAll(existingColumns)
                                updateColumnsDisplay()
                                Log.d("TableBuilder", "Columns updated in UI")
                            }
                        } ?: run {
                            Log.w("TableBuilder", "Table not found with ID: $editTableId")
                            runOnUiThread {
                                Toast.makeText(this@TableBuilderActivity, "Table not found", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("TableBuilder", "Error loading table", e)
                        runOnUiThread {
                            Toast.makeText(this@TableBuilderActivity, "Error loading table: ${e.message}", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
            }
            template != null -> {
                Log.d("TableBuilder", "Loading template: $template")
                val (tableTemplate, templateColumns) = TableTemplates.getTemplate(template)

                binding.etTableName.setText(tableTemplate.name)
                binding.etTableDescription.setText(tableTemplate.description)
                selectedIcon = tableTemplate.icon
                binding.tvSelectedIcon.text = selectedIcon

                columns.clear()
                columns.addAll(templateColumns)
                updateColumnsDisplay()
                Log.d("TableBuilder", "Template loaded successfully")
            }
            else -> {
                Log.d("TableBuilder", "Setting up blank table")
                setupBlankTable()
            }
        }
    }

    private fun setupBlankTable() {
        binding.etTableName.setText("")
        binding.etTableDescription.setText("")
        selectedIcon = "📄"
        binding.tvSelectedIcon.text = selectedIcon
        columns.clear()
        updateColumnsDisplay()
    }

    private fun showAddColumnDialog() {
        showColumnDialog(null)
    }

    private fun editColumn(column: TableColumn) {
        showColumnDialog(column)
    }

    private fun deleteColumn(column: TableColumn) {
        AlertDialog.Builder(this)
            .setTitle("Delete Column")
            .setMessage("Are you sure you want to delete '${column.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                columns.remove(column)
                updateColumnsDisplay()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showColumnDialog(existingColumn: TableColumn?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_column_builder, null)

        // Find views (using your existing IDs)
        val etColumnName = dialogView.findViewById<EditText>(R.id.et_column_name)
        val spinnerColumnType = dialogView.findViewById<Spinner>(R.id.spinner_column_type)
        val cbRequired = dialogView.findViewById<CheckBox>(R.id.cb_required)
        val etDefaultValue = dialogView.findViewById<EditText>(R.id.et_default_value)

        // Try to find the options layout (will be null if using your original layout)
        val layoutOptions = dialogView.findViewById<LinearLayout>(R.id.layout_options)

        // Setup column type spinner
        val columnTypes = ColumnType.values()
        val adapter = object : ArrayAdapter<ColumnType>(this, android.R.layout.simple_spinner_item, columnTypes) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view as TextView
                textView.text = "${columnTypes[position].icon} ${columnTypes[position].displayName}"
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view as TextView
                textView.text = "${columnTypes[position].icon} ${columnTypes[position].displayName}"
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerColumnType.adapter = adapter

        // Populate existing data if editing
        existingColumn?.let { column ->
            etColumnName.setText(column.name)
            spinnerColumnType.setSelection(columnTypes.indexOf(column.type))
            cbRequired.isChecked = column.required
            etDefaultValue.setText(column.defaultValue)
        }

        // Handle column type changes (only if layoutOptions exists)
        spinnerColumnType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedType = columnTypes[position]
                updateDefaultValueInput(etDefaultValue, selectedType)

                // Only setup advanced options if the container exists
                layoutOptions?.let { container ->
                    setupColumnTypeOptions(container, selectedType, existingColumn?.options ?: emptyMap())
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Trigger initial setup
        val initialType = if (existingColumn != null) existingColumn.type else ColumnType.TEXT
        updateDefaultValueInput(etDefaultValue, initialType)
        layoutOptions?.let { container ->
            setupColumnTypeOptions(container, initialType, existingColumn?.options ?: emptyMap())
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existingColumn != null) "Edit Column" else "Add Column")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etColumnName.text.toString().trim()
                val type = columnTypes[spinnerColumnType.selectedItemPosition]
                val required = cbRequired.isChecked
                val defaultValue = etDefaultValue.text.toString().trim()

                // Only collect advanced options if container exists
                val options = layoutOptions?.let { collectColumnOptions(it, type) } ?: emptyMap()

                if (name.isEmpty()) {
                    Toast.makeText(this, "Column name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newColumn = TableColumn(
                    id = existingColumn?.id ?: UUID.randomUUID().toString(),
                    name = name,
                    type = type,
                    required = required,
                    defaultValue = defaultValue,
                    options = options
                )

                if (existingColumn != null) {
                    val index = columns.indexOf(existingColumn)
                    columns[index] = newColumn
                } else {
                    columns.add(newColumn)
                }

                updateColumnsDisplay()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun setupColumnTypeOptions(layout: LinearLayout, type: ColumnType, existingOptions: Map<String, Any>) {
        layout.removeAllViews()

        when (type) {
            ColumnType.SELECT -> {
                val optionsContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                }

                val headerText = TextView(this).apply {
                    text = "Dropdown Options (one per line):"
                    setPadding(0, 16, 0, 8)
                }
                optionsContainer.addView(headerText)

                val optionsEditText = EditText(this).apply {
                    hint = "Option 1\nOption 2\nOption 3"
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    minLines = 3
                    maxLines = 8

                    // Load existing options
                    val existingOptionsList = existingOptions["options"] as? List<*>
                    if (existingOptionsList != null) {
                        setText(existingOptionsList.joinToString("\n"))
                    }
                }
                optionsContainer.addView(optionsEditText)
                layout.addView(optionsContainer)
            }

            ColumnType.CURRENCY -> {
                val currencyLabel = TextView(this).apply {
                    text = "Currency Symbol: "
                    setPadding(0, 16, 0, 8)
                }
                layout.addView(currencyLabel)

                val currencySpinner = Spinner(this).apply {
                    adapter = ArrayAdapter(
                        this@TableBuilderActivity,
                        android.R.layout.simple_spinner_item,
                        arrayOf("$ USD", "€ EUR", "£ GBP", "¥ JPY", "Rp IDR")
                    ).also {
                        it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                }
                layout.addView(currencySpinner)
            }

            ColumnType.DATE, ColumnType.DATETIME, ColumnType.TIME -> {
                // Add calendar integration options for date fields
                val calendarLabel = TextView(this).apply {
                    text = "Calendar Integration:"
                    setPadding(0, 16, 0, 8)
                    setTextColor(getColor(android.R.color.black))
                }
                layout.addView(calendarLabel)

                val showInCalendarCheckbox = CheckBox(this).apply {
                    text = "📅 Show in Calendar"
                    isChecked = existingOptions["showInCalendar"] as? Boolean ?: true

                    setOnCheckedChangeListener { _, isChecked ->
                        val recurringCheckbox = layout.findViewWithTag<CheckBox>("recurring_checkbox")
                        val frequencySpinner = layout.findViewWithTag<Spinner>("frequency_spinner")
                        recurringCheckbox?.visibility = if (isChecked) View.VISIBLE else View.GONE
                        frequencySpinner?.visibility = if (isChecked && recurringCheckbox?.isChecked == true) View.VISIBLE else View.GONE
                    }
                }
                layout.addView(showInCalendarCheckbox)

                val recurringCheckbox = CheckBox(this).apply {
                    text = "🔄 Recurring Event"
                    tag = "recurring_checkbox"
                    isChecked = existingOptions["isRecurring"] as? Boolean ?: false
                    visibility = if (showInCalendarCheckbox.isChecked) View.VISIBLE else View.GONE

                    setOnCheckedChangeListener { _, isChecked ->
                        val frequencySpinner = layout.findViewWithTag<Spinner>("frequency_spinner")
                        frequencySpinner?.visibility = if (isChecked) View.VISIBLE else View.GONE
                    }
                }
                layout.addView(recurringCheckbox)

                val frequencySpinner = Spinner(this).apply {
                    tag = "frequency_spinner"
                    adapter = ArrayAdapter(
                        this@TableBuilderActivity,
                        android.R.layout.simple_spinner_item,
                        arrayOf("Monthly", "Weekly", "Yearly", "Every 2 weeks", "Every 3 months", "Every 6 months")
                    ).also {
                        it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }

                    // Set existing value
                    val currentFrequency = existingOptions["recurrenceFrequency"] as? String ?: "Monthly"
                    val frequencyOptions = arrayOf("Monthly", "Weekly", "Yearly", "Every 2 weeks", "Every 3 months", "Every 6 months")
                    setSelection(frequencyOptions.indexOf(currentFrequency).coerceAtLeast(0))

                    visibility = if (showInCalendarCheckbox.isChecked && recurringCheckbox.isChecked) View.VISIBLE else View.GONE
                }
                layout.addView(frequencySpinner)
            }

            else -> {
                // For now, only implement SELECT and CURRENCY
                // Other types can be added later
            }
        }
    }

    private fun updateDefaultValueInput(editText: EditText, type: ColumnType) {
        when (type) {
            ColumnType.NUMBER, ColumnType.CURRENCY -> {
                editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                editText.hint = "0.00"
            }
            ColumnType.EMAIL -> {
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                editText.hint = "example@email.com"
            }
            ColumnType.PHONE -> {
                editText.inputType = InputType.TYPE_CLASS_PHONE
                editText.hint = "+1234567890"
            }
            ColumnType.URL -> {
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
                editText.hint = "https://example.com"
            }
            ColumnType.BOOLEAN -> {
                editText.isEnabled = false
                editText.hint = "true/false (set via checkbox in forms)"
            }
            ColumnType.FORMULA -> {
                editText.isEnabled = false
                editText.hint = "Calculated automatically (configure formula above)"
            }
            else -> {
                editText.inputType = InputType.TYPE_CLASS_TEXT
                editText.hint = "Default value"
            }
        }
    }

    private fun collectColumnOptions(layout: LinearLayout, type: ColumnType): Map<String, Any> {
        val options = mutableMapOf<String, Any>()

        when (type) {
            ColumnType.SELECT -> {
                // Find the options EditText by searching through child views
                for (i in 0 until layout.childCount) {
                    val child = layout.getChildAt(i)
                    if (child is LinearLayout) {
                        for (j in 0 until child.childCount) {
                            val grandChild = child.getChildAt(j)
                            if (grandChild is EditText) {
                                val optionsList = grandChild.text.toString().split("\n")
                                    .filter { it.trim().isNotEmpty() }
                                options["options"] = optionsList
                                break
                            }
                        }
                    }
                }
            }

            ColumnType.CURRENCY -> {
                // Find the currency spinner
                for (i in 0 until layout.childCount) {
                    val child = layout.getChildAt(i)
                    if (child is Spinner) {
                        val currencySymbol = when (child.selectedItemPosition) {
                            1 -> "€"
                            2 -> "£"
                            3 -> "¥"
                            4 -> "Rp"
                            else -> "$"
                        }
                        options["currencySymbol"] = currencySymbol
                        break
                    }
                }
            }

            ColumnType.DATE, ColumnType.DATETIME, ColumnType.TIME -> {
                // Collect calendar options
                var showInCalendar = true
                var isRecurring = false
                var recurrenceFrequency = "Monthly"

                for (i in 0 until layout.childCount) {
                    val child = layout.getChildAt(i)
                    when (child) {
                        is CheckBox -> {
                            when (child.text.toString()) {
                                "📅 Show in Calendar" -> showInCalendar = child.isChecked
                                "🔄 Recurring Event" -> isRecurring = child.isChecked
                            }
                        }
                        is Spinner -> {
                            if (child.tag == "frequency_spinner") {
                                recurrenceFrequency = child.selectedItem?.toString() ?: "Monthly"
                            }
                        }
                    }
                }

                options["showInCalendar"] = showInCalendar
                options["isRecurring"] = isRecurring
                options["recurrenceFrequency"] = recurrenceFrequency
            }

            else -> {
                // No special options for basic types
            }
        }

        return options
    }

    private fun updateColumnsDisplay() {
        if (columns.isEmpty()) {
            binding.tvNoColumns.visibility = View.VISIBLE
            binding.rvColumns.visibility = View.GONE
        } else {
            binding.tvNoColumns.visibility = View.GONE
            binding.rvColumns.visibility = View.VISIBLE
            columnAdapter.submitList(columns.toList())
        }
    }

    private fun createTable() {
        Log.d("TableBuilder", "createTable called")
        val name = binding.etTableName.text.toString().trim()
        val description = binding.etTableDescription.text.toString().trim()
        val editTableId = intent.getStringExtra("edit_table_id")

        if (name.isEmpty()) {
            binding.etTableName.error = "Table name is required"
            return
        }

        if (columns.isEmpty()) {
            Toast.makeText(this, "Please add at least one column", Toast.LENGTH_SHORT).show()
            return
        }

        if (editTableId != null) {
            // Update existing table
            lifecycleScope.launch {
                val existingTable = viewModel.getTableById(editTableId)
                existingTable?.let { table ->
                    val updatedTable = table.copy(
                        name = name,
                        description = description,
                        icon = selectedIcon,
                        columns = Gson().toJson(columns),
                        updatedAt = System.currentTimeMillis()
                    )

                    viewModel.updateTable(updatedTable)
                    Toast.makeText(this@TableBuilderActivity, "Table '$name' updated!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            // Create new table
            val table = CustomTable(
                name = name,
                description = description,
                icon = selectedIcon,
                tableType = "data",
                columns = Gson().toJson(columns)
            )

            viewModel.insertTable(table)
            Toast.makeText(this, "Table '$name' created successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showIconPicker() {
        val icons = arrayOf(
            "📄", "🔐", "💰", "🧮", "📞", "📚", "🎬", "🏋️",
            "💡", "⭐", "🎯", "📊", "🎨", "🔧", "🏠", "🚗"
        )

        AlertDialog.Builder(this)
            .setTitle("Choose Icon")
            .setItems(icons) { _, which ->
                selectedIcon = icons[which]
                binding.tvSelectedIcon.text = selectedIcon
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}