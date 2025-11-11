package com.example.receiptify.repository

import android.util.Log
import com.example.receiptify.api.RetrofitClient
import com.example.receiptify.api.models.UserPreferences
import com.example.receiptify.api.models.UserResponse

class UserRepository {

    private val api = RetrofitClient.api

    companion object {
        private const val TAG = "UserRepository"
    }

    /**
     * Firebase 로그인 후 MongoDB와 사용자 동기화
     * 신규 사용자면 생성, 기존 사용자면 업데이트
     */
    suspend fun syncUser(): Result<UserResponse> {
        return try {
            Log.d(TAG, "🔄 사용자 동기화 시작...")

            val response = api.syncUser()

            if (response.isSuccessful && response.body()?.success == true) {
                val user = response.body()?.data!!
                Log.d(TAG, "✅ 사용자 동기화 성공: ${user.email}")
                Result.success(user)
            } else {
                val errorMsg = response.body()?.message ?: "Failed to sync user"
                Log.e(TAG, "❌ 사용자 동기화 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 사용자 동기화 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 내 정보 조회
     */
    suspend fun getMe(): Result<UserResponse> {
        return try {
            Log.d(TAG, "내 정보 조회 중...")

            val response = api.getMe()

            if (response.isSuccessful && response.body()?.success == true) {
                val user = response.body()?.data!!
                Log.d(TAG, "✅ 내 정보 조회 성공: ${user.email}")
                Result.success(user)
            } else {
                val errorMsg = response.body()?.message ?: "Failed to get user"
                Log.e(TAG, "❌ 내 정보 조회 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 내 정보 조회 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 설정 업데이트
     */
    suspend fun updatePreferences(preferences: UserPreferences): Result<UserResponse> {
        return try {
            Log.d(TAG, "설정 업데이트 중...")

            val response = api.updatePreferences(preferences)

            if (response.isSuccessful && response.body()?.success == true) {
                val user = response.body()?.data!!
                Log.d(TAG, "✅ 설정 업데이트 성공")
                Result.success(user)
            } else {
                val errorMsg = response.body()?.message ?: "Failed to update preferences"
                Log.e(TAG, "❌ 설정 업데이트 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 설정 업데이트 오류", e)
            Result.failure(e)
        }
    }
}