package com.example.receiptify.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.receiptify.databinding.ActivityReceiptDetailBinding
import com.example.receiptify.repository.ReceiptImageRepository
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import java.io.File

class ReceiptDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReceiptDetailBinding
    private lateinit var repository: ReceiptImageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = ReceiptImageRepository(this)

        // 1) 카테고리 탭에서 넘겨준 receiptId 받기
        val receiptId = intent.getStringExtra("receipt_id")
        if (receiptId == null) {
            Toast.makeText(this, "잘못된 영수증입니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2) Room에서 이미지 정보 가져와서 화면에 표시
        lifecycleScope.launch {
            val entity = repository.getImage(receiptId)
            entity?.let { imageEntity ->
                val file = File(imageEntity.imagePath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    binding.imageReceipt.setImageBitmap(bitmap)
                }
            }

            // TODO: MongoDB에서 영수증 상세 정보 불러와서 텍스트 표시하기
        }

        // 3) 삭제 버튼 클릭 시 → 로컬 삭제 + Room 삭제
        binding.btnDelete.setOnClickListener {
            lifecycleScope.launch {
                val result = repository.deleteImage(receiptId)

                // TODO: 원하면 여기에 MongoDB 삭제 API도 추가 가능
                // api.deleteReceipt(receiptId)

                if (result.isSuccess) {
                    Toast.makeText(this@ReceiptDetailActivity, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ReceiptDetailActivity, "삭제 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
