package com.scrolllock.app.data.room.dao

import androidx.room.*
import com.scrolllock.app.data.model.Swipe

@Dao
interface SwipeDao {
    @Insert
    suspend fun insert(swipe: Swipe)

    @Query("SELECT * FROM swipes WHERE packageName = :packageName AND timestamp > :since ORDER BY timestamp DESC")
    suspend fun getRecentSwipes(packageName: String, since: Long): List<Swipe>

    @Query("DELETE FROM swipes WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM swipes WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
