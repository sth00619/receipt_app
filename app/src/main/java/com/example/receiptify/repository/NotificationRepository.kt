package com.example.receiptify.repository

import android.util.Log
import com.example.receiptify.api.RetrofitClient
import com.example.receiptify.api.models.NotificationsResponse

class NotificationRepository {

    private val api = RetrofitClient.api

    companion object {
        private const val TAG = "NotificationRepository"
    }

    /**
     * Get notifications with optional filters
     */
    suspend fun getNotifications(
        unreadOnly: Boolean = false
    ): Result<NotificationsResponse> {
        return try {
            Log.d(TAG, "📬 Fetching notifications (unreadOnly: $unreadOnly)")

            val response = api.getNotifications()

            Log.d(TAG, "Response code: ${response.code()}")
            Log.d(TAG, "Is successful: ${response.isSuccessful}")

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data!!
                Log.d(TAG, "✅ Notifications fetched: ${data.notifications.size} total, ${data.unreadCount} unread")
                Result.success(data)
            } else {
                val errorBody = response.errorBody()?.string()
                val bodyMessage = response.body()?.message

                Log.e(TAG, "❌ Failed to fetch notifications")
                Log.e(TAG, "  - HTTP Status: ${response.code()}")
                Log.e(TAG, "  - Body message: $bodyMessage")
                Log.e(TAG, "  - Error body: $errorBody")

                val errorMsg = bodyMessage ?: errorBody ?: "Failed to fetch notifications"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception while fetching notifications: ${e.javaClass.simpleName}", e)
            Log.e(TAG, "  - Message: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Mark notification as read
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            Log.d(TAG, "📖 Marking notification as read: $notificationId")

            val response = api.markNotificationAsRead(notificationId)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "✅ Notification marked as read")
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = response.body()?.message ?: errorBody ?: "Failed to mark as read"
                Log.e(TAG, "❌ Failed to mark as read: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception while marking as read", e)
            Result.failure(e)
        }
    }

    /**
     * Mark all notifications as read
     */
    suspend fun markAllAsRead(): Result<Unit> {
        return try {
            Log.d(TAG, "📖 Marking all notifications as read")

            val response = api.markAllNotificationsAsRead()

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "✅ All notifications marked as read")
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = response.body()?.message ?: errorBody ?: "Failed to mark all as read"
                Log.e(TAG, "❌ Failed to mark all as read: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception while marking all as read", e)
            Result.failure(e)
        }
    }

    /**
     * Delete notification
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🗑️ Deleting notification: $notificationId")

            val response = api.deleteNotification(notificationId)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "✅ Notification deleted")
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = response.body()?.message ?: errorBody ?: "Failed to delete notification"
                Log.e(TAG, "❌ Failed to delete: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception while deleting notification", e)
            Result.failure(e)
        }
    }
}
