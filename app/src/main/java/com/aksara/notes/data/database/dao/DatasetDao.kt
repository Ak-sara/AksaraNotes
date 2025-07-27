package com.aksara.notes.data.database.dao

import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.query.Sort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.aksara.notes.data.database.entities.Dataset
import com.aksara.notes.data.database.entities.TableColumn
import com.aksara.notes.data.database.RealmDatabase

class DatasetDao {
    private val realm: Realm get() = RealmDatabase.getInstance()

    fun getAllDatasets(): Flow<List<Dataset>> {
        return realm.query<Dataset>()
            .sort("updatedAt", Sort.DESCENDING)
            .asFlow()
            .map { it.list }
    }

    suspend fun getDatasetById(id: String): Dataset? {
        val dataset = realm.query<Dataset>("id == $0", id).first().find()
        android.util.Log.d("DatasetDao", "getDatasetById($id) found dataset: ${dataset?.name} with ${dataset?.columns?.size ?: 0} columns")
        dataset?.columns?.forEach { column ->
            android.util.Log.d("DatasetDao", "Loaded column: ${column.name} (${column.type})")
        }
        return dataset
    }

    suspend fun insertDataset(dataset: Dataset) {
        android.util.Log.d("DatasetDao", "insertDataset called with ${dataset.columns.size} columns")
        dataset.columns.forEach { column ->
            android.util.Log.d("DatasetDao", "Column to insert: ${column.name} (${column.type})")
        }
        
        realm.write {
            // Create a new dataset and properly copy all nested objects
            val newDataset = Dataset().apply {
                id = dataset.id
                name = dataset.name
                description = dataset.description
                icon = dataset.icon
                datasetType = dataset.datasetType
                createdAt = dataset.createdAt
                updatedAt = dataset.updatedAt
            }
            
            // Copy columns
            android.util.Log.d("DatasetDao", "Copying ${dataset.columns.size} columns to new dataset")
            dataset.columns.forEach { column ->
                android.util.Log.d("DatasetDao", "Copying column: ${column.name}")
                // Create new Realm objects instead of copying potentially outdated ones
                val newColumn = TableColumn().apply {
                    id = column.id
                    this.name = column.name
                    this.type = column.type
                    this.required = column.required
                    this.defaultValue = column.defaultValue
                    this.options = column.options
                    this.displayName = column.displayName
                    this.icon = column.icon
                }
                newDataset.columns.add(copyToRealm(newColumn))
            }
            android.util.Log.d("DatasetDao", "New dataset has ${newDataset.columns.size} columns")
            
            // Copy settings if present
            dataset.settings?.let { settings ->
                newDataset.settings = copyToRealm(settings)
            }
            
            copyToRealm(newDataset)
            android.util.Log.d("DatasetDao", "Dataset inserted successfully")
        }
    }

    suspend fun updateDataset(dataset: Dataset) {
        android.util.Log.d("DatasetDao", "updateDataset called with ${dataset.columns.size} columns")
        dataset.columns.forEach { column ->
            android.util.Log.d("DatasetDao", "Column to save: ${column.name} (${column.type})")
        }
        
        realm.write {
            val existingDataset = query<Dataset>("id == $0", dataset.id).first().find()
            existingDataset?.let {
                android.util.Log.d("DatasetDao", "Found existing dataset: ${it.name}")
                it.name = dataset.name
                it.description = dataset.description
                it.icon = dataset.icon
                it.datasetType = dataset.datasetType
                // Properly update columns by clearing and adding all
                it.columns.clear()
                android.util.Log.d("DatasetDao", "Cleared existing columns, adding ${dataset.columns.size} new columns")
                dataset.columns.forEach { column ->
                    android.util.Log.d("DatasetDao", "Adding column: ${column.name}")
                    // Create new Realm objects instead of copying potentially outdated ones
                    val newColumn = TableColumn().apply {
                        id = column.id
                        this.name = column.name
                        this.type = column.type
                        this.required = column.required
                        this.defaultValue = column.defaultValue
                        this.options = column.options
                        this.displayName = column.displayName
                        this.icon = column.icon
                    }
                    it.columns.add(copyToRealm(newColumn))
                }
                android.util.Log.d("DatasetDao", "Final column count: ${it.columns.size}")
                it.settings = dataset.settings?.let { settings -> copyToRealm(settings) }
                it.updatedAt = dataset.updatedAt
            } ?: run {
                android.util.Log.e("DatasetDao", "Existing dataset not found!")
            }
        }
    }

    suspend fun deleteDataset(dataset: Dataset) {
        realm.write {
            val datasetToDelete = query<Dataset>("id == $0", dataset.id).first().find()
            datasetToDelete?.let { delete(it) }
        }
    }

    fun searchDatasets(searchQuery: String): Flow<List<Dataset>> {
        return realm.query<Dataset>("name CONTAINS[c] $0 OR description CONTAINS[c] $0", searchQuery)
            .sort("name", Sort.ASCENDING)
            .asFlow()
            .map { it.list }
    }

    fun getDatasetsByType(type: String): Flow<List<Dataset>> {
        return realm.query<Dataset>("datasetType == $0", type)
            .sort("updatedAt", Sort.DESCENDING)
            .asFlow()
            .map { it.list }
    }
    
    suspend fun insertDataset(name: String, description: String, icon: String, datasetType: String, columns: List<TableColumn>) {
        android.util.Log.d("DatasetDao", "insertDataset with parameters called with ${columns.size} columns")
        columns.forEach { column ->
            android.util.Log.d("DatasetDao", "Column parameter to insert: ${column.name} (${column.type})")
        }
        
        realm.write {
            // Create a new dataset and properly copy all nested objects
            val newDataset = Dataset().apply {
                this.name = name
                this.description = description
                this.icon = icon
                this.datasetType = datasetType
                this.createdAt = System.currentTimeMillis()
                this.updatedAt = System.currentTimeMillis()
            }
            
            // Copy columns
            android.util.Log.d("DatasetDao", "Creating ${columns.size} new columns")
            columns.forEach { column ->
                android.util.Log.d("DatasetDao", "Creating column: ${column.name}")
                // Create new Realm objects instead of copying potentially outdated ones
                val newColumn = TableColumn().apply {
                    id = column.id
                    this.name = column.name
                    this.type = column.type
                    this.required = column.required
                    this.defaultValue = column.defaultValue
                    this.options = column.options
                    this.displayName = column.displayName
                    this.icon = column.icon
                }
                newDataset.columns.add(copyToRealm(newColumn))
            }
            android.util.Log.d("DatasetDao", "New dataset has ${newDataset.columns.size} columns")
            
            copyToRealm(newDataset)
            android.util.Log.d("DatasetDao", "Dataset inserted successfully")
        }
    }
    
    suspend fun updateDataset(datasetId: String, name: String, description: String, icon: String, datasetType: String, columns: List<TableColumn>, createdAt: Long) {
        android.util.Log.d("DatasetDao", "updateDataset with parameters called with ${columns.size} columns")
        columns.forEach { column ->
            android.util.Log.d("DatasetDao", "Column parameter: ${column.name} (${column.type})")
        }
        
        realm.write {
            val existingDataset = query<Dataset>("id == $0", datasetId).first().find()
            existingDataset?.let {
                android.util.Log.d("DatasetDao", "Found existing dataset: ${it.name}")
                it.name = name
                it.description = description
                it.icon = icon
                it.datasetType = datasetType
                // Properly update columns by clearing and adding all
                it.columns.clear()
                android.util.Log.d("DatasetDao", "Cleared existing columns, adding ${columns.size} new columns")
                columns.forEach { column ->
                    android.util.Log.d("DatasetDao", "Adding column parameter: ${column.name}")
                    // Create new Realm objects instead of copying potentially outdated ones
                    val newColumn = TableColumn().apply {
                        id = column.id
                        this.name = column.name
                        this.type = column.type
                        this.required = column.required
                        this.defaultValue = column.defaultValue
                        this.options = column.options
                        this.displayName = column.displayName
                        this.icon = column.icon
                    }
                    it.columns.add(copyToRealm(newColumn))
                }
                android.util.Log.d("DatasetDao", "Final column count: ${it.columns.size}")
                it.updatedAt = System.currentTimeMillis()
            } ?: run {
                android.util.Log.e("DatasetDao", "Existing dataset not found with ID: $datasetId")
            }
        }
    }
}