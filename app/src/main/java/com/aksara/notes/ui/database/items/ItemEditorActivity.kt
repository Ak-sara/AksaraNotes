package com.aksara.notes.ui.database.items

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope  // ADD THIS LINE
import com.aksara.notes.databinding.ActivityItemEditorBinding
import com.aksara.notes.ui.database.DatabaseViewModel
import com.aksara.notes.data.database.entities.CustomTable
import com.aksara.notes.data.database.entities.TableItem
import com.aksara.notes.data.models.TableColumn
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch  // ADD THIS LINE

class ItemEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemEditorBinding
    private lateinit var viewModel: DatabaseViewModel
    private var currentTable: CustomTable? = null
    private var currentItem: TableItem? = null
    private lateinit var formBuilder: DynamicFormBuilder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DatabaseViewModel::class.java]
        formBuilder = DynamicFormBuilder(this, binding.layoutFormFields)

        setupToolbar()
        loadTable()
        setupSaveButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun loadTable() {
        val tableId = intent.getStringExtra("table_id") ?: return
        val itemId = intent.getStringExtra("item_id")

        lifecycleScope.launch {
            currentTable = viewModel.getTableById(tableId)
            currentTable?.let { table ->
                supportActionBar?.title = if (itemId == null) {
                    "Add ${table.name} Item"
                } else {
                    "Edit ${table.name} Item"
                }

                // Parse columns from JSON
                val gson = Gson()
                val type = object : TypeToken<List<TableColumn>>() {}.type
                val columns: List<TableColumn> = try {
                    gson.fromJson(table.columns, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }

                // Build dynamic form
                formBuilder.buildForm(columns)

                // Load existing item if editing
                if (itemId != null) {
                    currentItem = viewModel.getItemById(itemId)
                    currentItem?.let { item ->
                        formBuilder.populateForm(item.data)
                    }
                }
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveItem()
        }
    }

    private fun saveItem() {
        val tableId = currentTable?.id ?: return

        // Get form data
        val formData = formBuilder.getFormData()
        val dataJson = Gson().toJson(formData)

        if (formData.isEmpty()) {
            Toast.makeText(this, "Please fill in at least one field", Toast.LENGTH_SHORT).show()
            return
        }

        val item = if (currentItem != null) {
            // Update existing item
            currentItem!!.copy(
                data = dataJson,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            // Create new item
            TableItem(
                tableId = tableId,
                data = dataJson
            )
        }

        if (currentItem != null) {
            viewModel.updateItem(item)
            Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insertItem(item)
            Toast.makeText(this, "Item created", Toast.LENGTH_SHORT).show()
        }

        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}