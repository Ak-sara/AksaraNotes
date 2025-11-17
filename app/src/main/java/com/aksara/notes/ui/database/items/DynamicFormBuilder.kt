package com.aksara.notes.ui.database.items

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.aksara.notes.R
import com.aksara.notes.data.database.entities.TableColumn
import com.aksara.notes.data.models.ColumnType
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class DynamicFormBuilder(
    private val context: Context,
    private val parentLayout: LinearLayout
) {
    private val formFields = mutableMapOf<String, Any>()
    private val formulaFields = mutableMapOf<String, TextView>()
    private val formulaValues = mutableMapOf<String, Double>()
    private val columnsList = mutableListOf<TableColumn>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun buildForm(columns: List<TableColumn>) {
        android.util.Log.d("DynamicFormBuilder", "buildForm called with ${columns.size} columns")
        parentLayout.removeAllViews()
        formFields.clear()
        formulaFields.clear()
        formulaValues.clear()
        columnsList.clear()
        columnsList.addAll(columns)

        columns.forEach { column ->
            android.util.Log.d("DynamicFormBuilder", "Creating field for column: ${column.name} (${column.type})")
            val fieldView = createFieldView(column)
            fieldView?.let {
                android.util.Log.d("DynamicFormBuilder", "Adding field view to parent layout")
                parentLayout.addView(it)
            } ?: run {
                android.util.Log.w("DynamicFormBuilder", "Field view is null for column: ${column.name}")
            }
        }

        // Calculate initial formula values after all fields are created
        parentLayout.post {
            updateAllFormulas()
        }
    }

    private fun createFieldView(column: TableColumn): View? {
        android.util.Log.d("DynamicFormBuilder", "createFieldView called for ${column.name} with type ${column.type}")
        val columnType = try {
            ColumnType.valueOf(column.type)
        } catch (e: Exception) {
            android.util.Log.w("DynamicFormBuilder", "Failed to parse column type ${column.type}, using TEXT fallback", e)
            ColumnType.TEXT // fallback
        }
        android.util.Log.d("DynamicFormBuilder", "Resolved column type: $columnType")
        return when (columnType) {
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
            ColumnType.FREQUENCY -> createFrequencyInput(column)
        }
    }

    private fun createStyledContainer(): LinearLayout {
        return LinearLayout(context, null, 0, R.style.Widget_AksaraNotes_FormFieldContainer).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }
    }

    private fun createTextInput(column: TableColumn): View {
        val container = createStyledContainer()

        val label = TextView(context).apply {
            text = column.name + if (column.required) " *" else ""
            textSize = 16f
            setTextColor(context.getColor(R.color.on_surface))
            setPadding(0, 0, 0, 8)
        }

        val editText = EditText(context).apply {
            setText(column.defaultValue)
            hint = "Enter text"
            textSize = 16f
            setPadding(0, 8, 0, 16)
            setBackgroundColor(Color.TRANSPARENT)
            maxLines = if (column.name.contains("note", ignoreCase = true) ||
                column.name.contains("description", ignoreCase = true)) 5 else 1

            addTextChangedListener(createFormulaUpdateWatcher())
        }

        container.addView(label)
        container.addView(editText)

        formFields[column.name] = editText
        return container
    }

    private fun createNumberInput(column: TableColumn): View {
        val container = createStyledContainer()

        val label = TextView(context).apply {
            text = column.name + if (column.required) " *" else ""
            textSize = 16f
            setTextColor(context.getColor(R.color.on_surface))
            setPadding(0, 0, 0, 8)
        }

        val editText = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(column.defaultValue)
            hint = "Enter number"
            textSize = 16f
            setPadding(0, 8, 0, 16)
            setBackgroundColor(Color.TRANSPARENT)

            addTextChangedListener(createFormulaUpdateWatcher())
        }

        container.addView(label)
        container.addView(editText)

        formFields[column.name] = editText
        return container
    }

    private fun createCurrencyInput(column: TableColumn): View {
        val container = createStyledContainer()

        // Parse options from JSON string
        val options = try {
            if (column.options.isNotEmpty()) {
                val gson = Gson()
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson<Map<String, Any>>(column.options, mapType)
            } else {
                emptyMap<String, Any>()
            }
        } catch (e: Exception) {
            emptyMap<String, Any>()
        }
        
        val currencySymbol = options["currencySymbol"] as? String ?: "$"
        val currencyCode = when (currencySymbol) {
            "$" -> "USD"
            "€" -> "EUR"
            "£" -> "GBP"
            "¥" -> "JPY"
            "Rp" -> "IDR"
            else -> "USD"
        }

        // Currency display label (formatted)
        val currencyLabel = TextView(context).apply {
            text = "${column.name}: ${formatCurrency(0.0, currencyCode)}"
            textSize = 16f
            setTextColor(context.getColor(R.color.on_surface))
            setPadding(0, 0, 0, 8)
        }

        // Unformatted input field - raw numbers only
        val editText = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(column.defaultValue)
            hint = "Enter amount"
            textSize = 16f
            setPadding(0, 8, 0, 16)
            setBackgroundColor(Color.TRANSPARENT)

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    val number = s.toString().toDoubleOrNull() ?: 0.0
                    currencyLabel.text = "${column.name}: ${formatCurrency(number, currencyCode)}"
                    updateAllFormulas()
                }
            })
        }

        container.addView(currencyLabel)
        container.addView(editText)

        formFields[column.name] = editText
        return container
    }

    private fun createEmailInput(column: TableColumn): View {
        val container = createStyledContainer()

        val label = TextView(context).apply {
            text = column.name + if (column.required) " *" else ""
            textSize = 16f
            setTextColor(context.getColor(R.color.on_surface))
            setPadding(0, 0, 0, 8)
        }

        val editText = EditText(context).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(column.defaultValue)
            hint = "Enter email"
            textSize = 16f
            setPadding(0, 8, 0, 16)
            setBackgroundColor(Color.TRANSPARENT)

            addTextChangedListener(createFormulaUpdateWatcher())
        }

        container.addView(label)
        container.addView(editText)

        formFields[column.name] = editText
        return container
    }

    private fun createPhoneInput(column: TableColumn): View {
        val container = createStyledContainer()

        val label = TextView(context).apply {
            text = column.name + if (column.required) " *" else ""
            textSize = 16f
            setTextColor(context.getColor(R.color.on_surface))
            setPadding(0, 0, 0, 8)
        }

        val editText = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_PHONE
            setText(column.defaultValue)
            hint = "Enter phone"
            textSize = 16f
            setPadding(0, 8, 0, 16)
            setBackgroundColor(Color.TRANSPARENT)
        }

        container.addView(label)
        container.addView(editText)

        formFields[column.name] = editText
        return container
    }

    private fun createUrlInput(column: TableColumn): View {
        val container = createStyledContainer()

        val label = TextView(context).apply {
            text = column.name + if (column.required) " *" else ""
            textSize = 16f
            setTextColor(context.getColor(R.color.on_surface))
            setPadding(0, 0, 0, 8)
        }

        val editText = EditText(context).apply {
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setText(column.defaultValue)
            hint = "Enter URL"
            textSize = 16f
            setPadding(0, 8, 0, 16)
            setBackgroundColor(Color.TRANSPARENT)
        }

        container.addView(label)
        container.addView(editText)

        formFields[column.name] = editText
        return container
    }

    private fun createDateInput(column: TableColumn): View {
        val container = createStyledContainer()

        val label = TextView(context).apply {
            text = column.name + if (column.required) " *" else ""
            textSize = 16f
            setTextColor(context.getColor(R.color.on_surface))
            setPadding(0, 0, 0, 8)
        }

        val editText = EditText(context).apply {
            isFocusable = false
            isClickable = true
            setText(if (column.defaultValue == "today") dateFormat.format(Date()) else column.defaultValue)
            hint = "Select date"
            textSize = 16f
            setPadding(0, 8, 0, 16)
            setBackgroundColor(Color.TRANSPARENT)

            setOnClickListener {
                showDatePicker { date ->
                    setText(dateFormat.format(date))
                    updateAllFormulas()
                }
            }
        }

        container.addView(label)
        container.addView(editText)

        formFields[column.name] = editText
        return container
    }

    private fun createBooleanInput(column: TableColumn): View {
        val container = createStyledContainer()

        val label = TextView(context).apply {
            text = column.name + if (column.required) " *" else ""
            textSize = 16f
            setTextColor(context.getColor(R.color.on_surface))
            setPadding(0, 0, 0, 8)
        }

        val checkBox = CheckBox(context).apply {
            text = "Yes"
            isChecked = column.defaultValue.toBoolean()
            textSize = 16f

            setOnCheckedChangeListener { _, _ ->
                updateAllFormulas()
            }
        }

        container.addView(label)
        container.addView(checkBox)

        formFields[column.name] = checkBox
        return container
    }

    private fun createSelectInput(column: TableColumn): View {
        val container = createStyledContainer()

        val label = TextView(context).apply {
            text = column.name + if (column.required) " *" else ""
            textSize = 16f
            setTextColor(context.getColor(R.color.on_surface))
            setPadding(0, 0, 0, 8)
        }

        // Parse options from JSON string
        val columnOptions = try {
            if (column.options.isNotEmpty()) {
                val gson = Gson()
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson<Map<String, Any>>(column.options, mapType)
            } else {
                emptyMap<String, Any>()
            }
        } catch (e: Exception) {
            emptyMap<String, Any>()
        }
        
        val options = try {
            val optionsList = columnOptions["options"] as? List<*>
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
        val container = createStyledContainer()

        val label = TextView(context).apply {
            text = column.name + if (column.required) " *" else ""
            textSize = 16f
            setTextColor(context.getColor(R.color.on_surface))
            setPadding(0, 0, 0, 8)
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
        val container = createStyledContainer()

        // Formula expression label (top)
        val formulaLabel = TextView(context).apply {
            val formula = column.defaultValue
            text = if (formula.isNotEmpty()) "Formula: $formula" else "No formula defined"
            textSize = 16f
            setTextColor(context.getColor(R.color.on_surface))
            setPadding(0, 0, 0, 8)
        }

        // Calculated result display (bottom)
        val resultView = TextView(context).apply {
            text = "0.00"
            textSize = 16f
            setTextColor(context.getColor(R.color.on_surface))
            setPadding(0, 8, 0, 16)
            setBackgroundColor(Color.TRANSPARENT)
        }

        container.addView(formulaLabel)
        container.addView(resultView)

        formulaFields[column.name] = resultView
        return container
    }

    private fun createFrequencyInput(column: TableColumn): View {
        val container = createStyledContainer()

        val label = TextView(context).apply {
            text = column.name + if (column.required) " *" else ""
            textSize = 16f
            setTextColor(context.getColor(R.color.on_surface))
            setPadding(0, 0, 0, 8)
        }

        // Parse frequency options from column.options
        val columnOptions = try {
            if (column.options.isNotEmpty()) {
                val gson = Gson()
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson<Map<String, Any>>(column.options, mapType)
            } else {
                emptyMap<String, Any>()
            }
        } catch (e: Exception) {
            emptyMap<String, Any>()
        }

        // Get available frequency options
        val frequencies = arrayOf(
            "Daily", "Weekly", "Bi-weekly", "Monthly", 
            "Quarterly", "Semi-annually", "Annually",
            "Every 2 weeks", "Every 3 months", "Every 6 months"
        )

        val spinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, frequencies).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            // Set default selection based on column options
            val currentFrequency = columnOptions["frequency"]?.toString() ?: "Monthly"
            val index = frequencies.indexOf(currentFrequency)
            if (index >= 0) {
                setSelection(index)
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
        repeat(3) { iteration ->
            formulaFields.forEach { (columnName, textView) ->
                val column = columnsList.find { it.name == columnName }
                if (column != null) {
                    // For FORMULA columns, read from defaultValue
                    val formula = if (column.type == "FORMULA") {
                        column.defaultValue
                    } else {
                        // Parse options from JSON string
                        val columnOptions = try {
                            if (column.options.isNotEmpty()) {
                                val gson = Gson()
                                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                                gson.fromJson<Map<String, Any>>(column.options, mapType)
                            } else {
                                emptyMap<String, Any>()
                            }
                        } catch (e: Exception) {
                            emptyMap<String, Any>()
                        }
                        columnOptions["formula"] as? String ?: ""
                    }

                    if (formula.isEmpty()) {
                        textView.text = "0.00"
                        formulaValues[columnName] = 0.0
                    } else {
                        val result = calculateFormula(formula)
                        formulaValues[columnName] = result
                        val formattedResult = formatFormulaResult(result, formula, column)
                        textView.text = formattedResult
                    }
                }
            }
        }
    }

    private fun calculateFormula(formula: String): Double {
        try {
            if (formula.isBlank()) return 0.0

            var expression = formula

            // Create a map of all available values
            val allValues = mutableMapOf<String, Double>()

            // Add form field values
            formFields.forEach { (fieldName, view) ->
                val value = getFieldValue(view)
                allValues[fieldName.lowercase()] = value
                allValues[fieldName.uppercase()] = value
                allValues[fieldName] = value
            }

            // Add formula values
            formulaValues.forEach { (fieldName, value) ->
                allValues[fieldName.lowercase()] = value
                allValues[fieldName.uppercase()] = value
                allValues[fieldName] = value
            }

            // Find and replace all placeholders
            val placeholderPattern = Regex("\\{([^}]+)\\}")
            val placeholders = placeholderPattern.findAll(expression).map { it.value to it.groupValues[1] }.toList()

            placeholders.forEach { (placeholder, fieldName) ->
                val value = allValues[fieldName.lowercase()]
                    ?: allValues[fieldName.uppercase()]
                    ?: allValues[fieldName]
                    ?: 0.0

                expression = expression.replace(placeholder, value.toString())
            }

            // Handle percentages
            expression = expression.replace(Regex("(\\d+(?:\\.\\d+)?)%")) { matchResult ->
                val percentage = matchResult.groupValues[1].toDouble() / 100.0
                percentage.toString()
            }

            expression = expression.trim().replace("\\s+".toRegex(), "")

            // Check if any placeholders remain
            if (expression.contains(Regex("\\{[^}]+\\}"))) {
                return 0.0
            }

            return evaluateExpression(expression)

        } catch (e: Exception) {
            return 0.0
        }
    }

    private fun evaluateExpression(expression: String): Double {
        try {
            // Handle parentheses first
            var expr = expression
            while (expr.contains("(")) {
                val start = expr.lastIndexOf("(")
                val end = expr.indexOf(")", start)
                if (end != -1) {
                    val subExpr = expr.substring(start + 1, end)
                    val result = evaluateExpression(subExpr)
                    expr = expr.substring(0, start) + result + expr.substring(end + 1)
                } else {
                    break
                }
            }

            // Split by + and - (lowest precedence)
            val addSubTerms = mutableListOf<String>()
            val addSubOps = mutableListOf<String>()

            var currentTerm = ""
            var i = 0
            while (i < expr.length) {
                val char = expr[i]
                when {
                    char == '+' || (char == '-' && i > 0 && expr[i-1].isDigit()) -> {
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

            // Evaluate each term (handle * and /)
            val evaluatedTerms = addSubTerms.map { term ->
                evaluateMultiplyDivide(term)
            }

            // Apply addition and subtraction
            var result = evaluatedTerms[0]
            for (j in addSubOps.indices) {
                when (addSubOps[j]) {
                    "+" -> result += evaluatedTerms[j + 1]
                    "-" -> result -= evaluatedTerms[j + 1]
                }
            }

            return result
        } catch (e: Exception) {
            return 0.0
        }
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

    private fun formatFormulaResult(result: Double, formula: String, column: TableColumn): String {
        // Check if this formula should be formatted as currency
        val isCurrencyResult = shouldFormatAsCurrency(formula, column)

        return if (isCurrencyResult) {
            val currencyCode = getCurrencyCodeFromFormula(formula)
            formatCurrency(result, currencyCode)
        } else {
            String.format("%.2f", result)
        }
    }

    private fun shouldFormatAsCurrency(formula: String, column: TableColumn): Boolean {
        // Check if formula references currency fields
        val referencesCurrency = formFields.any { (fieldName, view) ->
            formula.contains("{$fieldName}", ignoreCase = true) && isCurrencyField(fieldName)
        }

        // Check if column has currency format option
        val columnOptions = try {
            if (column.options.isNotEmpty()) {
                val gson = Gson()
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson<Map<String, Any>>(column.options, mapType)
            } else {
                emptyMap<String, Any>()
            }
        } catch (e: Exception) {
            emptyMap<String, Any>()
        }
        val hasCurrencyOption = columnOptions["formatAsCurrency"] as? Boolean ?: false

        return referencesCurrency || hasCurrencyOption
    }

    private fun isCurrencyField(fieldName: String): Boolean {
        val column = columnsList.find { it.name == fieldName }
        return column?.type == "CURRENCY"
    }

    private fun getCurrencyCodeFromFormula(formula: String): String {
        formFields.forEach { (fieldName, view) ->
            if (formula.contains("{$fieldName}", ignoreCase = true) && isCurrencyField(fieldName)) {
                val column = columnsList.find { it.name == fieldName }
                // Parse options from JSON string
                val columnOptions = try {
                    if (column?.options?.isNotEmpty() == true) {
                        val gson = Gson()
                        val mapType = object : TypeToken<Map<String, Any>>() {}.type
                        gson.fromJson<Map<String, Any>>(column.options, mapType)
                    } else {
                        emptyMap<String, Any>()
                    }
                } catch (e: Exception) {
                    emptyMap<String, Any>()
                }
                val currencySymbol = columnOptions["currencySymbol"] as? String ?: "$"
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
        return "USD"
    }

    private fun formatCurrency(amount: Double, currencyCode: String): String {
        return when (currencyCode) {
            "USD" -> "$${String.format("%.2f", amount)}"
            "EUR" -> "€${String.format("%.2f", amount)}"
            "GBP" -> "£${String.format("%.2f", amount)}"
            "JPY" -> "¥${String.format("%.0f", amount)}"
            "IDR" -> "Rp ${String.format("%,.0f", amount)}"
            else -> "$${String.format("%.2f", amount)}"
        }
    }

    private fun getFieldValue(view: Any): Double {
        return when (view) {
            is EditText -> {
                val text = view.text?.toString()?.trim() ?: ""
                text.toDoubleOrNull() ?: 0.0
            }
            is CheckBox -> if (view.isChecked) 1.0 else 0.0
            is RatingBar -> view.rating.toDouble()
            is Spinner -> {
                val selected = view.selectedItem?.toString() ?: ""
                selected.toDoubleOrNull() ?: 0.0
            }
            else -> 0.0
        }
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
            val view = formFields[fieldName]
            when (view) {
                is EditText -> {
                    val textValue = when (value) {
                        is Number -> value.toString()
                        is String -> value
                        else -> value.toString()
                    }
                    view.setText(textValue)
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
                null -> {
                    // Handle formula fields
                    val formulaView = formulaFields[fieldName]
                    if (formulaView != null) {
                        val numericValue = when (value) {
                            is Number -> value.toDouble()
                            is String -> value.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                        formulaValues[fieldName] = numericValue
                    }
                }
            }
        }

        updateAllFormulas()
    }

    fun getFormData(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()

        formFields.forEach { (fieldName, view) ->
            when (view) {
                is EditText -> {
                    val text = view.text?.toString()?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        val value = text.toDoubleOrNull() ?: text
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

        // Add calculated formula values
        formulaValues.forEach { (fieldName, value) ->
            data[fieldName] = value
        }

        return data
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