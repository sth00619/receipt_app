package com.example.receiptify.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.receiptify.database.AppDatabase
import com.example.receiptify.database.ReceiptImageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class ReceiptImageRepository(context: Context) {

    private val dao = AppDatabase.getDatabase(context).receiptImageDao()
    private val context = context.applicationContext

    companion object {
        private const val TAG = "ReceiptImageRepository"
        private const val MAX_IMAGE_SIZE = 1024 * 1024 * 2  // 2MB
    }

    /**
     * 영수증 이미지 저장
     */
    suspend fun saveImage(receiptId: String, imageUri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 이미지를 로컬 파일로 저장
                val imagePath = saveImageToFile(receiptId, imageUri)

                // 2. 썸네일 생성
                val thumbnailData = createThumbnail(imageUri)

                // 3. Room DB에 저장
                val entity = ReceiptImageEntity(
                    receiptId = receiptId,
                    imagePath = imagePath,
                    imageData = null,  // 파일 경로만 저장
                    thumbnailData = thumbnailData
                )

                dao.insertImage(entity)

                Log.d(TAG, "✅ 이미지 저장 완료: $receiptId")
                Result.success(imagePath)

            } catch (e: Exception) {
                Log.e(TAG, "❌ 이미지 저장 실패", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 영수증 이미지 조회
     */
    suspend fun getImage(receiptId: String): ReceiptImageEntity? {
        return withContext(Dispatchers.IO) {
            dao.getImageById(receiptId)
        }
    }

    /**
     * 영수증 이미지 Flow로 조회
     */
    fun getImageFlow(receiptId: String): Flow<ReceiptImageEntity?> {
        return dao.getImageByIdFlow(receiptId)
    }

    /**
     * 모든 이미지 조회
     */
    fun getAllImages(): Flow<List<ReceiptImageEntity>> {
        return dao.getAllImages()
    }

    /**
     * 영수증 이미지 삭제
     */
    suspend fun deleteImage(receiptId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val entity = dao.getImageById(receiptId)
                entity?.let {
                    // 파일 삭제
                    val file = File(it.imagePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }

                dao.deleteImage(receiptId)
                Log.d(TAG, "✅ 이미지 삭제 완료: $receiptId")
                Result.success(Unit)

            } catch (e: Exception) {
                Log.e(TAG, "❌ 이미지 삭제 실패", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 이미지를 파일로 저장
     */
    private fun saveImageToFile(receiptId: String, imageUri: Uri): String {
        val dir = File(context.filesDir, "receipt_images")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val file = File(dir, "$receiptId.jpg")

        context.contentResolver.openInputStream(imageUri)?.use { input ->
            // 이미지 크기 조정
            val bitmap = BitmapFactory.decodeStream(input)
            val resizedBitmap = resizeBitmap(bitmap, 1920, 1920)

            file.outputStream().use { output ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            }

            resizedBitmap.recycle()
            bitmap.recycle()
        }

        return file.absolutePath
    }

    /**
     * 썸네일 생성
     */
    private fun createThumbnail(imageUri: Uri): ByteArray {
        val bitmap = context.contentResolver.openInputStream(imageUri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: throw IllegalArgumentException("Cannot decode image")

        val thumbnail = resizeBitmap(bitmap, 200, 200)

        val outputStream = ByteArrayOutputStream()
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)

        thumbnail.recycle()
        bitmap.recycle()

        return outputStream.toByteArray()
    }

    /**
     * Bitmap 크기 조정
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratio = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 저장된 이미지 개수 조회
     */
    suspend fun getImageCount(): Int {
        return withContext(Dispatchers.IO) {
            dao.getImageCount()
        }
    }
}