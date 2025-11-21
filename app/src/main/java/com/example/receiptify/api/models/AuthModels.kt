package com.example.receiptify.api.models

import com.google.gson.annotations.SerializedName

// 회원가입 요청
data class RegisterRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("displayName")
    val displayName: String? = null
)

// 로그인 요청
data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)

// 토큰 검증 요청
data class VerifyTokenRequest(
    @SerializedName("token")
    val token: String
)

// 인증 응답
data class AuthResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("token")
    val token: String?,

    @SerializedName("data")
    val data: UserData?
)

// 사용자 데이터
data class UserData(
    @SerializedName("_id")
    val id: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("displayName")
    val displayName: String?,

    @SerializedName("photoUrl")
    val photoUrl: String?,

    @SerializedName("provider")
    val provider: String,

    @SerializedName("preferences")
    val preferences: UserPreferences,

    @SerializedName("stats")
    val stats: UserStats,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("lastLoginAt")
    val lastLoginAt: String?
)