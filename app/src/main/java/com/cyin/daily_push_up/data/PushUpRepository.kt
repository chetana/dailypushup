package com.cyin.daily_push_up.data

import androidx.lifecycle.LiveData
import com.cyin.daily_push_up.api.RetrofitClient
import com.cyin.daily_push_up.api.ValidateRequest

class PushUpRepository(private val dao: PushUpDao) {

    private val api = RetrofitClient.api

    val statsLiveData: LiveData<CachedStats?> = dao.observeStats()

    suspend fun sync(): Result<Unit> {
        return try {
            val statsResponse = api.getStats()
            val entriesResponse = api.getEntries()

            val stats = CachedStats(
                id = 0,
                totalPushups = statsResponse.totalPushups,
                totalDays = statsResponse.totalDays,
                currentStreak = statsResponse.currentStreak,
                longestStreak = statsResponse.longestStreak,
                todayValidated = statsResponse.todayValidated,
                todayTarget = statsResponse.todayTarget,
                lastSyncedAt = System.currentTimeMillis()
            )
            dao.insertStats(stats)

            val entries = entriesResponse.map { entry ->
                PushUpEntry(
                    date = entry.date,
                    pushups = entry.pushups,
                    validated = entry.validated,
                    validatedAt = entry.validatedAt,
                    createdAt = entry.createdAt
                )
            }
            dao.clearEntries()
            dao.insertEntries(entries)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateToday(pushups: Int): Result<Boolean> {
        return try {
            val response = api.validate(ValidateRequest(pushups))
            if (response.success) {
                sync()
            }
            Result.success(response.success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllEntries(): List<PushUpEntry> {
        return dao.getAllEntries()
    }

    suspend fun getStats(): CachedStats? {
        return dao.getStats()
    }
}
