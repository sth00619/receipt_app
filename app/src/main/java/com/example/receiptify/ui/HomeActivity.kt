package com.example.receiptify.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.receiptify.R
import com.example.receiptify.adapter.TransactionAdapter
import com.example.receiptify.databinding.ActivityHomeBinding
import com.example.receiptify.model.Transaction
import com.example.receiptify.repository.AuthRepository
import com.example.receiptify.repository.ReceiptRepository
import com.navercorp.nid.NaverIdLoginSDK
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var receiptRepository: ReceiptRepository

    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    companion object {
        private const val TAG = "HomeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "HomeActivity onCreate called")

        authRepository = AuthRepository(this)
        receiptRepository = ReceiptRepository()

        // 로그인 확인
        if (!isUserLoggedIn()) {
            Log.d(TAG, "User not logged in, navigating to LoginActivity")
            navigateToLogin()
            return
        }

        Log.d(TAG, "User is logged in, setting up HomeActivity")
        setupUI()
        setupRecyclerView()
        loadDataFromMongoDB()
        setupClickListeners()
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "Back pressed - finishing app")
                finishAffinity()
            }
        })
    }

    private fun isUserLoggedIn(): Boolean {
        // JWT 토큰 확인
        val hasToken = authRepository.isLoggedIn()

        // Naver 로그인 확인
        val naverToken = NaverIdLoginSDK.getAccessToken()
        val naverPref = getSharedPreferences("receiptify_auth", Context.MODE_PRIVATE)
            .getBoolean("naver_logged_in", false)

        Log.d(TAG, "Login check - JWT: $hasToken, Naver Token: ${naverToken != null}, Naver Pref: $naverPref")

        return hasToken || (naverToken != null && naverPref)
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // 사용자 이름 표시
        val userEmail = authRepository.getUserEmail()
        if (userEmail != null) {
            val userName = userEmail.split("@")[0]
            // toolbar에 사용자 이름 표시 가능
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()
        binding.rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = transactionAdapter
        }
    }

    private fun loadDataFromMongoDB() {
        // JWT 토큰이 있으면 자동으로 userId 추출됨
        Log.d(TAG, "✅ Loading data for authenticated user")

        lifecycleScope.launch {
            try {
                // 1. 영수증 목록 조회
                val receiptsResult = receiptRepository.getReceipts(limit = 5)

                receiptsResult.onSuccess { receipts ->
                    Log.d(TAG, "✅ ${receipts.size}개 영수증 로드 완료")

                    if (receipts.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()

                        val transactions = receipts.map { receipt ->
                            Transaction(
                                id = receipt.id,
                                storeName = receipt.storeName,
                                category = receipt.category,
                                amount = receipt.totalAmount.toLong(),
                                date = parseDate(receipt.transactionDate),
                                userId = authRepository.getUserId() ?: ""
                            )
                        }

                        transactionAdapter.submitList(transactions)
                    }
                }.onFailure { error ->
                    Log.e(TAG, "❌ 영수증 로드 실패", error)

                    if (error.message?.contains("401") == true ||
                        error.message?.contains("Token") == true) {
                        Toast.makeText(
                            this@HomeActivity,
                            "세션이 만료되었습니다. 다시 로그인해주세요.",
                            Toast.LENGTH_LONG
                        ).show()
                        navigateToLogin()
                    } else {
                        Toast.makeText(
                            this@HomeActivity,
                            "데이터를 불러오는데 실패했습니다: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        showEmptyState()
                    }
                }

                // 2. 통계 조회
                val statsResult = receiptRepository.getStats()

                statsResult.onSuccess { stats ->
                    Log.d(TAG, "✅ 통계 로드 완료: ${stats.total.totalAmount}")

                    val totalAmount = stats.total.totalAmount.toLong()
                    updateMonthlyData(totalAmount, 12, true)

                    val todayAmount = (totalAmount / 30).coerceAtLeast(0)
                    updateTodaySpending(todayAmount)

                }.onFailure { error ->
                    Log.e(TAG, "❌ 통계 로드 실패", error)
                    updateMonthlyData(0, 0, true)
                    updateTodaySpending(0)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 데이터 로드 중 오류", e)
                Toast.makeText(
                    this@HomeActivity,
                    "오류가 발생했습니다",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun parseDate(dateString: String): Long {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, "날짜 파싱 실패: $dateString", e)
            System.currentTimeMillis()
        }
    }

    private fun setupClickListeners() {
        binding.cardScanReceipt.setOnClickListener {
            Log.d(TAG, "Scan receipt button clicked")
            startActivity(Intent(this, ReceiptScanActivity::class.java))
        }

        binding.cardViewReceipts.setOnClickListener {
            Toast.makeText(this, R.string.view_receipts_coming_soon, Toast.LENGTH_SHORT).show()
        }

        binding.btnViewDetails.setOnClickListener {
            Toast.makeText(this, R.string.statistics_coming_soon, Toast.LENGTH_SHORT).show()
        }

        binding.tvViewAll.setOnClickListener {
            Toast.makeText(this, R.string.all_transactions_coming_soon, Toast.LENGTH_SHORT).show()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_categories -> {
                    Toast.makeText(this, R.string.categories_coming_soon, Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.nav_receipts -> {
                    startActivity(Intent(this, ReceiptScanActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun updateMonthlyData(total: Long, percentageChange: Int, isIncrease: Boolean) {
        binding.tvMonthlyTotal.text = "₩ ${numberFormat.format(total)}"
        binding.tvTrendPercentage.text = "$percentageChange%"

        if (isIncrease) {
            binding.ivTrendIcon.setImageResource(R.drawable.ic_arrow_up)
        } else {
            binding.ivTrendIcon.setImageResource(R.drawable.ic_arrow_down)
        }
    }

    private fun updateTodaySpending(amount: Long) {
        binding.tvTodaySpending.text = "₩ ${numberFormat.format(amount)}"
    }

    private fun showEmptyState() {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.rvRecentTransactions.visibility = View.GONE
    }

    private fun hideEmptyState() {
        binding.layoutEmptyState.visibility = View.GONE
        binding.rvRecentTransactions.visibility = View.VISIBLE
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}