package com.aksara.aksaranotes.ui.database.builder

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aksara.aksaranotes.databinding.ActivityTableBuilderBinding

class TableBuilderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTableBuilderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTableBuilderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUI()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Table Builder"
    }

    private fun setupUI() {
        // Check if this is from a template
        val template = intent.getStringExtra("template")

        when (template) {
            "accounts" -> setupAccountsTemplate()
            "subscriptions" -> setupSubscriptionsTemplate()
            "bond_calculator" -> setupBondCalculatorTemplate()
            else -> setupBlankTable()
        }

        binding.btnCancel.setOnClickListener { finish() }
        binding.btnCreateTable.setOnClickListener { createTable() }
        binding.btnAddColumn.setOnClickListener { showAddColumnDialog() }
        binding.btnChooseIcon.setOnClickListener { showIconPicker() }
    }

    private fun setupAccountsTemplate() {
        binding.etTableName.setText("Accounts & Passwords")
        binding.etTableDescription.setText("Store login credentials and account information")
        binding.tvSelectedIcon.text = "🔐"
        // TODO: Add predefined columns
    }

    private fun setupSubscriptionsTemplate() {
        binding.etTableName.setText("Subscriptions")
        binding.etTableDescription.setText("Track recurring payments and due dates")
        binding.tvSelectedIcon.text = "💰"
        // TODO: Add predefined columns
    }

    private fun setupBondCalculatorTemplate() {
        binding.etTableName.setText("Bond Calculator")
        binding.etTableDescription.setText("Calculate bond prices and returns")
        binding.tvSelectedIcon.text = "🧮"
        // TODO: Add predefined columns with formulas
    }

    private fun setupBlankTable() {
        binding.etTableName.setText("")
        binding.etTableDescription.setText("")
        binding.tvSelectedIcon.text = "📄"
    }

    private fun createTable() {
        val name = binding.etTableName.text.toString().trim()
        if (name.isEmpty()) {
            binding.etTableName.error = "Table name is required"
            return
        }

        // TODO: Create table with columns
        // For now, just show success and close
        android.widget.Toast.makeText(this, "Table '$name' created!", android.widget.Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showAddColumnDialog() {
        // TODO: Show column creation dialog
        android.widget.Toast.makeText(this, "Column builder coming soon!", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showIconPicker() {
        val icons = arrayOf("📄", "🔐", "💰", "🧮", "📞", "📚", "🎬", "🏋️", "💡", "⭐", "🎯", "📊")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Choose Icon")
            .setItems(icons) { _, which ->
                binding.tvSelectedIcon.text = icons[which]
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}