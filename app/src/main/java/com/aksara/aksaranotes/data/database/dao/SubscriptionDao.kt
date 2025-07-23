package com.aksara.aksaranotes.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.aksara.aksaranotes.data.database.entities.Subscription

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions WHERE isActive = 1 ORDER BY nextDueDate ASC")
    fun getAllActiveSubscriptions(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions ORDER BY nextDueDate ASC")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getSubscriptionById(id: String): Subscription?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription)

    @Update
    suspend fun updateSubscription(subscription: Subscription)

    @Delete
    suspend fun deleteSubscription(subscription: Subscription)

    @Query("SELECT * FROM subscriptions WHERE name LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%' ORDER BY nextDueDate ASC")
    fun searchSubscriptions(searchQuery: String): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE nextDueDate BETWEEN :startDate AND :endDate ORDER BY nextDueDate ASC")
    fun getSubscriptionsDueBetween(startDate: Long, endDate: Long): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE category = :category AND isActive = 1 ORDER BY nextDueDate ASC")
    fun getSubscriptionsByCategory(category: String): Flow<List<Subscription>>

    @Query("SELECT DISTINCT category FROM subscriptions ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>

    // Get overdue subscriptions
    @Query("SELECT * FROM subscriptions WHERE nextDueDate < :currentTime AND isActive = 1 ORDER BY nextDueDate ASC")
    fun getOverdueSubscriptions(currentTime: Long): Flow<List<Subscription>>

    // Get subscriptions due in next N days
    @Query("SELECT * FROM subscriptions WHERE nextDueDate BETWEEN :currentTime AND :futureTime AND isActive = 1 ORDER BY nextDueDate ASC")
    fun getUpcomingSubscriptions(currentTime: Long, futureTime: Long): Flow<List<Subscription>>
}