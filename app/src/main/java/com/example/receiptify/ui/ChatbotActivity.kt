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
import com.google.gson.Gson
import kotlinx.coroutines.launch

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
        val timestamp: Long = System.currentTimeMillis()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        setupClickListeners()

        // 초기 인사 메시지
        addBotMessage("안녕하세요! 저는 Receiptify 소비 관리 도우미입니다. 😊\n\n무엇을 도와드릴까요?")
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
        chatAdapter = ChatMessageAdapter(messages)

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatbotActivity)
            adapter = chatAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        // 빠른 질문 버튼들
        binding.btnQuickTotal.setOnClickListener {
            sendQuickMessage("이번 달 총 지출은 얼마야?")
        }

        binding.btnQuickFood.setOnClickListener {
            sendQuickMessage("식비 지출은 어때?")
        }

        binding.btnQuickTips.setOnClickListener {
            sendQuickMessage("절약 팁 알려줘")
        }

        binding.btnQuickAnalysis.setOnClickListener {
            sendQuickMessage("소비 분석해줘")
        }
    }

    /**
     * 메시지 전송
     */
    private fun sendMessage() {
        val message = binding.etMessage.text.toString().trim()

        if (message.isBlank()) {
            return
        }

        // 사용자 메시지 추가
        addUserMessage(message)

        // 입력 필드 초기화
        binding.etMessage.text?.clear()

        // 챗봇 응답 요청
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
        messages.add(ChatMessage(text, isUser = true))
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }

    /**
     * 봇 메시지 추가
     */
    private fun addBotMessage(text: String) {
        messages.add(ChatMessage(text, isUser = false))
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
                Log.d(TAG, "💬 챗봇 요청: $message")

                val requestBody = mapOf("message" to message)
                val response = RetrofitClient.api.sendChatbotMessage(requestBody)

                if (response.isSuccessful && response.body()?.success == true) {
                    val botMessage = response.body()?.data?.get("message") as? String
                        ?: "죄송합니다. 응답을 생성할 수 없습니다."

                    Log.d(TAG, "✅ 챗봇 응답: $botMessage")
                    addBotMessage(botMessage)

                } else {
                    val errorMsg = response.body()?.message ?: "알 수 없는 오류"
                    Log.e(TAG, "❌ 챗봇 응답 실패: $errorMsg")
                    addBotMessage("죄송합니다. 오류가 발생했습니다. 다시 시도해주세요.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 챗봇 오류", e)
                addBotMessage("죄송합니다. 네트워크 오류가 발생했습니다.")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}