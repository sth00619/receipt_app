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
import com.example.receiptify.api.models.ChatSessionResponse
import com.example.receiptify.databinding.ActivityChatListBinding
import kotlinx.coroutines.launch

class ChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatListBinding
    private lateinit var sessionAdapter: ChatSessionAdapter
    private val sessions = mutableListOf<ChatSessionResponse>()

    companion object {
        private const val TAG = "ChatListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadChatSessions()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "대화 목록"

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        sessionAdapter = ChatSessionAdapter(
            onSessionClick = { session ->
                val intent = Intent(this, ChatbotActivity::class.java)
                intent.putExtra("SESSION_ID", session.id)
                startActivity(intent)
            },
            onDeleteClick = { session ->
                showDeleteConfirmDialog(session)
            }
        )

        binding.rvChatSessions.apply {
            layoutManager = LinearLayoutManager(this@ChatListActivity)
            adapter = sessionAdapter
        }
    }

    private fun setupListeners() {
        binding.fabNewChat.setOnClickListener {
            val intent = Intent(this, ChatbotActivity::class.java)
            intent.putExtra("SESSION_ID", "new")
            startActivity(intent)
        }
    }

    private fun loadChatSessions() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                // 이전 데이터 클리어 방지 (깜빡임 최소화)
                // if (sessions.isEmpty()) ...

                val response = RetrofitClient.api.fetchChatSessions()

                if (response.isSuccessful && response.body()?.success == true) {
                    val sessionList = response.body()?.data ?: emptyList()
                    sessions.clear()
                    sessions.addAll(sessionList)
                    sessionAdapter.submitList(sessions.toList())

                    binding.tvEmptyState.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Log.e(TAG, "❌ 세션 목록 로드 실패: ${response.code()}")
                    Toast.makeText(this@ChatListActivity, "목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 세션 목록 로드 오류", e)
                Toast.makeText(this@ChatListActivity, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showDeleteConfirmDialog(session: ChatSessionResponse) {
        AlertDialog.Builder(this)
            .setTitle("대화 삭제")
            .setMessage("'${session.title}' 대화를 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                deleteSession(session)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteSession(session: ChatSessionResponse) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.removeChatSession(session.id)

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@ChatListActivity, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    loadChatSessions() // 목록 갱신
                } else {
                    Toast.makeText(this@ChatListActivity, "삭제 실패", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 삭제 중 오류", e)
                Toast.makeText(this@ChatListActivity, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}