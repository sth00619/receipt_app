package com.example.receiptify.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProfileImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profileImage: ProfileImageEntity)

    @Query("SELECT * FROM profile_images WHERE userId = :userId")
    suspend fun getProfileImage(userId: String): ProfileImageEntity?

    @Query("DELETE FROM profile_images WHERE userId = :userId")
    suspend fun deleteProfileImage(userId: String): Int
}
