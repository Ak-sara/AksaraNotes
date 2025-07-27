package com.aksara.notes.data.database.entities

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class Form : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()
    var id: String = _id.toHexString()
    var datasetId: String = "" // References Dataset.id
    var data: String = "" // JSON string of field values {"field_name": "value", ...}
    var createdAt: Long = System.currentTimeMillis()
    var updatedAt: Long = System.currentTimeMillis()
}