package com.example.receiptify.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.receiptify.database.AppDatabase
import com.example.receiptify.database.ProfileImageEntity
import java.io.ByteArrayOutputStream

class ProfileImageRepository(context: Context) {

    private val profileImageDao = AppDatabase.getDatabase(context).profileImageDao()

    companion object {
        private const val TAG = "ProfileImageRepository"
        private const val MAX_IMAGE_SIZE = 500 // Max width/height in pixels
        private const val COMPRESSION_QUALITY = 80
    }

    /**
     * Save profile image to database
     */
    suspend fun saveProfileImage(userId: String, bitmap: Bitmap): Result<Unit> {
        return try {
            // Compress and resize image
            val compressedBitmap = compressImage(bitmap)
            val byteArray = bitmapToByteArray(compressedBitmap)

            val entity = ProfileImageEntity(
                userId = userId,
                imageData = byteArray,
                updatedAt = System.currentTimeMillis()
            )

            profileImageDao.insertOrUpdate(entity)
            Log.d(TAG, "✅ Profile image saved for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save profile image", e)
            Result.failure(e)
        }
    }

    /**
     * Get profile image from database
     */
    suspend fun getProfileImage(userId: String): Bitmap? {
        return try {
            val entity = profileImageDao.getProfileImage(userId)
            entity?.let {
                val bitmap = byteArrayToBitmap(it.imageData)
                Log.d(TAG, "✅ Profile image loaded for user: $userId")
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load profile image", e)
            null
        }
    }

    /**
     * Delete profile image from database
     */
    suspend fun deleteProfileImage(userId: String): Result<Unit> {
        return try {
            profileImageDao.deleteProfileImage(userId)
            Log.d(TAG, "✅ Profile image deleted for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete profile image", e)
            Result.failure(e)
        }
    }

    /**
     * Compress image to reduce size
     */
    private fun compressImage(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate scale factor
        val scale = if (width > height) {
            MAX_IMAGE_SIZE.toFloat() / width
        } else {
            MAX_IMAGE_SIZE.toFloat() / height
        }

        // Only scale down if image is larger than max size
        return if (scale < 1.0f) {
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    /**
     * Convert bitmap to byte array
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, stream)
        return stream.toByteArray()
    }

    /**
     * Convert byte array to bitmap
     */
    private fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}
