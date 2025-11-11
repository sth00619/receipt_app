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
import com.example.receiptify.auth.FirebaseAuthManager
import com.example.receiptify.databinding.ActivityHomeBinding
import com.example.receiptify.model.Transaction
import com.example.receiptify.repository.ReceiptRepository
import com.example.receiptify.repository.UserRepository
import com.navercorp.nid.NaverIdLoginSDK
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var authManager: FirebaseAuthManager
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var receiptRepository: ReceiptRepository
    private lateinit var userRepository: UserRepository

    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    companion object {
        private const val TAG = "HomeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "HomeActivity onCreate called")

        authManager = FirebaseAuthManager.getInstance()
        receiptRepository = ReceiptRepository()
        userRepository = UserRepository()

        // 로그인 확인 - Firebase와 Naver 모두 체크
        if (!isUserLoggedIn()) {
            Log.d(TAG, "User not logged in, navigating to LoginActivity")
            navigateToLogin()
            return
        }

        Log.d(TAG, "User is logged in, setting up HomeActivity")
        setupUI()
        setupRecyclerView()
        loadDataFromMongoDB()  // ✨ MongoDB에서 데이터 로드
        setupClickListeners()
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        // 최신 방식의 뒤로가기 처리
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 뒤로가기 버튼을 누르면 앱 종료
                Log.d(TAG, "Back pressed - finishing app")
                finishAffinity()
            }
        })
    }

    private fun isUserLoggedIn(): Boolean {
        val firebaseUser = authManager.currentUser
        val naverToken = NaverIdLoginSDK.getAccessToken()
        val naverPref = getSharedPreferences("receiptify_auth", Context.MODE_PRIVATE)
            .getBoolean("naver_logged_in", false)

        Log.d(TAG, "Login check - Firebase: ${firebaseUser != null}, Naver Token: ${naverToken != null}, Naver Pref: $naverPref")

        // Firebase 사용자가 있거나 (Naver 토큰과 Pref 플래그 둘 다 있으면) 로그인된 것으로 간주
        return firebaseUser != null || (naverToken != null && naverPref)
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()
        binding.rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = transactionAdapter
        }
    }

    // ✨ MongoDB에서 실제 로그인한 사용자 데이터 로드
    private fun loadDataFromMongoDB() {
        // ✅ 실제 Firebase 사용자 UID 사용
        val userId = authManager.currentUser?.uid

        if (userId == null) {
            Log.e(TAG, "❌ 사용자 ID가 없습니다 (Naver 로그인 또는 미로그인)")
            // Naver 로그인이거나 로그인 안된 상태
            showEmptyState()
            updateMonthlyData(0, 0, true)
            updateTodaySpending(0)
            return
        }

        Log.d(TAG, "✅ 로그인된 사용자 ID: $userId")

        lifecycleScope.launch {
            try {
                // 1. 영수증 목록 조회 (더 이상 userId 파라미터 불필요 - 토큰에서 자동 추출)
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
                                userId = userId
                            )
                        }

                        transactionAdapter.submitList(transactions)
                    }
                }.onFailure { error ->
                    Log.e(TAG, "❌ 영수증 로드 실패", error)

                    if (error.message?.contains("401") == true ||
                        error.message?.contains("Token") == true ||
                        error.message?.contains("Unauthorized") == true) {
                        // 토큰 만료 - 다시 로그인
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

    // 날짜 파싱 헬퍼 함수
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
        // 영수증 스캔 - ReceiptScanActivity로 이동
        binding.cardScanReceipt.setOnClickListener {
            Log.d(TAG, "Scan receipt button clicked")
            startActivity(Intent(this, ReceiptScanActivity::class.java))
        }

        // 영수증 내역 보기
        binding.cardViewReceipts.setOnClickListener {
            Toast.makeText(this, R.string.view_receipts_coming_soon, Toast.LENGTH_SHORT).show()
        }

        // 자세히 보기
        binding.btnViewDetails.setOnClickListener {
            Toast.makeText(this, R.string.statistics_coming_soon, Toast.LENGTH_SHORT).show()
        }

        // 전체보기
        binding.tvViewAll.setOnClickListener {
            Toast.makeText(this, R.string.all_transactions_coming_soon, Toast.LENGTH_SHORT).show()
        }

        // Bottom Navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // 현재 화면
                    true
                }
                R.id.nav_categories -> {
                    Toast.makeText(this, R.string.categories_coming_soon, Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.nav_receipts -> {
                    // 영수증 화면으로 이동
                    startActivity(Intent(this, ReceiptScanActivity::class.java))
                    false
                }
                R.id.nav_profile -> {
                    // 프로필 화면으로 이동
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