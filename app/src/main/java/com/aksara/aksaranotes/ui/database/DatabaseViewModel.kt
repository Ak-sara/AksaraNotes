package com.aksara.aksaranotes.ui.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.aksara.aksaranotes.data.database.AppDatabase
import com.aksara.aksaranotes.data.database.entities.CustomTable
import com.aksara.aksaranotes.data.database.entities.TableItem

class DatabaseViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val customTableDao = database.customTableDao()
    private val tableItemDao = database.tableItemDao()

    // Custom Tables
    val allTables: LiveData<List<CustomTable>> = customTableDao.getAllTables().asLiveData()

    // All Items from all tables
    val allItems: LiveData<List<TableItem>> = tableItemDao.getAllItems().asLiveData()

    // Table operations
    fun insertTable(table: CustomTable) = viewModelScope.launch {
        customTableDao.insertTable(table)
    }

    fun updateTable(table: CustomTable) = viewModelScope.launch {
        customTableDao.updateTable(table)
    }

    fun deleteTable(table: CustomTable) = viewModelScope.launch {
        // First delete all items in this table
        tableItemDao.deleteItemsByTable(table.id)
        // Then delete the table itself
        customTableDao.deleteTable(table)
    }

    suspend fun getTableById(id: String): CustomTable? {
        return customTableDao.getTableById(id)
    }

    // Item operations
    fun insertItem(item: TableItem) = viewModelScope.launch {
        tableItemDao.insertItem(item)
    }

    fun updateItem(item: TableItem) = viewModelScope.launch {
        tableItemDao.updateItem(item)
    }

    fun deleteItem(item: TableItem) = viewModelScope.launch {
        tableItemDao.deleteItem(item)
    }

    suspend fun getItemById(id: String): TableItem? {
        return tableItemDao.getItemById(id)
    }

    suspend fun getItemCountForTable(tableId: String): Int {
        return tableItemDao.getItemCountForTable(tableId)
    }

    fun getItemsByTable(tableId: String): LiveData<List<TableItem>> {
        return tableItemDao.getItemsByTable(tableId).asLiveData()
    }
}