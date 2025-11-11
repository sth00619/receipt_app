package com.example.receiptify.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.tasks.await

object OcrEngine {
    // 주어진 이미지 URI를 ML Kit OCR 엔진에 전달하여 텍스트를 추출한다.

    /**
     * 이미지 크기를 최적화하여 메모리 사용을 줄인다.
     * @param context Context
     * @param uri 이미지 URI
     * @param maxWidth 최대 너비 (기본값: 1024)
     * @param maxHeight 최대 높이 (기본값: 1024)
     * @return 리사이즈된 Bitmap
     */
    private fun getResizedBitmap(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1024,
        maxHeight: Int = 1024
    ): Bitmap {
        // 먼저 이미지 크기만 확인 (메모리에 로드하지 않음)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }

        // 샘플링 비율 계산
        var scale = 1
        while (options.outWidth / scale > maxWidth || options.outHeight / scale > maxHeight) {
            scale *= 2
        }

        // 실제 이미지 로드 (리사이즈된 크기로)
        val options2 = BitmapFactory.Options().apply {
            inSampleSize = scale
        }

        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options2)
                ?: throw IllegalArgumentException("Failed to decode bitmap")
        } ?: throw IllegalArgumentException("Failed to open input stream")
    }

    suspend fun recognize(context: Context, imageUri: Uri): String {
        // 이미지를 리사이즈하여 메모리 사용 최적화
        val resizedBitmap = getResizedBitmap(context, imageUri)

        try {
            // 리사이즈된 Bitmap으로 InputImage 생성
            val image = InputImage.fromBitmap(resizedBitmap, 0)

            // ML Kit 한글 문자 인식기 생성
            val recognizer = TextRecognition.getClient(
                KoreanTextRecognizerOptions.Builder().build()
            )

            val result = recognizer.process(image).await()

            // 추출된 전체 텍스트 반환
            return result.text
        } finally {
            // 사용 후 메모리 해제
            resizedBitmap.recycle()
        }
    }
}