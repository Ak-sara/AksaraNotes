package com.aksara.aksaranotes.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.aksara.aksaranotes.data.database.entities.Account

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY organization ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account)

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("SELECT * FROM accounts WHERE organization LIKE '%' || :searchQuery || '%' OR appWebsite LIKE '%' || :searchQuery || '%' OR username LIKE '%' || :searchQuery || '%' ORDER BY organization ASC")
    fun searchAccounts(searchQuery: String): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE category = :category ORDER BY organization ASC")
    fun getAccountsByCategory(category: String): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE isFavorite = 1 ORDER BY organization ASC")
    fun getFavoriteAccounts(): Flow<List<Account>>

    @Query("SELECT DISTINCT category FROM accounts ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>
}