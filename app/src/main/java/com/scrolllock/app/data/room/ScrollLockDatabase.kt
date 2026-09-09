package com.scrolllock.app.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.scrolllock.app.data.model.*
import com.scrolllock.app.data.room.dao.*

@Database(
    entities = [
        AppInfo::class,
        Swipe::class,
        CustomAntiScrollMode::class,
        ScheduleRule::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ScrollLockDatabase : RoomDatabase() {
    abstract fun appInfoDao(): AppInfoDao
    abstract fun swipeDao(): SwipeDao
    abstract fun antiScrollDao(): AntiScrollDao
    abstract fun scheduleRuleDao(): ScheduleRuleDao

    companion object {
        @Volatile
        private var INSTANCE: ScrollLockDatabase? = null

        fun getInstance(context: Context): ScrollLockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScrollLockDatabase::class.java,
                    "scrolllock_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
