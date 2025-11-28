package com.example.receiptify.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.receiptify.adapter.ChatMessageAdapter
import com.example.receiptify.api.RetrofitClient
import com.example.receiptify.databinding.ActivityChatbotBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private lateinit var chatAdapter: ChatMessageAdapter
    private val messages = mutableListOf<ChatMessage>()

    companion object {
        private const val TAG = "ChatbotActivity"
    }

    data class ChatMessage(
        val text: String,
        val isUser: Boolean,
        val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()

        // ✅ 알림에서 넘어왔는지 확인
        val notificationId = intent.getStringExtra("notification_id")
        val notificationTitle = intent.getStringExtra("notification_title")

        if (notificationId != null && notificationTitle != null) {
            // 알림 기반 조언 요청
            addBotMessage("안녕하세요! 😊\n'$notificationTitle' 알림에 대해 상세히 설명해드릴게요.")
            requestNotificationAdvice(notificationId)
        } else {
            // 일반 환영 메시지
            addBotMessage("안녕하세요! 😊 소비 도우미입니다.\n\n궁금한 점을 물어보세요!")
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "💬 소비 도우미"

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatMessageAdapter(messages)

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatbotActivity)
            adapter = chatAdapter
        }
    }

    private fun setupClickListeners() {
        // 전송 버튼
        binding.fabSend.setOnClickListener {
            val message = binding.etMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
            }
        }

        // 빠른 질문 버튼들
        binding.btnQuickTotal.setOnClickListener {
            sendQuickMessage("총 지출 얼마야?")
        }

        binding.btnQuickFood.setOnClickListener {
            sendQuickMessage("식비 분석해줘")
        }

        binding.btnQuickTips.setOnClickListener {
            sendQuickMessage("절약 팁 알려줘")
        }

        binding.btnQuickAnalysis.setOnClickListener {
            sendQuickMessage("이번 달 소비 분석")
        }
    }

    /**
     * 사용자 메시지 전송
     */
    private fun sendMessage(message: String) {
        binding.etMessage.text?.clear()
        addUserMessage(message)
        requestChatbotResponse(message)
    }

    /**
     * 빠른 질문 전송
     */
    private fun sendQuickMessage(message: String) {
        addUserMessage(message)
        requestChatbotResponse(message)
    }

    /**
     * 사용자 메시지 추가
     */
    private fun addUserMessage(text: String) {
        val message = ChatMessage(text, isUser = true)
        messages.add(message)
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }

    /**
     * 봇 메시지 추가
     */
    private fun addBotMessage(text: String) {
        val message = ChatMessage(text, isUser = false)
        messages.add(message)
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }

    /**
     * 챗봇 응답 요청
     */
    private fun requestChatbotResponse(message: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                Log.d(TAG, "💬 챗봇에게 메시지 전송: $message")

                val requestBody = mapOf("message" to message)
                val response = RetrofitClient.api.sendChatbotMessage(requestBody)

                if (response.isSuccessful && response.body()?.success == true) {
                    // ✅ Nullable 안전 처리
                    val responseData = response.body()?.data
                    val botResponse = if (responseData != null) {
                        responseData["response"] as? String ?: "응답을 받지 못했습니다."
                    } else {
                        "응답을 받지 못했습니다."
                    }

                    Log.d(TAG, "✅ 챗봇 응답: $botResponse")
                    addBotMessage(botResponse)

                } else {
                    Log.e(TAG, "❌ 챗봇 응답 실패: ${response.code()}")
                    addBotMessage("죄송해요, 응답을 생성하는데 실패했습니다. 😢")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 챗봇 통신 중 오류", e)
                addBotMessage("오류가 발생했습니다. 다시 시도해주세요.")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    /**
     * ✅ 알림 기반 상세 조언 요청
     */
    private fun requestNotificationAdvice(notificationId: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                Log.d(TAG, "💬 알림 기반 조언 요청: $notificationId")

                val response = RetrofitClient.api.getChatbotAdvice(notificationId)

                if (response.isSuccessful && response.body()?.success == true) {
                    val responseData = response.body()?.data

                    if (responseData != null) {
                        val advice = responseData["advice"] as? String
                            ?: "조언을 받지 못했습니다."

                        Log.d(TAG, "✅ 알림 조언 수신")
                        addBotMessage(advice)
                    } else {
                        addBotMessage("조언을 받지 못했습니다.")
                    }

                } else {
                    Log.e(TAG, "❌ 알림 조언 실패: ${response.code()}")
                    addBotMessage("조언을 가져오는데 실패했습니다.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 알림 조언 요청 중 오류", e)
                addBotMessage("오류가 발생했습니다.")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}