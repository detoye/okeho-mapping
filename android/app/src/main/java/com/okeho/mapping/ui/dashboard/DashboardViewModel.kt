package com.okeho.mapping.ui.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okeho.mapping.data.remote.SupabaseClient
import com.okeho.mapping.data.remote.CaptureDto
import com.okeho.mapping.data.remote.StreetDto
import com.okeho.mapping.domain.model.SyncStatus
import com.okeho.mapping.domain.repository.CaptureRepository
import com.okeho.mapping.domain.repository.StreetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SyncResult(val message: String, val success: Boolean)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val captureRepository: CaptureRepository,
    private val streetRepository: StreetRepository
) : ViewModel() {

    private val _syncState = MutableStateFlow<String?>(null)
    val syncState: StateFlow<String?> = _syncState.asStateFlow()

    fun sync(onDone: (String) -> Unit) {
        viewModelScope.launch {
            _syncState.value = "Syncing..."
            try {
                withContext(Dispatchers.IO) {
                    val pendingCaptures = captureRepository.getPendingCaptures()
                    Log.d("Sync", "Pending captures: ${pendingCaptures.size}")
                    pendingCaptures.forEach { Log.d("Sync", "  id=${it.id} name=${it.name} status=${it.syncStatus}") }

                    Log.d("Sync", "Pending streets: ${pendingStreets.size}")
                    var syncedCaptures = 0
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
                            SupabaseClient.getClient().from("captures").insert(dto)
                            captureRepository.updateSyncStatus(capture.id, SyncStatus.SYNCED.name)
                            syncedCaptures++
                        } catch (e: Exception) {
                            Log.e("Sync", "Failed to sync capture ${capture.id}", e)
                            captureRepository.updateSyncStatus(capture.id, SyncStatus.FAILED.name)
                        }
                    }

                    val pendingStreets = streetRepository.getPendingStreets()
                    var syncedStreets = 0
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
                            SupabaseClient.getClient().from("streets").insert(dto)
                            streetRepository.updateSyncStatus(street.id, SyncStatus.SYNCED.name)
                            syncedStreets++
                        } catch (e: Exception) {
                            Log.e("Sync", "Failed to sync street ${street.id}", e)
                            streetRepository.updateSyncStatus(street.id, SyncStatus.FAILED.name)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        val msg = "Synced $syncedCaptures captures, $syncedStreets streets"
                        _syncState.value = msg
                        onDone(msg)
                    }
                }
            } catch (e: Exception) {
                Log.e("Sync", "Sync failed", e)
                withContext(Dispatchers.Main) {
                    val msg = "Sync failed: ${e.message}"
                    _syncState.value = msg
                    onDone(msg)
                }
            }
        }
    }
}
