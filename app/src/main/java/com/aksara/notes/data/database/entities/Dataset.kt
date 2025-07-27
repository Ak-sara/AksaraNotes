package com.aksara.notes.data.database.entities

import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId

class Dataset : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()
    var id: String = _id.toHexString()
    var name: String = ""
    var description: String = ""
    var icon: String = "📄"
    var datasetType: String = "data"
    var columns: RealmList<TableColumn> = realmListOf() // Native Realm list
    var settings: TableSettings? = null // Native Realm object
    var createdAt: Long = System.currentTimeMillis()
    var updatedAt: Long = System.currentTimeMillis()
}