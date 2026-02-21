package com.cyin.daily_push_up.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_stats")
data class CachedStats(
    @PrimaryKey
    val id: Int = 0,
    val totalPushups: Int = 0,
    val totalDays: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val todayValidated: Boolean = false,
    val todayTarget: Int = 0,
    val lastSyncedAt: Long = 0L
)
