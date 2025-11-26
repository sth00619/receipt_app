package com.example.receiptify.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.receiptify.R
import com.example.receiptify.adapter.NotificationAdapter
import com.example.receiptify.api.RetrofitClient
import com.example.receiptify.api.models.NotificationItem
import com.example.receiptify.databinding.ActivityNotificationsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var notificationAdapter: NotificationAdapter

    private val notifications = mutableListOf<NotificationItem>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)

    companion object {
        private const val TAG = "NotificationsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        loadNotifications()
        setupClickListeners()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            notifications = notifications,
            onItemClick = { notification ->
                markAsRead(notification)
            },
            onDeleteClick = { notification ->
                deleteNotification(notification)
            }
        )

        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            adapter = notificationAdapter
        }
    }

    private fun setupClickListeners() {
        // 모두 읽음 처리
        binding.btnMarkAllRead.setOnClickListener {
            markAllAsRead()
        }
    }

    /**
     * 알림 목록 로드
     */
    private fun loadNotifications() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                Log.d(TAG, "📥 알림 로드 시작")

                val response = RetrofitClient.api.getNotifications(limit = 100)

                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data!!

                    Log.d(TAG, "✅ 알림 ${data.notifications.size}개 로드, 읽지 않음: ${data.unreadCount}개")

                    notifications.clear()
                    notifications.addAll(data.notifications)
                    notificationAdapter.notifyDataSetChanged()

                    // 읽지 않은 알림 배지
                    if (data.unreadCount > 0) {
                        binding.tvUnreadCount.text = "${data.unreadCount}개의 새 알림"
                        binding.tvUnreadCount.visibility = View.VISIBLE
                    } else {
                        binding.tvUnreadCount.visibility = View.GONE
                    }

                    // 빈 상태 표시
                    if (notifications.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvNotifications.visibility = View.GONE
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        binding.rvNotifications.visibility = View.VISIBLE
                    }

                } else {
                    val errorMsg = response.body()?.message ?: "알 수 없는 오류"
                    Log.e(TAG, "❌ 알림 로드 실패: $errorMsg")
                    Toast.makeText(this@NotificationsActivity, "로드 실패: $errorMsg", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 알림 로드 오류", e)
                Toast.makeText(this@NotificationsActivity, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    /**
     * 알림 읽음 처리
     */
    private fun markAsRead(notification: NotificationItem) {
        if (notification.isRead) return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.markNotificationAsRead(notification.id)

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d(TAG, "✅ 알림 읽음 처리: ${notification.id}")

                    // 목록 업데이트
                    val index = notifications.indexOfFirst { it.id == notification.id }
                    if (index != -1) {
                        notifications[index] = response.body()?.data!!
                        notificationAdapter.notifyItemChanged(index)
                    }

                    // 상세 내용 표시
                    showNotificationDetail(notification)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 알림 읽음 처리 오류", e)
            }
        }
    }

    /**
     * 모든 알림 읽음 처리
     */
    private fun markAllAsRead() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.markAllNotificationsAsRead()

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d(TAG, "✅ 모든 알림 읽음 처리")
                    Toast.makeText(this@NotificationsActivity, "모든 알림을 읽음 처리했습니다", Toast.LENGTH_SHORT).show()

                    // 목록 새로고침
                    loadNotifications()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 전체 읽음 처리 오류", e)
                Toast.makeText(this@NotificationsActivity, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 알림 삭제
     */
    private fun deleteNotification(notification: NotificationItem) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.deleteNotification(notification.id)

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d(TAG, "✅ 알림 삭제: ${notification.id}")

                    val index = notifications.indexOf(notification)
                    if (index != -1) {
                        notifications.removeAt(index)
                        notificationAdapter.notifyItemRemoved(index)
                    }

                    Toast.makeText(this@NotificationsActivity, "알림을 삭제했습니다", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 알림 삭제 오류", e)
                Toast.makeText(this@NotificationsActivity, "삭제 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 알림 상세 내용 표시
     */
    private fun showNotificationDetail(notification: NotificationItem) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(notification.title)
            .setMessage(notification.message)
            .setPositiveButton("확인", null)
            .show()
    }
}