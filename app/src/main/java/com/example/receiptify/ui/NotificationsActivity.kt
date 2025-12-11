package com.example.receiptify.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.receiptify.adapter.NotificationAdapter
import com.example.receiptify.api.RetrofitClient
import com.example.receiptify.api.models.NotificationItem
import com.example.receiptify.databinding.ActivityNotificationsBinding
import kotlinx.coroutines.launch

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var notificationAdapter: NotificationAdapter

    private var notifications = mutableListOf<NotificationItem>()
    private var unreadCount = 0

    companion object {
        private const val TAG = "NotificationsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // JWT 토큰 확인
        val authRepository = com.example.receiptify.repository.AuthRepository(this)
        val token = authRepository.getToken()
        Log.d(TAG, "🔍 현재 저장된 JWT 토큰: ${token?.take(30) ?: "없음"}")

        if (token == null) {
            Log.e(TAG, "❌ JWT 토큰이 없습니다! 로그인이 필요합니다.")
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show()
        }

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadNotifications()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "알림"
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            onItemClick = { notification ->
                showNotificationDetail(notification)
            },
            onDeleteClick = { notification ->
                showDeleteConfirmDialog(notification)
            }
        )

        binding.rvNotifications.apply {
            adapter = notificationAdapter
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
        }
    }

    private fun setupClickListeners() {

        // 모두 읽음 버튼
        binding.btnMarkAllRead.setOnClickListener {
            if (unreadCount > 0) {
                markAllAsRead()
            } else {
                Toast.makeText(this, "읽지 않은 알림이 없습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadNotifications() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "📬 알림 목록 조회 중...")

                val response = RetrofitClient.api.getNotifications()

                if (response.isSuccessful) {
                    val responseData = response.body()?.data
                    if (responseData != null) {
                        notifications = responseData.notifications.toMutableList()
                        unreadCount = responseData.unreadCount

                        Log.d(TAG, "✅ 알림 ${notifications.size}개 로드 완료 (읽지 않음: $unreadCount)")

                        notificationAdapter.submitList(notifications)
                        updateEmptyState()
                    } else {
                        Log.e(TAG, "❌ Response data is null")
                        notifications = mutableListOf()
                        notificationAdapter.submitList(emptyList())
                        updateEmptyState()
                    }
                } else {
                    Log.e(TAG, "❌ 알림 조회 실패: ${response.code()}")
                    notifications = mutableListOf()
                    notificationAdapter.submitList(emptyList())
                    updateEmptyState()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 알림 조회 중 예외 발생", e)
                notifications = mutableListOf()
                notificationAdapter.submitList(emptyList())
                updateEmptyState()
            }
        }
    }

    private fun markAsRead(notificationId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.markNotificationAsRead(notificationId)

                if (response.isSuccessful) {
                    val index = notifications.indexOfFirst { it._id == notificationId }
                    if (index != -1) {
                        notifications[index] = notifications[index].copy(isRead = true)
                        notificationAdapter.submitList(notifications.toList())

                        if (unreadCount > 0) {
                            unreadCount--
                        }
                    }

                    Log.d(TAG, "✅ 알림 읽음 처리 완료")
                } else {
                    Log.e(TAG, "❌ 알림 읽음 처리 실패: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 알림 읽음 처리 중 오류", e)
            }
        }
    }

    private fun markAllAsRead() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.markAllNotificationsAsRead()

                if (response.isSuccessful) {
                    notifications = notifications.map { it.copy(isRead = true) }.toMutableList()
                    notificationAdapter.submitList(notifications)
                    unreadCount = 0

                    Toast.makeText(
                        this@NotificationsActivity,
                        "모든 알림을 읽음 처리했습니다",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d(TAG, "✅ 모든 알림 읽음 처리 완료")
                } else {
                    Log.e(TAG, "❌ 모든 알림 읽음 처리 실패: ${response.code()}")
                    Toast.makeText(
                        this@NotificationsActivity,
                        "알림 읽음 처리에 실패했습니다",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 모든 알림 읽음 처리 중 오류", e)
                Toast.makeText(
                    this@NotificationsActivity,
                    "오류가 발생했습니다",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showDeleteConfirmDialog(notification: NotificationItem) {
        AlertDialog.Builder(this)
            .setTitle("알림 삭제")
            .setMessage("이 알림을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                deleteNotification(notification._id)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteNotification(notificationId: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.deleteNotification(notificationId)

                if (response.isSuccessful) {
                    val removedNotification = notifications.find { it._id == notificationId }
                    notifications.removeAll { it._id == notificationId }
                    notificationAdapter.submitList(notifications.toList())

                    if (removedNotification?.isRead == false && unreadCount > 0) {
                        unreadCount--
                    }

                    updateEmptyState()

                    Toast.makeText(
                        this@NotificationsActivity,
                        "알림이 삭제되었습니다",
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d(TAG, "✅ 알림 삭제 완료")
                } else {
                    Log.e(TAG, "❌ 알림 삭제 실패: ${response.code()}")
                    Toast.makeText(
                        this@NotificationsActivity,
                        "알림 삭제에 실패했습니다",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 알림 삭제 중 오류", e)
                Toast.makeText(
                    this@NotificationsActivity,
                    "오류가 발생했습니다",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showNotificationDetail(notification: NotificationItem) {
        if (!notification.isRead) {
            markAsRead(notification._id)
        }

        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle(notification.title)
            .setMessage(notification.message)
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
            }

        if (notification.metadata?.chatbotSuggested == true) {
            dialogBuilder.setNeutralButton("💬 챗봇 조언") { _, _ ->
                openChatbotWithAdvice(notification)
            }
        }

        dialogBuilder.setNegativeButton("삭제") { _, _ ->
            deleteNotification(notification._id)
        }
            .show()
    }

    private fun openChatbotWithAdvice(notification: NotificationItem) {
        val intent = Intent(this, ChatbotActivity::class.java).apply {
            putExtra("notification_id", notification._id)
            putExtra("notification_title", notification.title)
            putExtra("notification_category", notification.category)
        }
        startActivity(intent)
    }

    private fun analyzeSpending() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "🔍 소비 패턴 분석 중...")

                val response = RetrofitClient.api.analyzeSpending()

                if (response.isSuccessful) {
                    val responseData = response.body()?.data
                    if (responseData != null) {
                        Toast.makeText(
                            this@NotificationsActivity,
                            "새로운 알림 ${responseData.newNotifications}개가 생성되었습니다",
                            Toast.LENGTH_SHORT
                        ).show()

                        Log.d(TAG, "✅ 분석 완료: ${responseData.message}")

                        // 알림 목록 새로고침
                        loadNotifications()
                    } else {
                        Log.e(TAG, "❌ Response data is null")
                        Toast.makeText(
                            this@NotificationsActivity,
                            "분석에 실패했습니다",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e(TAG, "❌ 분석 실패: ${response.code()}")
                    Toast.makeText(
                        this@NotificationsActivity,
                        "분석에 실패했습니다",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 분석 중 오류", e)
                Toast.makeText(
                    this@NotificationsActivity,
                    "오류가 발생했습니다",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateEmptyState() {
        if (notifications.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvNotifications.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvNotifications.visibility = View.VISIBLE
        }
    }
}