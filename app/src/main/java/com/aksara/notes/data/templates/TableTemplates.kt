package com.aksara.notes.data.templates

import com.aksara.notes.data.models.TableColumn
import com.aksara.notes.data.models.ColumnType
import com.aksara.notes.data.models.TableSettings
import com.aksara.notes.data.database.entities.CustomTable
import com.google.gson.Gson

object TableTemplates {

    fun getTemplate(templateName: String): Pair<CustomTable, List<TableColumn>> {
        return when (templateName) {
            "accounts" -> getAccountsTemplate()
            "subscriptions" -> getSubscriptionsTemplate()
            "bond_calculator" -> getBondCalculatorTemplate()
            "contacts" -> getContactsTemplate()
            "books" -> getBooksTemplate()
            "movies" -> getMoviesTemplate()
            "workout" -> getWorkoutTemplate()
            else -> getBlankTemplate()
        }
    }

    private fun getAccountsTemplate(): Pair<CustomTable, List<TableColumn>> {
        val columns = listOf(
            TableColumn(
                name = "organization",
                type = ColumnType.TEXT,
                required = true
            ),
            TableColumn(
                name = "website",
                type = ColumnType.URL
            ),
            TableColumn(
                name = "username",
                type = ColumnType.TEXT,
                required = true
            ),
            TableColumn(
                name = "email",
                type = ColumnType.EMAIL
            ),
            TableColumn(
                name = "password",
                type = ColumnType.TEXT,
                required = true
            ),
            TableColumn(
                name = "account_number",
                type = ColumnType.TEXT
            ),
            TableColumn(
                name = "category",
                type = ColumnType.SELECT,
                options = mapOf(
                    "options" to listOf("Banking", "Social Media", "Work", "Shopping", "Entertainment", "Other")
                )
            ),
            TableColumn(
                name = "is_favorite",
                type = ColumnType.BOOLEAN
            ),
            TableColumn(
                name = "notes",
                type = ColumnType.TEXT
            )
        )

        val table = CustomTable(
            name = "Accounts & Passwords",
            description = "Store login credentials and account information",
            icon = "🔐",
            tableType = "data",
            columns = Gson().toJson(columns)
        )

        return Pair(table, columns)
    }

    private fun getSubscriptionsTemplate(): Pair<CustomTable, List<TableColumn>> {
        val columns = listOf(
            TableColumn(
                name = "service_name",
                type = ColumnType.TEXT,
                required = true
            ),
            TableColumn(
                name = "amount",
                type = ColumnType.CURRENCY,
                required = true
            ),
            TableColumn(
                name = "billing_cycle",
                type = ColumnType.SELECT,
                required = true,
                options = mapOf(
                    "options" to listOf("Monthly", "Yearly", "Weekly", "Daily")
                )
            ),
            TableColumn(
                name = "next_due_date",
                type = ColumnType.DATE,
                required = true
            ),
            TableColumn(
                name = "last_paid_date",
                type = ColumnType.DATE
            ),
            TableColumn(
                name = "category",
                type = ColumnType.SELECT,
                options = mapOf(
                    "options" to listOf("Entertainment", "Software", "Utilities", "Health", "Education", "Other")
                )
            ),
            TableColumn(
                name = "website",
                type = ColumnType.URL
            ),
            TableColumn(
                name = "is_active",
                type = ColumnType.BOOLEAN,
                defaultValue = "true"
            ),
            TableColumn(
                name = "auto_renew",
                type = ColumnType.BOOLEAN,
                defaultValue = "true"
            ),
            TableColumn(
                name = "payment_method",
                type = ColumnType.TEXT
            ),
            TableColumn(
                name = "notes",
                type = ColumnType.TEXT
            )
        )

        val settings = TableSettings(
            showInCalendar = true,
            calendarDateField = "next_due_date",
            reminderDays = 3
        )

        val table = CustomTable(
            name = "Subscriptions",
            description = "Track recurring payments and due dates",
            icon = "💰",
            tableType = "data",
            columns = Gson().toJson(columns),
            settings = Gson().toJson(settings)
        )

        return Pair(table, columns)
    }

    private fun getBondCalculatorTemplate(): Pair<CustomTable, List<TableColumn>> {
        val columns = listOf(
            TableColumn(
                name = "bond_name",
                type = ColumnType.TEXT,
                required = true
            ),
            TableColumn(
                name = "face_value",
                type = ColumnType.CURRENCY,
                required = true,
                defaultValue = "1000"
            ),
            TableColumn(
                name = "coupon_rate",
                type = ColumnType.NUMBER,
                required = true
            ),
            TableColumn(
                name = "years_to_maturity",
                type = ColumnType.NUMBER,
                required = true
            ),
            TableColumn(
                name = "market_rate",
                type = ColumnType.NUMBER,
                required = true
            ),
            TableColumn(
                name = "calculation_date",
                type = ColumnType.DATE,
                defaultValue = "today"
            ),
            TableColumn(
                name = "notes",
                type = ColumnType.TEXT
            )
        )

        val table = CustomTable(
            name = "Bond Calculator",
            description = "Calculate bond prices and returns",
            icon = "🧮",
            tableType = "calculator",
            columns = Gson().toJson(columns)
        )

        return Pair(table, columns)
    }

    private fun getContactsTemplate(): Pair<CustomTable, List<TableColumn>> {
        val columns = listOf(
            TableColumn(
                name = "name",
                type = ColumnType.TEXT,
                required = true
            ),
            TableColumn(
                name = "phone",
                type = ColumnType.PHONE
            ),
            TableColumn(
                name = "email",
                type = ColumnType.EMAIL
            ),
            TableColumn(
                name = "company",
                type = ColumnType.TEXT
            ),
            TableColumn(
                name = "position",
                type = ColumnType.TEXT
            ),
            TableColumn(
                name = "birthday",
                type = ColumnType.DATE
            ),
            TableColumn(
                name = "relationship",
                type = ColumnType.SELECT,
                options = mapOf(
                    "options" to listOf("Family", "Friend", "Colleague", "Business", "Other")
                )
            ),
            TableColumn(
                name = "address",
                type = ColumnType.TEXT
            ),
            TableColumn(
                name = "rating",
                type = ColumnType.RATING
            ),
            TableColumn(
                name = "notes",
                type = ColumnType.TEXT
            )
        )

        val table = CustomTable(
            name = "Contacts",
            description = "Personal and business contacts",
            icon = "📞",
            tableType = "data",
            columns = Gson().toJson(columns)
        )

        return Pair(table, columns)
    }

    private fun getBooksTemplate(): Pair<CustomTable, List<TableColumn>> {
        val columns = listOf(
            TableColumn(
                name = "title",
                type = ColumnType.TEXT,
                required = true
            ),
            TableColumn(
                name = "author",
                type = ColumnType.TEXT,
                required = true
            ),
            TableColumn(
                name = "status",
                type = ColumnType.SELECT,
                required = true,
                options = mapOf(
                    "options" to listOf("Want to Read", "Reading", "Finished", "Abandoned")
                )
            ),
            TableColumn(
                name = "rating",
                type = ColumnType.RATING
            ),
            TableColumn(
                name = "pages",
                type = ColumnType.NUMBER
            ),
            TableColumn(
                name = "date_started",
                type = ColumnType.DATE
            ),
            TableColumn(
                name = "date_finished",
                type = ColumnType.DATE
            ),
            TableColumn(
                name = "genre",
                type = ColumnType.SELECT,
                options = mapOf(
                    "options" to listOf("Fiction", "Non-Fiction", "Science", "Biography", "History", "Other")
                )
            ),
            TableColumn(
                name = "notes",
                type = ColumnType.TEXT
            )
        )

        val table = CustomTable(
            name = "Books to Read",
            description = "Track your reading list and progress",
            icon = "📚",
            tableType = "data",
            columns = Gson().toJson(columns)
        )

        return Pair(table, columns)
    }

    private fun getMoviesTemplate(): Pair<CustomTable, List<TableColumn>> {
        val columns = listOf(
            TableColumn(
                name = "title",
                type = ColumnType.TEXT,
                required = true
            ),
            TableColumn(
                name = "year",
                type = ColumnType.NUMBER
            ),
            TableColumn(
                name = "status",
                type = ColumnType.SELECT,
                required = true,
                options = mapOf(
                    "options" to listOf("Want to Watch", "Watching", "Finished", "Abandoned")
                )
            ),
            TableColumn(
                name = "rating",
                type = ColumnType.RATING
            ),
            TableColumn(
                name = "genre",
                type = ColumnType.SELECT,
                options = mapOf(
                    "options" to listOf("Action", "Comedy", "Drama", "Horror", "Sci-Fi", "Documentary", "Other")
                )
            ),
            TableColumn(
                name = "director",
                type = ColumnType.TEXT
            ),
            TableColumn(
                name = "date_watched",
                type = ColumnType.DATE
            ),
            TableColumn(
                name = "platform",
                type = ColumnType.TEXT
            ),
            TableColumn(
                name = "notes",
                type = ColumnType.TEXT
            )
        )

        val table = CustomTable(
            name = "Movies & Shows",
            description = "Track movies and TV shows",
            icon = "🎬",
            tableType = "data",
            columns = Gson().toJson(columns)
        )

        return Pair(table, columns)
    }

    private fun getWorkoutTemplate(): Pair<CustomTable, List<TableColumn>> {
        val columns = listOf(
            TableColumn(
                name = "date",
                type = ColumnType.DATE,
                required = true
            ),
            TableColumn(
                name = "exercise_type",
                type = ColumnType.SELECT,
                required = true,
                options = mapOf(
                    "options" to listOf("Cardio", "Strength", "Yoga", "Sports", "Other")
                )
            ),
            TableColumn(
                name = "duration_minutes",
                type = ColumnType.NUMBER,
                required = true
            ),
            TableColumn(
                name = "intensity",
                type = ColumnType.SELECT,
                options = mapOf(
                    "options" to listOf("Low", "Medium", "High")
                )
            ),
            TableColumn(
                name = "calories_burned",
                type = ColumnType.NUMBER
            ),
            TableColumn(
                name = "exercises",
                type = ColumnType.TEXT
            ),
            TableColumn(
                name = "notes",
                type = ColumnType.TEXT
            )
        )

        val table = CustomTable(
            name = "Workout Log",
            description = "Track your fitness activities",
            icon = "🏋️",
            tableType = "data",
            columns = Gson().toJson(columns)
        )

        return Pair(table, columns)
    }

    private fun getBlankTemplate(): Pair<CustomTable, List<TableColumn>> {
        val table = CustomTable(
            name = "",
            description = "",
            icon = "📄",
            tableType = "data"
        )

        return Pair(table, emptyList())
    }
}