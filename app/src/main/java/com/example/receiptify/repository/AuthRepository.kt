package com.example.receiptify.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.receiptify.api.RetrofitClient
import com.example.receiptify.api.models.LoginRequest
import com.example.receiptify.api.models.RegisterRequest
import com.example.receiptify.api.models.UserData
import com.example.receiptify.api.models.VerifyTokenRequest

class AuthRepository(context: Context) {

    private val api = RetrofitClient.api
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "receiptify_auth",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val TAG = "AuthRepository"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
    }

    /**
     * 회원가입
     */
    suspend fun register(
        email: String,
        password: String,
        displayName: String? = null
    ): Result<UserData> {
        return try {
            Log.d(TAG, "회원가입 시도: $email")

            val request = RegisterRequest(email, password, displayName)
            val response = api.register(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()!!
                val token = authResponse.token!!
                val userData = authResponse.data!!

                // 토큰 저장
                saveToken(token)
                saveUserInfo(userData.id, userData.email)

                Log.d(TAG, "✅ 회원가입 성공: ${userData.email}")
                Result.success(userData)
            } else {
                val errorMsg = response.body()?.message ?: "Registration failed"
                Log.e(TAG, "❌ 회원가입 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 회원가입 중 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 로그인
     */
    suspend fun login(email: String, password: String): Result<UserData> {
        return try {
            Log.d(TAG, "로그인 시도: $email")

            val request = LoginRequest(email, password)
            val response = api.login(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()!!
                val token = authResponse.token!!
                val userData = authResponse.data!!

                // 토큰 저장
                saveToken(token)
                saveUserInfo(userData.id, userData.email)

                Log.d(TAG, "✅ 로그인 성공: ${userData.email}")
                Result.success(userData)
            } else {
                val errorMsg = response.body()?.message ?: "Login failed"
                Log.e(TAG, "❌ 로그인 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 로그인 중 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 토큰 검증
     */
    suspend fun verifyToken(): Result<UserData> {
        return try {
            val token = getToken()
            if (token == null) {
                return Result.failure(Exception("No token found"))
            }

            val request = VerifyTokenRequest(token)
            val response = api.verifyToken(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val userData = response.body()!!.data!!
                Log.d(TAG, "✅ 토큰 검증 성공")
                Result.success(userData)
            } else {
                Log.e(TAG, "❌ 토큰 검증 실패")
                Result.failure(Exception("Invalid token"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 토큰 검증 중 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 로그아웃
     */
    fun logout() {
        prefs.edit().apply {
            remove(KEY_AUTH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            apply()
        }
        Log.d(TAG, "로그아웃 완료")
    }

    /**
     * 토큰 저장
     */
    private fun saveToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    /**
     * 사용자 정보 저장
     */
    private fun saveUserInfo(userId: String, email: String) {
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
    }

    /**
     * 토큰 가져오기
     */
    fun getToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    /**
     * 사용자 ID 가져오기
     */
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    /**
     * 사용자 이메일 가져오기
     */
    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    /**
     * 로그인 여부 확인
     */
    fun isLoggedIn(): Boolean {
        return getToken() != null
    }
}