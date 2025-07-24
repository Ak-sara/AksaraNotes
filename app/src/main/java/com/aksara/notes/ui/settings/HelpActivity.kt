package com.aksara.notes.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aksara.notes.databinding.ActivityHelpBinding

class HelpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Help & Support"
    }

    private fun setupClickListeners() {
        // Getting Started
        binding.cardGettingStarted.setOnClickListener {
            showGettingStartedGuide()
        }

        // Notes Tutorial
        binding.cardNotesTutorial.setOnClickListener {
            showNotesTutorial()
        }

        // Database Tutorial
        binding.cardDatabaseTutorial.setOnClickListener {
            showDatabaseTutorial()
        }

        // Calendar Tutorial
        binding.cardCalendarTutorial.setOnClickListener {
            showCalendarTutorial()
        }

        // Security Guide
        binding.cardSecurityGuide.setOnClickListener {
            showSecurityGuide()
        }

        // FAQ
        binding.cardFaq.setOnClickListener {
            showFAQ()
        }

        // Report Bug
        binding.cardReportBug.setOnClickListener {
            reportBug()
        }

        // Feature Request
        binding.cardFeatureRequest.setOnClickListener {
            requestFeature()
        }

        // Tips & Tricks
        binding.cardTipsTricks.setOnClickListener {
            showTipsAndTricks()
        }

        // Troubleshooting
        binding.cardTroubleshooting.setOnClickListener {
            showTroubleshooting()
        }
    }

    private fun showGettingStartedGuide() {
        AlertDialog.Builder(this)
            .setTitle("🚀 Getting Started with Aksara Notes")
            .setMessage(buildString {
                appendLine("Welcome to Aksara Notes! Here's how to get started:")
                appendLine()
                appendLine("1️⃣ Set Up Security")
                appendLine("• Create a master password")
                appendLine("• Enable biometric authentication (optional)")
                appendLine("• Configure auto-lock settings")
                appendLine()
                appendLine("2️⃣ Create Your First Note")
                appendLine("• Tap the '+' button in Notes tab")
                appendLine("• Add title and content")
                appendLine("• Use PIN protection for sensitive notes")
                appendLine()
                appendLine("3️⃣ Build Custom Databases")
                appendLine("• Go to Database tab")
                appendLine("• Tap '+' to create a custom table")
                appendLine("• Choose from templates or create from scratch")
                appendLine()
                appendLine("4️⃣ Manage Your Calendar")
                appendLine("• Add events in Calendar tab")
                appendLine("• Database items with dates appear automatically")
                appendLine()
                appendLine("🎯 Pro Tip: Use the navigation drawer to access settings and more features!")
            })
            .setPositiveButton("Got it!", null)
            .show()
    }

    private fun showNotesTutorial() {
        AlertDialog.Builder(this)
            .setTitle("📝 Notes Tutorial")
            .setMessage(buildString {
                appendLine("Master the Notes feature:")
                appendLine()
                appendLine("📄 Creating Notes:")
                appendLine("• Tap '+' to create a new note")
                appendLine("• Add title and rich content")
                appendLine("• Use formatting options")
                appendLine()
                appendLine("🔒 Security Features:")
                appendLine("• Toggle PIN protection for sensitive notes")
                appendLine("• PIN-protected notes show a lock icon")
                appendLine("• Require authentication to view/edit")
                appendLine()
                appendLine("⭐ Organization:")
                appendLine("• Mark important notes as favorites")
                appendLine("• Use search to find notes quickly")
                appendLine("• Sort by date, title, or favorites")
                appendLine()
                appendLine("📤 Sharing:")
                appendLine("• Share notes as text via any app")
                appendLine("• PIN-protected notes require unlock first")
                appendLine()
                appendLine("✏️ Editing:")
                appendLine("• Tap any note to view/edit")
                appendLine("• Changes are saved automatically")
                appendLine("• Delete with the trash icon")
            })
            .setPositiveButton("Clear!", null)
            .show()
    }

    private fun showDatabaseTutorial() {
        AlertDialog.Builder(this)
            .setTitle("🗄️ Database Tutorial")
            .setMessage(buildString {
                appendLine("Create powerful custom databases:")
                appendLine()
                appendLine("🏗️ Building Tables:")
                appendLine("• Tap '+' to create a new table")
                appendLine("• Choose from pre-built templates")
                appendLine("• Or build completely custom tables")
                appendLine()
                appendLine("📋 Field Types:")
                appendLine("• Text - For names, descriptions")
                appendLine("• Number - For quantities, amounts")
                appendLine("• Currency - For money values")
                appendLine("• Date - Auto-integrates with calendar")
                appendLine("• Boolean - For yes/no questions")
                appendLine("• Select - For dropdown choices")
                appendLine("• Rating - For 1-5 star ratings")
                appendLine()
                appendLine("📊 Managing Data:")
                appendLine("• Tap any table to view items")
                appendLine("• Add new items with '+' button")
                appendLine("• Edit items by tapping them")
                appendLine("• Required fields are marked with *")
                appendLine()
                appendLine("📅 Calendar Integration:")
                appendLine("• Tables with date fields link to calendar")
                appendLine("• Events appear automatically")
                appendLine("• Perfect for subscriptions, birthdays, etc.")
            })
            .setPositiveButton("Awesome!", null)
            .show()
    }

    private fun showCalendarTutorial() {
        AlertDialog.Builder(this)
            .setTitle("📅 Calendar Tutorial")
            .setMessage(buildString {
                appendLine("Stay organized with the smart calendar:")
                appendLine()
                appendLine("📍 Viewing Events:")
                appendLine("• Monthly view shows all events")
                appendLine("• Tap any date to see daily events")
                appendLine("• Events from database tables appear automatically")
                appendLine()
                appendLine("➕ Adding Events:")
                appendLine("• Tap '+' to create manual events")
                appendLine("• Set title, date, and time")
                appendLine("• Choose event colors")
                appendLine()
                appendLine("🔗 Database Integration:")
                appendLine("• Any table with date fields creates events")
                appendLine("• Subscription renewal dates")
                appendLine("• Birthday reminders")
                appendLine("• Appointment tracking")
                appendLine()
                appendLine("🎨 Customization:")
                appendLine("• Different colors for different event types")
                appendLine("• Clear visual indicators")
                appendLine("• Easy date navigation")
                appendLine()
                appendLine("💡 Smart Features:")
                appendLine("• Today's date is highlighted")
                appendLine("• Quick access to today's events")
                appendLine("• Seamless month navigation")
            })
            .setPositiveButton("Perfect!", null)
            .show()
    }

    private fun showSecurityGuide() {
        AlertDialog.Builder(this)
            .setTitle("🔐 Security Guide")
            .setMessage(buildString {
                appendLine("Keep your data secure:")
                appendLine()
                appendLine("🔑 Master Password:")
                appendLine("• Required to access the app")
                appendLine("• Cannot be recovered if forgotten")
                appendLine("• Used to encrypt all data")
                appendLine("• Choose a strong, memorable password")
                appendLine()
                appendLine("👆 Biometric Authentication:")
                appendLine("• Quick access with fingerprint/face")
                appendLine("• Enable in Security Settings")
                appendLine("• Falls back to master password")
                appendLine()
                appendLine("🔒 Auto-Lock:")
                appendLine("• Automatically locks app when inactive")
                appendLine("• Configurable timeout periods")
                appendLine("• Immediate, 1min, 5min, 15min, 1hour")
                appendLine()
                appendLine("📌 PIN Protection:")
                appendLine("• Additional security for sensitive notes")
                appendLine("• Uses same authentication method")
                appendLine("• Protects individual notes")
                appendLine()
                appendLine("🛡️ Data Encryption:")
                appendLine("• All data encrypted at rest")
                appendLine("• Industry-standard encryption")
                appendLine("• Data never leaves your device unencrypted")
            })
            .setPositiveButton("Secure!", null)
            .show()
    }

    private fun showFAQ() {
        AlertDialog.Builder(this)
            .setTitle("❓ Frequently Asked Questions")
            .setMessage(buildString {
                appendLine("Q: I forgot my master password. What do I do?")
                appendLine("A: Unfortunately, master passwords cannot be recovered. You'll need to reset the app, which will erase all data. This is by design for security.")
                appendLine()
                appendLine("Q: Can I sync data across devices?")
                appendLine("A: Currently, data is stored locally for security. Cloud sync is planned for future updates with end-to-end encryption.")
                appendLine()
                appendLine("Q: How do I backup my data?")
                appendLine("A: The backup feature is coming soon! You'll be able to create encrypted backups to your device storage.")
                appendLine()
                appendLine("Q: Why do I need to authenticate so often?")
                appendLine("A: This is for security. You can adjust auto-lock settings in Security Settings to reduce frequency.")
                appendLine()
                appendLine("Q: Can I import data from other apps?")
                appendLine("A: The database builder allows creating tables for any data structure. Manual import tools are planned for future updates.")
                appendLine()
                appendLine("Q: Is my biometric data stored in the app?")
                appendLine("A: No! Biometric authentication uses your device's secure enclave. No biometric data is stored in Aksara Notes.")
                appendLine()
                appendLine("Q: How do I create complex database relationships?")
                appendLine("A: Currently, tables are independent. Relationship features are planned for future updates.")
            })
            .setPositiveButton("Thanks!", null)
            .show()
    }

    private fun showTipsAndTricks() {
        AlertDialog.Builder(this)
            .setTitle("💡 Tips & Tricks")
            .setMessage(buildString {
                appendLine("Get the most out of Aksara Notes:")
                appendLine()
                appendLine("🚀 Productivity Tips:")
                appendLine("• Use templates to quickly create common tables")
                appendLine("• Pin frequently accessed notes as favorites")
                appendLine("• Set up subscription tracking with the template")
                appendLine("• Create a birthday database for reminders")
                appendLine()
                appendLine("🔍 Search Like a Pro:")
                appendLine("• Search works across all note content")
                appendLine("• Use keywords from titles and content")
                appendLine("• Favorites appear at the top")
                appendLine()
                appendLine("🗂️ Organization Strategies:")
                appendLine("• Use consistent naming conventions")
                appendLine("• Create separate tables for different data types")
                appendLine("• Use the rating field for priorities")
                appendLine("• Date fields automatically create calendar events")
                appendLine()
                appendLine("🔒 Security Best Practices:")
                appendLine("• Use PIN protection for sensitive notes only")
                appendLine("• Enable biometric auth for quick access")
                appendLine("• Set appropriate auto-lock timeouts")
                appendLine("• Regularly review security settings")
                appendLine()
                appendLine("⚡ Quick Actions:")
                appendLine("• Swipe navigation drawer for quick settings")
                appendLine("• Use bottom tabs for main features")
                appendLine("• Long-press for additional options (coming soon)")
            })
            .setPositiveButton("Great tips!", null)
            .show()
    }

    private fun showTroubleshooting() {
        AlertDialog.Builder(this)
            .setTitle("🔧 Troubleshooting")
            .setMessage(buildString {
                appendLine("Common issues and solutions:")
                appendLine()
                appendLine("🔐 Authentication Issues:")
                appendLine("• Biometric not working? Check device settings")
                appendLine("• App keeps asking for auth? Check auto-lock settings")
                appendLine("• Can't remember password? Unfortunately, reset is required")
                appendLine()
                appendLine("📱 App Performance:")
                appendLine("• App slow? Restart the app")
                appendLine("• Crashes? Clear app cache in device settings")
                appendLine("• Storage full? Archive old notes or tables")
                appendLine()
                appendLine("🗄️ Database Problems:")
                appendLine("• Can't create table? Ensure valid field names")
                appendLine("• Form validation errors? Check required fields (marked with *)")
                appendLine("• Date picker not working? Try tapping the date field directly")
                appendLine()
                appendLine("📅 Calendar Issues:")
                appendLine("• Events not showing? Check if table has date fields")
                appendLine("• Wrong dates? Verify date format in table items")
                appendLine("• Can't navigate? Use month arrows or tap dates")
                appendLine()
                appendLine("💾 Data Concerns:")
                appendLine("• Data missing? Check if you're in the right tab")
                appendLine("• Changes not saving? Ensure you tap 'Save' button")
                appendLine("• Want to export? Backup feature coming soon")
                appendLine()
                appendLine("Still having issues? Contact support!")
            })
            .setPositiveButton("Helpful!", null)
            .show()
    }

    private fun reportBug() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:bugs@aksaranotes.com")
            putExtra(Intent.EXTRA_SUBJECT, "Aksara Notes - Bug Report")
            putExtra(Intent.EXTRA_TEXT, buildString {
                appendLine("Bug Report for Aksara Notes")
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
            data = Uri.parse("mailto:features@aksaranotes.com")
            putExtra(Intent.EXTRA_SUBJECT, "Aksara Notes - Feature Request")
            putExtra(Intent.EXTRA_TEXT, buildString {
                appendLine("Feature Request for Aksara Notes")
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