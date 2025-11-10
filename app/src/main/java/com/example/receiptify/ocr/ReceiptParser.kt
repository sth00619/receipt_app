package com.example.receiptify.ocr
// OCR 결과 텍스트에서 각 품목과 총액을 추출하는 데이터 파서

// 품목, 수량, 단가, 금액
data class ParsedItem(val name: String, val qty: Int, val unitPrice: Int?, val amount: Int)

// 상점명, 총 결제 금액, 품목 리스트
data class ParsedReceipt(
    val storeName: String?, val totalAmount: Int?, val items: List<ParsedItem>
)

object ReceiptParser {
    // 💰 금액 형태 인식용 정규식 (예: 1,000 / 15000)
    private val money = Regex("""(\d{1,3}(?:,\d{3})*|\d+)""")

    // 💵 총액 라인 탐지 (결제금액 / 총액 / 합계 등)
    private val totalRegex = Regex("""(결제금액|총액|합계)\s*[:\-]?\s*${money.pattern}""")

    // 📦 품목 라인 패턴 A: [이름] [수량] [단가] [금액]
    private val pA = Regex("""^(.+?)\s+(\d+)\s+${money.pattern}\s+${money.pattern}$""")

    // 📦 품목 라인 패턴 B: [이름] [금액]
    private val pB = Regex("""^(.+?)\s+${money.pattern}$""")


    //OCR 텍스트를 파싱하여 영수증 정보를 추출한다.
    fun parse(raw: String): ParsedReceipt {
        // 불필요한 공백 제거 및 라인 정리
        val text = raw.replace("￦","원").replace(Regex("[ ]{2,}")," ").trim()
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 상점명 추정: 처음 3줄 중 전화/주소/사업자 등이 아닌 줄
        val store = lines.take(3).firstOrNull {
            it.length in 2..30 && !it.contains(Regex("전화|TEL|주소|사업자|승인|카드"))
        }

        // 품목 라인 매칭 (패턴 A → 패턴 B 순으로 검사)
        val items = lines.mapNotNull { ln ->
            pA.matchEntire(ln)?.let {
                ParsedItem(
                    name = it.groupValues[1].trim(),
                    qty = it.groupValues[2].toInt(),
                    unitPrice = it.groupValues[3].replace(",","").toInt(),
                    amount = it.groupValues[4].replace(",","").toInt()
                )
            } ?: pB.matchEntire(ln)?.let {
                val amt = it.groupValues[2].replace(",","").toInt()
                ParsedItem(it.groupValues[1].trim(), 1, amt, amt)
            }
        }

        // 총액 추출 (결제금액|합계 등)
        val total = totalRegex.find(text)?.groupValues?.lastOrNull()
            ?.replace(",","")?.toIntOrNull()

        // 파싱 결과 반환
        return ParsedReceipt(store, total, items)
    }
}
