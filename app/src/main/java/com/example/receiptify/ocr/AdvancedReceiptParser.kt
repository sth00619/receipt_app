package com.example.receiptify.ocr

import android.icu.text.SimpleDateFormat
import android.util.Log
import java.util.*
import java.util.Date

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
        val headerKeywords = listOf("상품명", "수량", "단가", "금액")

        val storeName = lines
            .take(6) // 위에서 5~6줄만 후보로
            .firstOrNull { line ->
                val trimmed = line.trim()
                trimmed.isNotEmpty() &&
                        headerKeywords.none { trimmed.contains(it) } &&          // 헤더 아님
                        !trimmed.matches(Regex("^[0-9\\-:년월일시 ]+$")) &&     // 날짜/시간/숫자 덩어리 아님
                        !trimmed.contains("전화")                               // 전화 라인 아님
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
        val pattern = Regex("일시[:\\s]*(\\d{4})-(\\d{2})-(\\d{2})\\s+(\\d{2}):(\\d{2})")
        val match = pattern.find(text) ?: return null

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
        val dateString = "${match.groupValues[1]}-${match.groupValues[2]}-${match.groupValues[3]} ${match.groupValues[4]}:${match.groupValues[5]}"
        return formatter.parse(dateString)
    }


    private fun extractTime(text: String): String? {
        val timeRegex = Regex("(\\d{1,2}):(\\d{2})(:\\d{2})?")
        val time = timeRegex.find(text)?.value
        Log.d(TAG, "⏰ 시간: $time")
        return time
    }

    private fun extractTotalAmount(text: String): Int? {
        val pattern = Regex("총액[:\\s]*([\\d,]+)[^0-9]*")
        val match = pattern.find(text) ?: return null
        return match.groupValues[1].replace(",", "").toInt()
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

        // 예: "1) 토피 넛 라떼 / 수량: 1 / 단가: 6,500 / 금액: 6,500"
        val regex = Regex(
            """\d+\)\s*(.+?)\s*/\s*수량[:\s]*(\d+)\s*/\s*단가[:\s]*([\d,]+)\s*/\s*금액[:\s]*([\d,]+)"""
        )

        for (line in lines) {
            val m = regex.find(line) ?: continue

            val name = m.groupValues[1].trim()
            val qty = m.groupValues[2].toInt()
            val unit = m.groupValues[3].replace(",", "").toInt()
            val total = m.groupValues[4].replace(",", "").toInt()

            items.add(
                ReceiptItem(
                    name = name,
                    quantity = qty,
                    unitPrice = unit,
                    totalPrice = total
                )
            )
        }

        return items
    }




    private fun suggestCategory(text: String): String {
        val t = text.lowercase()

        // 1️⃣ 편의점
        val convenienceKeywords = listOf("cu", "gs25", "세븐일레븐", "이마트24", "편의점")
        if (convenienceKeywords.any { t.contains(it) }) return "convenience"

        // 2️⃣ 카페 / 커피
        val cafeKeywords = listOf("스타벅스", "이디야", "투썸", "카페", "할리스", "커피")
        if (cafeKeywords.any { t.contains(it) }) return "cafe"

        // 3️⃣ 음식점(대분류)
        val restaurantKeywords = listOf(
            "식당", "국밥", "순두부", "덮밥", "칼국수", "보쌈", "떡볶이",
            "고기", "삼겹살", "버거", "라멘", "라면", "돈까스", "카츠"
        )
        if (restaurantKeywords.any { t.contains(it) }) return "food"

        // 4️⃣ 우리가 사용하는 고유 매장들 (화이트리스트)
        val ourShops = listOf(
            "아소코", "나진국밥", "온달네", "쪼매매운", "쪼매매운떡볶이",
            "세겹먹는날", "공릉순두부", "엽기떡볶이", "동대문엽기떡볶이",
            "버거킹", "맥도날드", "던킨"
        )
        if (ourShops.any { t.contains(it) }) return "food"

        // 5️⃣ 교통
        val transportKeywords = listOf("택시", "버스", "지하철", "요금", "주유")
        if (transportKeywords.any { t.contains(it) }) return "transport"

        // 6️⃣ 쇼핑
        val shoppingKeywords = listOf("다이소", "올리브영", "쿠팡", "이마트")
        if (shoppingKeywords.any { t.contains(it) }) return "shopping"

        return "others"
    }

}