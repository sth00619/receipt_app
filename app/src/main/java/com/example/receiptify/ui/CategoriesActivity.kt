package com.example.receiptify.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.receiptify.R
import com.example.receiptify.adapter.CategoryAdapter
import com.example.receiptify.databinding.ActivityCategoriesBinding
import com.example.receiptify.model.CategorySummary
import com.example.receiptify.repository.ReceiptRepository
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class CategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesBinding
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var receiptRepository: ReceiptRepository

    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    // 현재 선택된 기간
    private var currentPeriodType: PeriodType = PeriodType.ALL
    private var customStartDate: Date? = null
    private var customEndDate: Date? = null

    enum class PeriodType {
        ALL,        // 전체
        THIS_WEEK,  // 이번 주
        THIS_MONTH, // 이번 달
        CUSTOM      // 기타 (사용자 지정)
    }

    companion object {
        private const val TAG = "CategoriesActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        receiptRepository = ReceiptRepository()

        setupUI()
        setupRecyclerView()
        setupTabLayout()
        loadCategories(PeriodType.ALL)
        setupClickListeners()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter { category ->
            val intent = Intent(this, CategoryDetailActivity::class.java).apply {
                putExtra("category_code", category.code)     // food
                putExtra("category_name", category.name)     // Food
            }
            startActivity(intent)
        }


        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@CategoriesActivity)
            adapter = categoryAdapter
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        Log.d(TAG, "📅 탭 선택: 전체")
                        loadCategories(PeriodType.ALL)
                    }
                    1 -> {
                        Log.d(TAG, "📅 탭 선택: 이번 주")
                        loadCategories(PeriodType.THIS_WEEK)
                    }
                    2 -> {
                        Log.d(TAG, "📅 탭 선택: 이번 달")
                        loadCategories(PeriodType.THIS_MONTH)
                    }
                    3 -> {
                        Log.d(TAG, "📅 탭 선택: 기타 (날짜 선택)")
                        showDateRangePicker()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    /**
     * 카테고리 데이터 로드 (기간별)
     */
    private fun loadCategories(periodType: PeriodType) {
        currentPeriodType = periodType

        lifecycleScope.launch {
            try {
                // 날짜 범위 계산
                val (year, month) = calculateYearMonth(periodType)

                Log.d(TAG, "📅 기간: ${periodType.name}, year: $year, month: $month")

                // 기존 getStats() 메서드 사용
                val statsResult = receiptRepository.getStats(month, year)

                statsResult.onSuccess { stats ->
                    Log.d(TAG, "✅ 통계 로드 완료 (${periodType.name})")

                    // ✅ 통계 데이터 상세 로깅
                    Log.d(TAG, "📊 전체 통계: 총액=${stats.total.totalAmount}, 개수=${stats.total.count}")
                    Log.d(TAG, "📊 카테고리 개수: ${stats.byCategory.size}")

                    if (stats.byCategory.isEmpty()) {
                        Log.e(TAG, "❌ 카테고리 데이터가 비어있습니다!")
                        Log.e(TAG, "   총 거래는 ${stats.total.count}건인데 카테고리별 그룹화가 안 됨")
                    }

                    stats.byCategory.forEachIndexed { index, categoryStat ->
                        Log.d(TAG, "📊 카테고리[$index]: code='${categoryStat.category}', 금액=${categoryStat.totalAmount}, 개수=${categoryStat.count}")
                    }

                    // 총 지출 업데이트
                    updateTotalSpending(
                        stats.total.totalAmount.toLong(),
                        stats.total.count
                    )

                    // 카테고리별 데이터 변환
                    if (stats.byCategory.isEmpty()) {
                        Log.w(TAG, "⚠️ byCategory가 비어있어서 리스트를 표시할 수 없습니다")
                        showEmptyState(periodType)
                        categoryAdapter.submitList(emptyList())
                        return@onSuccess
                    }

                    val categories = stats.byCategory.map { categoryStat ->
                        val categoryInfo = getCategoryInfo(categoryStat.category)

                        Log.d(TAG, "🔄 변환 중: code='${categoryStat.category}' -> name='${categoryInfo.first}'")

                        CategorySummary(
                            code = categoryStat.category,
                            name = categoryInfo.first,
                            icon = categoryInfo.second,
                            color = categoryInfo.third,
                            amount = categoryStat.totalAmount.toLong(),
                            count = categoryStat.count,
                            percentage = if (stats.total.totalAmount > 0) {
                                (categoryStat.totalAmount / stats.total.totalAmount * 100).toFloat()
                            } else 0f
                        )
                    }.sortedByDescending { it.amount }

                    Log.d(TAG, "📊 변환된 카테고리 개수: ${categories.size}")
                    categories.forEachIndexed { index, cat ->
                        Log.d(TAG, "📊 변환[$index]: name='${cat.name}', 금액=₩${numberFormat.format(cat.amount)}, 비율=${String.format("%.1f", cat.percentage)}%")
                    }

                    Log.d(TAG, "✅ Adapter에 ${categories.size}개 카테고리 제출")
                    categoryAdapter.submitList(categories)

                    if (categories.isEmpty()) {
                        showEmptyState(periodType)
                    }

                }.onFailure { error ->
                    Log.e(TAG, "❌ 통계 로드 실패 (${periodType.name})", error)
                    Toast.makeText(
                        this@CategoriesActivity,
                        "데이터를 불러오는데 실패했습니다: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    updateTotalSpending(0, 0)
                    categoryAdapter.submitList(emptyList())
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 데이터 로드 중 오류 (${periodType.name})", e)
                Toast.makeText(
                    this@CategoriesActivity,
                    "오류가 발생했습니다",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * 기간별 year/month 계산
     */
    private fun calculateYearMonth(periodType: PeriodType): Pair<Int?, Int?> {
        val calendar = Calendar.getInstance()

        return when (periodType) {
            PeriodType.ALL -> {
                // 전체: 필터 없음
                Pair(null, null)
            }
            PeriodType.THIS_WEEK, PeriodType.THIS_MONTH -> {
                // 이번 주, 이번 달: 현재 월 데이터 가져와서 클라이언트에서 필터링
                Pair(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
            }
            PeriodType.CUSTOM -> {
                // 사용자 지정: 선택한 날짜의 월
                val customCal = Calendar.getInstance()
                if (customStartDate != null) {
                    customCal.time = customStartDate!!
                    Pair(customCal.get(Calendar.YEAR), customCal.get(Calendar.MONTH) + 1)
                } else {
                    Pair(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
                }
            }
        }
    }

    /**
     * 날짜 범위 선택 다이얼로그
     */
    private fun showDateRangePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth, 0, 0, 0)
                customStartDate = calendar.time

                showEndDatePicker()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("시작 날짜 선택")
            show()
        }
    }

    private fun showEndDatePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth, 23, 59, 59)
                customEndDate = calendar.time

                if (customStartDate != null && customEndDate != null) {
                    if (customStartDate!! > customEndDate!!) {
                        Toast.makeText(
                            this,
                            "시작 날짜는 종료 날짜보다 이전이어야 합니다",
                            Toast.LENGTH_SHORT
                        ).show()
                        customStartDate = null
                        customEndDate = null
                        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(2))
                    } else {
                        loadCategories(PeriodType.CUSTOM)
                    }
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("종료 날짜 선택")
            customStartDate?.let {
                datePicker.minDate = it.time
            }
            show()
        }
    }

    private fun showEmptyState(periodType: PeriodType) {
        val message = when (periodType) {
            PeriodType.ALL -> "아직 등록된 영수증이 없습니다"
            PeriodType.THIS_WEEK -> "이번 주에 등록된 영수증이 없습니다"
            PeriodType.THIS_MONTH -> "이번 달에 등록된 영수증이 없습니다"
            PeriodType.CUSTOM -> "선택한 기간에 등록된 영수증이 없습니다"
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateTotalSpending(totalAmount: Long, transactionCount: Int) {
        binding.tvTotalSpending.text = "₩ ${numberFormat.format(totalAmount)}"
        binding.tvTransactionCount.text = "${transactionCount}건"

        val average = if (transactionCount > 0) {
            totalAmount / transactionCount
        } else 0L

        binding.tvAverageSpending.text = "₩ ${numberFormat.format(average)}"
    }

    private fun getCategoryInfo(code: String?): Triple<String, Int, Int> {
        Log.d(TAG, "🔍 getCategoryInfo: code='$code'")

        // ✅ null이거나 빈 문자열인 경우 "others"로 처리
        val safeCode = code?.lowercase()?.takeIf { it.isNotBlank() } ?: "others"

        return when (safeCode) {
            "food" -> Triple(
                getString(R.string.category_food),
                R.drawable.ic_receipt,
                R.color.category_food
            )
            "transport" -> Triple(
                getString(R.string.category_transport),
                R.drawable.ic_camera,
                R.color.category_transport
            )
            "shopping" -> Triple(
                getString(R.string.category_shopping),
                R.drawable.ic_list,
                R.color.category_shopping
            )
            "healthcare" -> Triple(
                "건강/의료",
                R.drawable.ic_receipt,
                R.color.category_healthcare
            )
            "entertainment" -> Triple(
                "문화/여가",
                R.drawable.ic_camera,
                R.color.category_entertainment
            )
            "utilities" -> Triple(
                "공과금",
                R.drawable.ic_list,
                R.color.category_utilities
            )
            "others" -> Triple(
                getString(R.string.category_others),
                R.drawable.ic_back,
                R.color.category_others
            )
            else -> {
                Log.w(TAG, "⚠️ 알 수 없는 카테고리 코드: '$code', 기타로 처리")
                Triple(
                    getString(R.string.category_others),
                    R.drawable.ic_back,
                    R.color.category_others
                )
            }
        }
    }

    private fun setupClickListeners() {
        binding.bottomNavigation.selectedItemId = R.id.nav_categories

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // ✅ 명시적으로 Context 지정
                    val intent = Intent(this@CategoriesActivity, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_categories -> true
                R.id.nav_receipts -> {
                    // ✅ 명시적으로 Context 지정
                    val intent = Intent(this@CategoriesActivity, ReceiptScanActivity::class.java)
                    startActivity(intent)
                    false
                }
                R.id.nav_profile -> {
                    // ✅ 명시적으로 Context 지정
                    val intent = Intent(this@CategoriesActivity, ProfileActivity::class.java)
                    startActivity(intent)
                    false
                }
                else -> false
            }
        }
    }
}