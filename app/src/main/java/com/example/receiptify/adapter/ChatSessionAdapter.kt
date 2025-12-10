package com.example.receiptify.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptify.api.models.ChatSession
import com.example.receiptify.databinding.ItemChatSessionBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatSessionAdapter(
    private val sessions: List<ChatSession>,
    private val onSessionClick: (ChatSession) -> Unit,
    private val onSessionLongClick: (ChatSession) -> Unit
) : RecyclerView.Adapter<ChatSessionAdapter.SessionViewHolder>() {

    inner class SessionViewHolder(private val binding: ItemChatSessionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(session: ChatSession) {
            binding.tvTitle.text = session.title
            binding.tvLastMessage.text = if (session.lastMessage.isNotEmpty()) {
                session.lastMessage
            } else {
                "새 대화"
            }
            binding.tvMessageCount.text = "${session.messageCount}개 메시지"
            binding.tvTime.text = formatTime(session.updatedAt)

            binding.root.setOnClickListener {
                onSessionClick(session)
            }

            binding.root.setOnLongClickListener {
                onSessionLongClick(session)
                true
            }
        }

        private fun formatTime(isoTime: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(isoTime)

                val now = Date()
                val diff = now.time - (date?.time ?: 0)

                when {
                    diff < 60 * 1000 -> "방금 전"
                    diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}분 전"
                    diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}시간 전"
                    diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}일 전"
                    else -> {
                        val outputFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                        outputFormat.format(date ?: now)
                    }
                }
            } catch (e: Exception) {
                ""
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemChatSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(sessions[position])
    }

    override fun getItemCount(): Int = sessions.size
}
