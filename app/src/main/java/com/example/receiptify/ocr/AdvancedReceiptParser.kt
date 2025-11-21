package com.example.receiptify.ocr

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Advanced Receipt Parsing Engine
 * Extracts all receipt information based on regular expressions.
 */
object AdvancedReceiptParser {
    private const val TAG = "AdvancedReceiptParser"

    // ========== Regular Expression Patterns ==========
    // 💰 Amount Pattern (1,000원, 10000원, 1,000, 10,000 etc.)
    private val moneyPattern = Regex("""(\d{1,3}(?:,\d{3})*|\d+)(?:원)?""")

    // 💵 Total Amount Keywords
    private val totalKeywords = listOf(
        "합계", "총액", "결제금액", "받을금액", "결제", "지불금액",
        "total", "amount", "pay", "payment"
    )

    // 🏪 Store Name Pattern (Search in the first 1-3 lines)
    private val storeNamePattern = Regex("""^[가-힣a-zA-Z0-9\s&.-]{2,30}$""")

    // 📞 Phone Number Pattern
    private val phonePattern = Regex("""0\d{1,2}-?\d{3,4}-?\d{4}""")

    // 🏢 Business Registration Number Pattern
    private val businessNumberPattern = Regex("""\d{3}-?\d{2}-?\d{5}""")

    // 📍 Address Pattern
    private val addressPattern = Regex("""[가-힣]+[시도]\s?[가-힣]+[시군구]\s?[가-힣\s\d-]+""")

    // 📅 Date Patterns
    private val datePatterns = listOf(
        Regex("""(\d{4})[/-](\d{1,2})[/-](\d{1,2})"""),  // 2024-11-19, 2024/11/19
        Regex("""(\d{4})\.(\d{1,2})\.(\d{1,2})"""),      // 2024.11.19
        Regex("""(\d{2})[/-](\d{1,2})[/-](\d{1,2})""")   // 24-11-19
    )

    // ⏰ Time Pattern
    private val timePattern = Regex("""(\d{1,2}):(\d{2})(?::(\d{2}))?""")

    // 💳 Card Number Pattern (Masking)
    private val cardPattern = Regex("""카드\s*[:\-]?\s*\*+(\d{4})""")

    // 🧾 Approval Number Pattern
    private val approvalPattern = Regex("""승인[번호]*\s*[:\-]?\s*(\d{8,})""")

    // 📦 Item Line Patterns
    // Pattern A: [Item Name] [Quantity] [Unit Price] [Total Price]
    private val itemPatternA = Regex(
        """^(.+?)\s+(\d+)\s+${moneyPattern.pattern}\s+${moneyPattern.pattern}$"""
    )

    // Pattern B: [Item Name] [Amount]
    private val itemPatternB = Regex(
        """^(.+?)\s+${moneyPattern.pattern}$"""
    )

    // Pattern C: [Item Name] x [Quantity] [Amount]
    private val itemPatternC = Regex(
        """^(.+?)\s*[xX*×]\s*(\d+)\s+${moneyPattern.pattern}$"""
    )

    // Keywords to exclude lines
    private val excludeKeywords = listOf(
        "전화", "tel", "사업자", "주소", "address", "대표", "고객센터",
        "영수증", "receipt", "감사합니다", "thank", "방문", "visit",
        "카드", "card", "현금", "cash", "승인", "일시", "date", "time"
    )

    /**
     * Parses the receipt text and returns structured data.
     */
    fun parse(rawText: String): ParsedReceiptData {
        Log.d(TAG, "========== Starting Receipt Parsing ==========")
        Log.d(TAG, "Raw Text:\n$rawText")

        // Preprocessing
        val cleanedText = preprocessText(rawText)
        val lines = cleanedText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        Log.d(TAG, "Number of lines after preprocessing: ${lines.size}")

        // Extracting information
        val storeName = extractStoreName(lines)
        val phoneNumber = extractPhoneNumber(lines)
        val businessNumber = extractBusinessNumber(lines)
        val address = extractAddress(lines)
        val dateTime = extractDateTime(lines)
        val totalAmount = extractTotalAmount(lines)
        val paymentInfo = extractPaymentInfo(lines)
        val items = extractItems(lines, totalAmount)

        // Suggest Category
        val suggestedCategory = suggestCategory(storeName, items)

        Log.d(TAG, "========== Parsing Result ==========")
        Log.d(TAG, "Store Name: $storeName")
        Log.d(TAG, "Total Amount: $totalAmount")
        Log.d(TAG, "Number of Items: ${items.size}")
        Log.d(TAG, "Suggested Category: $suggestedCategory")

        return ParsedReceiptData(
            storeName = storeName,
            storePhone = phoneNumber,
            businessNumber = businessNumber,
            storeAddress = address,
            transactionDate = dateTime.first,
            transactionTime = dateTime.second,
            totalAmount = totalAmount,
            paymentMethod = paymentInfo.first,
            cardNumber = paymentInfo.second,
            approvalNumber = paymentInfo.third,
            items = items,
            suggestedCategory = suggestedCategory,
            rawText = rawText
        )
    }

    /**
     * Text Preprocessing
     */
    private fun preprocessText(text: String): String {
        return text
            .replace("￦", "")
            .replace("₩", "")
            .replace("원", "")
            .replace(Regex("""\s{2,}"""), " ")  // Replace multiple spaces with a single one
            .trim()
    }

    /**
     * Extracts store name (from the first 3 lines)
     */
    private fun extractStoreName(lines: List<String>): String? {
        return lines.take(3).firstOrNull { line ->
            // Check if it's not a phone number, address, or business number, and has a suitable length
            !phonePattern.containsMatchIn(line) &&
                    !businessNumberPattern.containsMatchIn(line) &&
                    !addressPattern.containsMatchIn(line) &&
                    line.length in 2..30 &&
                    storeNamePattern.matches(line)
        }
    }

    /**
     * Extracts phone number
     */
    private fun extractPhoneNumber(lines: List<String>): String? {
        return lines.firstNotNullOfOrNull { line ->
            phonePattern.find(line)?.value
        }
    }

    /**
     * Extracts business registration number
     */
    private fun extractBusinessNumber(lines: List<String>): String? {
        return lines.firstNotNullOfOrNull { line ->
            businessNumberPattern.find(line)?.value
        }
    }

    /**
     * Extracts address
     */
    private fun extractAddress(lines: List<String>): String? {
        return lines.firstNotNullOfOrNull { line ->
            addressPattern.find(line)?.value
        }
    }

    /**
     * Extracts date/time
     */
    private fun extractDateTime(lines: List<String>): Pair<Date?, String?> {
        var date: Date? = null
        var time: String? = null

        for (line in lines) {
            // Find Date
            if (date == null) {
                for (pattern in datePatterns) {
                    val match = pattern.find(line)
                    if (match != null) {
                        try {
                            val year = match.groupValues[1].toInt()
                            val month = match.groupValues[2].toInt()
                            val day = match.groupValues[3].toInt()

                            val actualYear = if (year < 100) 2000 + year else year

                            val calendar = Calendar.getInstance()
                            calendar.set(actualYear, month - 1, day)
                            date = calendar.time
                            break
                        } catch (e: Exception) {
                            Log.e(TAG, "Date parsing failed", e)
                        }
                    }
                }
            }

            // Find Time
            if (time == null) {
                val timeMatch = timePattern.find(line)
                if (timeMatch != null) {
                    time = timeMatch.value
                }
            }

            if (date != null && time != null) break
        }

        return Pair(date, time)
    }

    /**
     * Extracts total amount
     */
    private fun extractTotalAmount(lines: List<String>): Int? {
        // Find lines containing total amount keywords
        for (line in lines) {
            val lowerLine = line.lowercase()
            val hasKeyword = totalKeywords.any { keyword ->
                lowerLine.contains(keyword.lowercase())
            }

            if (hasKeyword) {
                // Extract amount from that line
                val amounts = extractMoneyFromLine(line)
                if (amounts.isNotEmpty()) {
                    Log.d(TAG, "Total amount found: $line -> ${amounts.maxOrNull()}")
                    return amounts.maxOrNull()  // Return the largest amount
                }
            }
        }

        // If no keyword found, search the largest amount in the last part
        val lastLines = lines.takeLast(10)
        val allAmounts = lastLines.flatMap { extractMoneyFromLine(it) }
        return allAmounts.maxOrNull()
    }

    /**
     * Extracts payment information (method, card number, approval number)
     */
    private fun extractPaymentInfo(lines: List<String>): Triple<String?, String?, String?> {
        var paymentMethod: String? = null
        var cardNumber: String? = null
        var approvalNumber: String? = null

        for (line in lines) {
            val lowerLine = line.lowercase()

            // Payment Method
            if (paymentMethod == null) {
                when {
                    lowerLine.contains("카드") || lowerLine.contains("card") -> paymentMethod = "card"
                    lowerLine.contains("현금") || lowerLine.contains("cash") -> paymentMethod = "cash"
                    lowerLine.contains("계좌") || lowerLine.contains("이체") -> paymentMethod = "transfer"
                }
            }

            // Card Number
            if (cardNumber == null) {
                cardPattern.find(line)?.let {
                    cardNumber = "**** " + it.groupValues[1]
                }
            }

            // Approval Number
            if (approvalNumber == null) {
                approvalPattern.find(line)?.let {
                    approvalNumber = it.groupValues[1]
                }
            }
        }

        return Triple(paymentMethod, cardNumber, approvalNumber)
    }

    /**
     * Extracts list of items
     */
    private fun extractItems(lines: List<String>, totalAmount: Int?): List<ReceiptItem> {
        val items = mutableListOf<ReceiptItem>()

        for (line in lines) {
            // Check exclusion keywords
            if (shouldExcludeLine(line)) continue

            // Attempt pattern matching
            var matched = false

            // Pattern A: [Item Name] [Quantity] [Unit Price] [Total Price]
            itemPatternA.matchEntire(line)?.let { match ->
                try {
                    val name = match.groupValues[1].trim()
                    val quantity = match.groupValues[2].toInt()
                    val unitPrice = parseMoneyString(match.groupValues[3])
                    val totalPrice = parseMoneyString(match.groupValues[4])

                    items.add(ReceiptItem(name, quantity, unitPrice, totalPrice))
                    matched = true
                } catch (e: Exception) {
                    Log.e(TAG, "Pattern A parsing failed: $line", e)
                }
            }

            if (matched) continue

            // Pattern C: [Item Name] x [Quantity] [Total Price]
            itemPatternC.matchEntire(line)?.let { match ->
                try {
                    val name = match.groupValues[1].trim()
                    val quantity = match.groupValues[2].toInt()
                    val totalPrice = parseMoneyString(match.groupValues[3])
                    val unitPrice = if (quantity > 0) totalPrice / quantity else null

                    items.add(ReceiptItem(name, quantity, unitPrice, totalPrice))
                    matched = true
                } catch (e: Exception) {
                    Log.e(TAG, "Pattern C parsing failed: $line", e)
                }
            }

            if (matched) continue

            // Pattern B: [Item Name] [Amount]
            itemPatternB.matchEntire(line)?.let { match ->
                try {
                    val name = match.groupValues[1].trim()
                    val totalPrice = parseMoneyString(match.groupValues[2])

                    // Exclude if item name is too short or just numbers
                    if (name.length >= 2 && !name.matches(Regex("""\d+"""))) {
                        items.add(ReceiptItem(name, 1, totalPrice, totalPrice))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Pattern B parsing failed: $line", e)
                }
            }
        }

        // Warning if total amount and item sum differ significantly
        if (totalAmount != null && items.isNotEmpty()) {
            val itemSum = items.sumOf { it.totalPrice }
            val diff = kotlin.math.abs(totalAmount - itemSum)
            if (diff > totalAmount * 0.1) {  // More than 10% difference
                Log.w(TAG, "⚠️ Total amount ($totalAmount) and item sum ($itemSum) differ by $diff")
            }
        }

        return items
    }

    /**
     * Determines whether a line should be excluded
     */
    private fun shouldExcludeLine(line: String): Boolean {
        val lowerLine = line.lowercase()
        return excludeKeywords.any { lowerLine.contains(it) }
    }

    /**
     * Extracts all amounts from a single line
     */
    private fun extractMoneyFromLine(line: String): List<Int> {
        return moneyPattern.findAll(line)
            .mapNotNull { parseMoneyString(it.value) }
            .toList()
    }

    /**
     * Converts a money string to Int
     */
    private fun parseMoneyString(str: String): Int {
        return str.replace(",", "")
            .replace("원", "")
            .toInt()
    }

    /**
     * Suggests a category
     */
    private fun suggestCategory(storeName: String?, items: List<ReceiptItem>): String {
        val name = storeName?.lowercase() ?: ""

        // Store name based
        return when {
            name.contains("스타벅스") || name.contains("카페") || name.contains("coffee") ||
                    name.contains("gs25") || name.contains("cu") || name.contains("편의점") ||
                    name.contains("맥도날드") || name.contains("버거킹") || name.contains("롯데리아") -> "food"

            name.contains("지하철") || name.contains("버스") || name.contains("택시") ||
                    name.contains("주유") || name.contains("oil") -> "transport"

            name.contains("쿠팡") || name.contains("마켓") || name.contains("mart") ||
                    name.contains("이마트") || name.contains("홈플러스") -> "shopping"

            else -> {
                // Item based
                val itemNames = items.joinToString(" ") { it.name.lowercase() }
                when {
                    itemNames.contains("커피") || itemNames.contains("라떼") ||
                            itemNames.contains("음료") || itemNames.contains("김밥") -> "food"

                    itemNames.contains("휘발유") || itemNames.contains("경유") -> "transport"
                    else -> "others"
                }
            }
        }
    }
}

/**
 * Parsed Receipt Data
 */
data class ParsedReceiptData(
    val storeName: String?,
    val storePhone: String?,
    val businessNumber: String?,
    val storeAddress: String?,
    val transactionDate: Date?,
    val transactionTime: String?,
    val totalAmount: Int?,
    val paymentMethod: String?,
    val cardNumber: String?,
    val approvalNumber: String?,
    val items: List<ReceiptItem>,
    val suggestedCategory: String,
    val rawText: String
)

/**
 * Parsed Item
 */
data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Int?,
    val totalPrice: Int
)