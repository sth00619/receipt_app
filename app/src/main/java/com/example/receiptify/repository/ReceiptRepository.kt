package com.example.receiptify.repository

import android.util.Log
import com.example.receiptify.api.RetrofitClient
import com.example.receiptify.api.models.CreateReceiptRequest
import com.example.receiptify.api.models.ReceiptResponse
import com.example.receiptify.api.models.StatsResponse

class ReceiptRepository {

    private val api = RetrofitClient.api

    companion object {
        private const val TAG = "ReceiptRepository"
    }

    /**
     * 영수증 목록 조회
     */
    suspend fun getReceipts(
        category: String? = null,
        limit: Int = 50
    ): Result<List<ReceiptResponse>> {
        return try {
            Log.d(TAG, "📋 영수증 목록 조회 중... (category: $category, limit: $limit)")

            val response = api.getReceipts(category, limit = limit)

            // ✅ 상세 응답 로깅
            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Response message: ${response.message()}")
            Log.d(TAG, "Is successful: ${response.isSuccessful}")
            Log.d(TAG, "Body success field: ${response.body()?.success}")
            Log.d(TAG, "Body message field: ${response.body()?.message}")
            Log.d(TAG, "Body data size: ${response.body()?.data?.size}")

            if (response.isSuccessful && response.body()?.success == true) {
                val receipts = response.body()?.data ?: emptyList()
                Log.d(TAG, "✅ 영수증 ${receipts.size}개 조회 성공")
                Result.success(receipts)
            } else {
                // ✅ 실패 원인 상세 로깅
                val errorBody = response.errorBody()?.string()
                val bodyMessage = response.body()?.message
                val bodyError = response.body()?.error

                Log.e(TAG, "❌ 영수증 조회 실패")
                Log.e(TAG, "  - HTTP Status: ${response.code()}")
                Log.e(TAG, "  - Success flag: ${response.body()?.success}")
                Log.e(TAG, "  - Body message: $bodyMessage")
                Log.e(TAG, "  - Body error: $bodyError")
                Log.e(TAG, "  - Error body: $errorBody")

                val errorMsg = bodyMessage ?: bodyError ?: errorBody ?: "Failed to fetch receipts"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 영수증 조회 중 예외 발생: ${e.javaClass.simpleName}", e)
            Log.e(TAG, "  - Message: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 영수증 생성
     */
    suspend fun createReceipt(receipt: CreateReceiptRequest): Result<ReceiptResponse> {
        return try {
            Log.d(TAG, "📝 영수증 생성 중... (storeName: ${receipt.storeName}, amount: ${receipt.totalAmount})")

            val response = api.createReceipt(receipt)

            // ✅ 상세 응답 로깅
            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Response success: ${response.body()?.success}")

            if (response.isSuccessful && response.body()?.success == true) {
                val createdReceipt = response.body()?.data!!
                Log.d(TAG, "✅ 영수증 생성 성공: ${createdReceipt.id}")
                Result.success(createdReceipt)
            } else {
                val errorBody = response.errorBody()?.string()
                val bodyMessage = response.body()?.message

                Log.e(TAG, "❌ 영수증 생성 실패")
                Log.e(TAG, "  - HTTP Status: ${response.code()}")
                Log.e(TAG, "  - Body message: $bodyMessage")
                Log.e(TAG, "  - Error body: $errorBody")

                val errorMsg = bodyMessage ?: errorBody ?: "Failed to create receipt"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 영수증 생성 중 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 통계 조회
     */
    suspend fun getStats(
        month: Int? = null,
        year: Int? = null
    ): Result<StatsResponse> {
        return try {
            Log.d(TAG, "📊 통계 조회 중... (year: $year, month: $month)")

            val response = api.getStats(month, year)

            // ✅ 상세 응답 로깅
            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Response message: ${response.message()}")
            Log.d(TAG, "Is successful: ${response.isSuccessful}")
            Log.d(TAG, "Body success field: ${response.body()?.success}")
            Log.d(TAG, "Body message field: ${response.body()?.message}")

            if (response.isSuccessful && response.body()?.success == true) {
                val stats = response.body()?.data!!
                Log.d(TAG, "✅ 통계 조회 성공: 총액 ${stats.total.totalAmount}, 개수 ${stats.total.count}")
                Result.success(stats)
            } else {
                // ✅ 실패 원인 상세 로깅
                val errorBody = response.errorBody()?.string()
                val bodyMessage = response.body()?.message
                val bodyError = response.body()?.error

                Log.e(TAG, "❌ 통계 조회 실패")
                Log.e(TAG, "  - HTTP Status: ${response.code()}")
                Log.e(TAG, "  - Success flag: ${response.body()?.success}")
                Log.e(TAG, "  - Body message: $bodyMessage")
                Log.e(TAG, "  - Body error: $bodyError")
                Log.e(TAG, "  - Error body: $errorBody")

                val errorMsg = bodyMessage ?: bodyError ?: errorBody ?: "Failed to fetch stats"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 통계 조회 중 예외 발생: ${e.javaClass.simpleName}", e)
            Log.e(TAG, "  - Message: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 특정 영수증 조회
     */
    suspend fun getReceipt(id: String): Result<ReceiptResponse> {
        return try {
            Log.d(TAG, "🔍 영수증 조회 중... (id: $id)")

            val response = api.getReceipt(id)

            if (response.isSuccessful && response.body()?.success == true) {
                val receipt = response.body()?.data!!
                Log.d(TAG, "✅ 영수증 조회 성공: ${receipt.storeName}")
                Result.success(receipt)
            } else {
                val errorMsg = response.body()?.message ?: "Receipt not found"
                Log.e(TAG, "❌ 영수증 조회 실패: $errorMsg (Status: ${response.code()})")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 영수증 조회 중 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 영수증 삭제
     */
    suspend fun deleteReceipt(id: String): Result<Unit> {
        return try {
            Log.d(TAG, "🗑️ 영수증 삭제 중... (id: $id)")

            val response = api.deleteReceipt(id)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "✅ 영수증 삭제 성공")
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.message ?: "Failed to delete receipt"
                Log.e(TAG, "❌ 영수증 삭제 실패: $errorMsg (Status: ${response.code()})")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 영수증 삭제 중 오류", e)
            Result.failure(e)
        }
    }
}