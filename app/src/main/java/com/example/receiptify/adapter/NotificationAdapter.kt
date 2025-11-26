package com.example.receiptify.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptify.R
import com.example.receiptify.api.models.NotificationItem
import com.example.receiptify.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val notifications: List<NotificationItem>,
    private val onItemClick: (NotificationItem) -> Unit,
    private val onDeleteClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.KOREA)

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationItem) {
            binding.tvTitle.text = notification.title
            binding.tvMessage.text = notification.message

            // 날짜 표시
            try {
                val createdDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    .parse(notification.createdAt)

                if (createdDate != null) {
                    binding.tvDate.text = dateFormat.format(createdDate)
                }
            } catch (e: Exception) {
                binding.tvDate.text = ""
            }

            // 읽지 않은 알림 강조
            if (!notification.isRead) {
                binding.tvTitle.setTypeface(null, Typeface.BOLD)
                binding.cardView.strokeWidth = 2
                binding.cardView.strokeColor = ContextCompat.getColor(
                    binding.root.context,
                    R.color.primary_purple
                )
                binding.viewUnreadIndicator.visibility = android.view.View.VISIBLE
            } else {
                binding.tvTitle.setTypeface(null, Typeface.NORMAL)
                binding.cardView.strokeWidth = 0
                binding.viewUnreadIndicator.visibility = android.view.View.GONE
            }

            // 우선순위별 아이콘 색상
            val iconColor = when (notification.priority) {
                "high" -> R.color.error_red
                "medium" -> R.color.primary_purple
                else -> R.color.text_secondary
            }
            binding.ivIcon.setColorFilter(
                ContextCompat.getColor(binding.root.context, iconColor)
            )

            // 클릭 리스너
            binding.root.setOnClickListener {
                onItemClick(notification)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(notification)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size
}