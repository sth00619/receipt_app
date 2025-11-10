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
import com.example.receiptify.ocr.ReceiptParser
import kotlinx.coroutines.launch
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.content.ContextCompat
import com.example.receiptify.R

class ReceiptScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReceiptScanBinding

    // 임시로 촬영한 이미지의 URI 저장용
    private var tempPhotoUri: Uri? = null

    // 갤러리에서 이미지 선택
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) runOcr(uri)
    }

    // 카메라 촬영 결과
    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok && tempPhotoUri != null) runOcr(tempPhotoUri!!)
    }

    // 임시 이미지 파일 생성
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

    // 갤러리 접근 권한 요청
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

    // 카메라 권한 요청
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

    // 카메라 권한 체크 및 촬영
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

    // 갤러리 접근 권한 체크
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
        // 갤러리에서 선택
        binding.btnGallery.setOnClickListener {
            ensurePermissionsAndPickImage()
        }

        // 카메라로 촬영
        binding.btnCamera.setOnClickListener {
            ensureCameraPermissionAndShoot()
        }
    }

    // OCR 처리 중 버튼 비활성화
    private fun setBusy(busy: Boolean) {
        binding.btnGallery.isEnabled = !busy
        binding.btnCamera.isEnabled = !busy
        if (busy) {
            binding.tvResult.text = "영수증을 인식하는 중입니다...\n잠시만 기다려주세요."
        }
    }

    // OCR 실행
    private fun runOcr(uri: Uri) {
        lifecycleScope.launch {
            try {
                setBusy(true)
                val text = OcrEngine.recognize(this@ReceiptScanActivity, uri)
                val parsed = ReceiptParser.parse(text)

                // 결과 표시
                binding.tvResult.text = buildString {
                    appendLine("✅ 영수증 인식 완료!\n")
                    appendLine("📍 상점명: ${parsed.storeName ?: "인식 실패"}")
                    appendLine("💰 총액: ${parsed.totalAmount?.let { "₩ ${String.format("%,d", it)}" } ?: "인식 실패"}")
                    appendLine("📦 품목 수: ${parsed.items.size}개\n")

                    if (parsed.items.isNotEmpty()) {
                        appendLine("📋 품목 상세:")
                        parsed.items.forEachIndexed { index, item ->
                            appendLine("${index + 1}. ${item.name}")
                            appendLine("   수량: ${item.qty}개 | 금액: ₩ ${String.format("%,d", item.amount)}")
                        }
                    }

                    appendLine("\n" + "=".repeat(30))
                    appendLine("\n🔍 원본 텍스트:")
                    appendLine(text)
                }

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
            }
        }
    }
}