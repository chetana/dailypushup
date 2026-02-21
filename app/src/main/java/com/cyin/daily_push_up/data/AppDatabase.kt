package com.cyin.daily_push_up.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PushUpEntry::class, CachedStats::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pushUpDao(): PushUpDao
}
