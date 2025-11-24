package com.example.receiptify.ocr

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class AdvancedReceiptParser {

    companion object {
        private const val TAG = "AdvancedReceiptParser"
    }

    fun parse(text: String): ParsedReceiptData {
        Log.d(TAG, "📝 파싱 시작")

        val lines = text.lines().filter { it.isNotBlank() }

        return ParsedReceiptData(
            storeName = extractStoreName(lines),
            storePhone = extractPhoneNumber(text),
            storeAddress = extractAddress(text),
            businessNumber = extractBusinessNumber(text),
            transactionDate = extractDate(text),
            transactionTime = extractTime(text),
            totalAmount = extractTotalAmount(text),
            paymentMethod = extractPaymentMethod(text),
            cardNumber = extractCardNumber(text),
            approvalNumber = extractApprovalNumber(text),
            items = extractItems(lines),
            suggestedCategory = suggestCategory(text)
        )
    }

    private fun extractStoreName(lines: List<String>): String? {
        // 첫 몇 줄에서 상점명 추출
        val storeName = lines.take(5)
            .firstOrNull { line ->
                line.length in 2..30 &&
                        !line.contains(Regex("\\d{3}-\\d{3,4}-\\d{4}")) && // 전화번호 제외
                        !line.contains(Regex("\\d{10}")) // 사업자번호 제외
            }

        Log.d(TAG, "🏪 상점명: $storeName")
        return storeName
    }

    private fun extractPhoneNumber(text: String): String? {
        val phoneRegex = Regex("(\\d{2,3}[-.]?\\d{3,4}[-.]?\\d{4})")
        val phone = phoneRegex.find(text)?.value
        Log.d(TAG, "📞 전화번호: $phone")
        return phone
    }

    private fun extractAddress(text: String): String? {
        val addressRegex = Regex("([가-힣]+[시도]\\s+[가-힣]+[시군구]\\s+[가-힣\\s]+)")
        val address = addressRegex.find(text)?.value?.trim()
        Log.d(TAG, "📍 주소: $address")
        return address
    }

    private fun extractBusinessNumber(text: String): String? {
        val bizNumRegex = Regex("(\\d{3}[-]?\\d{2}[-]?\\d{5})")
        val bizNum = bizNumRegex.find(text)?.value
        Log.d(TAG, "🏢 사업자번호: $bizNum")
        return bizNum
    }

    private fun extractDate(text: String): Date? {
        val datePatterns = listOf(
            "yyyy-MM-dd" to Regex("(\\d{4})[-./](\\d{1,2})[-./](\\d{1,2})"),
            "yyyy.MM.dd" to Regex("(\\d{4})\\.(\\d{1,2})\\.(\\d{1,2})"),
            "yy-MM-dd" to Regex("(\\d{2})[-./](\\d{1,2})[-./](\\d{1,2})")
        )

        for ((pattern, regex) in datePatterns) {
            val match = regex.find(text)
            if (match != null) {
                try {
                    val dateFormat = SimpleDateFormat(pattern, Locale.KOREA)
                    val date = dateFormat.parse(match.value)
                    Log.d(TAG, "📅 날짜: $date")
                    return date
                } catch (e: Exception) {
                    Log.w(TAG, "날짜 파싱 실패: ${match.value}")
                }
            }
        }

        Log.d(TAG, "📅 날짜: null (인식 실패)")
        return null
    }

    private fun extractTime(text: String): String? {
        val timeRegex = Regex("(\\d{1,2}):(\\d{2})(:\\d{2})?")
        val time = timeRegex.find(text)?.value
        Log.d(TAG, "⏰ 시간: $time")
        return time
    }

    private fun extractTotalAmount(text: String): Int? {
        val amountPatterns = listOf(
            Regex("합\\s*계[:\\s]*([\\d,]+)"),
            Regex("총\\s*액[:\\s]*([\\d,]+)"),
            Regex("결제금액[:\\s]*([\\d,]+)"),
            Regex("합계금액[:\\s]*([\\d,]+)")
        )

        for (pattern in amountPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val amount = match.groupValues[1].replace(",", "").toIntOrNull()
                if (amount != null && amount > 0) {
                    Log.d(TAG, "💰 총액: $amount")
                    return amount
                }
            }
        }

        Log.d(TAG, "💰 총액: null (인식 실패)")
        return null
    }

    private fun extractPaymentMethod(text: String): String? {
        return when {
            text.contains("신용카드") || text.contains("카드") -> "card"
            text.contains("현금") -> "cash"
            text.contains("계좌이체") || text.contains("이체") -> "transfer"
            else -> null
        }
    }

    private fun extractCardNumber(text: String): String? {
        val cardRegex = Regex("\\*{4,}\\d{4}")
        return cardRegex.find(text)?.value
    }

    private fun extractApprovalNumber(text: String): String? {
        val approvalRegex = Regex("승인[번호]*[:\\s]*(\\d{8,})")
        return approvalRegex.find(text)?.groupValues?.get(1)
    }

    private fun extractItems(lines: List<String>): List<ReceiptItem> {
        val items = mutableListOf<ReceiptItem>()

        val itemRegex = Regex("([가-힣a-zA-Z\\s]+)\\s+(\\d+)\\s+([\\d,]+)")

        for (line in lines) {
            val match = itemRegex.find(line)
            if (match != null) {
                try {
                    val name = match.groupValues[1].trim()
                    val quantity = match.groupValues[2].toIntOrNull() ?: 1
                    val price = match.groupValues[3].replace(",", "").toIntOrNull() ?: 0

                    if (price > 0) {
                        items.add(
                            ReceiptItem(
                                name = name,
                                quantity = quantity,
                                unitPrice = if (quantity > 0) price / quantity else null,
                                totalPrice = price
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "품목 파싱 오류: $line")
                }
            }
        }

        Log.d(TAG, "📦 품목 ${items.size}개 추출")
        return items
    }

    private fun suggestCategory(text: String): String {
        val lowerText = text.lowercase()

        return when {
            lowerText.contains("스타벅스") ||
                    lowerText.contains("카페") ||
                    lowerText.contains("음식") ||
                    lowerText.contains("식당") ||
                    lowerText.contains("치킨") ||
                    lowerText.contains("피자") -> "food"

            lowerText.contains("gs25") ||
                    lowerText.contains("cu") ||
                    lowerText.contains("세븐일레븐") ||
                    lowerText.contains("편의점") -> "food"

            lowerText.contains("택시") ||
                    lowerText.contains("버스") ||
                    lowerText.contains("지하철") ||
                    lowerText.contains("주유") -> "transport"

            lowerText.contains("이마트") ||
                    lowerText.contains("쿠팡") ||
                    lowerText.contains("다이소") ||
                    lowerText.contains("올리브영") -> "shopping"

            else -> "others"
        }
    }
}