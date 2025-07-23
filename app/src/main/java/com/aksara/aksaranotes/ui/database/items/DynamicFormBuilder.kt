package com.aksara.aksaranotes.ui.database.items

import android.app.DatePickerDialog
import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.*
import com.aksara.aksaranotes.data.models.TableColumn
import com.aksara.aksaranotes.data.models.ColumnType
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
    private val formFields = mutableMapOf<String, Any>() // Store actual input views
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun buildForm(columns: List<TableColumn>) {
        parentLayout.removeAllViews()
        formFields.clear()

        columns.forEach { column ->
            val fieldView = createFieldView(column)
            fieldView?.let {
                parentLayout.addView(it)
            }
        }
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
        }

        layout.addView(editText)
        formFields[column.name] = editText // Store the EditText directly
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
        }

        layout.addView(editText)
        formFields[column.name] = editText
        return layout
    }

    private fun createCurrencyInput(column: TableColumn): View {
        val layout = TextInputLayout(context).apply {
            hint = column.name + if (column.required) " *" else ""
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            prefixText = "$"
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
        }

        layout.addView(editText)
        formFields[column.name] = editText
        return layout
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
            isFocusable = false
            isClickable = true
            setText(if (column.defaultValue == "today") dateFormat.format(Date()) else column.defaultValue)

            setOnClickListener {
                showDatePicker { date ->
                    setText(dateFormat.format(date))
                }
            }
        }

        layout.addView(editText)
        formFields[column.name] = editText
        return layout
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
        }

        container.addView(label)
        container.addView(spinner)
        formFields[column.name] = spinner // Store the Spinner directly
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
        }

        container.addView(label)
        container.addView(ratingBar)
        formFields[column.name] = ratingBar // Store the RatingBar directly
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
        val textView = TextView(context).apply {
            text = "${column.name}: (Calculated)"
            textSize = 16f
            setTextColor(context.getColor(android.R.color.black))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
            setPadding(16, 16, 16, 16)
            setBackgroundColor(context.getColor(android.R.color.background_light))
        }
        // Don't store formula fields in formFields as they're calculated
        return textView
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

    fun getFormData(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()

        formFields.forEach { (fieldName, view) ->
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