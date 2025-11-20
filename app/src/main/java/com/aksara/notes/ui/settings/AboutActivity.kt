package com.aksara.notes.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aksara.notes.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "About"
    }

    private fun setupViews() {
        // Set app version using PackageManager
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            binding.tvVersion.text = "Version $versionName ($versionCode)"
        } catch (e: PackageManager.NameNotFoundException) {
            binding.tvVersion.text = "Version 1.0.0"
        }

        // Set build info
        binding.tvBuildInfo.text = "by Ak'sara"
    }

    private fun setupClickListeners() {
        // Rate App
        binding.cardRateApp.setOnClickListener {
            rateApp()
        }

        // Share App
        binding.cardShareApp.setOnClickListener {
            shareApp()
        }

        // Privacy Policy
        binding.cardPrivacyPolicy.setOnClickListener {
            showPrivacyPolicy()
        }

        // Terms of Service
        binding.cardTermsOfService.setOnClickListener {
            showTermsOfService()
        }

        // What's New
        binding.cardWhatsNew.setOnClickListener {
            showWhatsNew()
        }

        // Help & Documentation
        binding.cardHelpDocumentation.setOnClickListener {
            openHelpCenter()
        }

        // Report Bug
        binding.cardReportBug.setOnClickListener {
            reportBug()
        }

        // Feature Request
        binding.cardFeatureRequest.setOnClickListener {
            requestFeature()
        }
    }

    private fun rateApp() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                setPackage("com.android.vending")
            }
            startActivity(intent)
        } catch (e: Exception) {
            // If Play Store is not available, open in browser
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                }
                startActivity(intent)
            } catch (ex: Exception) {
                showToast("Unable to open Play Store")
            }
        }
    }

    private fun shareApp() {
        val shareText = buildString {
            appendLine("🚀 Check out Notes by Aksara!")
            appendLine()
            appendLine("📝 Secure Notes with PIN protection")
            appendLine("🗄️ Dynamic Database builder")
            appendLine("📅 Smart Calendar integration")
            appendLine("🔒 Military-grade encryption")
            appendLine()
            appendLine("Download now:")
            appendLine("https://play.google.com/store/apps/details?id=$packageName")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Notes by Aksara - Secure Note Taking App")
        }

        try {
            startActivity(Intent.createChooser(intent, "Share Notes by Aksara"))
        } catch (e: Exception) {
            showToast("Unable to share app")
        }
    }

    private fun openWebPage(url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Unable to open web page")
        }
    }

    private fun showPrivacyPolicy() {
        openWebPage("https://linheriawan.github.io/docs/aksaranotes/privacy-policy.html")
    }

    private fun showTermsOfService() {
        openWebPage("https://linheriawan.github.io/docs/aksaranotes/terms-of-service.html")
    }

    private fun showWhatsNew() {
        AlertDialog.Builder(this)
            .setTitle("✨ What's New in This Version")
            .setMessage(buildString {
                appendLine("🎉 Version 1.0.0 - Initial Release")
                appendLine()
                appendLine("🔒 Security Features:")
                appendLine("• Master password protection")
                appendLine("• Biometric authentication")
                appendLine("• Auto-lock functionality")
                appendLine("• PIN-protected notes")
                appendLine()
                appendLine("📝 Note Taking:")
                appendLine("• Rich text editing")
                appendLine("• Search and favorites")
                appendLine("• Secure sharing")
                appendLine()
                appendLine("🗄️ Dynamic Database:")
                appendLine("• Custom table builder")
                appendLine("• Multiple field types")
                appendLine("• Template system")
                appendLine()
                appendLine("📅 Smart Calendar:")
                appendLine("• Event management")
                appendLine("• Date field integration")
                appendLine("• Monthly/daily views")
            })
            .setPositiveButton("Awesome!", null)
            .show()
    }

    private fun openHelpCenter() {
        openWebPage("https://linheriawan.github.io/docs/aksaranotes/index.html")
    }

    private fun reportBug() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:contact.aksara@tuta.com")
            putExtra(Intent.EXTRA_SUBJECT, "Notes by Aksara - Bug Report")
            putExtra(Intent.EXTRA_TEXT, buildString {
                appendLine("Bug Report for Notes by Aksara")
                appendLine()
                appendLine("Device Info:")
                appendLine("- Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                appendLine("- Android: ${android.os.Build.VERSION.RELEASE}")
                appendLine("- App Version: 1.0.0")
                appendLine()
                appendLine("Bug Description:")
                appendLine()
                appendLine("Steps to Reproduce:")
                appendLine("1. ")
                appendLine("2. ")
                appendLine("3. ")
                appendLine()
                appendLine("Expected Behavior:")
                appendLine()
                appendLine("Actual Behavior:")
                appendLine()
                appendLine("Screenshots (if applicable):")
                appendLine("Please attach screenshots if they help explain the issue.")
            })
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            showToast("No email app found")
        }
    }

    private fun requestFeature() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:contact.aksara@tuta.com")
            putExtra(Intent.EXTRA_SUBJECT, "Notes by Aksara - Feature Request")
            putExtra(Intent.EXTRA_TEXT, buildString {
                appendLine("Feature Request for Notes by Aksara")
                appendLine()
                appendLine("Requested Feature:")
                appendLine()
                appendLine("Why is this feature important?")
                appendLine()
                appendLine("How would you use this feature?")
                appendLine()
                appendLine("Additional Details:")
                appendLine()
            })
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            showToast("No email app found")
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}