package com.example.receiptify.ocr

import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.util.*

class ReceiptOCRProcessor {

    private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    private val parser = AdvancedReceiptParser()

    companion object {
        private const val TAG = "ReceiptOCRProcessor"
    }

    fun processReceipt(
        image: InputImage,
        callback: (Result<ParsedReceiptData>) -> Unit
    ) {
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text
                Log.d(TAG, "✅ OCR 성공\n텍스트:\n$extractedText")

                try {
                    val parsedData = parser.parse(extractedText)
                    callback(Result.success(parsedData))
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 파싱 실패", e)
                    callback(Result.failure(e))
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ OCR 실패", e)
                callback(Result.failure(e))
            }
    }
}

data class ParsedReceiptData(
    val storeName: String? = null,
    val storePhone: String? = null,
    val storeAddress: String? = null,
    val businessNumber: String? = null,
    val transactionDate: Date? = null,
    val transactionTime: String? = null,
    val totalAmount: Int? = null,
    val paymentMethod: String? = null,
    val cardNumber: String? = null,
    val approvalNumber: String? = null,
    val items: List<ReceiptItem> = emptyList(),
    val suggestedCategory: String = "others"
)

data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Int? = null,
    val totalPrice: Int
)