package com.aksara.aksaranotes.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.aksara.aksaranotes.data.database.entities.CustomTable

@Dao
interface CustomTableDao {
    @Query("SELECT * FROM custom_tables ORDER BY updatedAt DESC")
    fun getAllTables(): Flow<List<CustomTable>>

    @Query("SELECT * FROM custom_tables WHERE id = :id")
    suspend fun getTableById(id: String): CustomTable?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: CustomTable)

    @Update
    suspend fun updateTable(table: CustomTable)

    @Delete
    suspend fun deleteTable(table: CustomTable)

    @Query("SELECT * FROM custom_tables WHERE name LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%' ORDER BY name ASC")
    fun searchTables(searchQuery: String): Flow<List<CustomTable>>

    @Query("SELECT * FROM custom_tables WHERE tableType = :type ORDER BY updatedAt DESC")
    fun getTablesByType(type: String): Flow<List<CustomTable>>
}