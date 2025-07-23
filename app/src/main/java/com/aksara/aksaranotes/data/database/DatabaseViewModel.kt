package com.aksara.aksaranotes.ui.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.aksara.aksaranotes.data.database.AppDatabase
import com.aksara.aksaranotes.data.database.entities.CustomTable

class DatabaseViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val customTableDao = database.customTableDao()

    // Custom Tables
    val allTables: LiveData<List<CustomTable>> = customTableDao.getAllTables().asLiveData()

    // Table operations
    fun insertTable(table: CustomTable) = viewModelScope.launch {
        customTableDao.insertTable(table)
    }

    fun updateTable(table: CustomTable) = viewModelScope.launch {
        customTableDao.updateTable(table)
    }

    fun deleteTable(table: CustomTable) = viewModelScope.launch {
        customTableDao.deleteTable(table)
    }

    suspend fun getTableById(id: String): CustomTable? {
        return customTableDao.getTableById(id)
    }
}