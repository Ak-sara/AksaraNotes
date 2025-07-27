package com.aksara.notes.data.database.entities

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class TableColumn : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()
    var id: String = _id.toHexString()
    var name: String = ""
    var type: String = "TEXT" // Store ColumnType as string
    var required: Boolean = false
    var defaultValue: String = ""
    var options: String = "{}" // Keep as JSON for now, can be improved later
    var displayName: String = ""
    var icon: String = ""
}