package com.aksara.notes.ui.database

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope  // ADD THIS LINE
import androidx.recyclerview.widget.LinearLayoutManager
import com.aksara.notes.R
import com.aksara.notes.databinding.FragmentDatabaseBinding
import com.aksara.notes.ui.database.builder.DatasetBuilderActivity
import com.aksara.notes.ui.database.view.DatasetViewActivity
import com.aksara.notes.ui.database.forms.FormEditorActivity
import com.aksara.notes.data.database.entities.Dataset
import kotlinx.coroutines.launch  // ADD THIS LINE

class DatabaseFragment : Fragment() {

    private var _binding: FragmentDatabaseBinding? = null
    private val binding get() = _binding!!

    private lateinit var databaseViewModel: DatabaseViewModel
    private lateinit var datasetAdapter: DatasetOverviewAdapter

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
        
        // Enable options menu
        setHasOptionsMenu(true)

        databaseViewModel = ViewModelProvider(this)[DatabaseViewModel::class.java]

        setupRecyclerView()
        setupClickListeners()
        observeData()
    }

    // In setupRecyclerView() method, update the adapter creation:

    private fun setupRecyclerView() {
        datasetAdapter = DatasetOverviewAdapter(
            onDatasetClick = { dataset ->
                // Open table view showing all forms in this dataset
                val intent = Intent(requireContext(), DatasetViewActivity::class.java)
                intent.putExtra("dataset_id", dataset.id)
                startActivity(intent)
            },
            onCreateFormClick = { dataset ->
                // Open form editor for this specific dataset
                val intent = Intent(requireContext(), FormEditorActivity::class.java)
                intent.putExtra("dataset_id", dataset.id)
                startActivity(intent)
            },
            onDeleteDataset = { dataset ->  // ADD THIS CALLBACK
                databaseViewModel.deleteDataset(dataset)
            }
        )

        binding.rvTables.apply {
            adapter = datasetAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_database_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_templates -> {
                showQuickDatasetDialog()
                true
            }
            R.id.action_all_forms -> {
                android.widget.Toast.makeText(requireContext(), "All Items view coming soon!", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_create_dataset -> {
                startActivity(Intent(requireContext(), DatasetBuilderActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupClickListeners() {
        // Only keep the empty state button
        binding.btnCreateFirstTable.setOnClickListener {
            showQuickDatasetDialog()
        }
    }

    private fun observeData() {
        databaseViewModel.allDatasets.observe(viewLifecycleOwner) { datasets ->
            updateUI(datasets)
        }

    }

    private fun updateUI(datasets: List<Dataset>) {
        if (datasets.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvTables.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvTables.visibility = View.VISIBLE

            // Convert datasets to DatasetOverviewItems with form counts
            lifecycleScope.launch {
                val datasetItems = datasets.map { dataset ->
                    val formCount = databaseViewModel.getFormCountForDataset(dataset.id)
                    DatasetOverviewItem(dataset, formCount)
                }
                datasetAdapter.submitList(datasetItems)
            }
        }

    }

    private fun showQuickDatasetDialog() {
        val quickTemplates = arrayOf(
            "🔐 Accounts & Passwords",
            "💰 Subscriptions",
            "📞 Contacts",
            "📚 Books to Read",
            "🎬 Movies & Shows",
            "🏋️ Workout Log",
            "💡 Custom Dataset..."
        )

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Create Dataset from Template")
            .setItems(quickTemplates) { _, which ->
                when (which) {
                    0 -> createFromTemplate("accounts")
                    1 -> createFromTemplate("subscriptions")
                    2 -> createFromTemplate("contacts")
                    3 -> createFromTemplate("books")
                    4 -> createFromTemplate("movies")
                    5 -> createFromTemplate("workout")
                    6 -> startActivity(Intent(requireContext(), DatasetBuilderActivity::class.java))
                }
            }
            .show()
    }

    private fun createFromTemplate(template: String) {
        val intent = Intent(requireContext(), DatasetBuilderActivity::class.java)
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