package com.example.receiptify.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.receiptify.R
import com.example.receiptify.api.RetrofitClient
import com.example.receiptify.repository.NotificationRepository
import com.example.receiptify.databinding.ActivityAnalyticsViewBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class AnalyticsViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsViewBinding
    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    private lateinit var notificationRepository: NotificationRepository

    // Chart Colors (Banksalad style)
    private val chartColors = listOf(
        Color.parseColor("#00D4AA"),  // Mint
        Color.parseColor("#4ECDC4"),  // Teal
        Color.parseColor("#FFD93D"),  // Yellow
        Color.parseColor("#FF6B6B"),  // Coral
        Color.parseColor("#A29BFE"),  // Purple
        Color.parseColor("#74B9FF"),  // Blue
        Color.parseColor("#FD79A8")   // Pink
    )

    // Category names in English
    private val categoryNames = mapOf(
        "food" to "Food & Dining",
        "transport" to "Transportation",
        "shopping" to "Shopping",
        "healthcare" to "Healthcare",
        "entertainment" to "Entertainment",
        "utilities" to "Utilities",
        "others" to "Others"
    )

    companion object {
        private const val TAG = "AnalyticsView"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationRepository = NotificationRepository()
        binding = ActivityAnalyticsViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCharts()
        loadData()

        setupClickListeners()
        setupBottomNavigation()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Notification icon click
        binding.layoutNotificationIcon.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)
        }

        // Chatbot icon click
        binding.btnChatbot.setOnClickListener {
            val intent = Intent(this, ChatbotActivity::class.java)
            startActivity(intent)
        }

        // Load notification count
        loadNotificationCount()
    }
    private fun loadNotificationCount() {
        lifecycleScope.launch {
            try {
                val result = notificationRepository.getNotifications(unreadOnly = false)
                result.onSuccess { response ->
                    val unreadCount = response.unreadCount
                    withContext(Dispatchers.Main) {
                        if (unreadCount > 0) {
                            binding.tvNotificationBadge.visibility = android.view.View.VISIBLE
                            binding.tvNotificationBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
                        } else {
                            binding.tvNotificationBadge.visibility = android.view.View.GONE
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(AnalyticsViewActivity.Companion.TAG, "Failed to load notification count", e)
            }
        }
    }

    private fun setupCharts() {
        // Pie Chart Setup
        binding.pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1400, Easing.EaseInOutQuad)
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                orientation = Legend.LegendOrientation.VERTICAL
                setDrawInside(false)
                xEntrySpace = 7f
                yEntrySpace = 0f
                yOffset = 0f
            }
            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
        }

        // Bar Chart Setup
        binding.barChart.apply {
            description.isEnabled = false
            setMaxVisibleValueCount(60)
            setPinchZoom(false)
            setDrawBarShadow(false)
            setDrawGridBackground(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.parseColor("#6B7280")
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E5E7EB")
                textColor = Color.parseColor("#6B7280")
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
            animateY(1500)
        }

        // Line Chart Setup (Spending Trend)
        binding.lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#6B7280")
                granularity = 1f
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E5E7EB")
                textColor = Color.parseColor("#6B7280")
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
            legend.apply {
                form = Legend.LegendForm.LINE
                textColor = Color.parseColor("#6B7280")
            }
            animateX(1500)
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                Log.d(TAG, "📊 Loading analytics data...")

                val response = RetrofitClient.api.getStatsByMonth()

                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data

                    Log.d(TAG, "✅ Data loaded successfully: $data")

                    @Suppress("UNCHECKED_CAST")
                    val categoryStats = data?.get("byCategory") as? List<Map<String, Any>> ?: emptyList()

                    @Suppress("UNCHECKED_CAST")
                    val totalData = data?.get("total") as? Map<String, Any>
                    val totalAmount = (totalData?.get("totalAmount") as? Number)?.toLong() ?: 0L

                    @Suppress("UNCHECKED_CAST")
                    val dailyStats = data?.get("dailyStats") as? List<Map<String, Any>> ?: emptyList()

                    runOnUiThread {
                        updateSummary(totalAmount, categoryStats.size)
                        updatePieChart(categoryStats)
                        updateBarChart(categoryStats)
                        updateLineChart(dailyStats)
                    }

                } else {
                    Log.e(TAG, "❌ Failed to load data: ${response.code()}")
                    runOnUiThread {
                        Toast.makeText(this@AnalyticsViewActivity, "Failed to load data", Toast.LENGTH_SHORT).show()
                        showSampleData()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading data", e)
                runOnUiThread {
                    Toast.makeText(this@AnalyticsViewActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    showSampleData()
                }
            } finally {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun showSampleData() {
        val sampleCategories = listOf(
            mapOf("category" to "food", "totalAmount" to 250000),
            mapOf("category" to "transport", "totalAmount" to 80000),
            mapOf("category" to "shopping", "totalAmount" to 150000),
            mapOf("category" to "entertainment", "totalAmount" to 60000),
            mapOf("category" to "utilities", "totalAmount" to 100000)
        )

        updateSummary(640000, 5)
        updatePieChart(sampleCategories)
        updateBarChart(sampleCategories)

        val sampleDaily = (1..30).map { day ->
            mapOf("_id" to day, "amount" to (10000..50000).random())
        }
        updateLineChart(sampleDaily)
    }

    private fun updateSummary(totalAmount: Long, categoryCount: Int) {
        binding.tvTotalSpending.text = "₩${numberFormat.format(totalAmount)}"
        binding.tvCategoryCount.text = "$categoryCount Categories"

        if (categoryCount > 0) {
            val average = totalAmount / categoryCount
            binding.tvAverageSpending.text = "₩${numberFormat.format(average)}"
        }
    }

    private fun updatePieChart(categoryStats: List<Map<String, Any>>) {
        val entries = ArrayList<PieEntry>()

        categoryStats.forEach { stat ->
            val category = stat["category"] as? String ?: "others"
            val amount = (stat["totalAmount"] as? Number)?.toFloat() ?: 0f
            val displayName = categoryNames[category] ?: category.replaceFirstChar { it.uppercase() }

            if (amount > 0) {
                entries.add(PieEntry(amount, displayName))
            }
        }

        if (entries.isEmpty()) {
            binding.pieChart.visibility = View.GONE
            return
        }

        binding.pieChart.visibility = View.VISIBLE

        val dataSet = PieDataSet(entries, "").apply {
            setDrawIcons(false)
            sliceSpace = 3f
            iconsOffset = MPPointF(0f, 40f)
            selectionShift = 5f
            colors = chartColors.take(entries.size)
        }

        val data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(binding.pieChart))
            setValueTextSize(11f)
            setValueTextColor(Color.WHITE)
        }

        binding.pieChart.apply {
            this.data = data
            highlightValues(null)
            centerText = "Spending\nBreakdown"
            setCenterTextSize(14f)
            setCenterTextColor(Color.parseColor("#1A1A2E"))
            invalidate()
        }
    }

    private fun updateBarChart(categoryStats: List<Map<String, Any>>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        categoryStats.forEachIndexed { index, stat ->
            val category = stat["category"] as? String ?: "others"
            val amount = (stat["totalAmount"] as? Number)?.toFloat() ?: 0f
            val displayName = categoryNames[category] ?: category.replaceFirstChar { it.uppercase() }

            entries.add(BarEntry(index.toFloat(), amount))
            labels.add(displayName.take(8)) // Truncate long names
        }

        if (entries.isEmpty()) {
            binding.barChart.visibility = View.GONE
            return
        }

        binding.barChart.visibility = View.VISIBLE

        val dataSet = BarDataSet(entries, "Spending by Category").apply {
            setDrawIcons(false)
            colors = chartColors.take(entries.size)
            valueTextColor = Color.parseColor("#1A1A2E")
            valueTextSize = 10f
        }

        binding.barChart.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = labels.size
            data = BarData(dataSet).apply {
                setValueTextSize(10f)
                barWidth = 0.7f
            }
            setFitBars(true)
            invalidate()
        }
    }

    private fun updateLineChart(dailyStats: List<Map<String, Any>>) {
        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        val sortedStats = dailyStats.sortedBy { (it["_id"] as? Number)?.toInt() ?: 0 }

        sortedStats.forEachIndexed { index, stat ->
            val day = (stat["_id"] as? Number)?.toInt() ?: (index + 1)
            val amount = (stat["amount"] as? Number)?.toFloat() ?: 0f

            entries.add(Entry(index.toFloat(), amount))
            labels.add("$day")
        }

        if (entries.isEmpty()) {
            for (i in 1..30) {
                entries.add(Entry((i - 1).toFloat(), (Math.random() * 100000).toFloat()))
                labels.add("$i")
            }
        }

        val dataSet = LineDataSet(entries, "Daily Spending").apply {
            setDrawIcons(false)
            color = Color.parseColor("#00D4AA")
            setCircleColor(Color.parseColor("#00D4AA"))
            lineWidth = 2f
            circleRadius = 3f
            setDrawCircleHole(false)
            valueTextSize = 9f
            setDrawFilled(true)
            fillColor = Color.parseColor("#00D4AA")
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        binding.lineChart.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = minOf(labels.size, 10)
            data = LineData(dataSet)
            invalidate()
        }
    }

    private fun setupClickListeners() {
        binding.bottomNavigation.selectedItemId = R.id.nav_analytics

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // ✅ 명시적으로 Context 지정
                    val intent = Intent(this@AnalyticsViewActivity, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_receipts -> {
                    // ✅ 명시적으로 Context 지정
                    val intent = Intent(this@AnalyticsViewActivity, ReceiptScanActivity::class.java)
                    startActivity(intent)
                    false
                }
                R.id.nav_profile -> {
                    // ✅ 명시적으로 Context 지정
                    val intent = Intent(this@AnalyticsViewActivity, ProfileActivity::class.java)
                    startActivity(intent)
                    false
                }
                else -> false
            }
        }
    }

    private fun setupBottomNavigation() {
        // ✅ 현재 화면이 'Analytics'임을 네비게이션 바에 알림
        binding.bottomNavigation.selectedItemId = R.id.nav_analytics

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_categories -> {
                    val intent = Intent(this, CategoriesActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }

                R.id.nav_analytics -> true // 현재 화면 유지

                R.id.nav_receipts -> {
                    val intent = Intent(this, ReceiptScanActivity::class.java)
                    startActivity(intent)
                    false
                }

                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}