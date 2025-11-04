package com.example.receiptify.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.receiptify.R
import com.example.receiptify.adapter.TransactionAdapter
import com.example.receiptify.auth.FirebaseAuthManager
import com.example.receiptify.databinding.ActivityHomeBinding
import com.example.receiptify.model.Transaction
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var authManager: FirebaseAuthManager
    private lateinit var firestore: FirebaseFirestore
    private lateinit var transactionAdapter: TransactionAdapter

    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = FirebaseAuthManager.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // 로그인 확인
        if (authManager.currentUser == null) {
            navigateToLogin()
            return
        }

        setupUI()
        setupRecyclerView()
        loadTransactions()
        setupClickListeners()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // 임시 데이터 표시
        updateMonthlyData(226000, 12, true)
        updateTodaySpending(18500)
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()
        binding.rvRecentTransactions.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = transactionAdapter
        }
    }

    private fun setupClickListeners() {
        // 영수증 스캔
        binding.cardScanReceipt.setOnClickListener {
            Toast.makeText(this, R.string.scan_receipt_coming_soon, Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show()
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

    private fun loadTransactions() {
        val userId = authManager.currentUser?.uid ?: return

        firestore.collection("transactions")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(5)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    showEmptyState()
                    return@addSnapshotListener
                }

                val transactions = snapshot?.toObjects(Transaction::class.java) ?: emptyList()

                if (transactions.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                    transactionAdapter.submitList(transactions)
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
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}