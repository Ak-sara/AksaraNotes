package com.aksara.notes.data.database.entities

import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class Note : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()
    var id: String = _id.toHexString()
    var title: String = ""
    var content: String = ""
    var createdAt: Long = System.currentTimeMillis()
    var updatedAt: Long = System.currentTimeMillis()
    var requiresPin: Boolean = false
    var isEncrypted: Boolean = false
    var isFavorite: Boolean = false
    var tags: String = "" // JSON string of tags
}
