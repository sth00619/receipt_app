package com.example.receiptify.api.models

import com.google.gson.annotations.SerializedName

// ==================== API 공통 응답 래퍼 ====================

/**
 * 모든 API 응답의 공통 구조
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("count")
    val count: Int? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: String? = null
)

// ==================== 사용자 관련 모델 ====================

/**
 * 사용자 응답
 */
data class UserResponse(
    @SerializedName("_id")
    val id: String,

    @SerializedName("uid")
    val uid: String,

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

/**
 * 사용자 설정
 */
data class UserPreferences(
    @SerializedName("notifications")
    val notifications: Boolean = true,

    @SerializedName("darkMode")
    val darkMode: Boolean = false,

    @SerializedName("language")
    val language: String = "ko"
)

/**
 * 사용자 통계
 */
data class UserStats(
    @SerializedName("totalReceipts")
    val totalReceipts: Int = 0,

    @SerializedName("totalTransactions")
    val totalTransactions: Int = 0,

    @SerializedName("totalSpending")
    val totalSpending: Double = 0.0
)

// ==================== 영수증 관련 모델 ====================

/**
 * 영수증 아이템 (품목)
 */
data class ReceiptItem(
    @SerializedName("name")
    val name: String,

    @SerializedName("quantity")
    val quantity: Int = 1,

    @SerializedName("unitPrice")
    val unitPrice: Double? = null,

    @SerializedName("amount")
    val amount: Double
)

/**
 * 영수증 응답
 */
data class ReceiptResponse(
    @SerializedName("_id")
    val id: String,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("storeName")
    val storeName: String,

    @SerializedName("storeAddress")
    val storeAddress: String? = null,

    @SerializedName("storePhone")
    val storePhone: String? = null,

    @SerializedName("totalAmount")
    val totalAmount: Double,

    @SerializedName("taxAmount")
    val taxAmount: Double? = null,

    @SerializedName("discountAmount")
    val discountAmount: Double? = null,

    @SerializedName("transactionDate")
    val transactionDate: String,

    @SerializedName("paymentMethod")
    val paymentMethod: String? = null,

    @SerializedName("category")
    val category: String,

    @SerializedName("subcategory")
    val subcategory: String? = null,

    @SerializedName("items")
    val items: List<ReceiptItem> = emptyList(),

    @SerializedName("ocrText")
    val ocrText: String? = null,

    @SerializedName("imageUrl")
    val imageUrl: String? = null,

    @SerializedName("imagePath")
    val imagePath: String? = null,

    @SerializedName("tags")
    val tags: List<String> = emptyList(),

    @SerializedName("notes")
    val notes: String? = null,

    @SerializedName("isVerified")
    val isVerified: Boolean = false,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String
)

/**
 * 영수증 생성 요청
 */
data class CreateReceiptRequest(
    @SerializedName("userId")
    val userId: String,

    @SerializedName("storeName")
    val storeName: String,

    @SerializedName("storeAddress")
    val storeAddress: String? = null,

    @SerializedName("storePhone")
    val storePhone: String? = null,

    @SerializedName("totalAmount")
    val totalAmount: Double,

    @SerializedName("taxAmount")
    val taxAmount: Double? = null,

    @SerializedName("discountAmount")
    val discountAmount: Double? = null,

    @SerializedName("transactionDate")
    val transactionDate: String,

    @SerializedName("paymentMethod")
    val paymentMethod: String = "card",

    @SerializedName("category")
    val category: String,

    @SerializedName("subcategory")
    val subcategory: String? = null,

    @SerializedName("items")
    val items: List<ReceiptItem>,

    @SerializedName("ocrText")
    val ocrText: String? = null,

    @SerializedName("imageUrl")
    val imageUrl: String? = null,

    @SerializedName("imagePath")
    val imagePath: String? = null,

    @SerializedName("tags")
    val tags: List<String> = emptyList(),

    @SerializedName("notes")
    val notes: String? = null
)

// ==================== 통계 관련 모델 ====================

/**
 * 통계 응답
 */
data class StatsResponse(
    @SerializedName("byCategory")
    val byCategory: List<CategoryStat>,

    @SerializedName("total")
    val total: TotalStat,

    @SerializedName("dailyStats")
    val dailyStats: List<DailyStat>
)

/**
 * 카테고리별 통계
 */
data class CategoryStat(
    @SerializedName("_id")
    val category: String,

    @SerializedName("totalAmount")
    val totalAmount: Double,

    @SerializedName("count")
    val count: Int
)

/**
 * 전체 통계
 */
data class TotalStat(
    @SerializedName("totalAmount")
    val totalAmount: Double,

    @SerializedName("count")
    val count: Int
)

/**
 * 일별 통계
 */
data class DailyStat(
    @SerializedName("day")
    val day: Int,

    @SerializedName("amount")
    val amount: Double
)

// ==================== 거래 관련 모델 ====================

/**
 * 거래 응답 (간소화된 버전)
 */
data class TransactionResponse(
    @SerializedName("_id")
    val id: String,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("receiptId")
    val receiptId: String?,

    @SerializedName("storeName")
    val storeName: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("date")
    val date: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("imageUrl")
    val imageUrl: String? = null,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String
)