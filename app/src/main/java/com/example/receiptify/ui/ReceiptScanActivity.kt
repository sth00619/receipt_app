package com.example.receiptify.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.receiptify.databinding.ActivityReceiptScanBinding
import com.example.receiptify.ocr.ReceiptOCRProcessor
import com.example.receiptify.ocr.ParsedReceiptData
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ReceiptScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReceiptScanBinding
    private lateinit var ocrProcessor: ReceiptOCRProcessor

    private var currentImageUri: Uri? = null  // ✅ 현재 이미지 URI 저장

    companion object {
        private const val TAG = "ReceiptScanActivity"
        private const val REQUEST_CAMERA_PERMISSION = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ocrProcessor = ReceiptOCRProcessor()

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        // 카메라 버튼
        binding.btnCamera.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }

        // 갤러리 버튼
        binding.btnGallery.setOnClickListener {
            launchGallery()
        }
    }

    // ============ 권한 체크 ============

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                    this,
                    "카메라 권한이 필요합니다",
                    Toast.LENGTH_SHORT
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(
                this,
                "카메라 권한이 거부되었습니다",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ============ 카메라/갤러리 런처 ============

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentImageUri != null) {
            processImage(currentImageUri!!)
        } else {
            Toast.makeText(this, "사진 촬영에 실패했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            currentImageUri = it
            processImage(it)
        } ?: Toast.makeText(this, "이미지를 선택하지 않았습니다", Toast.LENGTH_SHORT).show()
    }

    // ============ 카메라/갤러리 실행 ============

    private fun launchCamera() {
        try {
            val photoFile = createImageFile()
            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )
            currentImageUri = uri  // ✅ 먼저 저장
            cameraLauncher.launch(uri)  // ✅ non-null Uri 전달
        } catch (e: Exception) {
            Log.e(TAG, "카메라 실행 실패", e)
            Toast.makeText(this, "카메라 실행 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(null)
        return File.createTempFile(
            "RECEIPT_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    // ============ 이미지 처리 (OCR) ============

    private fun processImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "📸 이미지 처리 시작: $uri")

                binding.tvResult.text = "영수증을 분석 중입니다...\n잠시만 기다려주세요."

                // ML Kit InputImage 생성
                val image = InputImage.fromFilePath(this@ReceiptScanActivity, uri)

                // OCR 실행
                ocrProcessor.processReceipt(image) { result ->
                    result.onSuccess { parsedData ->
                        Log.d(TAG, "✅ OCR 성공: $parsedData")
                        displayParsedResult(parsedData)
                    }.onFailure { error ->
                        Log.e(TAG, "❌ OCR 실패", error)
                        binding.tvResult.text = "영수증 인식에 실패했습니다.\n다시 시도해주세요.\n\n오류: ${error.message}"
                        Toast.makeText(
                            this@ReceiptScanActivity,
                            "OCR 실패: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ 이미지 처리 오류", e)
                binding.tvResult.text = "이미지 처리 중 오류가 발생했습니다.\n\n오류: ${e.message}"
                Toast.makeText(
                    this@ReceiptScanActivity,
                    "오류: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ============ OCR 결과 표시 ============

    private fun displayParsedResult(data: com.example.receiptify.ocr.ParsedReceiptData) {
        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA)

        binding.tvResult.text = buildString {
            appendLine("✅ 영수증 인식 완료!\n")

            appendLine("═══════════════════════════════")
            appendLine("📋 기본 정보")
            appendLine("═══════════════════════════════")

            data.storeName?.let {
                appendLine("🏪 상점명: $it")
            } ?: appendLine("🏪 상점명: 인식 실패")

            data.storePhone?.let {
                appendLine("📞 전화번호: $it")
            }

            data.storeAddress?.let {
                appendLine("📍 주소: $it")
            }

            data.businessNumber?.let {
                appendLine("🏢 사업자번호: $it")
            }

            appendLine()
            appendLine("═══════════════════════════════")
            appendLine("💰 결제 정보")
            appendLine("═══════════════════════════════")

            data.transactionDate?.let {
                appendLine("📅 날짜: ${dateFormat.format(it)}")
            }

            data.transactionTime?.let {
                appendLine("⏰ 시간: $it")
            }

            data.totalAmount?.let {
                appendLine("💵 총액: ₩ ${String.format("%,d", it)}")
            } ?: appendLine("💵 총액: 인식 실패")

            data.paymentMethod?.let {
                val methodName = when (it) {
                    "card" -> "카드"
                    "cash" -> "현금"
                    "transfer" -> "계좌이체"
                    else -> "기타"
                }
                appendLine("💳 결제방법: $methodName")
            }

            data.cardNumber?.let {
                appendLine("   카드번호: $it")
            }

            data.approvalNumber?.let {
                appendLine("   승인번호: $it")
            }

            appendLine()
            appendLine("═══════════════════════════════")
            appendLine("📦 품목 내역 (${data.items.size}개)")
            appendLine("═══════════════════════════════")

            if (data.items.isNotEmpty()) {
                data.items.forEachIndexed { index, item ->
                    appendLine()
                    appendLine("${index + 1}. ${item.name}")
                    appendLine("   수량: ${item.quantity}개")
                    item.unitPrice?.let {
                        appendLine("   단가: ₩ ${String.format("%,d", it)}")
                    }
                    appendLine("   금액: ₩ ${String.format("%,d", item.totalPrice)}")
                }

                appendLine()
                val itemsTotal = data.items.sumOf { it.totalPrice }
                appendLine("품목 합계: ₩ ${String.format("%,d", itemsTotal)}")

                data.totalAmount?.let { total ->
                    val diff = total - itemsTotal
                    if (diff != 0) {
                        appendLine("차액: ₩ ${String.format("%,d", diff)}")
                        if (diff > 0) {
                            appendLine("(세금, 봉사료 등 포함)")
                        }
                    }
                }
            } else {
                appendLine("품목을 인식하지 못했습니다.")
            }

            appendLine()
            appendLine("═══════════════════════════════")
            appendLine("🏷️ 추천 카테고리")
            appendLine("═══════════════════════════════")

            val categoryName = when (data.suggestedCategory) {
                "food" -> "🍔 식비"
                "transport" -> "🚗 교통"
                "shopping" -> "🛍️ 쇼핑"
                else -> "📌 기타"
            }
            appendLine(categoryName)

            appendLine()
            appendLine("═══════════════════════════════")
            appendLine()
            appendLine("💡 품목 내역이나 금액이 정확하지 않다면")
            appendLine("   위의 저장 및 수정 버튼을 눌러 수정할 수 있습니다.")
        }

        Toast.makeText(
            this@ReceiptScanActivity,
            "영수증 인식이 완료되었습니다!",
            Toast.LENGTH_SHORT
        ).show()

        // ✅ 저장 및 수정 버튼 표시
        showEditButton(data)
    }

    private fun showEditButton(data: com.example.receiptify.ocr.ParsedReceiptData) {
        binding.btnSaveAndEdit.visibility = View.VISIBLE

        binding.btnSaveAndEdit.setOnClickListener {
            val intent = Intent(this, ReceiptEditActivity::class.java)

            // ✅ 이미지 URI 전달
            currentImageUri?.let {
                intent.putExtra("imageUri", it.toString())
            }

            // ParsedReceiptData 전달
            intent.putExtra("storeName", data.storeName ?: "")
            intent.putExtra("totalAmount", data.totalAmount ?: 0)
            intent.putExtra("transactionDate", data.transactionDate?.time ?: System.currentTimeMillis())
            intent.putExtra("category", data.suggestedCategory ?: "others")

            // 품목 리스트를 JSON으로 변환하여 전달
            val gson = com.google.gson.Gson()
            val itemsJson = gson.toJson(data.items)
            intent.putExtra("items", itemsJson)

            startActivity(intent)
        }
    }
}