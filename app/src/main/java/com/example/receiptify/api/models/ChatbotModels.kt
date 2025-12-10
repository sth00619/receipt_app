package com.example.receiptify.api.models

import com.google.gson.annotations.SerializedName

// ==================== 챗봇 관련 모델 ====================

/**
 * 챗봇 메시지 응답
 */
data class ChatMessageResponse(
    @SerializedName("_id")
    val id: String,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("role")
    val role: String, // user, bot, system

    @SerializedName("message")
    val message: String,

    @SerializedName("metadata")
    val metadata: Map<String, Any>? = null,

    @SerializedName("relatedNotificationId")
    val relatedNotificationId: String? = null,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String
)

/**
 * 채팅 세션 (대화방) 응답
 */
data class ChatSessionResponse(
    @SerializedName("_id")
    val id: String,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("lastMessage")
    val lastMessage: String,

    @SerializedName("lastMessageAt")
    val lastMessageAt: String,

    @SerializedName("createdAt")
    val createdAt: String
)

/**
 * 챗봇 메시지 목록 응답
 */
data class ChatMessagesResponse(
    @SerializedName("messages")
    val messages: List<ChatMessageResponse>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("hasMore")
    val hasMore: Boolean
)

/**
 * 삭제 응답
 */
data class DeleteResponse(
    @SerializedName("deletedCount")
    val deletedCount: Int
)
