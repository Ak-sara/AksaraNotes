package com.aksara.notes.data.templates

import io.realm.kotlin.ext.realmListOf
import com.aksara.notes.data.models.ColumnType
import com.aksara.notes.data.database.entities.Dataset
import com.aksara.notes.data.database.entities.TableColumn
import com.aksara.notes.data.database.entities.TableSettings

object DatasetTemplates {

    fun getTemplate(templateName: String): Dataset {
        val template = when (templateName) {
            "accounts" -> getAccountsTemplate()
            "subscriptions" -> getSubscriptionsTemplate()
            "contacts" -> getContactsTemplate()
            "books" -> getBooksTemplate()
            "movies" -> getMoviesTemplate()
            "workout" -> getWorkoutTemplate()
            else -> getBlankTemplate()
        }
        android.util.Log.d("DatasetTemplates", "Created template '$templateName' with ${template.columns.size} columns")
        template.columns.forEach { column ->
            android.util.Log.d("DatasetTemplates", "Template column: ${column.name} (${column.type})")
        }
        return template
    }

    private fun getAccountsTemplate(): Dataset {
        return Dataset().apply {
            name = "Accounts & Passwords"
            description = "Store login credentials and account information"
            icon = "🔐"
            datasetType = "data"
            
            columns.apply {
                add(createColumn("organization", ColumnType.TEXT, required = true))
                add(createColumn("website", ColumnType.URL))
                add(createColumn("username", ColumnType.TEXT, required = true))
                add(createColumn("email", ColumnType.EMAIL))
                add(createColumn("password", ColumnType.TEXT, required = true))
                add(createColumn("category", ColumnType.SELECT, options = """{"options": ["Banking", "Social Media", "Work", "Shopping", "Entertainment", "Other"]}"""))
                add(createColumn("notes", ColumnType.TEXT))
            }
            
            settings = TableSettings().apply {
                primaryColor = "#F44336"
                showInCalendar = false
            }
        }
    }

    private fun getSubscriptionsTemplate(): Dataset {
        return Dataset().apply {
            name = "Subscriptions"
            description = "Track recurring payments and due dates"
            icon = "💰"
            datasetType = "data"
            
            columns.apply {
                add(createColumn("service_name", ColumnType.TEXT, required = true))
                add(createColumn("amount", ColumnType.CURRENCY, required = true))
                add(createColumn("billing_cycle", ColumnType.FREQUENCY, required = true, 
                    options = """{"frequency": "Monthly"}"""))
                add(createColumn("next_due_date", ColumnType.DATE, required = true,
                    options = """{"showInCalendar": true, "isRecurring": true, "frequencyField": "billing_cycle"}"""))
                add(createColumn("last_paid_date", ColumnType.DATE))
                add(createColumn("category", ColumnType.SELECT,
                    options = """{"options": ["Entertainment", "Software", "Utilities", "Health", "Education", "Other"]}"""))
                add(createColumn("website", ColumnType.URL))
                add(createColumn("is_active", ColumnType.BOOLEAN, defaultValue = "true"))
                add(createColumn("auto_renew", ColumnType.BOOLEAN, defaultValue = "true"))
                add(createColumn("payment_method", ColumnType.TEXT))
                add(createColumn("notes", ColumnType.TEXT))
            }
            
            settings = TableSettings().apply {
                primaryColor = "#4CAF50"
                showInCalendar = true
                calendarDateField = "next_due_date"
                reminderDays = 3
            }
        }
    }

    private fun getContactsTemplate(): Dataset {
        return Dataset().apply {
            name = "Contacts"
            description = "Personal and business contacts"
            icon = "📞"
            datasetType = "data"
            
            columns.apply {
                add(createColumn("name", ColumnType.TEXT, required = true))
                add(createColumn("phone", ColumnType.PHONE))
                add(createColumn("email", ColumnType.EMAIL))
                add(createColumn("company", ColumnType.TEXT))
                add(createColumn("position", ColumnType.TEXT))
                add(createColumn("birthday", ColumnType.DATE))
                add(createColumn("relationship", ColumnType.SELECT,
                    options = """{"options": ["Family", "Friend", "Colleague", "Business", "Other"]}"""))
                add(createColumn("address", ColumnType.TEXT))
                add(createColumn("rating", ColumnType.RATING))
                add(createColumn("notes", ColumnType.TEXT))
            }
            
            settings = TableSettings().apply {
                primaryColor = "#FF9800"
                showInCalendar = true
                calendarDateField = "birthday"
            }
        }
    }

    private fun getBooksTemplate(): Dataset {
        return Dataset().apply {
            name = "Books to Read"
            description = "Track your reading list and progress"
            icon = "📚"
            datasetType = "data"
            
            columns.apply {
                add(createColumn("title", ColumnType.TEXT, required = true))
                add(createColumn("author", ColumnType.TEXT, required = true))
                add(createColumn("status", ColumnType.SELECT, required = true,
                    options = """{"options": ["Want to Read", "Reading", "Finished", "Abandoned"]}"""))
                add(createColumn("rating", ColumnType.RATING))
                add(createColumn("pages", ColumnType.NUMBER))
                add(createColumn("date_started", ColumnType.DATE))
                add(createColumn("date_finished", ColumnType.DATE))
                add(createColumn("genre", ColumnType.SELECT,
                    options = """{"options": ["Fiction", "Non-Fiction", "Science", "Biography", "History", "Other"]}"""))
                add(createColumn("notes", ColumnType.TEXT))
            }
            
            settings = TableSettings().apply {
                primaryColor = "#795548"
                showInCalendar = false
            }
        }
    }

    private fun getMoviesTemplate(): Dataset {
        return Dataset().apply {
            name = "Movies & Shows"
            description = "Track movies and TV shows"
            icon = "🎬"
            datasetType = "data"
            
            columns.apply {
                add(createColumn("title", ColumnType.TEXT, required = true))
                add(createColumn("year", ColumnType.NUMBER))
                add(createColumn("status", ColumnType.SELECT, required = true,
                    options = """{"options": ["Want to Watch", "Watching", "Finished", "Abandoned"]}"""))
                add(createColumn("rating", ColumnType.RATING))
                add(createColumn("genre", ColumnType.SELECT,
                    options = """{"options": ["Action", "Comedy", "Drama", "Horror", "Sci-Fi", "Documentary", "Other"]}"""))
                add(createColumn("director", ColumnType.TEXT))
                add(createColumn("date_watched", ColumnType.DATE))
                add(createColumn("platform", ColumnType.TEXT))
                add(createColumn("notes", ColumnType.TEXT))
            }
            
            settings = TableSettings().apply {
                primaryColor = "#E91E63"
                showInCalendar = false
            }
        }
    }

    private fun getWorkoutTemplate(): Dataset {
        return Dataset().apply {
            name = "Workout Log"
            description = "Track your fitness activities"
            icon = "🏋️"
            datasetType = "data"
            
            columns.apply {
                add(createColumn("date", ColumnType.DATE, required = true))
                add(createColumn("exercise_type", ColumnType.SELECT, required = true,
                    options = """{"options": ["Cardio", "Strength", "Yoga", "Sports", "Other"]}"""))
                add(createColumn("duration_minutes", ColumnType.NUMBER, required = true))
                add(createColumn("intensity", ColumnType.SELECT,
                    options = """{"options": ["Low", "Medium", "High"]}"""))
                add(createColumn("calories_burned", ColumnType.NUMBER))
                add(createColumn("exercises", ColumnType.TEXT))
                add(createColumn("notes", ColumnType.TEXT))
            }
            
            settings = TableSettings().apply {
                primaryColor = "#9C27B0"
                showInCalendar = true
                calendarDateField = "date"
            }
        }
    }

    private fun getBlankTemplate(): Dataset {
        return Dataset().apply {
            name = ""
            description = ""
            icon = "📄"
            datasetType = "data"
            settings = TableSettings().apply {
                primaryColor = "#2196F3"
                showInCalendar = false
            }
        }
    }

    // Helper function to create TableColumn objects consistently
    private fun createColumn(
        name: String, 
        type: ColumnType, 
        required: Boolean = false,
        defaultValue: String = "",
        options: String = "{}"
    ): TableColumn {
        return TableColumn().apply {
            this.name = name
            this.type = type.name
            this.displayName = type.displayName
            this.icon = type.icon
            this.required = required
            this.defaultValue = defaultValue
            this.options = options
        }
    }
}