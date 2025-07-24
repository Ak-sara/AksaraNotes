package com.aksara.aksaranotes.ui.database.items

import android.app.DatePickerDialog
import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

class DynamicFormBuilder(
    private val context: Context,
    private val parentLayout: LinearLayout
) {
    private val formFields = mutableMapOf<String, Any>() // Store actual input views
    private val formulaFields = mutableMapOf<String, TextView>() // Store formula display views
    private val formulaValues = mutableMapOf<String, Double>() // Store raw calculated values
    private val columnsList = mutableListOf<TableColumn>() // Store all columns for formula calculation
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Currency formatters
    private val usdFormatter = DecimalFormat("#,##0.00")
    private val idrFormatter = DecimalFormat("#,##0.00") // Use standard pattern, we'll handle display manually

    fun buildForm(columns: List<TableColumn>) {
        parentLayout.removeAllViews()
        formFields.clear()
        formulaFields.clear()
        formulaValues.clear() // Clear raw formula values
        columnsList.clear()
        columnsList.addAll(columns)

        columns.forEach { column ->
            val fieldView = createFieldView(column)
            fieldView?.let {
                parentLayout.addView(it)
            }
        }

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
            text = "0.00 $currencyCode"
            textSize = 14f
            setTextColor(context.getColor(android.R.color.darker_gray))
            setPadding(16, 8, 16, 0)
        }

        // Add currency formatting text watcher
        editText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                val input = s.toString().replace(",", "").replace(".", "")
                if (input.isNotEmpty() && input.all { it.isDigit() }) {
                    val number = input.toDoubleOrNull() ?: 0.0
                    // Don't divide by 100 - user enters full amount
                    val formatted = when (currencyCode) {
                        "IDR" -> {
                            val baseFormatted = usdFormatter.format(number)
                            // Convert to Indonesian format: 1.234,56
                            val idrFormatted = baseFormatted.replace(",", "TEMP").replace(".", ",").replace("TEMP", ".")
                            "$idrFormatted $currencyCode"
                        }
                        else -> {
                            "${usdFormatter.format(number)} $currencyCode"
                        }
                    }
                    currencyLabel.text = formatted
                } else {
                    currencyLabel.text = "0,00 $currencyCode"
                }

                // Update formulas
                updateAllFormulas()
            }
        })

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
        // Calculate formulas multiple times to resolve dependencies
        // (formulas that reference other formulas)
        repeat(3) { // Usually 2-3 iterations are enough for most dependencies
            formulaFields.forEach { (columnName, textView) ->
                val column = columnsList.find { it.name == columnName }
                if (column != null) {
                    val formula = column.options["formula"] as? String ?: ""
                    val result = calculateFormula(formula)

                    // Store raw numeric value for other formulas to reference
                    formulaValues[columnName] = result

                    // Format and display the result
                    val formattedResult = formatFormulaResult(result, formula, column)
                    textView.text = formattedResult
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

            return when (currencyCode) {
                "IDR" -> {
                    val baseFormatted = usdFormatter.format(result)
                    val idrFormatted = baseFormatted.replace(",", "TEMP").replace(".", ",").replace("TEMP", ".")
                    "$idrFormatted $currencyCode"
                }
                else -> {
                    "${usdFormatter.format(result)} $currencyCode"
                }
            }
        } else {
            // Regular number formatting
            return String.format("%.2f", result)
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
            // Replace column references with actual values
            var expression = formula

            // First pass: replace from input fields
            formFields.forEach { (fieldName, view) ->
                val placeholder = "{$fieldName}"
                if (expression.contains(placeholder)) {
                    val value = getFieldValue(view)
                    expression = expression.replace(placeholder, value.toString())
                }
            }

            // Second pass: replace from formula fields using raw values
            formulaValues.forEach { (fieldName, rawValue) ->
                val placeholder = "{$fieldName}"
                if (expression.contains(placeholder)) {
                    expression = expression.replace(placeholder, rawValue.toString())
                }
            }

            // Handle percentage properly - convert any number followed by % to decimal
            expression = expression.replace(Regex("(\\d+(?:\\.\\d+)?)%")) { matchResult ->
                val percentage = matchResult.groupValues[1].toDouble() / 100.0
                percentage.toString()
            }

            // Clean up spaces
            expression = expression.replace(" ", "")

            // Evaluate the mathematical expression
            return evaluateExpression(expression)
        } catch (e: Exception) {
            return 0.0
        }
    }

    private fun getFieldValue(view: Any): Double {
        return when (view) {
            is TextInputEditText -> {
                val text = view.text?.toString()?.replace(",", "")?.replace("[^0-9.-]".toRegex(), "") ?: ""
                text.toDoubleOrNull() ?: 0.0
            }
            is CheckBox -> if (view.isChecked) 1.0 else 0.0
            is RatingBar -> view.rating.toDouble()
            is Spinner -> {
                // For spinner, try to extract numeric value from selection
                val selected = view.selectedItem?.toString() ?: ""
                selected.replace("[^0-9.-]".toRegex(), "").toDoubleOrNull() ?: 0.0
            }
            else -> 0.0
        }
    }

    private fun evaluateExpression(expression: String): Double {
        // Simple expression evaluator for basic math operations
        try {
            val cleanExpression = expression.replace(" ", "")
            return evaluateWithProperPrecedence(cleanExpression)
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
                            view.setText(value.toString())
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
                                data[fieldName] = value
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
}