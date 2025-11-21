package com.example.receiptify.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipt_images")
data class ReceiptImageEntity(
    @PrimaryKey
    val receiptId: String,  // MongoDB의 Receipt ID와 매칭
    val imagePath: String,  // 로컬 파일 경로
    val imageData: ByteArray?,  // 이미지 바이트 데이터 (선택)
    val thumbnailData: ByteArray?,  // 썸네일
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReceiptImageEntity

        if (receiptId != other.receiptId) return false
        if (imagePath != other.imagePath) return false
        if (imageData != null) {
            if (other.imageData == null) return false
            if (!imageData.contentEquals(other.imageData)) return false
        } else if (other.imageData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = receiptId.hashCode()
        result = 31 * result + imagePath.hashCode()
        result = 31 * result + (imageData?.contentHashCode() ?: 0)
        return result
    }
}