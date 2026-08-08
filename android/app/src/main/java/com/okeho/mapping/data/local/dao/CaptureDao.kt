package com.okeho.mapping.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.okeho.mapping.data.local.entity.CaptureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {
    @Query("SELECT * FROM captures ORDER BY createdAt DESC")
    fun getAllCaptures(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun getCaptureById(id: String): CaptureEntity?

    @Query("SELECT * FROM captures WHERE syncStatus = 'PENDING'")
    suspend fun getPendingCaptures(): List<CaptureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapture(capture: CaptureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaptures(captures: List<CaptureEntity>)

    @Update
    suspend fun updateCapture(capture: CaptureEntity)

    @Delete
    suspend fun deleteCapture(capture: CaptureEntity)

    @Query("UPDATE captures SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
