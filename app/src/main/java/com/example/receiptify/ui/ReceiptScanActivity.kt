package com.example.receiptify.ui

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.receiptify.databinding.ActivityReceiptScanBinding
import com.example.receiptify.ocr.OcrEngine
import com.example.receiptify.ocr.AdvancedReceiptParser
import kotlinx.coroutines.launch
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.content.ContextCompat
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.*

class ReceiptScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReceiptScanBinding
    private var tempPhotoUri: Uri? = null
    private var parsedReceiptData: com.example.receiptify.ocr.ParsedReceiptData? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) runOcr(uri)
    }

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok && tempPhotoUri != null) {
            runOcr(tempPhotoUri!!)
        }
    }

    private fun createTempImageUri(): Uri {
        val dir = File(filesDir, "receipts")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "${System.currentTimeMillis()}.jpg")

        return FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
    }

    private val requestPickPerms = registerForActivityResult(
        RequestMultiplePermissions()
    ) { result ->
        val ok = result[if (android.os.Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE] == true

        if (ok) {
            pickImage.launch("image/*")
        } else {
            Toast.makeText(this, "갤러리 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestCameraPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            tempPhotoUri = createTempImageUri()
            tempPhotoUri?.let { takePicture.launch(it) }
        } else {
            Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun ensureCameraPermissionAndShoot() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            tempPhotoUri = createTempImageUri()
            tempPhotoUri?.let { takePicture.launch(it) }
        } else {
            requestCameraPerm.launch(Manifest.permission.CAMERA)
        }
    }

    private fun ensurePermissionsAndPickImage() {
        val galleryPerm = if (android.os.Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(this, galleryPerm)
            == PackageManager.PERMISSION_GRANTED) {
            pickImage.launch("image/*")
        } else {
            requestPickPerms.launch(arrayOf(galleryPerm))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "영수증 스캔"
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        binding.btnGallery.setOnClickListener {
            ensurePermissionsAndPickImage()
        }

        binding.btnCamera.setOnClickListener {
            ensureCameraPermissionAndShoot()
        }
    }

    private fun setBusy(busy: Boolean) {
        binding.btnGallery.isEnabled = !busy
        binding.btnCamera.isEnabled = !busy
        if (busy) {
            binding.tvResult.text = "🔍 영수증을 인식하는 중입니다...\n잠시만 기다려주세요."
        }
    }

    private fun runOcr(uri: Uri) {
        lifecycleScope.launch {
            try {
                setBusy(true)

                // 1단계: OCR 텍스트 추출
                val rawText = OcrEngine.recognize(this@ReceiptScanActivity, uri)

                // 2단계: 고급 파싱
                val parsedData = AdvancedReceiptParser.parse(rawText)
                parsedReceiptData = parsedData

                // 3단계: 결과 표시
                displayParsedResult(parsedData)

                Toast.makeText(
                    this@ReceiptScanActivity,
                    "영수증 인식이 완료되었습니다!",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                binding.tvResult.text = "❌ OCR 실패\n\n오류 내용: ${e.message}"
                Toast.makeText(
                    this@ReceiptScanActivity,
                    "영수증 인식에 실패했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setBusy(false)
                System.gc()
            }
        }
    }

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
                val methodName = when(it) {
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

                // 총액과 품목 합계 차이 표시
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

            val categoryName = when(data.suggestedCategory) {
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
            appendLine("   [저장 및 수정] 버튼을 눌러 수정할 수 있습니다.")
        }

        // 저장 버튼 표시 (나중에 구현)
        showSaveButton()
    }

    private fun showSaveButton() {
        // TODO: 저장 버튼 UI 추가
        // 버튼을 누르면 ReceiptEditActivity로 이동하여 수정 가능하도록
    }

    override fun onDestroy() {
        super.onDestroy()
        tempPhotoUri?.let {
            try {
                val file = File(it.path ?: return@let)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}