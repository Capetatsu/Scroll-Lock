package com.scrolllock.app.data.room.dao

import androidx.room.*
import com.scrolllock.app.data.model.CustomAntiScrollMode
import kotlinx.coroutines.flow.Flow

@Dao
interface AntiScrollDao {
    @Query("SELECT * FROM custom_antiscroll_mode WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): CustomAntiScrollMode?

    @Query("SELECT * FROM custom_antiscroll_mode")
    fun getAll(): Flow<List<CustomAntiScrollMode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mode: CustomAntiScrollMode)

    @Delete
    suspend fun delete(mode: CustomAntiScrollMode)
}
