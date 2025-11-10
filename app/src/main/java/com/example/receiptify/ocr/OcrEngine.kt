package com.example.receiptify.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.tasks.await

object OcrEngine {
    // 주어진 이미지 URI를 ML Kit OCR 엔진에 전달하여 텍스트를 추출한다.


    suspend fun recognize(context: Context, imageUri: Uri): String {
        // URI로부터 InputImage 객체 생성 (ML Kit 입력용 포맷)
        val image = InputImage.fromFilePath(context, imageUri)

        // ML Kit 한글 문자 인식기 생성
        val recognizer = TextRecognition.getClient(
            KoreanTextRecognizerOptions.Builder().build()
        )
        val result = recognizer.process(image).await()

        // 추출된 전체 텍스트 반환
        return result.text
    }
}
