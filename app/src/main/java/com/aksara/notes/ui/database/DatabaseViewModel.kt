package com.aksara.notes.ui.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.aksara.notes.data.database.entities.Dataset
import com.aksara.notes.data.database.entities.TableColumn
import com.aksara.notes.data.database.entities.Form
import com.aksara.notes.data.repository.DatabaseRepository

class DatabaseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DatabaseRepository()

    // Datasets
    val allDatasets: LiveData<List<Dataset>> = repository.getAllDatasets().asLiveData()

    // All Forms from all datasets
    val allForms: LiveData<List<Form>> = repository.getAllForms().asLiveData()

    // Dataset operations
    fun insertDataset(dataset: Dataset) = viewModelScope.launch {
        repository.insertDataset(dataset)
    }
    
    fun insertDataset(name: String, description: String, icon: String, datasetType: String, columns: List<TableColumn>) = viewModelScope.launch {
        repository.insertDataset(name, description, icon, datasetType, columns)
    }

    fun updateDataset(dataset: Dataset) = viewModelScope.launch {
        repository.updateDataset(dataset)
    }
    
    fun updateDataset(datasetId: String, name: String, description: String, icon: String, datasetType: String, columns: List<TableColumn>, createdAt: Long) = viewModelScope.launch {
        repository.updateDataset(datasetId, name, description, icon, datasetType, columns, createdAt)
    }

    fun deleteDataset(dataset: Dataset) = viewModelScope.launch {
        // First delete all forms in this dataset
        repository.deleteFormsByDataset(dataset.id)
        // Then delete the dataset itself
        repository.deleteDataset(dataset)
    }

    suspend fun getDatasetById(id: String): Dataset? {
        return repository.getDatasetById(id)
    }

    // Form operations
    fun insertForm(form: Form) = viewModelScope.launch {
        repository.insertForm(form)
    }

    fun updateForm(form: Form) = viewModelScope.launch {
        repository.updateForm(form)
    }

    fun deleteForm(form: Form) = viewModelScope.launch {
        repository.deleteForm(form)
    }

    suspend fun getFormById(id: String): Form? {
        return repository.getFormById(id)
    }

    suspend fun getFormCountForDataset(datasetId: String): Int {
        return repository.getFormCountForDataset(datasetId)
    }

    fun getFormsByDataset(datasetId: String): LiveData<List<Form>> {
        return repository.getFormsByDataset(datasetId).asLiveData()
    }
}