package com.scrolllock.app.data.room.dao

import androidx.room.*
import com.scrolllock.app.data.model.AppInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDao {
    @Query("SELECT * FROM app_info")
    fun getAll(): Flow<List<AppInfo>>

    @Query("SELECT * FROM app_info")
    suspend fun getAllList(): List<AppInfo>

    @Query("SELECT * FROM app_info WHERE packageName = :packageName")
    suspend fun getByPackage(packageName: String): AppInfo?

    @Query("SELECT * FROM app_info WHERE packageName = :packageName")
    fun observeByPackage(packageName: String): Flow<AppInfo?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(appInfo: AppInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(appInfos: List<AppInfo>)

    @Delete
    suspend fun delete(appInfo: AppInfo)

    @Query("DELETE FROM app_info WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("UPDATE app_info SET lastBlockedTS = :timestamp WHERE packageName = :packageName")
    suspend fun updateLastBlocked(packageName: String, timestamp: Long)
}
