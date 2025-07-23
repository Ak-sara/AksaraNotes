package com.aksara.aksaranotes.ui.database.tables

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aksara.aksaranotes.databinding.ActivityCustomTablesBinding

class CustomTablesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomTablesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomTablesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUI()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Custom Tables"
    }

    private fun setupUI() {
        binding.tvTitle.text = "🗄️ Custom Table Builder"
        binding.tvSubtitle.text = "Create and manage custom database tables"

        binding.btnAddItem.text = "Create Table"
        binding.btnAddItem.setOnClickListener {
            // TODO: Open table creation wizard
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}