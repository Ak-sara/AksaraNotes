package com.aksara.notes.data.repository

import kotlinx.coroutines.flow.Flow
import com.aksara.notes.data.database.dao.DatasetDao
import com.aksara.notes.data.database.dao.FormDao
import com.aksara.notes.data.database.entities.Dataset
import com.aksara.notes.data.database.entities.TableColumn
import com.aksara.notes.data.database.entities.Form

class DatabaseRepository {
    private val datasetDao = DatasetDao()
    private val formDao = FormDao()

    // Dataset operations
    fun getAllDatasets(): Flow<List<Dataset>> = datasetDao.getAllDatasets()
    suspend fun getDatasetById(id: String): Dataset? = datasetDao.getDatasetById(id)
    suspend fun insertDataset(dataset: Dataset) = datasetDao.insertDataset(dataset)
    suspend fun insertDataset(name: String, description: String, icon: String, datasetType: String, columns: List<TableColumn>) = datasetDao.insertDataset(name, description, icon, datasetType, columns)
    suspend fun updateDataset(dataset: Dataset) = datasetDao.updateDataset(dataset)
    suspend fun updateDataset(datasetId: String, name: String, description: String, icon: String, datasetType: String, columns: List<TableColumn>, createdAt: Long) = datasetDao.updateDataset(datasetId, name, description, icon, datasetType, columns, createdAt)
    suspend fun deleteDataset(dataset: Dataset) = datasetDao.deleteDataset(dataset)
    fun searchDatasets(query: String): Flow<List<Dataset>> = datasetDao.searchDatasets(query)

    // Form operations
    fun getAllForms(): Flow<List<Form>> = formDao.getAllForms()
    fun getFormsByDataset(datasetId: String): Flow<List<Form>> = formDao.getFormsByDataset(datasetId)
    suspend fun getFormById(id: String): Form? = formDao.getFormById(id)
    suspend fun insertForm(form: Form) = formDao.insertForm(form)
    suspend fun updateForm(form: Form) = formDao.updateForm(form)
    suspend fun deleteForm(form: Form) = formDao.deleteForm(form)
    fun searchForms(query: String): Flow<List<Form>> = formDao.searchForms(query)
    suspend fun deleteFormsByDataset(datasetId: String) = formDao.deleteFormsByDataset(datasetId)
    suspend fun getFormCountForDataset(datasetId: String): Int = formDao.getFormCountForDataset(datasetId)
}