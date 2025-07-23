package com.aksara.aksaranotes.ui.database

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.aksara.aksaranotes.databinding.FragmentDatabaseBinding
import com.aksara.aksaranotes.ui.database.builder.TableBuilderActivity
import com.aksara.aksaranotes.data.database.entities.CustomTable

class DatabaseFragment : Fragment() {

    private var _binding: FragmentDatabaseBinding? = null
    private val binding get() = _binding!!

    private lateinit var databaseViewModel: DatabaseViewModel

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

        setupClickListeners()
        observeData()
    }

    private fun setupClickListeners() {
        binding.fabCreateTable.setOnClickListener {
            startActivity(Intent(requireContext(), TableBuilderActivity::class.java))
        }

        binding.btnCreateQuickTable.setOnClickListener {
            showQuickTableDialog()
        }

        binding.btnViewAllItems.setOnClickListener {
            // TODO: Open unified items view
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
    }

    private fun updateUI(tables: List<CustomTable>) {
        if (tables.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvTables.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvTables.visibility = View.VISIBLE
            // TODO: Setup table adapter when we create it
        }

        binding.tvTablesCount.text = "${tables.size} tables created"
        binding.tvTotalItems.text = "0 total items" // Placeholder for now
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