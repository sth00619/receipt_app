package com.example.receiptify.ui

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.receiptify.R
import com.example.receiptify.adapter.ReceiptItemEditAdapter
import com.example.receiptify.api.RetrofitClient
import com.example.receiptify.api.models.CreateReceiptRequest
import com.example.receiptify.api.models.ReceiptItem
import com.example.receiptify.databinding.ActivityReceiptEditBinding
import com.example.receiptify.utils.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReceiptEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReceiptEditBinding
    private lateinit var itemAdapter: ReceiptItemEditAdapter
    private lateinit var preferenceManager: PreferenceManager

    // ✅ Adapter의 data class 사용
    private val items = mutableListOf<ReceiptItemEditAdapter.ReceiptItemEdit>()
    private var selectedDate: Date = Date()
    private var receiptImageUri: Uri? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
    private val displayDateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA)

    companion object {
        private const val TAG = "ReceiptEditActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)

        setupUI()
        setupRecyclerView()
        loadIntentData()
        setupClickListeners()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // 카테고리 스피너 설정
        val categories = arrayOf(
            "식비", "교통", "쇼핑", "건강/의료", "문화/여가", "공과금", "기타"
        )
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter
    }

    private fun setupRecyclerView() {
        itemAdapter = ReceiptItemEditAdapter(
            items = items,
            onItemChanged = { calculateTotal() },
            onDeleteItem = { position ->
                items.removeAt(position)
                itemAdapter.notifyItemRemoved(position)
                calculateTotal()
            }
        )

        binding.rvItems.apply {
            layoutManager = LinearLayoutManager(this@ReceiptEditActivity)
            adapter = itemAdapter
        }
    }

    private fun loadIntentData() {
        try {
            // 영수증 이미지 URI
            val imageUriString = intent.getStringExtra("imageUri")
            if (imageUriString != null) {
                receiptImageUri = Uri.parse(imageUriString)

                Glide.with(this)
                    .load(receiptImageUri)
                    .centerCrop()
                    .into(binding.ivReceiptImage)

                binding.ivReceiptImage.visibility = View.VISIBLE
                Log.d(TAG, "✅ 영수증 이미지 로드: $receiptImageUri")
            } else {
                binding.ivReceiptImage.visibility = View.GONE
                Log.w(TAG, "⚠️ 이미지 URI 없음")
            }

            // 상점명
            val storeName = intent.getStringExtra("storeName")
            if (!storeName.isNullOrBlank()) {
                binding.etStoreName.setText(storeName)
                Log.d(TAG, "✅ 상점명: $storeName")
            }

            // 총액
            val totalAmount = intent.getIntExtra("totalAmount", 0)
            if (totalAmount > 0) {
                binding.etTotalAmount.setText(totalAmount.toString())
                Log.d(TAG, "✅ 총액: $totalAmount")
            }

            // 거래 날짜
            val transactionDateMillis = intent.getLongExtra("transactionDate", System.currentTimeMillis())
            selectedDate = Date(transactionDateMillis)
            binding.etTransactionDate.setText(displayDateFormat.format(selectedDate))
            Log.d(TAG, "✅ 거래 날짜: ${displayDateFormat.format(selectedDate)}")

            // 카테고리
            val category = intent.getStringExtra("category") ?: "others"
            val categoryIndex = when (category) {
                "food", "cafe", "convenience" -> 0
                "transport" -> 1
                "shopping" -> 2
                "healthcare" -> 3
                "entertainment" -> 4
                "utilities" -> 5
                else -> 6
            }
            binding.spinnerCategory.setSelection(categoryIndex)
            Log.d(TAG, "✅ 카테고리: $category -> index $categoryIndex")

            // 품목 리스트
            val itemsJson = intent.getStringExtra("items")
            if (!itemsJson.isNullOrBlank()) {
                val gson = Gson()
                val itemType = object : TypeToken<List<Map<String, Any>>>() {}.type
                val itemsList: List<Map<String, Any>> = gson.fromJson(itemsJson, itemType)

                items.clear()
                itemsList.forEach { itemMap ->
                    val name = itemMap["name"] as? String ?: "품목"
                    val quantity = (itemMap["quantity"] as? Double)?.toInt() ?: 1
                    val amount = ((itemMap["amount"] ?: itemMap["totalPrice"]) as? Double)?.toInt() ?: 0
                    val unitPrice = if (quantity > 0) amount / quantity else 0

                    items.add(ReceiptItemEditAdapter.ReceiptItemEdit(name, quantity, unitPrice, amount))
                }

                itemAdapter.notifyDataSetChanged()
                calculateTotal()

                Log.d(TAG, "✅ 품목 ${items.size}개 로드 완료")
            } else {
                Log.w(TAG, "⚠️ 품목 데이터 없음")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Intent 데이터 로드 실패", e)
            Toast.makeText(this, "데이터 로드 중 오류 발생", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        // 날짜 선택
        binding.etTransactionDate.setOnClickListener {
            showDatePicker()
        }

        // 품목 추가
        binding.btnAddItem.setOnClickListener {
            items.add(ReceiptItemEditAdapter.ReceiptItemEdit("새 품목", 1, 0, 0))
            itemAdapter.notifyItemInserted(items.size - 1)
            binding.rvItems.scrollToPosition(items.size - 1)
        }

        // 저장하기
        binding.btnSave.setOnClickListener {
            saveReceipt()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.time = selectedDate

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = calendar.time
                binding.etTransactionDate.setText(displayDateFormat.format(selectedDate))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun calculateTotal() {
        val total = items.sumOf { it.amount }
        binding.etTotalAmount.setText(total.toString())
    }

    private fun saveReceipt() {
        val storeName = binding.etStoreName.text.toString().trim()
        if (storeName.isBlank()) {
            Toast.makeText(this, "상점명을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val totalAmountText = binding.etTotalAmount.text.toString().trim()
        if (totalAmountText.isBlank()) {
            Toast.makeText(this, "총액을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val totalAmount = totalAmountText.toDoubleOrNull()
        if (totalAmount == null || totalAmount <= 0) {
            Toast.makeText(this, "올바른 금액을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedCategoryIndex = binding.spinnerCategory.selectedItemPosition
        val categoryCode = when (selectedCategoryIndex) {
            0 -> "food"
            1 -> "transport"
            2 -> "shopping"
            3 -> "healthcare"
            4 -> "entertainment"
            5 -> "utilities"
            else -> "others"
        }

        val userId = preferenceManager.getUserId() ?: ""

        val receiptItems = items.map { item ->
            ReceiptItem(
                name = item.name,
                quantity = item.quantity,
                unitPrice = item.unitPrice.toDouble(),
                amount = item.amount.toDouble()
            )
        }

        val request = CreateReceiptRequest(
            userId = userId,
            storeName = storeName,
            totalAmount = totalAmount,
            transactionDate = dateFormat.format(selectedDate),
            category = categoryCode,
            items = receiptItems,
            paymentMethod = "card"
        )

        lifecycleScope.launch {
            try {
                Log.d(TAG, "📤 영수증 저장 요청: $request")

                val response = RetrofitClient.receiptApi.createReceipt(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d(TAG, "✅ 영수증 저장 성공")
                    Toast.makeText(
                        this@ReceiptEditActivity,
                        "영수증이 저장되었습니다",
                        Toast.LENGTH_SHORT
                    ).show()

                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorMsg = response.body()?.message ?: "알 수 없는 오류"
                    Log.e(TAG, "❌ 영수증 저장 실패: $errorMsg")
                    Toast.makeText(
                        this@ReceiptEditActivity,
                        "저장 실패: $errorMsg",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 영수증 저장 중 오류", e)
                Toast.makeText(
                    this@ReceiptEditActivity,
                    "오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}