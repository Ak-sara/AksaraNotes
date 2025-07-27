package com.aksara.notes.data.database

import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import com.aksara.notes.data.database.entities.Note
import com.aksara.notes.data.database.entities.Dataset
import com.aksara.notes.data.database.entities.Form
import com.aksara.notes.data.database.entities.TableColumn
import com.aksara.notes.data.database.entities.TableSettings

object RealmDatabase {
    private var realm: Realm? = null
    
    fun initialize() {
        val config = RealmConfiguration.Builder(
            schema = setOf(
                Note::class,
                Dataset::class,
                Form::class,
                TableColumn::class,
                TableSettings::class
            )
        )
            .name("aksara_notes.realm")
            .schemaVersion(1)
            .build()
        
        realm = Realm.open(config)
    }
    
    fun getInstance(): Realm {
        return realm ?: throw IllegalStateException("Realm not initialized. Call initialize() first.")
    }
    
    fun close() {
        realm?.close()
        realm = null
    }
}