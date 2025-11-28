package com.example.receiptify.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptify.api.models.NotificationItem  // ✅ 변경
import com.example.receiptify.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val onItemClick: (NotificationItem) -> Unit,
    private val onDeleteClick: (NotificationItem) -> Unit
) : ListAdapter<NotificationItem, NotificationAdapter.NotificationViewHolder>(DiffCallback()) {  // ✅ 변경

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationItem) {  // ✅ 변경
            binding.apply {
                tvNotificationTitle.text = notification.title
                tvNotificationMessage.text = notification.message
                tvNotificationTime.text = formatTime(notification.createdAt)

                // 읽지 않은 알림 스타일
                if (!notification.isRead) {
                    tvNotificationTitle.setTypeface(null, Typeface.BOLD)
                    tvNotificationTitle.textSize = 16f
                    cardNotification.strokeWidth = 4
                } else {
                    tvNotificationTitle.setTypeface(null, Typeface.NORMAL)
                    tvNotificationTitle.textSize = 15f
                    cardNotification.strokeWidth = 0
                }

                root.setOnClickListener {
                    onItemClick(notification)
                }

                // 삭제 버튼 클릭
                btnDelete.setOnClickListener {
                    onDeleteClick(notification)
                }
            }
        }

        private fun formatTime(timestamp: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(timestamp)

                val now = Date()
                val diff = now.time - (date?.time ?: 0)

                val minutes = diff / (1000 * 60)
                val hours = diff / (1000 * 60 * 60)
                val days = diff / (1000 * 60 * 60 * 24)

                when {
                    minutes < 1 -> "방금 전"
                    minutes < 60 -> "${minutes}분 전"
                    hours < 24 -> "${hours}시간 전"
                    days < 7 -> "${days}일 전"
                    else -> {
                        val outputFormat = SimpleDateFormat("MM월 dd일", Locale.KOREAN)
                        outputFormat.format(date)
                    }
                }
            } catch (e: Exception) {
                timestamp
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<NotificationItem>() {  // ✅ 변경
        override fun areItemsTheSame(
            oldItem: NotificationItem,  // ✅ 변경
            newItem: NotificationItem   // ✅ 변경
        ): Boolean {
            return oldItem._id == newItem._id
        }

        override fun areContentsTheSame(
            oldItem: NotificationItem,  // ✅ 변경
            newItem: NotificationItem   // ✅ 변경
        ): Boolean {
            return oldItem == newItem
        }
    }
}