package com.okeho.mapping.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okeho.mapping.data.remote.AuthManager
import com.okeho.mapping.data.remote.CaptureDto
import com.okeho.mapping.data.remote.PhotoUploader
import com.okeho.mapping.data.remote.StreetDto
import com.okeho.mapping.domain.model.SyncStatus
import com.okeho.mapping.domain.repository.CaptureRepository
import com.okeho.mapping.domain.repository.StreetRepository
import com.okeho.mapping.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

data class SyncResult(val message: String, val success: Boolean)

data class AccountState(
    val name: String? = null,
    val email: String? = null,
    val pendingCount: Int = 0,
    val isSigningOut: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val captureRepository: CaptureRepository,
    private val streetRepository: StreetRepository,
    private val client: SupabaseClient,
    private val authManager: AuthManager,
    private val photoUploader: PhotoUploader,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _syncState = MutableStateFlow<String?>(null)
    val syncState: StateFlow<String?> = _syncState.asStateFlow()

    private val _accountState = MutableStateFlow(AccountState())
    val accountState: StateFlow<AccountState> = _accountState.asStateFlow()

    /** Refreshes the drawer header. Called when the drawer opens. */
    fun refreshAccount() {
        viewModelScope.launch {
            val pending = captureRepository.getPendingCaptures().size +
                streetRepository.getPendingStreets().size
            _accountState.value = _accountState.value.copy(
                name = authManager.currentUserName,
                email = authManager.currentUserEmail,
                pendingCount = pending
            )
        }
    }

    /**
     * Signs out and wipes local data via [SignOutUseCase]. The auth gate in
     * MainActivity swaps to the login graph off sessionStatus, so there is no
     * navigation to do here.
     */
    fun signOut() {
        viewModelScope.launch {
            _accountState.value = _accountState.value.copy(isSigningOut = true)
            signOutUseCase().onFailure {
                Log.e("Auth", "Sign out failed", it)
                _accountState.value = _accountState.value.copy(isSigningOut = false)
            }
        }
    }

    fun sync(onDone: (String) -> Unit) {
        viewModelScope.launch {
            _syncState.value = "Syncing..."
            try {
                val msg = withContext(Dispatchers.IO) { runSync() }
                _syncState.value = msg
                onDone(msg)
            } catch (e: Exception) {
                Log.e("Sync", "Sync failed", e)
                val msg = "Sync failed: ${e.message}"
                _syncState.value = msg
                onDone(msg)
            }
        }
    }

    private suspend fun runSync(): String {
        val userId = authManager.currentUserId
            ?: return "Not signed in -- sign in to sync"

        val pendingCaptures = captureRepository.getPendingCaptures()
        Log.d("Sync", "Pending captures: ${pendingCaptures.size}")

        var syncedCaptures = 0
        var failed = 0
        for (capture in pendingCaptures) {
            try {
                // A device-local URI is meaningless to the server, so it is
                // uploaded and replaced by its object path. If that upload
                // fails the column goes null rather than keeping the URI: the
                // record itself is the valuable part and still syncs, and a
                // null reads honestly as "no photo on the server". Any other
                // value is already an object path from an earlier sync.
                val localPhoto = capture.photoUrl?.takeIf {
                    it.startsWith("content://") || it.startsWith("file://")
                }
                val photoPath = if (localPhoto != null) {
                    photoUploader.upload(localPhoto, capture.id, userId)
                } else {
                    capture.photoUrl
                }
                val dto = CaptureDto(
                    id = capture.id,
                    user_id = userId,
                    name = capture.name,
                    feature_type = capture.featureType.name.lowercase(),
                    geometry = pointWkt(capture.latitude, capture.longitude),
                    accuracy = capture.accuracy,
                    photo_url = photoPath,
                    ocr_text = capture.ocrText,
                    sync_status = "synced"
                )
                client.from("captures").upsert(dto)
                captureRepository.updateSyncStatus(capture.id, SyncStatus.SYNCED.name)
                syncedCaptures++
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync capture ${capture.id}", e)
                captureRepository.updateSyncStatus(capture.id, SyncStatus.FAILED.name)
                failed++
            }
        }

        val pendingStreets = streetRepository.getPendingStreets()
        Log.d("Sync", "Pending streets: ${pendingStreets.size}")

        var syncedStreets = 0
        for (street in pendingStreets) {
            // A LINESTRING needs at least two vertices; anything less can never sync.
            if (street.points.size < 2) {
                Log.w("Sync", "Skipping street ${street.id}: only ${street.points.size} point(s)")
                failed++
                continue
            }
            try {
                val dto = StreetDto(
                    id = street.id,
                    user_id = userId,
                    name = street.name,
                    geometry = lineWkt(street.points),
                    surface_type = street.surfaceType.name.lowercase(),
                    traffic_direction = street.trafficDirection.name.lowercase(),
                    points_captured = street.points.size,
                    sync_status = "synced"
                )
                client.from("streets").upsert(dto)
                streetRepository.updateSyncStatus(street.id, SyncStatus.SYNCED.name)
                syncedStreets++
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync street ${street.id}", e)
                streetRepository.updateSyncStatus(street.id, SyncStatus.FAILED.name)
                failed++
            }
        }

        return buildString {
            append("Synced $syncedCaptures captures, $syncedStreets streets")
            if (failed > 0) append(" ($failed failed)")
        }
    }

    // WKT axis order is X Y, so longitude comes before latitude.
    private fun pointWkt(latitude: Double, longitude: Double) =
        "SRID=4326;POINT(${coord(longitude)} ${coord(latitude)})"

    private fun lineWkt(points: List<Pair<Double, Double>>) =
        points.joinToString(",", prefix = "SRID=4326;LINESTRING(", postfix = ")") {
            "${coord(it.second)} ${coord(it.first)}"
        }

    /** Avoids the scientific notation Double.toString() emits for small magnitudes. */
    private fun coord(value: Double) = BigDecimal.valueOf(value).toPlainString()
}
