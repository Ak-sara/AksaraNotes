package com.aksara.notes.data.database.entities

import io.realm.kotlin.types.RealmObject

class TableSettings : RealmObject {
    var primaryColor: String = "#2196F3"
    var showInCalendar: Boolean = false
    var calendarDateField: String = ""
    var reminderDays: Int = 3
}