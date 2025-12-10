package com.example.receiptify.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile_images")
data class ProfileImageEntity(
    @PrimaryKey
    val userId: String,  // User ID as primary key
    val imageData: ByteArray,  // Profile image as byte array
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProfileImageEntity

        if (userId != other.userId) return false
        if (!imageData.contentEquals(other.imageData)) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = userId.hashCode()
        result = 31 * result + imageData.contentHashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
