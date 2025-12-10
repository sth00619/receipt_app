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
import com.example.receiptify.repository.NotificationRepository
import com.example.receiptify.repository.ReceiptRepository
import com.google.firebase.auth.FirebaseAuth
import com.navercorp.nid.NaverIdLoginSDK
import com.example.receiptify.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var receiptRepository: ReceiptRepository
    private lateinit var notificationRepository: NotificationRepository

    // ✅ Store all transactions for dialogs
    private val allTransactions = mutableListOf<Transaction>()

    // ✅ Notification count
    private var unreadNotificationCount = 0

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
        notificationRepository = NotificationRepository()

        // ✅ 인증 토큰 디버깅
        checkAuthTokens()

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
        loadExchangeRates()
        setupClickListeners()
        setupBackPressHandler()
    }

    private fun checkAuthTokens() {
        val sharedPref = getSharedPreferences("receiptify_auth", Context.MODE_PRIVATE)

        // 1. JWT 토큰 확인
        val jwtToken = sharedPref.getString("auth_token", null)
        Log.d(TAG, "💳 JWT 토큰 존재: ${jwtToken != null}")
        Log.d(TAG, "💳 JWT 토큰 값: ${jwtToken?.take(50)?.plus("...") ?: "없음"}")

        // 2. SharedPreferences의 모든 키 출력 (디버깅용)
        val allEntries = sharedPref.all
        Log.d(TAG, "📦 SharedPreferences 전체 키: ${allEntries.keys}")

        // 3. Firebase 사용자 확인
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        Log.d(TAG, "🔥 Firebase 사용자: ${firebaseUser?.email ?: "없음"}")

        // 4. Naver 토큰 확인
        val naverToken = NaverIdLoginSDK.getAccessToken()
        val naverPref = sharedPref.getBoolean("naver_logged_in", false)
        Log.d(TAG, "🟢 Naver 토큰: ${naverToken?.take(30)?.plus("...") ?: "없음"}, Pref: $naverPref")
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
        transactionAdapter = TransactionAdapter { transaction ->
            // Navigate to ReceiptDetailActivity
            val intent = Intent(this, ReceiptDetailActivity::class.java).apply {
                putExtra("receipt_id", transaction.id)
            }
            startActivity(intent)
        }
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
                loadRecentReceipts()

                // 2. 통계 조회
                loadStats()

                // 3. 알림 개수 조회
                loadNotificationCount()

            } catch (e: Exception) {
                Log.e(TAG, "❌ 데이터 로드 중 오류", e)
                Toast.makeText(
                    this@HomeActivity,
                    "오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * 최근 영수증 목록 조회
     */
    private suspend fun loadRecentReceipts() {
        val receiptsResult = receiptRepository.getReceipts(limit = 10)

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

                // ✅ Store all transactions for dialogs
                allTransactions.clear()
                allTransactions.addAll(transactions)

                transactionAdapter.submitList(transactions)
            }
        }.onFailure { error ->
            Log.e(TAG, "❌ 영수증 로드 실패", error)

            if (error.message?.contains("401") == true ||
                error.message?.contains("Token") == true ||
                error.message?.contains("Unauthorized") == true) {
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
    }

    /**
     * 통계 조회 (개선된 버전)
     */
    private suspend fun loadStats() {
        try {
            Log.d(TAG, "📊 통계 조회 시작...")

            val statsResult = receiptRepository.getStats()

            statsResult.fold(
                onSuccess = { stats ->
                    // ✅ 현재 월 총액 사용 (all-time total 대신)
                    val currentMonthTotal = stats.currentMonthTotal.toLong()
                    val todayAmount = stats.todayTotal.toLong()
                    val monthlyChangePercent = stats.monthlyChangePercent

                    Log.d(TAG, "✅ 통계 로드 성공")
                    Log.d(TAG, "  - 현재 월 총액: ${currentMonthTotal}")
                    Log.d(TAG, "  - 오늘 지출: ${todayAmount}")
                    Log.d(TAG, "  - 월별 변화율: ${monthlyChangePercent}%")

                    // ✅ 실제 데이터로 UI 업데이트
                    val isIncrease = monthlyChangePercent >= 0
                    updateMonthlyData(currentMonthTotal, Math.abs(monthlyChangePercent), isIncrease)
                    updateTodaySpending(todayAmount)

                    Log.d(TAG, "✅ UI 업데이트 완료")
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ 통계 로드 실패", error)

                    // 에러 타입에 따라 다른 메시지
                    val errorMessage = when {
                        error.message?.contains("401") == true -> {
                            navigateToLogin()
                            "로그인이 필요합니다"
                        }
                        error.message?.contains("403") == true -> "권한이 없습니다"
                        error.message?.contains("500") == true -> "서버 오류가 발생했습니다"
                        error.message?.contains("timeout") == true -> "네트워크 연결을 확인해주세요"
                        error.message?.contains("Unable to resolve host") == true -> "네트워크 연결을 확인해주세요"
                        else -> "통계를 불러올 수 없습니다"
                    }

                    Toast.makeText(
                        this@HomeActivity,
                        errorMessage,
                        Toast.LENGTH_SHORT
                    ).show()

                    // 기본값으로 UI 업데이트
                    showEmptyStats()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ 통계 로드 중 예상치 못한 오류", e)
            Toast.makeText(
                this@HomeActivity,
                "오류가 발생했습니다",
                Toast.LENGTH_SHORT
            ).show()
            showEmptyStats()
        }
    }

    /**
     * Load notification count
     */
    private suspend fun loadNotificationCount() {
        try {
            Log.d(TAG, "📬 Loading notification count...")

            val result = notificationRepository.getNotifications(unreadOnly = false)

            result.onSuccess { response ->
                unreadNotificationCount = response.unreadCount
                Log.d(TAG, "✅ Unread notifications: $unreadNotificationCount")

                withContext(Dispatchers.Main) {
                    updateNotificationBadge()
                }
            }.onFailure { error ->
                Log.e(TAG, "❌ Failed to load notification count", error)
                // Don't show error toast for notifications, just log it
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception while loading notification count", e)
        }
    }


    /**
     * Update notification badge UI
     */
    private fun updateNotificationBadge() {
        if (unreadNotificationCount > 0) {
            binding.tvNotificationBadge.visibility = View.VISIBLE
            binding.tvNotificationBadge.text = if (unreadNotificationCount > 99) {
                "99+"
            } else {
                unreadNotificationCount.toString()
            }
            Log.d(TAG, "📍 Badge updated: $unreadNotificationCount")
        } else {
            binding.tvNotificationBadge.visibility = View.GONE
            Log.d(TAG, "📍 Badge hidden (no unread notifications)")
        }
    }


    /**
     * 빈 통계 표시
     */
    private fun showEmptyStats() {
        Log.d(TAG, "📊 빈 통계 표시")
        updateMonthlyData(0, 0, true)
        updateTodaySpending(0)
    }

    // 환율 정보 가져오기 및 UI 업데이트
    private fun loadExchangeRates() {
        Log.d(TAG, "🔄 환율 정보 로드 시작")
        binding.progressBarExchange.visibility = View.VISIBLE
        binding.layoutExchangeRates.removeAllViews() // 초기화

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 최신 환율 정보 가져오기
                val targets = "KRW,USD,JPY,CNY"
                Log.d(TAG, "📡 API 요청: getLatestRates")
                val response = RetrofitClient.exchangeRateApi.getLatestRates(symbols = targets)

                if (response.isSuccessful && response.body() != null) {
                    val latestData = response.body()!!
                    val rates = latestData.rates
                    val date = latestData.date // YYYY-MM-DD
                    Log.d(TAG, "✅ 환율 로드 성공: $date, rates=${rates.keys}")

                    val eurToKrw = rates["KRW"] ?: 0.0
                    val eurToUsd = rates["USD"] ?: 1.0
                    val eurToJpy = rates["JPY"] ?: 1.0
                    val eurToCny = rates["CNY"] ?: 1.0

                    // 어제 날짜 계산 (하루 전)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val todayDateObj = dateFormat.parse(date)
                    val calendar = java.util.Calendar.getInstance()
                    calendar.time = todayDateObj
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
                    val yesterdayDate = dateFormat.format(calendar.time)

                    Log.d(TAG, "📡 어제 데이터 요청: $yesterdayDate")
                    val prevResponse = RetrofitClient.exchangeRateApi.getRatesByDate(yesterdayDate, symbols = targets)
                    val prevRates = prevResponse.body()?.rates

                    val prevEurToKrw = prevRates?.get("KRW") ?: eurToKrw
                    val prevEurToUsd = prevRates?.get("USD") ?: eurToUsd
                    val prevEurToJpy = prevRates?.get("JPY") ?: eurToJpy
                    val prevEurToCny = prevRates?.get("CNY") ?: eurToCny

                    // KRW 기준 환율 계산
                    val usdToKrw = eurToKrw / eurToUsd
                    val jpyToKrw = (eurToKrw / eurToJpy) * 100
                    val cnyToKrw = eurToKrw / eurToCny
                    val eurToKrwFinal = eurToKrw

                    val prevUsdToKrw = prevEurToKrw / prevEurToUsd
                    val prevJpyToKrw = (prevEurToKrw / prevEurToJpy) * 100
                    val prevCnyToKrw = prevEurToKrw / prevEurToCny
                    val prevEurToKrwFinal = prevEurToKrw

                    val exchangeList = listOf(
                        Triple("USD", usdToKrw, usdToKrw - prevUsdToKrw),
                        Triple("EUR", eurToKrwFinal, eurToKrwFinal - prevEurToKrwFinal),
                        Triple("JPY (100)", jpyToKrw, jpyToKrw - prevJpyToKrw),
                        Triple("CNY", cnyToKrw, cnyToKrw - prevCnyToKrw)
                    )

                    withContext(Dispatchers.Main) {
                        binding.progressBarExchange.visibility = View.GONE
                        binding.layoutExchangeRates.removeAllViews()

                        val inflater = android.view.LayoutInflater.from(this@HomeActivity)
                        val numberFormat = NumberFormat.getInstance(Locale.getDefault())
                        numberFormat.maximumFractionDigits = 2
                        numberFormat.minimumFractionDigits = 2

                        for ((code, rate, change) in exchangeList) {
                            val itemView = inflater.inflate(R.layout.item_exchange_rate, binding.layoutExchangeRates, false)
                            val tvCode = itemView.findViewById<android.widget.TextView>(R.id.tvCurrencyCode)
                            val tvRate = itemView.findViewById<android.widget.TextView>(R.id.tvExchangeRate)
                            val tvChange = itemView.findViewById<android.widget.TextView>(R.id.tvChangeAmount)

                            tvCode.text = code
                            tvRate.text = numberFormat.format(rate)

                            val changeText = if (change > 0) "▲ ${numberFormat.format(change)}" else if (change < 0) "▼ ${numberFormat.format(kotlin.math.abs(change))}" else "- 0.00"
                            val changeColor = if (change > 0) android.graphics.Color.RED else if (change < 0) android.graphics.Color.BLUE else android.graphics.Color.GRAY

                            tvChange.text = changeText
                            tvChange.setTextColor(changeColor)

                            binding.layoutExchangeRates.addView(itemView)
                        }
                    }

                } else {
                    Log.e(TAG, "❌ 환율 API 응답 실패: ${response.code()} ${response.message()}")
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "❌ Error Body: $errorBody")

                    withContext(Dispatchers.Main) {
                        binding.progressBarExchange.visibility = View.GONE
                        Toast.makeText(this@HomeActivity, "환율 로드 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 환율 로드 중 예외 발생: $e", e)
                withContext(Dispatchers.Main) {
                    binding.progressBarExchange.visibility = View.GONE
                    Toast.makeText(this@HomeActivity, "오류: $e", Toast.LENGTH_LONG).show()
                }
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

        binding.cardExchangeRate.setOnClickListener {
            Toast.makeText(this, "환율 정보를 새로고침합니다.", Toast.LENGTH_SHORT).show()
            loadExchangeRates()
        }

        binding.btnViewDetails.setOnClickListener {
            showTodaySpendingDialog()
        }

        binding.tvViewAll.setOnClickListener {
            showAllTransactionsDialog()
        }

        // ✅ 알림 아이콘 클릭
        binding.layoutNotificationIcon.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)
        }

        binding.btnChatbot.setOnClickListener {
            val intent = Intent(this@HomeActivity, ChatbotActivity::class.java)
            startActivity(intent)
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_categories -> {
                    startActivity(Intent(this, CategoriesActivity::class.java))
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

    /**
     * Show today's spending in a modal dialog
     */
    private fun showTodaySpendingDialog() {
        val calendar = java.util.Calendar.getInstance()
        val today = calendar.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val tomorrow = calendar.apply {
            add(java.util.Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis

        // Filter today's transactions
        val todayTransactions = allTransactions.filter { transaction ->
            transaction.date >= today && transaction.date < tomorrow
        }

        Log.d(TAG, "📊 Today's transactions: ${todayTransactions.size} out of ${allTransactions.size}")

        showTransactionDialog(
            getString(R.string.dialog_today_spending_title),
            todayTransactions,
            getString(R.string.dialog_empty_today)
        )
    }

    /**
     * Show all recent transactions in a modal dialog
     */
    private fun showAllTransactionsDialog() {
        // Take first 10 transactions
        val recentTransactions = allTransactions.take(10)

        Log.d(TAG, "📊 Showing ${recentTransactions.size} recent transactions")

        showTransactionDialog(
            getString(R.string.dialog_all_transactions_title),
            recentTransactions,
            getString(R.string.dialog_empty_transactions)
        )
    }

    /**
     * Show transaction list in a modal dialog
     */
    private fun showTransactionDialog(
        title: String,
        transactions: List<Transaction>,
        emptyMessage: String
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_transaction_list, null)
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvDialogTransactions)
        val emptyStateView = dialogView.findViewById<android.widget.TextView>(R.id.tvEmptyState)

        // Setup RecyclerView with click listener
        val dialogAdapter = TransactionAdapter { transaction ->
            // Navigate to ReceiptDetailActivity
            val intent = Intent(this, ReceiptDetailActivity::class.java).apply {
                putExtra("receipt_id", transaction.id)
            }
            startActivity(intent)
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = dialogAdapter
        }

        // Show empty state or transactions
        if (transactions.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateView.visibility = View.VISIBLE
            emptyStateView.text = emptyMessage
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateView.visibility = View.GONE
            dialogAdapter.submitList(transactions)
        }

        // Create and show dialog
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setNegativeButton(R.string.dialog_close) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}