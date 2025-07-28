package com.aksara.notes.data.database.dao

import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.Sort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.aksara.notes.data.database.entities.Form
import com.aksara.notes.data.database.RealmDatabase

class FormDao {
    private val realm: Realm get() = RealmDatabase.getInstance()

    fun getAllForms(): Flow<List<Form>> {
        return realm.query<Form>()
            .sort("updatedAt", Sort.DESCENDING)
            .asFlow()
            .map { it.list }
    }

    fun getFormsByDataset(datasetId: String): Flow<List<Form>> {
        return realm.query<Form>("datasetId == $0", datasetId)
            .sort("updatedAt", Sort.DESCENDING)
            .asFlow()
            .map { it.list }
    }

    suspend fun getFormById(id: String): Form? {
        return realm.query<Form>("id == $0", id).first().find()
    }

    suspend fun insertForm(form: Form) {
        realm.write {
            copyToRealm(form)
        }
    }

    suspend fun updateForm(form: Form) {
        realm.write {
            val existingForm = query<Form>("id == $0", form.id).first().find()
            existingForm?.let {
                it.datasetId = form.datasetId
                it.data = form.data
                it.updatedAt = form.updatedAt
            }
        }
    }

    suspend fun deleteForm(form: Form) {
        realm.write {
            val formToDelete = query<Form>("id == $0", form.id).first().find()
            formToDelete?.let { delete(it) }
        }
    }

    fun searchForms(searchQuery: String): Flow<List<Form>> {
        return realm.query<Form>("data CONTAINS[c] $0", searchQuery)
            .sort("updatedAt", Sort.DESCENDING)
            .asFlow()
            .map { it.list }
    }

    suspend fun deleteFormsByDataset(datasetId: String) {
        realm.write {
            val formsToDelete = query<Form>("datasetId == $0", datasetId).find()
            delete(formsToDelete)
        }
    }

    suspend fun getFormCountForDataset(datasetId: String): Int {
        return realm.query<Form>("datasetId == $0", datasetId).count().find().toInt()
    }
    
    // Backup-specific methods
    suspend fun getAllFormsForBackup(): List<Form> {
        return realm.query<Form>()
            .sort("updatedAt", Sort.DESCENDING)
            .find()
    }
    
    suspend fun clearAllForms() {
        realm.write {
            val allForms = query<Form>().find()
            delete(allForms)
        }
    }
    
    suspend fun insertFormFromBackup(form: Form) {
        insertForm(form)
    }
}