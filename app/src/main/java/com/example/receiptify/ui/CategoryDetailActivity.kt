package com.example.receiptify.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.receiptify.databinding.ActivityCategoryDetailBinding
import com.example.receiptify.repository.ReceiptRepository
import com.example.receiptify.adapter.ReceiptListAdapter
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import java.text.SimpleDateFormat
import java.util.*


class CategoryDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryDetailBinding
    private lateinit var receiptAdapter: ReceiptListAdapter
    private val receiptRepository = ReceiptRepository()

    companion object {
        private const val TAG = "CategoryDetailActivity"
    }

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
            // ✅ Intent에서 날짜 범위 정보 받기
            val periodTypeStr = intent.getStringExtra("period_type")
            val customStartMillis = intent.getLongExtra("custom_start_date", -1L)
            val customEndMillis = intent.getLongExtra("custom_end_date", -1L)

            Log.d(TAG, "📅 Period type: $periodTypeStr")
            Log.d(TAG, "📅 Custom start: $customStartMillis, Custom end: $customEndMillis")

            // ✅ 날짜 범위 계산
            val (startDate, endDate) = calculateDateRange(
                periodTypeStr,
                if (customStartMillis != -1L) Date(customStartMillis) else null,
                if (customEndMillis != -1L) Date(customEndMillis) else null
            )

            Log.d(TAG, "📅 Calculated dates - Start: $startDate, End: $endDate")

            // ✅ 날짜 파라미터와 함께 API 호출
            val result = receiptRepository.getReceipts(
                category = category,
                startDate = startDate,
                endDate = endDate
            )

            result.onSuccess { list ->
                Log.d(TAG, "✅ Loaded ${list.size} receipts for category $category")
                // ✅ 날짜 기준 내림차순 정렬 (최신순)
                val sortedList = list.sortedByDescending { it.transactionDate }
                receiptAdapter.submitList(sortedList)
            }.onFailure { error ->
                Log.e(TAG, "❌ Failed to load receipts", error)
            }
        }
    }

    /**
     * 기간 타입에 따라 실제 시작/종료 날짜를 계산
     */
    private fun calculateDateRange(
        periodTypeStr: String?,
        customStart: Date?,
        customEnd: Date?
    ): Pair<String?, String?> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        return when (periodTypeStr) {
            "ALL" -> {
                // 전체: 필터 없음
                Pair(null, null)
            }
            "THIS_WEEK" -> {
                // 이번 주: 월요일 00:00 ~ 일요일 23:59
                val calendar = Calendar.getInstance()
                calendar.firstDayOfWeek = Calendar.MONDAY

                // 현재 요일 가져오기
                val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                // 월요일까지 거슬러 올라가기
                val daysFromMonday = when (currentDayOfWeek) {
                    Calendar.SUNDAY -> 6  // 일요일은 지난주가 아니라 이번주로 계산
                    else -> currentDayOfWeek - Calendar.MONDAY
                }

                calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val weekStart = calendar.time

                calendar.add(Calendar.DAY_OF_MONTH, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val weekEnd = calendar.time

                Pair(dateFormat.format(weekStart), dateFormat.format(weekEnd))
            }
            "THIS_MONTH" -> {
                // 이번 달: 1일 00:00 ~ 말일 23:59
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.time

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val monthEnd = calendar.time

                Pair(dateFormat.format(monthStart), dateFormat.format(monthEnd))
            }
            "CUSTOM" -> {
                // 사용자 지정: customStart ~ customEnd
                if (customStart != null && customEnd != null) {
                    Pair(dateFormat.format(customStart), dateFormat.format(customEnd))
                } else {
                    Pair(null, null)
                }
            }
            else -> {
                // 알 수 없는 타입: 필터 없음
                Pair(null, null)
            }
        }
    }
}



