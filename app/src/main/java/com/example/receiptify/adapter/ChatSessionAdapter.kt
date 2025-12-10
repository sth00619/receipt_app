package com.example.receiptify.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.receiptify.api.models.ChatSessionResponse
import android.widget.TextView
import com.example.receiptify.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ChatSessionAdapter(
    private val onSessionClick: (ChatSessionResponse) -> Unit,
    private val onDeleteClick: (ChatSessionResponse) -> Unit
) : ListAdapter<ChatSessionResponse, ChatSessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(com.example.receiptify.R.layout.chat_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SessionViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: android.widget.TextView = itemView.findViewById(com.example.receiptify.R.id.tv_title)
        private val tvLastMessage: android.widget.TextView = itemView.findViewById(com.example.receiptify.R.id.tv_last_message)
        private val tvDate: android.widget.TextView = itemView.findViewById(com.example.receiptify.R.id.tv_date)

        fun bind(session: ChatSessionResponse) {
            tvTitle.text = session.title
            tvLastMessage.text = session.lastMessage

            // Format date
            try {
                // ISO 8601 parsing might need adjustment depending on backend format
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(session.lastMessageAt)

                val outputFormat = SimpleDateFormat("MM/dd a hh:mm", Locale.getDefault())
                tvDate.text = if (date != null) outputFormat.format(date) else ""
            } catch (e: Exception) {
                tvDate.text = ""
            }

            itemView.setOnClickListener {
                onSessionClick(session)
            }

            itemView.setOnLongClickListener {
                onDeleteClick(session)
                true
            }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<ChatSessionResponse>() {
        override fun areItemsTheSame(oldItem: ChatSessionResponse, newItem: ChatSessionResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatSessionResponse, newItem: ChatSessionResponse): Boolean {
            return oldItem == newItem
        }
    }
}
