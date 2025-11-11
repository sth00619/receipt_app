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
     * userId 파라미터 제거 - 백엔드에서 토큰으로 자동 추출
     *
     * @param category 카테고리 필터 (선택사항)
     * @param limit 조회 개수 (기본 50개)
     * @return Result<List<ReceiptResponse>>
     */
    suspend fun getReceipts(
        category: String? = null,
        limit: Int = 50
    ): Result<List<ReceiptResponse>> {
        return try {
            Log.d(TAG, "영수증 목록 조회 중... (category: $category, limit: $limit)")

            val response = api.getReceipts(category, limit = limit)

            if (response.isSuccessful && response.body()?.success == true) {
                val receipts = response.body()?.data ?: emptyList()
                Log.d(TAG, "✅ 영수증 ${receipts.size}개 조회 성공")
                Result.success(receipts)
            } else {
                val errorMsg = response.body()?.message ?: "Failed to fetch receipts"
                Log.e(TAG, "❌ 영수증 조회 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 영수증 조회 중 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 영수증 생성
     * userId는 요청 body에서 제거 - 백엔드에서 토큰으로 설정
     *
     * @param receipt 영수증 생성 요청 데이터
     * @return Result<ReceiptResponse>
     */
    suspend fun createReceipt(receipt: CreateReceiptRequest): Result<ReceiptResponse> {
        return try {
            Log.d(TAG, "영수증 생성 중... (storeName: ${receipt.storeName}, amount: ${receipt.totalAmount})")

            val response = api.createReceipt(receipt)

            if (response.isSuccessful && response.body()?.success == true) {
                val createdReceipt = response.body()?.data!!
                Log.d(TAG, "✅ 영수증 생성 성공: ${createdReceipt.id}")
                Result.success(createdReceipt)
            } else {
                val errorMsg = response.body()?.message ?: "Failed to create receipt"
                Log.e(TAG, "❌ 영수증 생성 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 영수증 생성 중 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 통계 조회
     * userId 파라미터 제거 - 백엔드에서 토큰으로 자동 추출
     *
     * @param month 월 (1-12, 선택사항)
     * @param year 년도 (선택사항)
     * @return Result<StatsResponse>
     */
    suspend fun getStats(
        month: Int? = null,
        year: Int? = null
    ): Result<StatsResponse> {
        return try {
            Log.d(TAG, "통계 조회 중... (year: $year, month: $month)")

            val response = api.getStats(month, year)

            if (response.isSuccessful && response.body()?.success == true) {
                val stats = response.body()?.data!!
                Log.d(TAG, "✅ 통계 조회 성공: 총액 ${stats.total.totalAmount}")
                Result.success(stats)
            } else {
                val errorMsg = response.body()?.message ?: "Failed to fetch stats"
                Log.e(TAG, "❌ 통계 조회 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 통계 조회 중 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 특정 영수증 조회
     *
     * @param id 영수증 ID
     * @return Result<ReceiptResponse>
     */
    suspend fun getReceipt(id: String): Result<ReceiptResponse> {
        return try {
            Log.d(TAG, "영수증 조회 중... (id: $id)")

            val response = api.getReceipt(id)

            if (response.isSuccessful && response.body()?.success == true) {
                val receipt = response.body()?.data!!
                Log.d(TAG, "✅ 영수증 조회 성공: ${receipt.storeName}")
                Result.success(receipt)
            } else {
                val errorMsg = response.body()?.message ?: "Receipt not found"
                Log.e(TAG, "❌ 영수증 조회 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 영수증 조회 중 오류", e)
            Result.failure(e)
        }
    }

    /**
     * 영수증 삭제
     *
     * @param id 영수증 ID
     * @return Result<Unit>
     */
    suspend fun deleteReceipt(id: String): Result<Unit> {
        return try {
            Log.d(TAG, "영수증 삭제 중... (id: $id)")

            val response = api.deleteReceipt(id)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "✅ 영수증 삭제 성공")
                Result.success(Unit)
            } else {
                val errorMsg = response.body()?.message ?: "Failed to delete receipt"
                Log.e(TAG, "❌ 영수증 삭제 실패: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 영수증 삭제 중 오류", e)
            Result.failure(e)
        }
    }
}