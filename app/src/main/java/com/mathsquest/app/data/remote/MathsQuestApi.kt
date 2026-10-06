package com.mathsquest.app.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * The ASP.NET Core API. Not called in V1: the app runs offline and the server becomes the
 * source of truth once sync ships. The server re-checks every answer and coin rule.
 */
interface MathsQuestApi {
    @POST("api/v1/answers")
    suspend fun submitAnswer(@Body body: AnswerRequest): AnswerResponse

    @GET("api/v1/children/{childId}/balance")
    suspend fun balance(@Path("childId") childId: String): BalanceResponse

    @POST("api/v1/children/{childId}/reward-requests")
    suspend fun requestReward(@Path("childId") childId: String, @Body body: RewardRequestBody): RewardRequestDto

    @POST("api/v1/reward-requests/{requestId}/decision")
    suspend fun decide(@Path("requestId") requestId: String, @Body body: DecisionBody): RewardRequestDto
}

@Serializable
data class AnswerRequest(
    val childId: String,
    val questionId: String,
    val grade: Int,
    val topic: String,
    val difficulty: Int,
    val answer: Long,
    val attempt: Int,
    val elapsedMillis: Long,
)

@Serializable
data class AnswerResponse(val correct: Boolean, val coins: Int, val xp: Int, val reason: String, val balance: Int)

@Serializable
data class BalanceResponse(val balance: Int, val held: Int, val earnedThisMonth: Int, val monthlyCap: Int)

@Serializable
data class RewardRequestBody(val rewardId: String)

@Serializable
data class DecisionBody(val approve: Boolean)

@Serializable
data class RewardRequestDto(val id: String, val rewardId: String, val title: String, val cost: Int, val status: String)
