package com.aksara.aksaranotes.ui.database.accounts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aksara.aksaranotes.databinding.ActivityAccountsBinding

class AccountsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUI()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Accounts"
    }

    private fun setupUI() {
        binding.tvTitle.text = "💳 Account Manager"
        binding.tvSubtitle.text = "Manage your login credentials and account information"

        binding.btnAddItem.text = "Add Account"
        binding.btnAddItem.setOnClickListener {
            // TODO: Open account creation dialog/activity
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}