package com.example.receiptify.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptify.R
import com.example.receiptify.databinding.ItemChatMessageBinding
import com.example.receiptify.ui.ChatbotActivity
import java.text.SimpleDateFormat
import java.util.*

class ChatMessageAdapter(
    private val messages: List<ChatbotActivity.ChatMessage>
) : RecyclerView.Adapter<ChatMessageAdapter.ViewHolder>() {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.KOREA)

    inner class ViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatbotActivity.ChatMessage) {
            binding.tvMessage.text = message.text
            binding.tvTime.text = timeFormat.format(Date(message.timestamp))

            if (message.isUser) {
                // 사용자 메시지 (오른쪽)
                binding.layoutMessage.gravity = Gravity.END
                binding.cardMessage.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.primary_purple)
                )
                binding.tvMessage.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.white)
                )
            } else {
                // 봇 메시지 (왼쪽)
                binding.layoutMessage.gravity = Gravity.START
                binding.cardMessage.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.surface_light)
                )
                binding.tvMessage.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.text_primary)
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size
}