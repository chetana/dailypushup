package com.cyin.daily_push_up.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class StatsResponse(
    val totalPushups: Int,
    val totalDays: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val todayValidated: Boolean,
    val todayTarget: Int
)

data class EntryResponse(
    val id: String?,
    val date: String,
    val pushups: Int,
    val validated: Boolean,
    val validatedAt: String?,
    val createdAt: String?
)

data class ValidateRequest(
    val pushups: Int
)

data class ValidateResponse(
    val success: Boolean,
    val alreadyValidated: Boolean?,
    val date: String?,
    val pushups: Int?
)

interface HealthApiService {

    @GET("api/health/stats")
    suspend fun getStats(): StatsResponse

    @GET("api/health/entries")
    suspend fun getEntries(): List<EntryResponse>

    @POST("api/health/validate")
    suspend fun validate(@Body request: ValidateRequest): ValidateResponse
}
