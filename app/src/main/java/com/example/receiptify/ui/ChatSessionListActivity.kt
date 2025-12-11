package com.example.receiptify.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.receiptify.adapter.ChatSessionAdapter
import com.example.receiptify.api.RetrofitClient
import com.example.receiptify.api.models.ChatSession
import com.example.receiptify.api.models.CreateSessionRequest
import com.example.receiptify.databinding.ActivityChatSessionListBinding
import kotlinx.coroutines.launch

class ChatSessionListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatSessionListBinding
    private lateinit var sessionAdapter: ChatSessionAdapter
    private val sessions = mutableListOf<ChatSession>()

    companion object {
        private const val TAG = "ChatSessionList"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatSessionListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        loadSessions()
    }

    override fun onResume() {
        super.onResume()
        loadSessions()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "💬 대화 목록"

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        sessionAdapter = ChatSessionAdapter(
            sessions = sessions,
            onSessionClick = { session ->
                openSession(session)
            },
            onSessionLongClick = { session ->
                showSessionOptions(session)
            },
            onDeleteClick = { session ->
                confirmDeleteSession(session)
            }
        )

        binding.rvSessions.apply {
            layoutManager = LinearLayoutManager(this@ChatSessionListActivity)
            adapter = sessionAdapter
        }
    }

    private fun setupClickListeners() {
        // 새 대화 시작 버튼
        binding.fabNewChat.setOnClickListener {
            createNewSession()
        }
    }

    private fun loadSessions() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE

                Log.d(TAG, "📋 세션 목록 로딩...")

                val response = RetrofitClient.api.getChatSessions()

                if (response.isSuccessful && response.body()?.success == true) {
                    val sessionsData = response.body()?.data?.sessions ?: emptyList()

                    sessions.clear()
                    sessions.addAll(sessionsData)
                    sessionAdapter.notifyDataSetChanged()

                    Log.d(TAG, "✅ ${sessions.size}개 세션 로딩 완료")

                    // 세션이 없으면 빈 상태 표시
                    if (sessions.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                    }

                } else {
                    Log.e(TAG, "❌ 세션 로딩 실패: ${response.code()}")
                    Toast.makeText(this@ChatSessionListActivity, "대화 목록을 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 세션 로딩 중 오류", e)
                Toast.makeText(this@ChatSessionListActivity, "오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

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
                        Log.d(TAG, "✅ 새 세션 생성 완료: ${newSession.id}")
                        openSession(newSession)
                    }

                } else {
                    Log.e(TAG, "❌ 세션 생성 실패: ${response.code()}")
                    Toast.makeText(this@ChatSessionListActivity, "새 대화를 시작하는데 실패했습니다", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 세션 생성 중 오류", e)
                Toast.makeText(this@ChatSessionListActivity, "오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun openSession(session: ChatSession) {
        val intent = Intent(this, ChatbotActivity::class.java).apply {
            putExtra("session_id", session.id)
            putExtra("session_title", session.title)
        }
        startActivity(intent)
    }

    private fun showSessionOptions(session: ChatSession) {
        val options = arrayOf("대화 이어가기", "제목 수정", "삭제")

        AlertDialog.Builder(this)
            .setTitle(session.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openSession(session)
                    1 -> showRenameDialog(session)
                    2 -> confirmDeleteSession(session)
                }
            }
            .show()
    }

    private fun showRenameDialog(session: ChatSession) {
        val editText = android.widget.EditText(this).apply {
            setText(session.title)
            hint = "대화 제목"
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("제목 수정")
            .setView(editText)
            .setPositiveButton("저장") { _, _ ->
                val newTitle = editText.text.toString().trim()
                if (newTitle.isNotEmpty()) {
                    renameSession(session.id, newTitle)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun renameSession(sessionId: String, newTitle: String) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "✏️ 세션 이름 수정: $sessionId -> $newTitle")

                val request = mapOf("title" to newTitle)
                val response = RetrofitClient.api.updateSession(sessionId, request)

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d(TAG, "✅ 세션 이름 수정 완료")
                    loadSessions()
                    Toast.makeText(this@ChatSessionListActivity, "제목이 수정되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "❌ 세션 이름 수정 실패: ${response.code()}")
                    Toast.makeText(this@ChatSessionListActivity, "제목 수정에 실패했습니다", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 세션 이름 수정 중 오류", e)
                Toast.makeText(this@ChatSessionListActivity, "오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDeleteSession(session: ChatSession) {
        AlertDialog.Builder(this)
            .setTitle("대화 삭제")
            .setMessage("'${session.title}' 대화를 삭제하시겠습니까?\n\n모든 메시지가 삭제됩니다.")
            .setPositiveButton("삭제") { _, _ ->
                deleteSession(session.id)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteSession(sessionId: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                Log.d(TAG, "🗑️ 세션 삭제: $sessionId")

                val response = RetrofitClient.api.deleteSession(sessionId)

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d(TAG, "✅ 세션 삭제 완료")
                    loadSessions()
                    Toast.makeText(this@ChatSessionListActivity, "대화가 삭제되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "❌ 세션 삭제 실패: ${response.code()}")
                    Toast.makeText(this@ChatSessionListActivity, "대화 삭제에 실패했습니다", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 세션 삭제 중 오류", e)
                Toast.makeText(this@ChatSessionListActivity, "오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}
