package com.aksara.notes.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.aksara.notes.data.database.entities.TableItem

@Dao
interface TableItemDao {
    @Query("SELECT * FROM table_items ORDER BY updatedAt DESC")
    fun getAllItems(): Flow<List<TableItem>>

    @Query("SELECT * FROM table_items WHERE tableId = :tableId ORDER BY updatedAt DESC")
    fun getItemsByTable(tableId: String): Flow<List<TableItem>>

    @Query("SELECT * FROM table_items WHERE id = :id")
    suspend fun getItemById(id: String): TableItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: TableItem)

    @Update
    suspend fun updateItem(item: TableItem)

    @Delete
    suspend fun deleteItem(item: TableItem)

    @Query("SELECT * FROM table_items WHERE data LIKE '%' || :searchQuery || '%' ORDER BY updatedAt DESC")
    fun searchItems(searchQuery: String): Flow<List<TableItem>>

    @Query("DELETE FROM table_items WHERE tableId = :tableId")
    suspend fun deleteItemsByTable(tableId: String)

    @Query("SELECT COUNT(*) FROM table_items WHERE tableId = :tableId")
    suspend fun getItemCountForTable(tableId: String): Int
}