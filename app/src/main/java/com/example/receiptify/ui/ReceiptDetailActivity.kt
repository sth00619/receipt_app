package com.example.receiptify.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.receiptify.adapter.ReceiptItemAdapter
import com.example.receiptify.databinding.ActivityReceiptDetailBinding
import com.example.receiptify.repository.ReceiptImageRepository
import com.example.receiptify.repository.ReceiptRepository
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ReceiptDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReceiptDetailBinding
    private lateinit var imageRepository: ReceiptImageRepository
    private lateinit var receiptRepository: ReceiptRepository
    private lateinit var receiptItemAdapter: ReceiptItemAdapter

    private val numberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    private val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)

    companion object {
        private const val TAG = "ReceiptDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        imageRepository = ReceiptImageRepository(this)
        receiptRepository = ReceiptRepository()

        setupToolbar()
        setupRecyclerView()

        // Get receipt ID from intent
        val receiptId = intent.getStringExtra("receipt_id")
        if (receiptId == null) {
            Toast.makeText(this, "잘못된 영수증입니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadReceiptDetails(receiptId)
        setupClickListeners(receiptId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "영수증 상세"
        }
    }

    private fun setupRecyclerView() {
        receiptItemAdapter = ReceiptItemAdapter()
        binding.rvReceiptItems.apply {
            layoutManager = LinearLayoutManager(this@ReceiptDetailActivity)
            adapter = receiptItemAdapter
        }
    }

    private fun loadReceiptDetails(receiptId: String) {
        lifecycleScope.launch {
            try {
                // Show loading
                binding.progressBar.visibility = View.VISIBLE
                binding.scrollView.visibility = View.GONE

                // Load receipt image from Room database
                val imageEntity = imageRepository.getImage(receiptId)
                imageEntity?.let { entity ->
                    val file = File(entity.imagePath)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        binding.imageReceipt.setImageBitmap(bitmap)
                        binding.imageReceipt.visibility = View.VISIBLE
                    } else {
                        binding.imageReceipt.visibility = View.GONE
                    }
                } ?: run {
                    binding.imageReceipt.visibility = View.GONE
                }

                // Load receipt details from MongoDB
                val result = receiptRepository.getReceipt(receiptId)

                result.onSuccess { receipt ->
                    Log.d(TAG, "✅ Receipt loaded: ${receipt.storeName}")

                    // Display receipt information
                    binding.apply {
                        tvStoreName.text = receipt.storeName
                        // tvStoreAddress.text = receipt.storeAddress ?: "주소 정보 없음" // 요청에 의해 주소 숨김
                        tvStoreAddress.visibility = View.GONE
                        tvStorePhone.text = receipt.storePhone ?: "전화번호 정보 없음"

                        tvTotalAmount.text = "₩ ${numberFormat.format(receipt.totalAmount.toLong())}"
                        tvTaxAmount.text = "₩ ${numberFormat.format(receipt.taxAmount?.toLong() ?: 0)}"
                        tvDiscountAmount.text = "₩ ${numberFormat.format(receipt.discountAmount?.toLong() ?: 0)}"

                        tvTransactionDate.text = formatDate(receipt.transactionDate)
                        tvPaymentMethod.text = receipt.paymentMethod ?: "정보 없음"
                        tvCategory.text = getCategoryName(receipt.category)

                        // Show items if available
                        if (receipt.items.isNotEmpty()) {
                            rvReceiptItems.visibility = View.VISIBLE
                            tvNoItems.visibility = View.GONE
                            receiptItemAdapter.submitList(receipt.items)
                        } else {
                            rvReceiptItems.visibility = View.GONE
                            tvNoItems.visibility = View.VISIBLE
                        }

                    }

                    // Hide loading, show content
                    binding.progressBar.visibility = View.GONE
                    binding.scrollView.visibility = View.VISIBLE

                }.onFailure { error ->
                    Log.e(TAG, "❌ Failed to load receipt", error)
                    Toast.makeText(
                        this@ReceiptDetailActivity,
                        "영수증을 불러오는데 실패했습니다: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.GONE
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception while loading receipt", e)
                Toast.makeText(
                    this@ReceiptDetailActivity,
                    "오류가 발생했습니다",
                    Toast.LENGTH_SHORT
                ).show()
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners(receiptId: String) {
        binding.btnDelete.setOnClickListener {
            lifecycleScope.launch {
                val result = imageRepository.deleteImage(receiptId)

                // Also delete from MongoDB
                receiptRepository.deleteReceipt(receiptId)

                if (result.isSuccess) {
                    Toast.makeText(this@ReceiptDetailActivity, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ReceiptDetailActivity, "삭제 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = isoFormat.parse(dateString)
            date?.let { dateFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }

    private fun getCategoryName(category: String): String {
        return when (category.lowercase()) {
            "food" -> "식비"
            "transport" -> "교통"
            "shopping" -> "쇼핑"
            "healthcare" -> "건강/의료"
            "entertainment" -> "문화/여가"
            "utilities" -> "공과금"
            else -> "기타"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
