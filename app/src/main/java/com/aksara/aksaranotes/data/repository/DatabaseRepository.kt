package com.aksara.aksaranotes.data.repository

import kotlinx.coroutines.flow.Flow
import com.aksara.aksaranotes.data.database.dao.AccountDao
import com.aksara.aksaranotes.data.database.dao.SubscriptionDao
import com.aksara.aksaranotes.data.database.dao.CustomTableDao
import com.aksara.aksaranotes.data.database.entities.Account
import com.aksara.aksaranotes.data.database.entities.Subscription
import com.aksara.aksaranotes.data.database.entities.CustomTable

class DatabaseRepository(
    private val accountDao: AccountDao,
    private val subscriptionDao: SubscriptionDao,
    private val customTableDao: CustomTableDao
) {

    // Account operations
    fun getAllAccounts(): Flow<List<Account>> = accountDao.getAllAccounts()
    suspend fun getAccountById(id: String): Account? = accountDao.getAccountById(id)
    suspend fun insertAccount(account: Account) = accountDao.insertAccount(account)
    suspend fun updateAccount(account: Account) = accountDao.updateAccount(account)
    suspend fun deleteAccount(account: Account) = accountDao.deleteAccount(account)
    fun searchAccounts(query: String): Flow<List<Account>> = accountDao.searchAccounts(query)
    fun getAccountsByCategory(category: String): Flow<List<Account>> = accountDao.getAccountsByCategory(category)
    fun getFavoriteAccounts(): Flow<List<Account>> = accountDao.getFavoriteAccounts()
    suspend fun getAllAccountCategories(): List<String> = accountDao.getAllCategories()

    // Subscription operations
    fun getAllActiveSubscriptions(): Flow<List<Subscription>> = subscriptionDao.getAllActiveSubscriptions()
    fun getAllSubscriptions(): Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions()
    suspend fun getSubscriptionById(id: String): Subscription? = subscriptionDao.getSubscriptionById(id)
    suspend fun insertSubscription(subscription: Subscription) = subscriptionDao.insertSubscription(subscription)
    suspend fun updateSubscription(subscription: Subscription) = subscriptionDao.updateSubscription(subscription)
    suspend fun deleteSubscription(subscription: Subscription) = subscriptionDao.deleteSubscription(subscription)
    fun searchSubscriptions(query: String): Flow<List<Subscription>> = subscriptionDao.searchSubscriptions(query)
    fun getSubscriptionsDueBetween(startDate: Long, endDate: Long): Flow<List<Subscription>> = subscriptionDao.getSubscriptionsDueBetween(startDate, endDate)
    fun getSubscriptionsByCategory(category: String): Flow<List<Subscription>> = subscriptionDao.getSubscriptionsByCategory(category)
    suspend fun getAllSubscriptionCategories(): List<String> = subscriptionDao.getAllCategories()
    fun getOverdueSubscriptions(currentTime: Long): Flow<List<Subscription>> = subscriptionDao.getOverdueSubscriptions(currentTime)
    fun getUpcomingSubscriptions(currentTime: Long, futureTime: Long): Flow<List<Subscription>> = subscriptionDao.getUpcomingSubscriptions(currentTime, futureTime)

    // Custom table operations
    fun getAllTables(): Flow<List<CustomTable>> = customTableDao.getAllTables()
    suspend fun getTableById(id: String): CustomTable? = customTableDao.getTableById(id)
    suspend fun insertTable(table: CustomTable) = customTableDao.insertTable(table)
    suspend fun updateTable(table: CustomTable) = customTableDao.updateTable(table)
    suspend fun deleteTable(table: CustomTable) = customTableDao.deleteTable(table)
    fun searchTables(query: String): Flow<List<CustomTable>> = customTableDao.searchTables(query)
}