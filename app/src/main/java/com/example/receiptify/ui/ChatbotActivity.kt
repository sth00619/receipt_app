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
import com.example.receiptify.api.models.ChatMessageItem
import com.example.receiptify.api.models.CreateSessionRequest
import com.example.receiptify.databinding.ActivityChatbotBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private lateinit var chatAdapter: ChatMessageAdapter
    private val messages = mutableListOf<ChatMessage>()

    private var sessionId: String? = null
    private var sessionTitle: String = "New Conversation"

    companion object {
        private const val TAG = "ChatbotActivity"
    }

    data class ChatMessage(
        val text: String,
        val isUser: Boolean,
        val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
        val messageId: String? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent에서 세션 정보 가져오기
        sessionId = intent.getStringExtra("session_id")
        sessionTitle = intent.getStringExtra("session_title") ?: "New Conversation"

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()

        // 알림에서 넘어왔는지 확인
        val notificationId = intent.getStringExtra("notification_id")
        val notificationTitle = intent.getStringExtra("notification_title")

        if (notificationId != null && notificationTitle != null) {
            // 알림 기반 조언 요청
            addBotMessage("안녕하세요! 😊\n'$notificationTitle' 알림에 대해 상세히 설명해드릴게요.")
            requestNotificationAdvice(notificationId)
        } else if (sessionId != null) {
            // 기존 세션 메시지 로드
            loadSessionMessages()
        } else {
            // 새 세션 생성
            createNewSession()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "💬 $sessionTitle"

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
            sendQuickMessage("이번 달 총 지출 얼마야?")
        }

        binding.btnQuickFood.setOnClickListener {
            sendQuickMessage("이번 주 식비 분석해줘")
        }

        binding.btnQuickTips.setOnClickListener {
            sendQuickMessage("절약 팁 알려줘")
        }

        binding.btnQuickAnalysis.setOnClickListener {
            sendQuickMessage("이번 달 소비 분석")
        }
    }

    /**
     * 새 세션 생성
     */
    private fun createNewSession() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                Log.d(TAG, "🆕 새 세션 생성...")

                val request = CreateSessionRequest(title = null)
                val response = RetrofitClient.api.createChatSession(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    val newSession = response.body()?.data?.session

                    if (newSession != null) {
                        sessionId = newSession.id
                        sessionTitle = newSession.title
                        supportActionBar?.title = "💬 $sessionTitle"
                        Log.d(TAG, "✅ 새 세션 생성 완료: ${newSession.id}")

                        // 환영 메시지
                        addBotMessage("안녕하세요! 😊 소비 도우미입니다.\n\n궁금한 점을 물어보세요! 예를 들어:\n• \"이번 달 총 지출 얼마야?\"\n• \"지난주 식비\"\n• \"교통비 분석해줘\"")
                    }

                } else {
                    Log.e(TAG, "❌ 세션 생성 실패: ${response.code()}")
                    addBotMessage("안녕하세요! 😊 소비 도우미입니다.\n\n궁금한 점을 물어보세요!")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 세션 생성 중 오류", e)
                addBotMessage("안녕하세요! 😊 소비 도우미입니다.\n\n궁금한 점을 물어보세요!")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    /**
     * 기존 세션 메시지 로드
     */
    private fun loadSessionMessages() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                Log.d(TAG, "📋 세션 메시지 로딩: $sessionId")

                val response = RetrofitClient.api.getSessionMessages(sessionId!!)

                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    val loadedMessages = data?.messages ?: emptyList()

                    // 세션 제목 업데이트
                    data?.session?.let { session ->
                        sessionTitle = session.title
                        supportActionBar?.title = "💬 $sessionTitle"
                    }

                    // 메시지 변환 및 추가
                    messages.clear()
                    for (msg in loadedMessages) {
                        val chatMessage = ChatMessage(
                            text = msg.message,
                            isUser = msg.role == "user",
                            timestamp = formatMessageTime(msg.createdAt),
                            messageId = msg.id
                        )
                        messages.add(chatMessage)
                    }
                    chatAdapter.notifyDataSetChanged()

                    if (messages.isNotEmpty()) {
                        binding.rvMessages.scrollToPosition(messages.size - 1)
                    }

                    Log.d(TAG, "✅ ${loadedMessages.size}개 메시지 로딩 완료")

                    // 메시지가 없으면 환영 메시지
                    if (messages.isEmpty()) {
                        addBotMessage("대화를 이어가세요! 😊\n\n무엇이 궁금하신가요?")
                    }

                } else {
                    Log.e(TAG, "❌ 메시지 로딩 실패: ${response.code()}")
                    addBotMessage("이전 대화를 불러오는데 실패했습니다. 새로운 질문을 해주세요!")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 메시지 로딩 중 오류", e)
                addBotMessage("이전 대화를 불러오는데 실패했습니다. 새로운 질문을 해주세요!")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
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

                val response = if (sessionId != null) {
                    // 세션이 있으면 세션 API 사용
                    val requestBody = mapOf("message" to message)
                    RetrofitClient.api.sendSessionMessage(sessionId!!, requestBody)
                } else {
                    // 없으면 레거시 API 사용 (새 세션 자동 생성)
                    val requestBody = mapOf("message" to message)
                    RetrofitClient.api.sendChatbotMessage(requestBody)
                }

                if (response.isSuccessful && response.body()?.success == true) {
                    val responseData = response.body()?.data

                    val botResponse = if (responseData != null) {
                        // SendMessageResponse 또는 Map에서 응답 추출
                        when (responseData) {
                            is Map<*, *> -> responseData["response"] as? String ?: "응답을 받지 못했습니다."
                            else -> "응답을 받지 못했습니다."
                        }
                    } else {
                        "응답을 받지 못했습니다."
                    }

                    // 세션 ID 업데이트 (레거시 API 사용 시)
                    if (sessionId == null && responseData is Map<*, *>) {
                        sessionId = responseData["sessionId"] as? String
                    }

                    Log.d(TAG, "✅ 챗봇 응답: ${botResponse.take(100)}...")
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
     * 알림 기반 상세 조언 요청
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

    /**
     * 메시지 시간 포맷
     */
    private fun formatMessageTime(isoTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(isoTime)

            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
    }
}
