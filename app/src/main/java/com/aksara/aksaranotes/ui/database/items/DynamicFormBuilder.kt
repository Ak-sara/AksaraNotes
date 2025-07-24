package com.aksara.aksaranotes.ui.database.items

import android.app.DatePickerDialog
import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import com.aksara.aksaranotes.data.models.TableColumn
import com.aksara.aksaranotes.data.models.ColumnType
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class DynamicFormBuilder(
    private val context: Context,
    private val parentLayout: LinearLayout
) {
    private val formFields = mutableMapOf<String, Any>() // Store actual input views
    private val formulaFields = mutableMapOf<String, TextView>() // Store formula display views
    private val formulaValues = mutableMapOf<String, Double>() // Store raw calculated values
    private val columnsList = mutableListOf<TableColumn>() // Store all columns for formula calculation
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Locale-aware formatters
    private val currentLocale = Locale.getDefault()
    private val numberFormat = NumberFormat.getNumberInstance(currentLocale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    private val decimalFormat = numberFormat as DecimalFormat
    private val decimalSeparator = decimalFormat.decimalFormatSymbols.decimalSeparator
    private val groupingSeparator = decimalFormat.decimalFormatSymbols.groupingSeparator

    init {
        // Debug locale info on initialization
        debugLocaleInfo()
    }

    // Locale-aware currency formatter
    private fun getCurrencyFormatter(currencyCode: String): NumberFormat {
        return NumberFormat.getCurrencyInstance(currentLocale).apply {
            try {
                currency = Currency.getInstance(currencyCode)
            } catch (e: Exception) {
                // Keep default currency if code not supported
            }
        }
    }

    // Safe number parsing that respects locale
    private fun parseNumberSafely(text: String): Double {
        if (text.isBlank()) return 0.0

        Log.d("DynamicForm", "parseNumberSafely input: '$text'")

        return try {
            // Method 1: Try parsing with current locale
            val result1 = numberFormat.parse(text.trim())?.toDouble() ?: 0.0
            Log.d("DynamicForm", "Locale parsing result: $result1")
            result1
        } catch (e: ParseException) {
            Log.d("DynamicForm", "Locale parsing failed: ${e.message}")
            try {
                // Method 2: Normalize to US format then parse
                val normalized = text
                    .replace(groupingSeparator.toString(), "") // Remove thousands separators
                    .replace(decimalSeparator.toString(), ".") // Convert decimal to dot
                    .replace(Regex("[^0-9.-]"), "") // Remove non-numeric chars except dot and minus

                Log.d("DynamicForm", "Normalized text: '$normalized'")
                val result2 = normalized.toDoubleOrNull() ?: 0.0
                Log.d("DynamicForm", "Normalized parsing result: $result2")
                result2
            } catch (ex: Exception) {
                Log.e("DynamicForm", "All parsing methods failed: ${ex.message}")
                0.0
            }
        }
    }

    fun buildForm(columns: List<TableColumn>) {
        parentLayout.removeAllViews()
        formFields.clear()
        formulaFields.clear()
        formulaValues.clear() // Clear raw formula values
        columnsList.clear()
        columnsList.addAll(columns)

        // DEBUG: Print all columns and their details
        Log.d("DynamicForm", "=== ALL COLUMNS DEBUG ===")
        columns.forEach { column ->
            Log.d("DynamicForm", "Column: '${column.name}' | Type: ${column.type} | Required: ${column.required}")
            Log.d("DynamicForm", "Default Value: '${column.defaultValue}'")
            Log.d("DynamicForm", "Options keys: ${column.options.keys}")
            if (column.type == ColumnType.FORMULA) {
                Log.d("DynamicForm", "*** FORMULA COLUMN DETAILS ***")
                column.options.forEach { (key, value) ->
                    Log.d("DynamicForm", "  '$key' -> '$value' (${value?.javaClass?.simpleName})")
                }
            }
        }
        Log.d("DynamicForm", "=== END ALL COLUMNS ===")

        columns.forEach { column ->
            val fieldView = createFieldView(column)
            fieldView?.let {
                parentLayout.addView(it)
            }
        }

        // Debug: Print all field names
        Log.d("DynamicForm", "=== FORM FIELDS DEBUG ===")
        formFields.forEach { (fieldName, view) ->
            Log.d("DynamicForm", "Field: '$fieldName' -> ${view.javaClass.simpleName}")
        }
        formulaFields.forEach { (fieldName, view) ->
            Log.d("DynamicForm", "Formula Field: '$fieldName'")
        }
        Log.d("DynamicForm", "=== END FORM FIELDS ===")

        // Calculate initial formula values
        updateAllFormulas()
    }

    private fun createFieldView(column: TableColumn): View? {
        return when (column.type) {
            ColumnType.TEXT -> createTextInput(column)
            ColumnType.NUMBER -> createNumberInput(column)
            ColumnType.CURRENCY -> createCurrencyInput(column)
            ColumnType.EMAIL -> createEmailInput(column)
            ColumnType.PHONE -> createPhoneInput(column)
            ColumnType.URL -> createUrlInput(column)
            ColumnType.DATE -> createDateInput(column)
            ColumnType.DATETIME -> createDateTimeInput(column)
            ColumnType.TIME -> createTimeInput(column)
            ColumnType.BOOLEAN -> createBooleanInput(column)
            ColumnType.SELECT -> createSelectInput(column)
            ColumnType.RATING -> createRatingInput(column)
            ColumnType.COLOR -> createColorInput(column)
            ColumnType.FORMULA -> createFormulaInput(column)
        }
    }

    private fun createTextInput(column: TableColumn): View {
        val layout = TextInputLayout(context).apply {
            hint = column.name + if (column.required) " *" else ""
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
        }

        val editText = TextInputEditText(context).apply {
            setText(column.defaultValue)
            maxLines = if (column.name.contains("note") || column.name.contains("description")) 5 else 1

            // Add text watcher for formula updates
            addTextChangedListener(createFormulaUpdateWatcher())
        }

        layout.addView(editText)
        formFields[column.name] = editText
        return layout
    }

    private fun createNumberInput(column: TableColumn): View {
        val layout = TextInputLayout(context).apply {
            hint = column.name + if (column.required) " *" else ""
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
        }

        val editText = TextInputEditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(column.defaultValue)

            // Add text watcher for formula updates
            addTextChangedListener(createFormulaUpdateWatcher())
        }

        layout.addView(editText)
        formFields[column.name] = editText
        return layout
    }

    private fun createCurrencyTextWatcher(currencyCode: String, currencyLabel: TextView): TextWatcher {
        return object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                val inputText = s.toString()
                val number = parseNumberSafely(inputText)

                // Use locale-aware currency formatting
                val formatted = getCurrencyFormatter(currencyCode).format(number)
                currencyLabel.text = formatted

                updateAllFormulas()
            }
        }
    }

    private fun createCurrencyInput(column: TableColumn): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
        }

        val layout = TextInputLayout(context).apply {
            hint = column.name + if (column.required) " *" else ""
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }

        // Get currency symbol from column options
        val currencySymbol = column.options["currencySymbol"] as? String ?: "$"
        val currencyCode = when (currencySymbol) {
            "$" -> "USD"
            "€" -> "EUR"
            "£" -> "GBP"
            "¥" -> "JPY"
            "Rp" -> "IDR"
            else -> "USD"
        }

        val editText = TextInputEditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(column.defaultValue)
        }

        // Currency display label
        val currencyLabel = TextView(context).apply {
            text = getCurrencyFormatter(currencyCode).format(0.0)
            textSize = 14f
            setTextColor(context.getColor(android.R.color.darker_gray))
            setPadding(16, 8, 16, 0)
        }

        // Use locale-aware text watcher
        editText.addTextChangedListener(createCurrencyTextWatcher(currencyCode, currencyLabel))

        layout.addView(editText)
        container.addView(layout)
        container.addView(currencyLabel)

        formFields[column.name] = editText
        return container
    }

    private fun createEmailInput(column: TableColumn): View {
        val layout = TextInputLayout(context).apply {
            hint = column.name + if (column.required) " *" else ""
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
        }

        val editText = TextInputEditText(context).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(column.defaultValue)
        }

        layout.addView(editText)
        formFields[column.name] = editText
        return layout
    }

    private fun createPhoneInput(column: TableColumn): View {
        val layout = TextInputLayout(context).apply {
            hint = column.name + if (column.required) " *" else ""
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
        }

        val editText = TextInputEditText(context).apply {
            inputType = InputType.TYPE_CLASS_PHONE
            setText(column.defaultValue)
        }

        layout.addView(editText)
        formFields[column.name] = editText
        return layout
    }

    private fun createUrlInput(column: TableColumn): View {
        val layout = TextInputLayout(context).apply {
            hint = column.name + if (column.required) " *" else ""
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
        }

        val editText = TextInputEditText(context).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setText(column.defaultValue)
        }

        layout.addView(editText)
        formFields[column.name] = editText
        return layout
    }

    private fun createDateInput(column: TableColumn): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
        }

        val layout = TextInputLayout(context).apply {
            hint = column.name + if (column.required) " *" else ""
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }

        val editText = TextInputEditText(context).apply {
            isFocusable = false
            isClickable = true
            setText(if (column.defaultValue == "today") dateFormat.format(Date()) else column.defaultValue)

            setOnClickListener {
                showDatePicker { date ->
                    setText(dateFormat.format(date))
                    updateAllFormulas()
                }
            }
        }

        layout.addView(editText)
        container.addView(layout)

        // Add calendar options if this date field has calendar integration
        val hasCalendarOptions = column.options["showInCalendar"] != null ||
                column.options["isRecurring"] != null

        if (hasCalendarOptions) {
            container.addView(createCalendarOptionsSection(column))
        }

        formFields[column.name] = editText
        return container
    }

    private fun createCalendarOptionsSection(column: TableColumn): View {
        val calendarContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 8, 16, 0)
            setBackgroundColor(context.getColor(android.R.color.background_light))
        }

        // Calendar toggle
        val showInCalendarCheckbox = CheckBox(context).apply {
            text = "📅 Show in Calendar"
            textSize = 14f
            isChecked = column.options["showInCalendar"] as? Boolean ?: true

            setOnCheckedChangeListener { _, isChecked ->
                // Toggle recurring options visibility
                val recurringSection = calendarContainer.findViewWithTag<View>("recurring_section")
                recurringSection?.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }
        calendarContainer.addView(showInCalendarCheckbox)

        // Recurring options section
        val recurringSection = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            tag = "recurring_section"
            visibility = if (showInCalendarCheckbox.isChecked) View.VISIBLE else View.GONE
        }

        // Recurring checkbox
        val isRecurringCheckbox = CheckBox(context).apply {
            text = "🔄 Recurring Event"
            textSize = 14f
            isChecked = column.options["isRecurring"] as? Boolean ?: false

            setOnCheckedChangeListener { _, isChecked ->
                val frequencySpinner = recurringSection.findViewWithTag<Spinner>("frequency_spinner")
                frequencySpinner?.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }
        recurringSection.addView(isRecurringCheckbox)

        // Frequency spinner
        val frequencyLabel = TextView(context).apply {
            text = "Repeat every:"
            textSize = 12f
            setPadding(0, 8, 0, 4)
        }
        recurringSection.addView(frequencyLabel)

        val frequencySpinner = Spinner(context).apply {
            tag = "frequency_spinner"
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                arrayOf("Monthly", "Weekly", "Yearly", "Every 2 weeks", "Every 3 months", "Every 6 months")
            ).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            visibility = if (isRecurringCheckbox.isChecked) View.VISIBLE else View.GONE

            // Set existing value
            val currentFrequency = column.options["recurrenceFrequency"] as? String ?: "Monthly"
            val frequencyOptions = arrayOf("Monthly", "Weekly", "Yearly", "Every 2 weeks", "Every 3 months", "Every 6 months")
            setSelection(frequencyOptions.indexOf(currentFrequency).coerceAtLeast(0))
        }
        recurringSection.addView(frequencySpinner)

        calendarContainer.addView(recurringSection)

        // Store references for data collection
        formFields["${column.name}_showInCalendar"] = showInCalendarCheckbox
        formFields["${column.name}_isRecurring"] = isRecurringCheckbox
        formFields["${column.name}_frequency"] = frequencySpinner

        return calendarContainer
    }

    private fun createBooleanInput(column: TableColumn): View {
        val checkBox = CheckBox(context).apply {
            text = column.name + if (column.required) " *" else ""
            isChecked = column.defaultValue.toBoolean()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }

            setOnCheckedChangeListener { _, _ ->
                updateAllFormulas()
            }
        }

        formFields[column.name] = checkBox
        return checkBox
    }

    private fun createSelectInput(column: TableColumn): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
        }

        val label = TextView(context).apply {
            text = column.name + if (column.required) " *" else ""
            textSize = 14f
            setTextColor(context.getColor(android.R.color.black))
        }

        val options = try {
            val optionsList = column.options["options"] as? List<*>
            optionsList?.map { it.toString() } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val spinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, options).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    updateAllFormulas()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        container.addView(label)
        container.addView(spinner)
        formFields[column.name] = spinner
        return container
    }

    private fun createRatingInput(column: TableColumn): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
        }

        val label = TextView(context).apply {
            text = column.name + if (column.required) " *" else ""
            textSize = 14f
            setTextColor(context.getColor(android.R.color.black))
        }

        val ratingBar = RatingBar(context).apply {
            numStars = 5
            stepSize = 0.5f
            rating = column.defaultValue.toFloatOrNull() ?: 0f

            setOnRatingBarChangeListener { _, _, _ ->
                updateAllFormulas()
            }
        }

        container.addView(label)
        container.addView(ratingBar)
        formFields[column.name] = ratingBar
        return container
    }

    private fun createColorInput(column: TableColumn): View {
        return createTextInput(column)
    }

    private fun createDateTimeInput(column: TableColumn): View {
        return createDateInput(column)
    }

    private fun createTimeInput(column: TableColumn): View {
        return createTextInput(column)
    }

    private fun createFormulaInput(column: TableColumn): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
        }

        val label = TextView(context).apply {
            text = column.name + if (column.required) " *" else ""
            textSize = 14f
            setTextColor(context.getColor(android.R.color.black))
            setPadding(0, 0, 0, 8)
        }

        val resultView = TextView(context).apply {
            text = "0.00"
            textSize = 18f
            setTextColor(context.getColor(android.R.color.black))
            setPadding(16, 16, 16, 16)
            setBackgroundColor(context.getColor(android.R.color.background_light))
        }

        val formulaView = TextView(context).apply {
            val formula = column.options["formula"] as? String ?: ""
            text = "Formula: $formula"
            textSize = 12f
            setTextColor(context.getColor(android.R.color.darker_gray))
            setPadding(16, 8, 16, 0)
        }

        container.addView(label)
        container.addView(resultView)
        container.addView(formulaView)

        formulaFields[column.name] = resultView
        return container
    }

    private fun createFormulaUpdateWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateAllFormulas()
            }
        }
    }

    private fun updateAllFormulas() {
        Log.d("DynamicForm", "updateAllFormulas() called - formFields count: ${formFields.size}, formulaFields count: ${formulaFields.size}")

        // Calculate formulas multiple times to resolve dependencies
        // (formulas that reference other formulas)
        repeat(3) { iteration ->
            Log.d("DynamicForm", "Formula calculation iteration: ${iteration + 1}")
            formulaFields.forEach { (columnName, textView) ->
                val column = columnsList.find { it.name == columnName }
                if (column != null) {
                    // DEBUG: Print all column options to see what's available
                    Log.d("DynamicForm", "=== COLUMN OPTIONS DEBUG for '$columnName' ===")
                    Log.d("DynamicForm", "Column type: ${column.type}")
                    Log.d("DynamicForm", "All options keys: ${column.options.keys}")
                    column.options.forEach { (key, value) ->
                        Log.d("DynamicForm", "Option '$key' -> '$value' (${value?.javaClass?.simpleName})")
                    }
                    Log.d("DynamicForm", "=== END COLUMN OPTIONS ===")

                    // Try different possible keys for the formula
                    val formula = when {
                        column.options["formula"] != null -> column.options["formula"] as? String ?: ""
                        column.options["formulaExpression"] != null -> column.options["formulaExpression"] as? String ?: ""
                        column.options["expression"] != null -> column.options["expression"] as? String ?: ""
                        column.options["calculation"] != null -> column.options["calculation"] as? String ?: ""
                        else -> {
                            Log.w("DynamicForm", "No formula found in options for column '$columnName'")
                            ""
                        }
                    }

                    Log.d("DynamicForm", "Processing formula for column '$columnName': '$formula'")
                    val result = calculateFormula(formula)

                    // Store raw numeric value for other formulas to reference
                    formulaValues[columnName] = result

                    // Format and display the result
                    val formattedResult = formatFormulaResult(result, formula, column)
                    textView.text = formattedResult
                    Log.d("DynamicForm", "Formula '$columnName' result: $result, formatted: '$formattedResult'")
                }
            }
        }
    }

    private fun formatFormulaResult(result: Double, formula: String, column: TableColumn): String {
        // Check if this formula references currency fields or should be treated as currency
        val isCurrencyResult = shouldFormatAsCurrency(formula, column)

        if (isCurrencyResult) {
            // Get currency from referenced fields or default to USD
            val currencyCode = getCurrencyCodeFromFormula(formula)
            return getCurrencyFormatter(currencyCode).format(result)
        } else {
            // Regular number formatting using locale
            return numberFormat.format(result)
        }
    }

    private fun shouldFormatAsCurrency(formula: String, column: TableColumn): Boolean {
        // Check if formula references currency fields
        val referencesCurrency = formFields.any { (fieldName, view) ->
            formula.contains("{$fieldName}") && isCurrencyField(fieldName)
        }

        // Check if column has currency format option
        val hasCurrencyOption = column.options["formatAsCurrency"] as? Boolean ?: false

        // Check if formula contains common currency calculation patterns
        val isCurrencyCalculation = formula.contains("*") || formula.contains("%") ||
                column.name.contains("total", ignoreCase = true) ||
                column.name.contains("price", ignoreCase = true) ||
                column.name.contains("cost", ignoreCase = true) ||
                column.name.contains("amount", ignoreCase = true) ||
                column.name.contains("tax", ignoreCase = true)

        return referencesCurrency || hasCurrencyOption || isCurrencyCalculation
    }

    private fun isCurrencyField(fieldName: String): Boolean {
        val column = columnsList.find { it.name == fieldName }
        return column?.type == ColumnType.CURRENCY
    }

    private fun getCurrencyCodeFromFormula(formula: String): String {
        // Find the first currency field referenced in the formula and get its currency code
        formFields.forEach { (fieldName, view) ->
            if (formula.contains("{$fieldName}") && isCurrencyField(fieldName)) {
                val column = columnsList.find { it.name == fieldName }
                val currencySymbol = column?.options?.get("currencySymbol") as? String ?: "$"
                return when (currencySymbol) {
                    "$" -> "USD"
                    "€" -> "EUR"
                    "£" -> "GBP"
                    "¥" -> "JPY"
                    "Rp" -> "IDR"
                    else -> "USD"
                }
            }
        }
        return "USD" // Default
    }

    private fun calculateFormula(formula: String): Double {
        try {
            Log.d("DynamicForm", "=== FORMULA CALCULATION DEBUG ===")
            Log.d("DynamicForm", "Original formula: $formula")

            // Replace column references with actual values
            var expression = formula

            // First pass: replace from input fields
            formFields.forEach { (fieldName, view) ->
                val placeholder = "{$fieldName}"
                if (expression.contains(placeholder)) {
                    val value = getFieldValue(view)
                    Log.d("DynamicForm", "Field '$fieldName' -> value: $value (view type: ${view.javaClass.simpleName})")
                    if (view is TextInputEditText) {
                        Log.d("DynamicForm", "TextInputEditText content: '${view.text}'")
                    }
                    expression = expression.replace(placeholder, value.toString())
                    Log.d("DynamicForm", "After replacing '$placeholder': $expression")
                }
            }

            // Second pass: replace from formula fields using raw values
            formulaValues.forEach { (fieldName, rawValue) ->
                val placeholder = "{$fieldName}"
                if (expression.contains(placeholder)) {
                    Log.d("DynamicForm", "Formula field '$fieldName' -> value: $rawValue")
                    expression = expression.replace(placeholder, rawValue.toString())
                }
            }

            Log.d("DynamicForm", "Final expression before evaluation: '$expression'")

            // Handle percentage properly - convert any number followed by % to decimal
            expression = expression.replace(Regex("(\\d+(?:\\.\\d+)?)%")) { matchResult ->
                val percentage = matchResult.groupValues[1].toDouble() / 100.0
                percentage.toString()
            }

            // Clean up spaces
            expression = expression.replace(" ", "")

            // Evaluate the mathematical expression
            val result = evaluateExpression(expression)
            Log.d("DynamicForm", "Final calculation result: $result")
            Log.d("DynamicForm", "=== END FORMULA DEBUG ===")

            return result
        } catch (e: Exception) {
            Log.e("DynamicForm", "Formula calculation error: ${e.message}", e)
            return 0.0
        }
    }

    private fun getFieldValue(view: Any): Double {
        val result = when (view) {
            is TextInputEditText -> {
                val text = view.text?.toString() ?: ""
                val parsed = parseNumberSafely(text)
                Log.d("DynamicForm", "TextInputEditText parsing: '$text' -> $parsed")
                parsed
            }
            is CheckBox -> if (view.isChecked) 1.0 else 0.0
            is RatingBar -> view.rating.toDouble()
            is Spinner -> {
                // For spinner, try to extract numeric value from selection
                val selected = view.selectedItem?.toString() ?: ""
                parseNumberSafely(selected)
            }
            else -> 0.0
        }
        Log.d("DynamicForm", "getFieldValue result: $result")
        return result
    }

    private fun evaluateExpression(expression: String): Double {
        try {
            // Normalize expression to US format for calculation
            val normalizedExpression = expression
                .replace(groupingSeparator.toString(), "") // Remove thousands separators
                .replace(decimalSeparator.toString(), ".") // Convert decimal to dot
                .replace(" ", "")

            return evaluateWithProperPrecedence(normalizedExpression)
        } catch (e: Exception) {
            return 0.0
        }
    }

    private fun evaluateWithProperPrecedence(expr: String): Double {
        // Handle parentheses first
        var expression = expr
        while (expression.contains("(")) {
            val start = expression.lastIndexOf("(")
            val end = expression.indexOf(")", start)
            if (end != -1) {
                val subExpr = expression.substring(start + 1, end)
                val result = evaluateWithProperPrecedence(subExpr)
                expression = expression.substring(0, start) + result + expression.substring(end + 1)
            } else {
                break
            }
        }

        // Split by + and - first (lowest precedence)
        val addSubTerms = mutableListOf<String>()
        val addSubOps = mutableListOf<String>()

        var currentTerm = ""
        var i = 0
        while (i < expression.length) {
            val char = expression[i]
            when {
                char == '+' || (char == '-' && i > 0 && expression[i-1].isDigit()) -> {
                    addSubTerms.add(currentTerm)
                    addSubOps.add(char.toString())
                    currentTerm = ""
                }
                else -> {
                    currentTerm += char
                }
            }
            i++
        }
        addSubTerms.add(currentTerm)

        // Evaluate each term (handle * and / within terms)
        val evaluatedTerms = addSubTerms.map { term ->
            evaluateMultiplyDivide(term)
        }

        // Apply addition and subtraction
        var result = evaluatedTerms[0]
        for (i in addSubOps.indices) {
            when (addSubOps[i]) {
                "+" -> result += evaluatedTerms[i + 1]
                "-" -> result -= evaluatedTerms[i + 1]
            }
        }

        return result
    }

    private fun evaluateMultiplyDivide(expr: String): Double {
        val terms = mutableListOf<String>()
        val ops = mutableListOf<String>()

        var currentTerm = ""
        for (char in expr) {
            when (char) {
                '*', '/' -> {
                    terms.add(currentTerm)
                    ops.add(char.toString())
                    currentTerm = ""
                }
                else -> {
                    currentTerm += char
                }
            }
        }
        terms.add(currentTerm)

        // Convert terms to numbers
        val numbers = terms.map { it.toDoubleOrNull() ?: 0.0 }.toMutableList()

        // Apply multiplication and division from left to right
        var i = 0
        while (i < ops.size) {
            when (ops[i]) {
                "*" -> {
                    numbers[i] = numbers[i] * numbers[i + 1]
                    numbers.removeAt(i + 1)
                    ops.removeAt(i)
                }
                "/" -> {
                    numbers[i] = if (numbers[i + 1] != 0.0) numbers[i] / numbers[i + 1] else 0.0
                    numbers.removeAt(i + 1)
                    ops.removeAt(i)
                }
                else -> i++
            }
        }

        return numbers.firstOrNull() ?: 0.0
    }

    fun populateForm(dataJson: String) {
        val gson = Gson()
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val data: Map<String, Any> = try {
            gson.fromJson(dataJson, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        data.forEach { (fieldName, value) ->
            when {
                fieldName.endsWith("_calendarOptions") -> {
                    // Handle calendar options - fix type casting
                    val baseFieldName = fieldName.removeSuffix("_calendarOptions")

                    // Convert value to map safely
                    val calendarOptions = when (value) {
                        is Map<*, *> -> value
                        is String -> {
                            // If it's a JSON string, parse it
                            try {
                                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                                gson.fromJson<Map<String, Any>>(value, mapType)
                            } catch (e: Exception) {
                                emptyMap<String, Any>()
                            }
                        }
                        else -> emptyMap<String, Any>()
                    }

                    // Set show in calendar checkbox
                    val showInCalendarField = formFields["${baseFieldName}_showInCalendar"] as? CheckBox
                    showInCalendarField?.isChecked = calendarOptions["showInCalendar"]?.toString()?.toBoolean() ?: true

                    // Set recurring checkbox
                    val isRecurringField = formFields["${baseFieldName}_isRecurring"] as? CheckBox
                    isRecurringField?.isChecked = calendarOptions["isRecurring"]?.toString()?.toBoolean() ?: false

                    // Set frequency spinner
                    val frequencyField = formFields["${baseFieldName}_frequency"] as? Spinner
                    val frequency = calendarOptions["recurrenceFrequency"]?.toString() ?: "Monthly"
                    val adapter = frequencyField?.adapter as? ArrayAdapter<String>
                    val position = adapter?.getPosition(frequency) ?: 0
                    frequencyField?.setSelection(position)
                }
                else -> {
                    val view = formFields[fieldName]
                    when (view) {
                        is TextInputEditText -> {
                            // Check if this is a number/currency field for proper formatting
                            val column = columnsList.find { it.name == fieldName }
                            if (column?.type == ColumnType.NUMBER || column?.type == ColumnType.CURRENCY) {
                                val numericValue = when (value) {
                                    is Number -> value.toDouble()
                                    is String -> parseNumberSafely(value)
                                    else -> 0.0
                                }
                                view.setText(numberFormat.format(numericValue))
                            } else {
                                view.setText(value.toString())
                            }
                        }
                        is CheckBox -> {
                            view.isChecked = value.toString().toBoolean()
                        }
                        is Spinner -> {
                            val adapter = view.adapter as? ArrayAdapter<String>
                            val position = adapter?.getPosition(value.toString()) ?: 0
                            view.setSelection(position)
                        }
                        is RatingBar -> {
                            view.rating = value.toString().toFloatOrNull() ?: 0f
                        }
                    }
                }
            }
        }

        // Update formulas after populating
        updateAllFormulas()
    }

    fun getFormData(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()

        formFields.forEach { (fieldName, view) ->
            when {
                fieldName.endsWith("_showInCalendar") -> {
                    if (view is CheckBox) {
                        val baseFieldName = fieldName.removeSuffix("_showInCalendar")

                        // Get calendar options safely
                        val showInCalendar = view.isChecked
                        val isRecurring = (formFields["${baseFieldName}_isRecurring"] as? CheckBox)?.isChecked ?: false
                        val recurrenceFrequency = getSelectedFrequency("${baseFieldName}_frequency")

                        data["${baseFieldName}_calendarOptions"] = mapOf<String, Any>(
                            "showInCalendar" to showInCalendar,
                            "isRecurring" to isRecurring,
                            "recurrenceFrequency" to recurrenceFrequency
                        )
                    }
                }
                fieldName.endsWith("_isRecurring") || fieldName.endsWith("_frequency") -> {
                    // Skip these as they're handled above
                }
                else -> {
                    when (view) {
                        is TextInputEditText -> {
                            val value = view.text?.toString()?.trim() ?: ""
                            if (value.isNotEmpty()) {
                                // For number/currency fields, store the parsed numeric value
                                val column = columnsList.find { it.name == fieldName }
                                if (column?.type == ColumnType.NUMBER || column?.type == ColumnType.CURRENCY) {
                                    data[fieldName] = parseNumberSafely(value)
                                } else {
                                    data[fieldName] = value
                                }
                            }
                        }
                        is CheckBox -> {
                            data[fieldName] = view.isChecked
                        }
                        is Spinner -> {
                            view.selectedItem?.toString()?.let { selectedValue ->
                                if (selectedValue.isNotEmpty()) {
                                    data[fieldName] = selectedValue
                                }
                            }
                        }
                        is RatingBar -> {
                            if (view.rating > 0) {
                                data[fieldName] = view.rating
                            }
                        }
                    }
                }
            }
        }

        // Add calculated formula values using raw numeric values
        formulaValues.forEach { (fieldName, value) ->
            data[fieldName] = value
        }

        return data
    }

    private fun getSelectedFrequency(frequencyFieldName: String): String {
        val spinner = formFields[frequencyFieldName] as? Spinner
        return spinner?.selectedItem?.toString() ?: "Monthly"
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                calendar.set(year, month, day)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Debug method to help identify locale issues
    fun debugLocaleInfo() {
        Log.d("DynamicForm", "=== LOCALE DEBUG INFO ===")
        Log.d("DynamicForm", "Current Locale: ${currentLocale.country}_${currentLocale.language}")
        Log.d("DynamicForm", "Display Name: ${currentLocale.displayName}")
        Log.d("DynamicForm", "Decimal Separator: '$decimalSeparator'")
        Log.d("DynamicForm", "Grouping Separator: '$groupingSeparator'")
        Log.d("DynamicForm", "Number format example: ${numberFormat.format(1234.56)}")
        Log.d("DynamicForm", "USD Currency format: ${getCurrencyFormatter("USD").format(1234.56)}")
        Log.d("DynamicForm", "IDR Currency format: ${getCurrencyFormatter("IDR").format(1234.56)}")
        Log.d("DynamicForm", "========================")
    }
}