package com.aksara.notes.ui.database.builder

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
import com.aksara.notes.R
import com.aksara.notes.databinding.ActivityDatasetBuilderBinding
import com.aksara.notes.data.models.ColumnType
import com.google.android.material.color.MaterialColors
import com.aksara.notes.data.database.entities.Dataset
import com.aksara.notes.data.database.entities.TableColumn
import com.aksara.notes.data.templates.DatasetTemplates
import com.aksara.notes.ui.database.DatabaseViewModel
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.UUID

// manage Dataset Structure
class DatasetBuilderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDatasetBuilderBinding
    private lateinit var viewModel: DatabaseViewModel
    private lateinit var columnAdapter: ColumnBuilderAdapter

    private val columns = mutableListOf<TableColumn>()
    private var selectedIcon = "📄"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("DatasetBuilder", "========== onCreate() called ==========")

        try {
            super.onCreate(savedInstanceState)
            Log.d("DatasetBuilder", "super.onCreate() completed")

            binding = ActivityDatasetBuilderBinding.inflate(layoutInflater)
            Log.d("DatasetBuilder", "Binding inflated")

            setContentView(binding.root)
            Log.d("DatasetBuilder", "setContentView completed")

            viewModel = ViewModelProvider(this)[DatabaseViewModel::class.java]
            Log.d("DatasetBuilder", "ViewModel created")

            setupToolbar()
            Log.d("DatasetBuilder", "Toolbar setup completed")

            setupRecyclerView()
            Log.d("DatasetBuilder", "RecyclerView setup completed")

            setupUI()
            Log.d("DatasetBuilder", "UI setup completed")

            loadDataset()
            Log.d("DatasetBuilder", "loadDataset() called")

        } catch (e: Exception) {
            Log.e("DatasetBuilder", "Error in onCreate()", e)
            throw e
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Dataset Builder"
    }

    private fun setupRecyclerView() {
        columnAdapter = ColumnBuilderAdapter(
            onColumnEdit = { column -> editColumn(column) },
            onColumnDelete = { column -> deleteColumn(column) }
        )

        binding.rvColumns.apply {
            adapter = columnAdapter
            layoutManager = LinearLayoutManager(this@DatasetBuilderActivity)
        }
    }

    private fun setupUI() {
        binding.btnCancel.setOnClickListener { finish() }
        binding.btnCreateTable.setOnClickListener { createDataset() }
        binding.btnAddColumn.setOnClickListener { showAddColumnDialog() }
        binding.btnChooseIcon.setOnClickListener { showIconPicker() }
        binding.tvSelectedIcon.setOnClickListener { showIconPicker() }

        updateColumnsDisplay()
    }

    private fun loadDataset() {
        val template = intent.getStringExtra("template")
        val editDatasetId = intent.getStringExtra("edit_dataset_id")

        Log.d("DatasetBuilder", "loadDataset called - template: $template, editDatasetId: $editDatasetId")

        when {
            editDatasetId != null -> {
                Log.d("DatasetBuilder", "Loading existing dataset with ID: $editDatasetId")
                supportActionBar?.title = getString(R.string.edit_dataset_title)
                binding.btnCreateTable.text = getString(R.string.update_dataset_button)

                lifecycleScope.launch {
                    try {
                        val existingDataset = viewModel.getDatasetById(editDatasetId)
                        Log.d("DatasetBuilder", "Found dataset: ${existingDataset?.name}")

                        existingDataset?.let { dataset ->
                            Log.d("DatasetBuilder", "Dataset data - name: ${dataset.name}, columns: ${dataset.columns}")

                            // Update UI on main thread
                            runOnUiThread {
                                binding.etTableName.setText(dataset.name)
                                binding.etTableDescription.setText(dataset.description)
                                selectedIcon = dataset.icon
                                binding.tvSelectedIcon.text = selectedIcon
                                Log.d("DatasetBuilder", "UI updated with dataset data")
                            }

                            // Access existing columns directly (no JSON parsing needed!)
                            val existingColumns = dataset.columns

                            Log.d("DatasetBuilder", "Found ${existingColumns.size} columns")

                            // Update columns on main thread
                            runOnUiThread {
                                columns.clear()
                                columns.addAll(existingColumns.toList()) // Convert RealmList to List
                                updateColumnsDisplay()
                                Log.d("DatasetBuilder", "Columns updated in UI")
                            }
                        } ?: run {
                            Log.w("DatasetBuilder", "Dataset not found with ID: $editDatasetId")
                            runOnUiThread {
                                Toast.makeText(this@DatasetBuilderActivity, getString(R.string.dataset_not_found_toast), Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DatasetBuilder", "Error loading dataset", e)
                        runOnUiThread {
                            Toast.makeText(this@DatasetBuilderActivity, getString(R.string.error_loading_dataset_toast, e.message), Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
            }
            template != null -> {
                Log.d("DatasetBuilder", "Loading template: $template")
                val datasetTemplate = DatasetTemplates.getTemplate(template)

                binding.etTableName.setText(datasetTemplate.name)
                binding.etTableDescription.setText(datasetTemplate.description)
                selectedIcon = datasetTemplate.icon
                binding.tvSelectedIcon.text = selectedIcon

                columns.clear()
                columns.addAll(datasetTemplate.columns.toList()) // Convert RealmList to List
                updateColumnsDisplay()
                Log.d("DatasetBuilder", "Template loaded successfully")
            }
            else -> {
                Log.d("DatasetBuilder", "Setting up blank dataset")
                setupBlankDataset()
            }
        }
    }

    private fun setupBlankDataset() {
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
            .setTitle(getString(R.string.delete_column_title))
            .setMessage(getString(R.string.delete_column_message, column.name))
            .setPositiveButton("Delete") { _, _ ->
                columns.remove(column)
                updateColumnsDisplay()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupColumnTypeOptions(layout: LinearLayout, type: ColumnType, existingOptions: Map<String, Any>) {
        layout.removeAllViews()

        when (type) {
            ColumnType.FORMULA -> {
                // Formula input section
                val formulaContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                }

                // Header
                val headerText = TextView(this).apply {
                    text = "Formula Expression"
                    textSize = 16f
                    setTextColor(MaterialColors.getColor(this@DatasetBuilderActivity, com.google.android.material.R.attr.colorOnSurface, getColor(R.color.on_surface)))
                    setPadding(0, 16, 0, 8)
                }
                formulaContainer.addView(headerText)

                // Formula input
                val formulaEditText = EditText(this).apply {
                    hint = "e.g., {Price} * {Quantity} or {Total} * 0.1"
                    inputType = InputType.TYPE_CLASS_TEXT
                    minLines = 2
                    maxLines = 4

                    // Load existing formula
                    val existingFormula = existingOptions["formula"] as? String ?: ""
                    setText(existingFormula)
                }
                formulaContainer.addView(formulaEditText)

                // Help text
                val helpText = TextView(this).apply {
                    text = """
                    Use {ColumnName} to reference other fields.
                    Supported operations: +, -, *, /, (), %
                    Examples:
                    • {Price} * {Quantity}
                    • ({Subtotal} + {Tax}) * 0.9
                    • {Income} * 15%
                """.trimIndent()
                    textSize = 12f
                    setTextColor(MaterialColors.getColor(this@DatasetBuilderActivity, com.google.android.material.R.attr.colorOnSurfaceVariant, getColor(R.color.on_surface_variant)))
                    setPadding(0, 8, 0, 0)
                }
                formulaContainer.addView(helpText)

                // Format as currency checkbox
                val formatAsCurrencyCheckbox = CheckBox(this).apply {
                    text = "💰 Format result as currency"
                    isChecked = existingOptions["formatAsCurrency"] as? Boolean ?: false
                }
                formulaContainer.addView(formatAsCurrencyCheckbox)

                layout.addView(formulaContainer)
            }
            ColumnType.SELECT -> {
                val optionsContainer = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                }

                val headerText = TextView(this).apply {
                    text = getString(R.string.column_options_dropdown_header)
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
                        this@DatasetBuilderActivity,
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
                    setTextColor(MaterialColors.getColor(this@DatasetBuilderActivity, com.google.android.material.R.attr.colorOnSurface, getColor(R.color.on_surface)))
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
                        this@DatasetBuilderActivity,
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
            ColumnType.FORMULA -> {
                editText.inputType = InputType.TYPE_CLASS_TEXT
                editText.hint = "e.g., {Price} * {Quantity} or {Total} * 0.1"
            }
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
            else -> {
                editText.inputType = InputType.TYPE_CLASS_TEXT
                editText.hint = "Default value"
            }
        }
    }

    private fun showColumnDialog(existingColumn: TableColumn?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_column_builder, null)

        val etColumnName = dialogView.findViewById<EditText>(R.id.et_column_name)
        val spinnerColumnType = dialogView.findViewById<Spinner>(R.id.spinner_column_type)
        val cbRequired = dialogView.findViewById<CheckBox>(R.id.cb_required)
        val etDefaultValue = dialogView.findViewById<EditText>(R.id.et_default_value)
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

        // Populate existing data
        existingColumn?.let { column ->
            etColumnName.setText(column.name)
            // Convert string type back to enum for spinner selection
            val columnType = try {
                ColumnType.valueOf(column.type)
            } catch (e: Exception) {
                ColumnType.TEXT
            }
            spinnerColumnType.setSelection(columnTypes.indexOf(columnType))
            cbRequired.isChecked = column.required
            etDefaultValue.setText(column.defaultValue)
        }

        // Handle column type changes
        spinnerColumnType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedType = columnTypes[position]
                updateDefaultValueInput(etDefaultValue, selectedType)

                layoutOptions?.let { container ->
                    val existingOptions = existingColumn?.let { column ->
                        try {
                            if (column.options.isNotEmpty()) {
                                val gson = Gson()
                                val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                                gson.fromJson<Map<String, Any>>(column.options, mapType)
                            } else {
                                emptyMap<String, Any>()
                            }
                        } catch (e: Exception) {
                            emptyMap<String, Any>()
                        }
                    } ?: emptyMap()
                    setupColumnTypeOptions(container, selectedType, existingOptions)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Trigger initial setup
        val initialType = if (existingColumn != null) {
            try {
                ColumnType.valueOf(existingColumn.type)
            } catch (e: Exception) {
                ColumnType.TEXT
            }
        } else {
            ColumnType.TEXT
        }
        updateDefaultValueInput(etDefaultValue, initialType)
        layoutOptions?.let { container ->
            val existingOptions = existingColumn?.let { column ->
                try {
                    if (column.options.isNotEmpty()) {
                        val gson = Gson()
                        val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                        gson.fromJson<Map<String, Any>>(column.options, mapType)
                    } else {
                        emptyMap<String, Any>()
                    }
                } catch (e: Exception) {
                    emptyMap<String, Any>()
                }
            } ?: emptyMap()
            setupColumnTypeOptions(container, initialType, existingOptions)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existingColumn != null) getString(R.string.edit_column_title) else getString(R.string.add_column_title))
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etColumnName.text.toString().trim()
                val type = columnTypes[spinnerColumnType.selectedItemPosition]
                val required = cbRequired.isChecked
                val defaultValue = etDefaultValue.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this, getString(R.string.column_name_required_toast), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Collect additional options for non-formula columns
                val options = if (type != ColumnType.FORMULA) {
                    layoutOptions?.let { collectColumnOptions(it, type) } ?: emptyMap()
                } else {
                    emptyMap() // For formula columns, formula goes in defaultValue
                }

                val newColumn = TableColumn().apply {
                    id = existingColumn?.id ?: UUID.randomUUID().toString()
                    this.name = name
                    this.type = type.name
                    this.required = required
                    this.defaultValue = defaultValue // Formula stored here for FORMULA columns
                    this.options = Gson().toJson(options) // Store as JSON string
                    this.displayName = type.displayName
                    this.icon = type.icon
                }

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

    private fun collectColumnOptions(layout: LinearLayout, type: ColumnType): Map<String, Any> {
        val options = mutableMapOf<String, Any>()

        when (type) {
            ColumnType.FORMULA -> {
                // Find the formula EditText and format checkbox
                for (i in 0 until layout.childCount) {
                    val child = layout.getChildAt(i)
                    if (child is LinearLayout) {
                        for (j in 0 until child.childCount) {
                            val grandChild = child.getChildAt(j)
                            when (grandChild) {
                                is EditText -> {
                                    val formula = grandChild.text.toString().trim()
                                    if (formula.isNotEmpty()) {
                                        options["formula"] = formula
                                        Log.d("DatasetBuilder", "Storing formula: '$formula'")
                                    }
                                }
                                is CheckBox -> {
                                    if (grandChild.text.contains("currency")) {
                                        options["formatAsCurrency"] = grandChild.isChecked
                                    }
                                }
                            }
                        }
                    }
                }
            }
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

    private fun createDataset() {
        Log.d("DatasetBuilder", "createDataset called")
        val name = binding.etTableName.text.toString().trim()
        val description = binding.etTableDescription.text.toString().trim()
        val editDatasetId = intent.getStringExtra("edit_dataset_id")

        if (name.isEmpty()) {
            binding.etTableName.error = getString(R.string.dataset_name_required_error)
            return
        }

        if (columns.isEmpty()) {
            Toast.makeText(this, getString(R.string.add_at_least_one_column), Toast.LENGTH_SHORT).show()
            return
        }

        if (editDatasetId != null) {
            // Update existing dataset
            lifecycleScope.launch {
                val existingDataset = viewModel.getDatasetById(editDatasetId)
                Log.d("BUILDER-UPDATE", "Updating dataset with ${columns.size} columns")
                columns.forEach { column ->
                    Log.d("BUILDER-UPDATE", "Column to update: ${column.name} (${column.type}) - ${column.id}")
                }
                existingDataset?.let { dataset ->
                    // Pass the data to DAO and let it handle the Realm object creation
                    Log.d("BUILDER-UPDATE", "Calling updateDataset with ${columns.size} columns")
                    viewModel.updateDataset(dataset.id, name, description, selectedIcon, dataset.datasetType, columns, dataset.createdAt)
                    Toast.makeText(this@DatasetBuilderActivity, getString(R.string.dataset_updated_toast, name), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        } else {
            // Create new dataset
            Log.d("DatasetBuilder", "Creating dataset with ${columns.size} columns")
            columns.forEach { column ->
                Log.d("DatasetBuilder", "Column: ${column.name} (${column.type}) - required: ${column.required}")
            }

            viewModel.insertDataset(name, description, selectedIcon, "data", columns)
            Toast.makeText(this, getString(R.string.dataset_created_toast, name), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showIconPicker() {
        val icons = arrayOf(
            "📄", "🔐", "💰", "🧮", "📞", "📚", "🎬", "🏋️",
            "💡", "⭐", "🎯", "📊", "🎨", "🔧", "🏠", "🚗"
        )

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_icon_title))
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