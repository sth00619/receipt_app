package com.example.receiptify.api

import com.example.receiptify.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface ReceiptApiService {

    // ============ 인증 관련 API ============

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("auth/verify")
    suspend fun verifyToken(
        @Body request: VerifyTokenRequest
    ): Response<AuthResponse>

    /**
     * 네이버 토큰으로 로그인 (새로 추가)
     */
    @POST("auth/naver")
    suspend fun loginWithNaver(
        @Body request: NaverLoginRequest
    ): Response<AuthResponse>

    /**
     * 구글 토큰으로 로그인 (새로 추가)
     */
    @POST("auth/google")
    suspend fun loginWithGoogle(
        @Body request: GoogleLoginRequest
    ): Response<AuthResponse>

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

    // 프로필 조회
    @GET("users/me")
    suspend fun getProfile(): Response<ApiResponse<ProfileResponse>>

    // 비밀번호 변경
    @PUT("users/change-password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Response<ApiResponse<Unit>>

    // 알림 설정 업데이트
    @PUT("users/settings/notifications")
    suspend fun updateNotificationSetting(
        @Body request: UpdateSettingRequest
    ): Response<ApiResponse<UserResponse>>

    // 다크모드 설정 업데이트
    @PUT("users/settings/darkmode")
    suspend fun updateDarkModeSetting(
        @Body request: UpdateSettingRequest
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

    // ✅ 날짜 범위로 통계 조회 (새로 추가)
    @GET("receipts/stats")
    suspend fun getStatsByDateRange(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<ApiResponse<StatsResponse>>

}