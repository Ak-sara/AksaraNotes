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

        // Open Source Licenses
        binding.cardOpenSourceLicenses.setOnClickListener {
            showOpenSourceLicenses()
        }

        // GitHub Repository
        binding.cardGithubRepo.setOnClickListener {
            openGitHubRepo()
        }

        // What's New
        binding.cardWhatsNew.setOnClickListener {
            showWhatsNew()
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
            appendLine("🚀 Check out Aksara Notes!")
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
            putExtra(Intent.EXTRA_SUBJECT, "Aksara Notes - Secure Note Taking App")
        }

        try {
            startActivity(Intent.createChooser(intent, "Share Aksara Notes"))
        } catch (e: Exception) {
            showToast("Unable to share app")
        }
    }

    private fun showPrivacyPolicy() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://your-privacy-policy-url.com")
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Privacy policy will be available soon")
        }
    }

    private fun showTermsOfService() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://your-terms-url.com")
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Terms of service will be available soon")
        }
    }

    private fun showOpenSourceLicenses() {
        AlertDialog.Builder(this)
            .setTitle("Open Source Licenses")
            .setMessage(buildString {
                appendLine("This app uses the following open source libraries:")
                appendLine()
                appendLine("• Android Jetpack Libraries")
                appendLine("  Apache License 2.0")
                appendLine()
                appendLine("• Material Design Components")
                appendLine("  Apache License 2.0")
                appendLine()
                appendLine("• Room Database")
                appendLine("  Apache License 2.0")
                appendLine()
                appendLine("• Kotlin Coroutines")
                appendLine("  Apache License 2.0")
                appendLine()
                appendLine("• BiometricPrompt")
                appendLine("  Apache License 2.0")
                appendLine()
                appendLine("Full license texts are available at:")
                appendLine("https://www.apache.org/licenses/LICENSE-2.0")
            })
            .setPositiveButton("OK", null)
            .show()
    }

    private fun openGitHubRepo() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://github.com/yourusername/aksara-notes")
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            showToast("GitHub repository will be available soon")
        }
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

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}