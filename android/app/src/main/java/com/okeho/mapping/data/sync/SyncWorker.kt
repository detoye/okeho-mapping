package com.okeho.mapping.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.okeho.mapping.domain.repository.CaptureRepository
import com.okeho.mapping.domain.repository.StreetRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val captureRepository: CaptureRepository,
    private val streetRepository: StreetRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            syncCaptures()
            syncStreets()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun syncCaptures() {
        val pendingCaptures = captureRepository.getPendingCaptures()
        for (capture in pendingCaptures) {
            try {
                // TODO: Upload to Supabase
                // supabase.from("captures").insert(captureDto)
                captureRepository.updateSyncStatus(capture.id, "SYNCED")
            } catch (e: Exception) {
                captureRepository.updateSyncStatus(capture.id, "FAILED")
            }
        }
    }

    private suspend fun syncStreets() {
        val pendingStreets = streetRepository.getPendingStreets()
        for (street in pendingStreets) {
            try {
                // TODO: Upload to Supabase
                // supabase.from("streets").insert(streetDto)
                streetRepository.updateSyncStatus(street.id, "SYNCED")
            } catch (e: Exception) {
                streetRepository.updateSyncStatus(street.id, "FAILED")
            }
        }
    }
}
