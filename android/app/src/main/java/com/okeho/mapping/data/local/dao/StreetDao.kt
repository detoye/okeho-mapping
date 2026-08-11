package com.okeho.mapping.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.okeho.mapping.data.local.entity.StreetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreetDao {
    @Query("SELECT * FROM streets ORDER BY createdAt DESC")
    fun getAllStreets(): Flow<List<StreetEntity>>

    @Query("SELECT * FROM streets WHERE id = :id")
    suspend fun getStreetById(id: String): StreetEntity?

    @Query("SELECT * FROM streets WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun getPendingStreets(): List<StreetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreet(street: StreetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreets(streets: List<StreetEntity>)

    @Update
    suspend fun updateStreet(street: StreetEntity)

    @Delete
    suspend fun deleteStreet(street: StreetEntity)

    @Query("UPDATE streets SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("SELECT COUNT(*) FROM streets WHERE syncStatus IN ('PENDING', 'FAILED')")
    suspend fun countUnsynced(): Int
}
