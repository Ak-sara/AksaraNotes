package com.aksara.aksaranotes.ui.database.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope  // ADD THIS LINE
import androidx.recyclerview.widget.LinearLayoutManager
import com.aksara.aksaranotes.databinding.ActivityTableViewBinding
import com.aksara.aksaranotes.ui.database.DatabaseViewModel
import com.aksara.aksaranotes.ui.database.items.ItemEditorActivity
import com.aksara.aksaranotes.data.database.entities.CustomTable
import com.aksara.aksaranotes.data.database.entities.TableItem
import kotlinx.coroutines.launch  // ADD THIS LINE

class TableViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTableViewBinding
    private lateinit var viewModel: DatabaseViewModel
    private lateinit var itemsAdapter: TableItemsAdapter
    private var currentTable: CustomTable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTableViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DatabaseViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        loadTable()
        setupFab()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        itemsAdapter = TableItemsAdapter { item ->
            // Open item editor
            val intent = Intent(this, ItemEditorActivity::class.java)
            intent.putExtra("table_id", item.tableId)
            intent.putExtra("item_id", item.id)
            startActivity(intent)
        }

        binding.rvItems.apply {
            adapter = itemsAdapter
            layoutManager = LinearLayoutManager(this@TableViewActivity)
        }
    }

    private fun loadTable() {
        val tableId = intent.getStringExtra("table_id") ?: return

        lifecycleScope.launch {
            currentTable = viewModel.getTableById(tableId)
            currentTable?.let { table ->
                supportActionBar?.title = "${table.icon} ${table.name}"

                // Observe items for this table
                viewModel.getItemsByTable(tableId).observe(this@TableViewActivity) { items ->
                    updateUI(items)
                }
            }
        }
    }

    private fun setupFab() {
        binding.fabAddItem.setOnClickListener {
            currentTable?.let { table ->
                val intent = Intent(this, ItemEditorActivity::class.java)
                intent.putExtra("table_id", table.id)
                startActivity(intent)
            }
        }
    }

    private fun updateUI(items: List<TableItem>) {
        if (items.isEmpty()) {
            binding.layoutEmptyState.visibility = android.view.View.VISIBLE
            binding.rvItems.visibility = android.view.View.GONE
        } else {
            binding.layoutEmptyState.visibility = android.view.View.GONE
            binding.rvItems.visibility = android.view.View.VISIBLE
            itemsAdapter.submitList(items)
        }

        binding.tvItemsCount.text = "${items.size} items"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}