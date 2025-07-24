package com.aksara.aksaranotes.data.repository

import kotlinx.coroutines.flow.Flow
import com.aksara.aksaranotes.data.database.dao.CustomTableDao
import com.aksara.aksaranotes.data.database.entities.CustomTable

class DatabaseRepository(
    private val customTableDao: CustomTableDao
) {
    // Custom table operations
    fun getAllTables(): Flow<List<CustomTable>> = customTableDao.getAllTables()
    suspend fun getTableById(id: String): CustomTable? = customTableDao.getTableById(id)
    suspend fun insertTable(table: CustomTable) = customTableDao.insertTable(table)
    suspend fun updateTable(table: CustomTable) = customTableDao.updateTable(table)
    suspend fun deleteTable(table: CustomTable) = customTableDao.deleteTable(table)
    fun searchTables(query: String): Flow<List<CustomTable>> = customTableDao.searchTables(query)
}