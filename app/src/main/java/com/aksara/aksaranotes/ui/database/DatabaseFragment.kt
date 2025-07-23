package com.aksara.aksaranotes.ui.database

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope  // ADD THIS LINE
import androidx.recyclerview.widget.LinearLayoutManager
import com.aksara.aksaranotes.databinding.FragmentDatabaseBinding
import com.aksara.aksaranotes.ui.database.builder.TableBuilderActivity
import com.aksara.aksaranotes.ui.database.view.TableViewActivity
import com.aksara.aksaranotes.ui.database.items.ItemEditorActivity
import com.aksara.aksaranotes.data.database.entities.CustomTable
import kotlinx.coroutines.launch  // ADD THIS LINE

class DatabaseFragment : Fragment() {

    private var _binding: FragmentDatabaseBinding? = null
    private val binding get() = _binding!!

    private lateinit var databaseViewModel: DatabaseViewModel
    private lateinit var tableAdapter: TableOverviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDatabaseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseViewModel = ViewModelProvider(this)[DatabaseViewModel::class.java]

        setupRecyclerView()
        setupClickListeners()
        observeData()
    }

    // In setupRecyclerView() method, update the adapter creation:

    private fun setupRecyclerView() {
        tableAdapter = TableOverviewAdapter(
            onTableClick = { table ->
                // Open table view showing all items in this table
                val intent = Intent(requireContext(), TableViewActivity::class.java)
                intent.putExtra("table_id", table.id)
                startActivity(intent)
            },
            onCreateItemClick = { table ->
                // Open item editor for this specific table
                val intent = Intent(requireContext(), ItemEditorActivity::class.java)
                intent.putExtra("table_id", table.id)
                startActivity(intent)
            },
            onDeleteTable = { table ->  // ADD THIS CALLBACK
                databaseViewModel.deleteTable(table)
            }
        )

        binding.rvTables.apply {
            adapter = tableAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.fabCreateTable.setOnClickListener {
            startActivity(Intent(requireContext(), TableBuilderActivity::class.java))
        }

        binding.btnCreateQuickTable.setOnClickListener {
            showQuickTableDialog()
        }

        binding.btnViewAllItems.setOnClickListener {
            // TODO: Open unified items view across all tables
            android.widget.Toast.makeText(requireContext(), "All Items view coming soon!", android.widget.Toast.LENGTH_SHORT).show()
        }

        binding.btnCreateFirstTable.setOnClickListener {
            showQuickTableDialog()
        }
    }

    private fun observeData() {
        databaseViewModel.allTables.observe(viewLifecycleOwner) { tables ->
            updateUI(tables)
        }

        databaseViewModel.allItems.observe(viewLifecycleOwner) { items ->
            binding.tvTotalItems.text = "${items.size} total items"
        }
    }

    private fun updateUI(tables: List<CustomTable>) {
        if (tables.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvTables.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvTables.visibility = View.VISIBLE

            // Convert tables to TableOverviewItems with item counts
            lifecycleScope.launch {
                val tableItems = tables.map { table ->
                    val itemCount = databaseViewModel.getItemCountForTable(table.id)
                    TableOverviewItem(table, itemCount)
                }
                tableAdapter.submitList(tableItems)
            }
        }

        binding.tvTablesCount.text = "${tables.size} tables created"
    }

    private fun showQuickTableDialog() {
        val quickTemplates = arrayOf(
            "🔐 Accounts & Passwords",
            "💰 Subscriptions",
            "🧮 Bond Calculator",
            "📞 Contacts",
            "📚 Books to Read",
            "🎬 Movies & Shows",
            "🏋️ Workout Log",
            "💡 Custom Table..."
        )

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Create Table from Template")
            .setItems(quickTemplates) { _, which ->
                when (which) {
                    0 -> createFromTemplate("accounts")
                    1 -> createFromTemplate("subscriptions")
                    2 -> createFromTemplate("bond_calculator")
                    3 -> createFromTemplate("contacts")
                    4 -> createFromTemplate("books")
                    5 -> createFromTemplate("movies")
                    6 -> createFromTemplate("workout")
                    7 -> startActivity(Intent(requireContext(), TableBuilderActivity::class.java))
                }
            }
            .show()
    }

    private fun createFromTemplate(template: String) {
        val intent = Intent(requireContext(), TableBuilderActivity::class.java)
        intent.putExtra("template", template)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from other activities
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}