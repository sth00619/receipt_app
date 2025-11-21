package com.example.receiptify.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ReceiptImageEntity)

    @Query("SELECT * FROM receipt_images WHERE receiptId = :receiptId")
    suspend fun getImageById(receiptId: String): ReceiptImageEntity?

    @Query("SELECT * FROM receipt_images WHERE receiptId = :receiptId")
    fun getImageByIdFlow(receiptId: String): Flow<ReceiptImageEntity?>

    @Query("SELECT * FROM receipt_images ORDER BY createdAt DESC")
    fun getAllImages(): Flow<List<ReceiptImageEntity>>

    @Query("DELETE FROM receipt_images WHERE receiptId = :receiptId")
    suspend fun deleteImage(receiptId: String)

    @Query("DELETE FROM receipt_images")
    suspend fun deleteAllImages()

    @Query("SELECT COUNT(*) FROM receipt_images")
    suspend fun getImageCount(): Int
}