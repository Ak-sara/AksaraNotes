package com.aksara.notes.ui.database.forms

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.aksara.notes.databinding.ActivityFormEditorBinding
import com.aksara.notes.ui.database.DatabaseViewModel
import com.aksara.notes.data.database.entities.Dataset
import com.aksara.notes.data.database.entities.Form
import com.aksara.notes.data.database.entities.TableColumn
import com.aksara.notes.ui.database.items.DynamicFormBuilder
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FormEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFormEditorBinding
    private lateinit var viewModel: DatabaseViewModel
    private var currentDataset: Dataset? = null
    private var currentForm: Form? = null
    private lateinit var formBuilder: DynamicFormBuilder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFormEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DatabaseViewModel::class.java]
        formBuilder = DynamicFormBuilder(this, binding.layoutFormFields)

        setupToolbar()
        loadDataset()
        setupSaveButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun testFormulaSystem() {
        // Add this temporarily to test if formulas are responsive
        lifecycleScope.launch {
            delay(2000) // Wait for form to load

            // Check if formula fields exist
            val formData = formBuilder.getFormData()
            android.util.Log.d("FormulaTest", "Form data: $formData")

            // Check if formula values are calculated
            formData.forEach { (key, value) ->
                android.util.Log.d("FormulaTest", "$key: $value")
            }
        }
    }

    private fun loadDataset() {
        val datasetId = intent.getStringExtra("dataset_id") ?: return
        val formId = intent.getStringExtra("form_id")

        lifecycleScope.launch {
            currentDataset = viewModel.getDatasetById(datasetId)
            currentDataset?.let { dataset ->
                android.util.Log.d("FormEditor", "Loaded dataset: ${dataset.name} with ${dataset.columns.size} columns")
                dataset.columns.forEach { column ->
                    android.util.Log.d("FormEditor", "Column: ${column.name} (${column.type})")
                }
                
                supportActionBar?.title = if (formId == null) {
                    "Add ${dataset.name} Form"
                } else {
                    "Edit ${dataset.name} Form"
                }

                // Access columns directly (no JSON parsing needed!)
                val columns = dataset.columns.toList() // Convert RealmList to List

                // Build dynamic form
                formBuilder.buildForm(columns)

                // Load existing form if editing
                if (formId != null) {
                    currentForm = viewModel.getFormById(formId)
                    currentForm?.let { form ->
                        formBuilder.populateForm(form.data)
                    }
                }
                testFormulaSystem()
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveForm()
        }
    }

    private fun saveForm() {
        val datasetId = currentDataset?.id ?: return

        // Get form data
        val formData = formBuilder.getFormData()
        val dataJson = Gson().toJson(formData)

        if (formData.isEmpty()) {
            Toast.makeText(this, "Please fill in at least one field", Toast.LENGTH_SHORT).show()
            return
        }

        val form = if (currentForm != null) {
            // Update existing form
            Form().apply {
                this.id = currentForm!!.id
                this.datasetId = currentForm!!.datasetId
                this.data = dataJson
                this.createdAt = currentForm!!.createdAt
                this.updatedAt = System.currentTimeMillis()
            }
        } else {
            // Create new form
            Form().apply {
                this.datasetId = datasetId
                this.data = dataJson
            }
        }

        if (currentForm != null) {
            viewModel.updateForm(form)
            Toast.makeText(this, "Form updated", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insertForm(form)
            Toast.makeText(this, "Form created", Toast.LENGTH_SHORT).show()
        }

        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}