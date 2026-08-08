package com.okeho.mapping.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.okeho.mapping.data.remote.SupabaseClient
import com.okeho.mapping.domain.model.SyncStatus
import com.okeho.mapping.domain.repository.CaptureRepository
import com.okeho.mapping.domain.repository.StreetRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class CaptureDto(
    val id: String,
    val user_id: String,
    val name: String,
    val feature_type: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val photo_url: String? = null,
    val ocr_text: String? = null,
    val sync_status: String = "synced"
)

@Serializable
data class StreetDto(
    val id: String,
    val user_id: String,
    val name: String,
    val surface_type: String,
    val traffic_direction: String,
    val points_captured: Int,
    val sync_status: String = "synced"
)

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val captureRepository: CaptureRepository,
    private val streetRepository: StreetRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val capturesSynced = syncCaptures()
            val streetsSynced = syncStreets()
            Log.d("SyncWorker", "Sync complete: $capturesSynced captures, $streetsSynced streets")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            Result.retry()
        }
    }

    private suspend fun syncCaptures(): Int {
        val pendingCaptures = captureRepository.getPendingCaptures()
        var synced = 0

        for (capture in pendingCaptures) {
            try {
                val dto = CaptureDto(
                    id = capture.id,
                    user_id = capture.userId.ifBlank { "anonymous" },
                    name = capture.name,
                    feature_type = capture.featureType.name,
                    latitude = capture.latitude,
                    longitude = capture.longitude,
                    accuracy = capture.accuracy,
                    photo_url = capture.photoUrl,
                    ocr_text = capture.ocrText,
                    sync_status = "synced"
                )

                SupabaseClient.getClient()
                    .from("captures")
                    .upsert(dto)

                captureRepository.updateSyncStatus(capture.id, SyncStatus.SYNCED.name)
                synced++
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to sync capture ${capture.id}", e)
                captureRepository.updateSyncStatus(capture.id, SyncStatus.FAILED.name)
            }
        }
        return synced
    }

    private suspend fun syncStreets(): Int {
        val pendingStreets = streetRepository.getPendingStreets()
        var synced = 0

        for (street in pendingStreets) {
            try {
                val dto = StreetDto(
                    id = street.id,
                    user_id = street.userId.ifBlank { "anonymous" },
                    name = street.name,
                    surface_type = street.surfaceType.name,
                    traffic_direction = street.trafficDirection.name,
                    points_captured = street.points.size,
                    sync_status = "synced"
                )

                SupabaseClient.getClient()
                    .from("streets")
                    .upsert(dto)

                streetRepository.updateSyncStatus(street.id, SyncStatus.SYNCED.name)
                synced++
            } catch (e: Exception) {
                Log.e("SyncWorker", "Failed to sync street ${street.id}", e)
                streetRepository.updateSyncStatus(street.id, SyncStatus.FAILED.name)
            }
        }
        return synced
    }
}
