package com.aksara.notes.ui.database.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aksara.notes.databinding.ActivityDatasetViewBinding
import com.aksara.notes.ui.database.DatabaseViewModel
import com.aksara.notes.ui.database.forms.FormEditorActivity
import com.aksara.notes.data.database.entities.Dataset
import com.aksara.notes.data.database.entities.Form
import kotlinx.coroutines.launch

class DatasetViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDatasetViewBinding
    private lateinit var viewModel: DatabaseViewModel
    private lateinit var formsAdapter: FormItemsAdapter
    private var currentDataset: Dataset? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDatasetViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DatabaseViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        loadDataset()
        setupFab()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        formsAdapter = FormItemsAdapter { form ->
            // Open form editor
            val intent = Intent(this, FormEditorActivity::class.java)
            intent.putExtra("dataset_id", form.datasetId)
            intent.putExtra("form_id", form.id)
            startActivity(intent)
        }

        binding.rvItems.apply {
            adapter = formsAdapter
            layoutManager = LinearLayoutManager(this@DatasetViewActivity)
        }
    }

    private fun loadDataset() {
        val datasetId = intent.getStringExtra("dataset_id") ?: return

        lifecycleScope.launch {
            currentDataset = viewModel.getDatasetById(datasetId)
            currentDataset?.let { dataset ->
                supportActionBar?.title = "${dataset.icon} ${dataset.name}"

                // Observe forms for this dataset
                viewModel.getFormsByDataset(datasetId).observe(this@DatasetViewActivity) { forms ->
                    updateUI(forms)
                }
            }
        }
    }

    private fun setupFab() {
        binding.fabAddItem.setOnClickListener {
            currentDataset?.let { dataset ->
                val intent = Intent(this, FormEditorActivity::class.java)
                intent.putExtra("dataset_id", dataset.id)
                startActivity(intent)
            }
        }
    }

    private fun updateUI(forms: List<Form>) {
        if (forms.isEmpty()) {
            binding.layoutEmptyState.visibility = android.view.View.VISIBLE
            binding.rvItems.visibility = android.view.View.GONE
        } else {
            binding.layoutEmptyState.visibility = android.view.View.GONE
            binding.rvItems.visibility = android.view.View.VISIBLE
            formsAdapter.submitList(forms)
        }

        binding.tvItemsCount.text = "${forms.size} forms"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}