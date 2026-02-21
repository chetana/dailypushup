package com.cyin.daily_push_up.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pushup_entries")
data class PushUpEntry(
    @PrimaryKey
    val date: String, // YYYY-MM-DD
    val pushups: Int,
    val validated: Boolean,
    val validatedAt: String?,
    val createdAt: String?
)
