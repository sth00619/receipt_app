package com.example.receiptify.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.receiptify.databinding.ActivityCategoryDetailBinding
import com.example.receiptify.repository.ReceiptRepository
import com.example.receiptify.adapter.ReceiptListAdapter
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope


class CategoryDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryDetailBinding
    private lateinit var receiptAdapter: ReceiptListAdapter
    private val receiptRepository = ReceiptRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val categoryCode = intent.getStringExtra("category_code") ?: return
        val categoryName = intent.getStringExtra("category_name") ?: categoryCode

        // ✅ Setup toolbar with back button
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        // ✅ Handle back button click
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.tvTitle.text = "${categoryName} 상세 내역"

        // 어댑터 생성
        receiptAdapter = ReceiptListAdapter { receipt ->
            val intent = Intent(this, ReceiptDetailActivity::class.java).apply {
                putExtra("receipt_id", receipt.id)
            }
            startActivity(intent)
        }

        // RecyclerView 연결 (오류 FIX)
        binding.rvReceipts.apply {
            adapter = receiptAdapter
            layoutManager = LinearLayoutManager(this@CategoryDetailActivity)
        }

        // 데이터 로드
        loadReceipts(categoryCode)
    }

    private fun loadReceipts(category: String) {
        lifecycleScope.launch {
            val result = receiptRepository.getReceipts(category = category)
            result.onSuccess { list ->
                receiptAdapter.submitList(list)
            }
        }
    }
}

