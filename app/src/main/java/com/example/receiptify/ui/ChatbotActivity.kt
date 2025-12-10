package com.example.receiptify.ui

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.receiptify.adapter.ChatMessageAdapter
import com.example.receiptify.api.RetrofitClient
import com.example.receiptify.api.models.*
import com.example.receiptify.databinding.ActivityChatbotBinding
import com.example.receiptify.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private lateinit var chatAdapter: ChatMessageAdapter
    private val messages = mutableListOf<ChatMessage>()
    private var sessionId: String? = null

    companion object {
        private const val TAG = "ChatbotActivity"
    }

    data class ChatMessage(
        val id: String? = null,
        val text: String,
        val isUser: Boolean,
        val role: String = if (isUser) "user" else "bot", // user, bot, system
        val timestamp: String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()

        // ✅ 세션 ID 확인
        sessionId = intent.getStringExtra("SESSION_ID")

        // ✅ 대화 내역 로드
        if (sessionId != null && sessionId != "new") {
            loadChatHistory()
        } else {
            // 새 대화인 경우 환영 메시지
            addBotMessage("안녕하세요! 😊 소비 도우미입니다.\n\n새로운 대화를 시작해보세요!")
        }

        // ✅ 알림에서 넘어왔는지 확인
        val notificationId = intent.getStringExtra("notification_id")
        val notificationTitle = intent.getStringExtra("notification_title")

        if (notificationId != null && notificationTitle != null) {
            // 알림 기반 조언 요청 (새 세션으로 시작하거나 현재 세션에 추가)
            addBotMessage("안녕하세요! 😊\n'$notificationTitle' 알림에 대해 상세히 설명해드릴게요.")
            requestNotificationAdvice(notificationId)
        } else if (sessionId == "new") {
            // 일반 환영 메시지 (이미 추가됨)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chatbot, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete_history -> {
                showDeleteConfirmDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
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
        val message = ChatMessage(text = text, isUser = true)
        messages.add(message)
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }

    /**
     * 봇 메시지 추가
     */
    private fun addBotMessage(text: String) {
        val message = ChatMessage(text = text, isUser = false)
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

                val requestBody = mutableMapOf("message" to message)
                sessionId?.let { requestBody["sessionId"] = it }
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

                    // 세션 ID 업데이트 (첫 메시지인 경우)
                    if (responseData != null && responseData.containsKey("sessionId")) {
                        sessionId = responseData["sessionId"] as? String
                    }

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

    /**
     * ✅ 대화 내역 로드
     */
    private fun loadChatHistory() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "📋 대화 내역 로드 시작")

                val response = RetrofitClient.api.fetchChatMessages(sessionId = sessionId)

                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    // Explicitly cast or type the list to help inference
                    val chatMessages: List<ChatMessageResponse> = data?.messages ?: emptyList()

                    Log.d(TAG, "✅ ${chatMessages.size}개 메시지 로드 완료")

                    // 메시지 변환 및 추가
                    chatMessages.forEach { msg ->
                        // Explicitly referencing properties of msg (ChatMessageResponse)
                        val roleString = msg.role
                        val createdAtString = msg.createdAt

                        val isUser = roleString == "user"
                        val timestamp = try {
                            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                            isoFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            val date = isoFormat.parse(createdAtString)
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date ?: Date())
                        } catch (e: Exception) {
                            "00:00"
                        }

                        messages.add(
                            ChatMessage(
                                id = msg.id,
                                text = msg.message,
                                isUser = isUser,
                                role = roleString,
                                timestamp = timestamp
                            )
                        )
                    }

                    chatAdapter.notifyDataSetChanged()
                    if (messages.isNotEmpty()) {
                        binding.rvMessages.scrollToPosition(messages.size - 1)
                    }

                } else {
                    Log.e(TAG, "❌ 대화 내역 로드 실패: ${response.code()}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 대화 내역 로드 중 오류", e)
            }
        }
    }

    /**
     * ✅ 삭제 확인 다이얼로그
     */
    private fun showDeleteConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("대화방 삭제")
            .setMessage("이 대화방을 삭제하시겠습니까?\n\n모든 대화 내역이 삭제됩니다.")
            .setPositiveButton("삭제") { _, _ ->
                deleteCurrentSession()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    /**
     * ✅ 전체 대화 내역 삭제
     */
    /**
     * ✅ 현재 세션 삭제
     */
    private fun deleteCurrentSession() {
        if (sessionId == null) return

        lifecycleScope.launch {
            try {
                Log.d(TAG, "🗑️ 세션 삭제 시작: $sessionId")

                val response = RetrofitClient.api.removeChatSession(sessionId!!)

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@ChatbotActivity, "대화방이 삭제되었습니다", Toast.LENGTH_SHORT).show()
                    finish() // 액티비티 종료
                } else {
                    Log.e(TAG, "❌ 세션 삭제 실패: ${response.code()}")
                    Toast.makeText(this@ChatbotActivity, "삭제에 실패했습니다", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 세션 삭제 중 오류", e)
                Toast.makeText(this@ChatbotActivity, "오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
}