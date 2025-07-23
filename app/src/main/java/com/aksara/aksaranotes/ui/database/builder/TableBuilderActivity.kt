package com.aksara.aksaranotes.ui.database.builder

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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

    // ... rest of the methods stay the same ...

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
        Log.d("TableBuilder", "showColumnDialog called")
        Toast.makeText(this, "Column builder coming soon!", Toast.LENGTH_SHORT).show()
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