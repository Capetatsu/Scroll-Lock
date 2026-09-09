package com.scrolllock.app.data.room.dao

import androidx.room.*
import com.scrolllock.app.data.model.ScheduleRule
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleRuleDao {
    @Query("SELECT * FROM schedule_rules")
    fun getAll(): Flow<List<ScheduleRule>>

    @Query("SELECT * FROM schedule_rules WHERE enabled = 1")
    suspend fun getEnabled(): List<ScheduleRule>

    @Query("SELECT * FROM schedule_rules WHERE id = :id")
    suspend fun getById(id: Long): ScheduleRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: ScheduleRule): Long

    @Delete
    suspend fun delete(rule: ScheduleRule)

    @Query("DELETE FROM schedule_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
