package com.okeho.mapping.domain.repository

import com.okeho.mapping.domain.model.Capture
import kotlinx.coroutines.flow.Flow

interface CaptureRepository {
    fun getAllCaptures(): Flow<List<Capture>>
    suspend fun getCaptureById(id: String): Capture?
    suspend fun insertCapture(capture: Capture)
    suspend fun updateCapture(capture: Capture)
    suspend fun deleteCapture(capture: Capture)
    suspend fun getPendingCaptures(): List<Capture>
    suspend fun updateSyncStatus(id: String, status: String)
}
