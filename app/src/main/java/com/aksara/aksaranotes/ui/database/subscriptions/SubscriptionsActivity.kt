package com.aksara.aksaranotes.ui.database.subscriptions

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aksara.aksaranotes.databinding.ActivitySubscriptionsBinding

class SubscriptionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubscriptionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUI()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Subscriptions"
    }

    private fun setupUI() {
        binding.tvTitle.text = "📅 Subscription Manager"
        binding.tvSubtitle.text = "Track your recurring payments and due dates"

        binding.btnAddItem.text = "Add Subscription"
        binding.btnAddItem.setOnClickListener {
            // TODO: Open subscription creation dialog/activity
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}