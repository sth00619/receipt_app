package com.example.receiptify.api

import com.example.receiptify.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface ReceiptApiService {

    // ============ 사용자 관련 API ============

    // 사용자 동기화 (로그인 후 호출)
    @POST("users/sync")
    suspend fun syncUser(): Response<ApiResponse<UserResponse>>

    // 내 정보 조회
    @GET("users/me")
    suspend fun getMe(): Response<ApiResponse<UserResponse>>

    // 설정 업데이트
    @PUT("users/preferences")
    suspend fun updatePreferences(
        @Body preferences: UserPreferences
    ): Response<ApiResponse<UserResponse>>

    // ============ 영수증 관련 API ============

    // Health Check
    @GET("health")
    suspend fun healthCheck(): Response<Map<String, Any>>

    // 내 영수증 목록 조회 (인증 필요)
    @GET("receipts")
    suspend fun getReceipts(
        @Query("category") category: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<ApiResponse<List<ReceiptResponse>>>

    // 영수증 생성
    @POST("receipts")
    suspend fun createReceipt(
        @Body receipt: CreateReceiptRequest
    ): Response<ApiResponse<ReceiptResponse>>

    // 특정 영수증 조회
    @GET("receipts/{id}")
    suspend fun getReceipt(
        @Path("id") id: String
    ): Response<ApiResponse<ReceiptResponse>>

    // 영수증 삭제
    @DELETE("receipts/{id}")
    suspend fun deleteReceipt(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>

    // 내 통계 조회
    @GET("receipts/stats")
    suspend fun getStats(
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): Response<ApiResponse<StatsResponse>>
}