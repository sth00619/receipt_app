package com.example.receiptify.ui

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.receiptify.R
import com.example.receiptify.ocr.OcrEngine
import com.example.receiptify.ocr.ReceiptParser
import kotlinx.coroutines.launch
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.example.receiptify.databinding.ActivityOcrTestBinding

class OcrTestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOcrTestBinding

    // 임시로 촬영한 이미지의 URI 저장용
    private var tempPhotoUri: Uri? = null

    // 갤러리에서 이미지 선택 (Activity Result API)
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) runOcr(uri)
    }

    // 카메라 촬영 결과를 받는 Activity Result 콜백
    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok && tempPhotoUri != null) runOcr(tempPhotoUri!!)
    }

    // 임시 이미지 파일을 생성하고 FileProvider를 통해 content:// URI 발급
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

    // 갤러리 접근 권한 요청 런처
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

    // 카메라 권한 요청 런처
    private val requestCameraPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            tempPhotoUri = createTempImageUri()
            tempPhotoUri?.let { takePicture.launch(it) }
        } else {
            Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 카메라 권한 체크 및 촬영 실행
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

    // 갤러리 접근 권한 체크 및 이미지 선택 실행
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

    // onCreate: 버튼 클릭 리스너 연결
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPick.setOnClickListener {
            ensurePermissionsAndPickImage()
        }

        binding.btnCamera.setOnClickListener {
            ensureCameraPermissionAndShoot()
        }
    }

    // OCR 처리 중 버튼 비활성화 (중복 실행 방지)
    private fun setBusy(busy: Boolean) {
        binding.btnPick.isEnabled = !busy
        binding.btnCamera.isEnabled = !busy
        if (busy) {

            binding.tvParsed.text = "인식 중… 잠시만요"
        }
    }

    // OCR 실행: 이미지 → 텍스트 인식 + 파싱 + 출력
    private fun runOcr(uri: Uri) {
        lifecycleScope.launch {
            try {
                setBusy(true)
                val text = OcrEngine.recognize(this@OcrTestActivity, uri)
                val parsed = ReceiptParser.parse(text)
                binding.tvParsed.text = buildString {
                    appendLine("=== OCR 원문 ===")
                    appendLine(text)
                    appendLine("\n=== 파싱 결과 ===")
                    appendLine("상점: ${parsed.storeName}")
                    appendLine("총액: ${parsed.totalAmount}")
                    appendLine("아이템 수: ${parsed.items.size}")
                }
            } catch (e: Exception) {
                binding.tvParsed.text = "OCR 실패: ${e.message}"
            } finally {
                setBusy(false)
            }
        }
    }

}
