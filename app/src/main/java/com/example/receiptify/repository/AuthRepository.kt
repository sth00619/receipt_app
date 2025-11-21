package com.example.receiptify.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.receiptify.api.RetrofitClient
import com.example.receiptify.api.models.LoginRequest
import com.example.receiptify.api.models.NaverLoginRequest
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
            Log.d(TAG, "📝 회원가입 시도: $email")

            val request = RegisterRequest(email, password, displayName)
            val response = api.register(request)

            Log.d(TAG, "응답 코드: ${response.code()}")

            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()!!
                val token = authResponse.token!!
                val userData = authResponse.data!!

                // 토큰 저장
                saveToken(token)
                saveUserInfo(userData.id, userData.email)

                Log.d(TAG, "✅ 회원가입 성공: ${userData.email}")
                Log.d(TAG, "🔑 토큰 저장 완료: ${token.take(30)}...")

                Result.success(userData)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = response.body()?.message ?: errorBody ?: "Registration failed"
                Log.e(TAG, "❌ 회원가입 실패 (${response.code()}): $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 회원가입 중 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 일반 이메일 로그인
     */
    suspend fun login(email: String, password: String): Result<UserData> {
        return try {
            Log.d(TAG, "📧 로그인 시도: $email")

            val request = LoginRequest(email, password)
            val response = api.login(request)

            Log.d(TAG, "응답 코드: ${response.code()}")

            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()!!
                val token = authResponse.token!!
                val userData = authResponse.data!!

                // 토큰 저장
                saveToken(token)
                saveUserInfo(userData.id, userData.email)

                Log.d(TAG, "✅ 로그인 성공: ${userData.email}")
                Log.d(TAG, "🔑 토큰 저장 완료: ${token.take(30)}...")

                Result.success(userData)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = response.body()?.message ?: errorBody ?: "Login failed"
                Log.e(TAG, "❌ 로그인 실패 (${response.code()}): $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 로그인 중 오류", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ 네이버 로그인 (새로 추가)
     */
    suspend fun loginWithNaver(
        accessToken: String,
        email: String? = null,
        name: String? = null
    ): Result<UserData> {
        return try {
            Log.d(TAG, "🟢 네이버 로그인 시도: $email")

            val request = NaverLoginRequest(accessToken, email, name)
            val response = api.loginWithNaver(request)

            Log.d(TAG, "응답 코드: ${response.code()}")

            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()!!
                val token = authResponse.token!!
                val userData = authResponse.data!!

                // JWT 토큰 저장
                saveToken(token)
                saveUserInfo(userData.id, userData.email)

                // 네이버 로그인 상태 저장
                prefs.edit().putBoolean("naver_logged_in", true).apply()

                Log.d(TAG, "✅ 네이버 로그인 성공: ${userData.email}")
                Log.d(TAG, "🔑 JWT 토큰 저장 완료: ${token.take(30)}...")

                Result.success(userData)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = response.body()?.message ?: errorBody ?: "Naver login failed"
                Log.e(TAG, "❌ 네이버 로그인 실패 (${response.code()}): $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 네이버 로그인 중 오류", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ 구글 로그인 (새로 추가)
     */
    suspend fun loginWithGoogle(
        idToken: String,
        email: String? = null,
        name: String? = null,
        photoUrl: String? = null
    ): Result<UserData> {
        return try {
            Log.d(TAG, "🔵 구글 로그인 시도: $email")

            val request = com.example.receiptify.api.models.GoogleLoginRequest(idToken, email, name, photoUrl)
            val response = api.loginWithGoogle(request)

            Log.d(TAG, "응답 코드: ${response.code()}")

            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()!!
                val token = authResponse.token!!
                val userData = authResponse.data!!

                // JWT 토큰 저장
                saveToken(token)
                saveUserInfo(userData.id, userData.email)

                Log.d(TAG, "✅ 구글 로그인 성공: ${userData.email}")
                Log.d(TAG, "🔑 JWT 토큰 저장 완료: ${token.take(30)}...")

                Result.success(userData)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = response.body()?.message ?: errorBody ?: "Google login failed"
                Log.e(TAG, "❌ 구글 로그인 실패 (${response.code()}): $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 구글 로그인 중 오류", e)
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

            Log.d(TAG, "🔍 토큰 검증 중...")

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
        Log.d(TAG, "🚪 로그아웃 시작")

        prefs.edit().apply {
            remove(KEY_AUTH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove("naver_logged_in")
            apply()
        }

        Log.d(TAG, "✅ 로그아웃 완료 - 모든 인증 정보 삭제됨")
    }

    /**
     * 토큰 저장
     */
    private fun saveToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
        Log.d(TAG, "💾 토큰 저장됨: ${token.take(30)}...")
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
        Log.d(TAG, "💾 사용자 정보 저장됨: $email (ID: $userId)")
    }

    /**
     * 토큰 가져오기
     */
    fun getToken(): String? {
        val token = prefs.getString(KEY_AUTH_TOKEN, null)
        if (token != null) {
            Log.d(TAG, "📌 토큰 조회: ${token.take(30)}...")
        } else {
            Log.w(TAG, "⚠️ 저장된 토큰 없음")
        }
        return token
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
        val hasToken = getToken() != null
        Log.d(TAG, "🔐 로그인 상태: $hasToken")
        return hasToken
    }
}