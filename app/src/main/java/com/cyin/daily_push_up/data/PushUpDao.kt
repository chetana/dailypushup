package com.cyin.daily_push_up.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PushUpDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<PushUpEntry>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: PushUpEntry)

    @Query("SELECT * FROM pushup_entries ORDER BY date DESC")
    suspend fun getAllEntries(): List<PushUpEntry>

    @Query("SELECT * FROM pushup_entries WHERE date = :date")
    suspend fun getEntryByDate(date: String): PushUpEntry?

    @Query("DELETE FROM pushup_entries")
    suspend fun clearEntries()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: CachedStats)

    @Query("SELECT * FROM cached_stats WHERE id = 0")
    fun observeStats(): LiveData<CachedStats?>

    @Query("SELECT * FROM cached_stats WHERE id = 0")
    suspend fun getStats(): CachedStats?
}
