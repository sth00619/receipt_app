package com.example.receiptify.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.receiptify.adapter.ChatMessageAdapter
import com.example.receiptify.api.RetrofitClient
import com.example.receiptify.api.models.CreateSessionRequest
import com.example.receiptify.databinding.ActivityChatbotBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private lateinit var chatAdapter: ChatMessageAdapter
    private val messages = mutableListOf<ChatMessage>()

    private var sessionId: String? = null
    private var sessionTitle: String = "New Conversation"

    // ============ 음성 인식 관련 변수 ============
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    companion object {
        private const val TAG = "ChatbotActivity"
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
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
        setupVoiceRecognition()  // 음성 인식 초기화 추가

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

    override fun onDestroy() {
        super.onDestroy()
        // 음성 인식 리소스 해제
        speechRecognizer?.destroy()
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

        // 음성 입력 버튼
        binding.fabVoice.setOnClickListener {
            if (isListening) {
                stopVoiceRecognition()
            } else {
                startVoiceRecognition()
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

    // ============ 음성 인식 기능 ============

    /**
     * 음성 인식 초기화
     */
    private fun setupVoiceRecognition() {
        // 음성 인식 사용 가능 여부 확인
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            binding.fabVoice.visibility = View.GONE
            Log.w(TAG, "⚠️ 이 기기에서는 음성 인식을 사용할 수 없습니다")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "🎤 음성 인식 준비됨")
                runOnUiThread {
                    binding.cardVoiceRecording.visibility = View.VISIBLE
                }
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "🎤 음성 입력 시작")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // 음성 볼륨 변화 (UI 애니메이션에 활용 가능)
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // 버퍼 수신
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "🎤 음성 입력 종료")
                runOnUiThread {
                    binding.cardVoiceRecording.visibility = View.GONE
                    isListening = false
                }
            }

            override fun onError(error: Int) {
                Log.e(TAG, "🎤 음성 인식 오류: $error")
                runOnUiThread {
                    binding.cardVoiceRecording.visibility = View.GONE
                    isListening = false

                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "음성을 인식하지 못했습니다. 다시 시도해주세요."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 입력 시간이 초과되었습니다."
                        SpeechRecognizer.ERROR_AUDIO -> "오디오 녹음 오류가 발생했습니다."
                        SpeechRecognizer.ERROR_NETWORK -> "네트워크 오류가 발생했습니다."
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 시간 초과입니다."
                        SpeechRecognizer.ERROR_CLIENT -> "클라이언트 오류입니다."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "마이크 권한이 필요합니다."
                        else -> "음성 인식 오류가 발생했습니다."
                    }
                    Toast.makeText(this@ChatbotActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    Log.d(TAG, "🎤 인식된 텍스트: $recognizedText")

                    runOnUiThread {
                        binding.cardVoiceRecording.visibility = View.GONE
                        isListening = false

                        // 인식된 텍스트로 메시지 전송
                        if (recognizedText.isNotEmpty()) {
                            sendMessage(recognizedText)
                        }
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // 부분 결과 (실시간 인식 표시에 활용 가능)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // 추가 이벤트
            }
        })
    }

    /**
     * 음성 인식 시작
     */
    private fun startVoiceRecognition() {
        // 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")  // 한국어
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "질문을 말씀해주세요...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            isListening = true
            speechRecognizer?.startListening(intent)
            Log.d(TAG, "🎤 음성 인식 시작")
        } catch (e: Exception) {
            Log.e(TAG, "🎤 음성 인식 시작 실패", e)
            isListening = false
            Toast.makeText(this, "음성 인식을 시작할 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 음성 인식 중지
     */
    private fun stopVoiceRecognition() {
        speechRecognizer?.stopListening()
        isListening = false
        binding.cardVoiceRecording.visibility = View.GONE
    }

    /**
     * 권한 요청 결과 처리
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 승인됨 - 음성 인식 시작
                startVoiceRecognition()
            } else {
                Toast.makeText(this, "음성 입력을 사용하려면 마이크 권한이 필요합니다", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ============ 세션 관리 ============

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
                        withContext(Dispatchers.Main) {
                            supportActionBar?.title = "💬 $sessionTitle"
                        }
                        Log.d(TAG, "✅ 새 세션 생성 완료: ${newSession.id}")

                        // 환영 메시지
                        withContext(Dispatchers.Main) {
                            addBotMessage("안녕하세요! 😊 소비 도우미입니다.\n\n궁금한 점을 물어보세요! 예를 들어:\n• \"이번 달 총 지출 얼마야?\"\n• \"지난주 식비\"\n• \"교통비 분석해줘\"\n\n🎤 마이크 버튼을 눌러 음성으로 질문할 수도 있어요!")
                        }
                    }

                } else {
                    Log.e(TAG, "❌ 세션 생성 실패: ${response.code()}")
                    withContext(Dispatchers.Main) {
                        addBotMessage("안녕하세요! 😊 소비 도우미입니다.\n\n궁금한 점을 물어보세요!")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 세션 생성 중 오류", e)
                withContext(Dispatchers.Main) {
                    addBotMessage("안녕하세요! 😊 소비 도우미입니다.\n\n궁금한 점을 물어보세요!")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
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
                        withContext(Dispatchers.Main) {
                            supportActionBar?.title = "💬 $sessionTitle"
                        }
                    }

                    // 메시지 변환 및 추가
                    withContext(Dispatchers.Main) {
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
                    }

                    Log.d(TAG, "✅ ${loadedMessages.size}개 메시지 로딩 완료")

                    // 메시지가 없으면 환영 메시지
                    if (loadedMessages.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            addBotMessage("대화를 이어가세요! 😊\n\n무엇이 궁금하신가요?")
                        }
                    }

                } else {
                    Log.e(TAG, "❌ 메시지 로딩 실패: ${response.code()}")
                    withContext(Dispatchers.Main) {
                        addBotMessage("이전 대화를 불러오는데 실패했습니다. 새로운 질문을 해주세요!")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 메시지 로딩 중 오류", e)
                withContext(Dispatchers.Main) {
                    addBotMessage("이전 대화를 불러오는데 실패했습니다. 새로운 질문을 해주세요!")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    // ============ 메시지 관리 ============

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
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.VISIBLE
                }

                Log.d(TAG, "💬 챗봇에게 메시지 전송: $message")
                Log.d(TAG, "   세션 ID: $sessionId")

                var botResponse: String = "응답을 받지 못했습니다."

                if (sessionId != null) {
                    // 세션이 있으면 세션 API 사용
                    val requestBody = mapOf("message" to message)
                    val response = RetrofitClient.api.sendSessionMessage(sessionId!!, requestBody)

                    Log.d(TAG, "   응답 코드: ${response.code()}")
                    Log.d(TAG, "   응답 성공: ${response.body()?.success}")

                    if (response.isSuccessful && response.body()?.success == true) {
                        val responseData = response.body()?.data
                        Log.d(TAG, "   응답 데이터: $responseData")

                        botResponse = responseData?.response ?: "응답을 받지 못했습니다."
                        Log.d(TAG, "✅ 챗봇 응답: ${botResponse.take(100)}...")
                    } else {
                        Log.e(TAG, "❌ 챗봇 응답 실패: ${response.code()}")
                        val errorBody = response.errorBody()?.string()
                        Log.e(TAG, "   에러 바디: $errorBody")
                        botResponse = "죄송해요, 응답을 생성하는데 실패했습니다. 😢"
                    }
                } else {
                    // 없으면 레거시 API 사용 (새 세션 자동 생성)
                    val requestBody = mapOf("message" to message)
                    val response = RetrofitClient.api.sendChatbotMessage(requestBody)

                    if (response.isSuccessful && response.body()?.success == true) {
                        val responseData = response.body()?.data

                        botResponse = responseData?.get("response") as? String ?: "응답을 받지 못했습니다."

                        // 세션 ID 업데이트
                        sessionId = responseData?.get("sessionId") as? String

                        Log.d(TAG, "✅ 챗봇 응답 (레거시): ${botResponse.take(100)}...")
                    } else {
                        Log.e(TAG, "❌ 챗봇 응답 실패 (레거시): ${response.code()}")
                        botResponse = "죄송해요, 응답을 생성하는데 실패했습니다. 😢"
                    }
                }

                withContext(Dispatchers.Main) {
                    addBotMessage(botResponse)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 챗봇 통신 중 오류", e)
                withContext(Dispatchers.Main) {
                    addBotMessage("오류가 발생했습니다. 다시 시도해주세요.")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    /**
     * 알림 기반 상세 조언 요청
     */
    private fun requestNotificationAdvice(notificationId: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.VISIBLE
                }

                Log.d(TAG, "💬 알림 기반 조언 요청: $notificationId")

                val response = RetrofitClient.api.getChatbotAdvice(notificationId)

                if (response.isSuccessful && response.body()?.success == true) {
                    val responseData = response.body()?.data

                    if (responseData != null) {
                        val advice = responseData["advice"] as? String
                            ?: "조언을 받지 못했습니다."

                        Log.d(TAG, "✅ 알림 조언 수신")
                        withContext(Dispatchers.Main) {
                            addBotMessage(advice)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            addBotMessage("조언을 받지 못했습니다.")
                        }
                    }

                } else {
                    Log.e(TAG, "❌ 알림 조언 실패: ${response.code()}")
                    withContext(Dispatchers.Main) {
                        addBotMessage("조언을 가져오는데 실패했습니다.")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 알림 조언 요청 중 오류", e)
                withContext(Dispatchers.Main) {
                    addBotMessage("오류가 발생했습니다.")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
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