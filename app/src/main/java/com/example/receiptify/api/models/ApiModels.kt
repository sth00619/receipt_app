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

// ==================== 인증 관련 모델 ====================

/**
 * 네이버 로그인 요청
 */
data class NaverLoginRequest(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("name")
    val name: String? = null
)

/**
 * 구글 로그인 요청
 */
data class GoogleLoginRequest(
    @SerializedName("idToken")
    val idToken: String,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("photoUrl")
    val photoUrl: String? = null
)

// ==================== 사용자 관련 모델 ====================

/**
 * 사용자 응답 (프로필 조회용)
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

// 프로필 응답
data class ProfileResponse(
    @SerializedName("user")
    val user: UserInfo,

    @SerializedName("stats")
    val stats: ProfileStats
)

data class UserInfo(
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

    @SerializedName("createdAt")
    val createdAt: String
)

data class ProfileStats(
    @SerializedName("monthlySpending")
    val monthlySpending: Double,

    @SerializedName("monthlyReceiptCount")
    val monthlyReceiptCount: Int,

    @SerializedName("totalReceipts")
    val totalReceipts: Int
)

// 비밀번호 변경 요청
data class ChangePasswordRequest(
    @SerializedName("currentPassword")
    val currentPassword: String,

    @SerializedName("newPassword")
    val newPassword: String
)

// 설정 업데이트 요청
data class UpdateSettingRequest(
    @SerializedName("enabled")
    val enabled: Boolean
)

/**
 * 사용자 통계
 */
data class UserStats(
    @SerializedName("totalReceipts")
    val totalReceipts: Int = 0,

    @SerializedName("totalTransactions")
    val totalTransactions: Int = 0,

    @SerializedName("monthlyReceipts")
    val monthlyReceipts: Int = 0,

    @SerializedName("totalSpending")
    val totalSpending: Double = 0.0
)

// 알림 관련 모델
data class NotificationItem(
    val _id: String,
    val userId: String,
    val type: String,  // budget_warning, spending_alert, category_alert, tip, monthly_summary
    val title: String,
    val message: String,
    val category: String? = null,
    val amount: Double? = null,
    val isRead: Boolean = false,
    val priority: String = "medium",  // low, medium, high
    val metadata: NotificationMetadata? = null,
    val createdAt: String,
    val updatedAt: String? = null
)

data class NotificationsResponse(
    val notifications: List<NotificationItem>,
    val unreadCount: Int
)

data class NotificationMetadata(
    val triggerType: String? = null,
    val limit: Double? = null,
    val overAmount: Double? = null,
    val chatbotSuggested: Boolean = false
)

data class AnalyzeResponse(
    val newNotifications: Int,
    val message: String,

    val alerts: List<NotificationItem>,
    val tips: List<NotificationItem>,
    val currentMonthStats: Map<String, CategoryStat>,
    val lastMonthStats: Map<String, CategoryStat>
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
    val dailyStats: List<DailyStat>,

    // ✅ 추가: 홈 화면용 통계
    @SerializedName("currentMonthTotal")
    val currentMonthTotal: Double = 0.0,

    @SerializedName("lastMonthTotal")
    val lastMonthTotal: Double = 0.0,

    @SerializedName("todayTotal")
    val todayTotal: Double = 0.0,

    @SerializedName("monthlyChangePercent")
    val monthlyChangePercent: Int = 0
)

/**
 * 카테고리별 통계
 * ✅ 수정: @SerializedName("_id") → @SerializedName("category")
 */
data class CategoryStat(
    @SerializedName("category")  // ✅ 수정: 백엔드에서 "category"로 보내고 있음
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

// ==================== 챗봇 세션 관련 모델 ====================

/**
 * 채팅 세션
 */
data class ChatSession(
    @SerializedName("_id")
    val id: String,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("lastMessage")
    val lastMessage: String,

    @SerializedName("messageCount")
    val messageCount: Int,

    @SerializedName("isActive")
    val isActive: Boolean,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String
)

/**
 * 채팅 메시지
 */
data class ChatMessageItem(
    @SerializedName("_id")
    val id: String,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("sessionId")
    val sessionId: String,

    @SerializedName("role")
    val role: String,  // "user", "bot", "system"

    @SerializedName("message")
    val message: String,

    @SerializedName("metadata")
    val metadata: Map<String, Any>? = null,

    @SerializedName("createdAt")
    val createdAt: String
)

/**
 * 세션 목록 응답
 */
data class SessionsResponse(
    @SerializedName("sessions")
    val sessions: List<ChatSession>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("hasMore")
    val hasMore: Boolean
)

/**
 * 세션 상세 응답
 */
data class SessionDetailResponse(
    @SerializedName("session")
    val session: ChatSession,

    @SerializedName("messages")
    val messages: List<ChatMessageItem>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("hasMore")
    val hasMore: Boolean
)

/**
 * 세션 생성 요청
 */
data class CreateSessionRequest(
    @SerializedName("title")
    val title: String? = null
)

/**
 * 세션 생성 응답
 */
data class CreateSessionResponse(
    @SerializedName("session")
    val session: ChatSession
)

/**
 * 메시지 전송 요청
 */
data class SendMessageRequest(
    @SerializedName("message")
    val message: String,

    @SerializedName("sessionId")
    val sessionId: String? = null
)

/**
 * 메시지 전송 응답
 */
data class SendMessageResponse(
    @SerializedName("response")
    val response: String,

    @SerializedName("stats")
    val stats: Map<String, Any>? = null,

    @SerializedName("messageId")
    val messageId: String? = null,

    @SerializedName("sessionId")
    val sessionId: String? = null,

    @SerializedName("userMessage")
    val userMessage: ChatMessageItem? = null,

    @SerializedName("botMessage")
    val botMessage: ChatMessageItem? = null
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