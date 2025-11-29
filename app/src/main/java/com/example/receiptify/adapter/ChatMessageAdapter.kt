package com.example.receiptify.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptify.R
import com.example.receiptify.databinding.ItemChatMessageBinding
import com.example.receiptify.ui.ChatbotActivity

class ChatMessageAdapter(
    private val messages: List<ChatbotActivity.ChatMessage>
) : RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(private val binding: ItemChatMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatbotActivity.ChatMessage) {
            binding.tvMessage.text = message.text
            binding.tvTime.text = message.timestamp

            if (message.isUser) {
                // 사용자 메시지 (오른쪽, 보라색)
                binding.cardMessage.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.primary_purple)
                )
                binding.tvMessage.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.white)
                )
                binding.tvTime.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.white)
                )

                // ✅ LinearLayout의 gravity 설정
                (binding.root.layoutParams as? androidx.recyclerview.widget.RecyclerView.LayoutParams)?.apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                }
                binding.layoutMessageContainer.gravity = Gravity.END

                // 마진 설정
                val params = binding.cardMessage.layoutParams as ViewGroup.MarginLayoutParams
                params.setMargins(80.dpToPx(), 0, 16.dpToPx(), 0)
                binding.cardMessage.layoutParams = params

            } else {
                // 봇 메시지 (왼쪽, 흰색)
                binding.cardMessage.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.white)
                )
                binding.tvMessage.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.black)
                )
                binding.tvTime.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.gray)
                )

                // ✅ LinearLayout의 gravity 설정
                binding.layoutMessageContainer.gravity = Gravity.START

                // 마진 설정
                val params = binding.cardMessage.layoutParams as ViewGroup.MarginLayoutParams
                params.setMargins(16.dpToPx(), 0, 80.dpToPx(), 0)
                binding.cardMessage.layoutParams = params
            }
        }

        // ✅ dp를 px로 변환하는 확장 함수
        private fun Int.dpToPx(): Int {
            return (this * itemView.context.resources.displayMetrics.density).toInt()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size
}