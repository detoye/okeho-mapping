package com.okeho.mapping.ui.capture

import android.content.Context
import android.location.Location
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.okeho.mapping.domain.model.Capture
import com.okeho.mapping.domain.model.FeatureType
import com.okeho.mapping.domain.model.SyncStatus
import com.okeho.mapping.domain.repository.CaptureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import javax.inject.Inject

data class CaptureUiState(
    val isCapturing: Boolean = false,
    val accuracy: Float = 0f,
    val samples: List<Location> = emptyList(),
    val bestLocation: Location? = null,
    val accuracyStatus: AccuracyStatus = AccuracyStatus.WAITING,
    val name: String = "",
    val photoUri: Uri? = null,
    val ocrText: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

enum class AccuracyStatus {
    WAITING, SAMPLING, GOOD, POOR
}

@HiltViewModel
class CaptureDetailsViewModel @Inject constructor(
    private val captureRepository: CaptureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private val locationSamples = mutableListOf<Location>()

    fun startGpsCapture(context: Context) {
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)

        _uiState.value = _uiState.value.copy(
            isCapturing = true,
            accuracyStatus = AccuracyStatus.SAMPLING,
            samples = emptyList(),
            bestLocation = null
        )

        locationSamples.clear()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    locationSamples.add(location)
                    _uiState.value = _uiState.value.copy(
                        accuracy = location.accuracy,
                        samples = locationSamples.toList()
                    )

                    if (locationSamples.size >= 5) {
                        stopGpsCapture()
                    }
                }
            }
        }

        try {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                context.mainLooper
            )
        } catch (e: SecurityException) {
            _uiState.value = _uiState.value.copy(
                error = "Location permission not granted",
                isCapturing = false,
                accuracyStatus = AccuracyStatus.WAITING
            )
        }
    }

    fun stopGpsCapture() {
        locationCallback?.let { callback ->
            fusedLocationClient?.removeLocationUpdates(callback)
        }

        if (locationSamples.isNotEmpty()) {
            val bestLocation = locationSamples.minByOrNull { it.accuracy }
            val bestAccuracy = bestLocation?.accuracy ?: 0f
            val status = if (bestAccuracy <= 15f) AccuracyStatus.GOOD else AccuracyStatus.POOR

            _uiState.value = _uiState.value.copy(
                isCapturing = false,
                bestLocation = bestLocation,
                accuracy = bestAccuracy,
                accuracyStatus = status
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isCapturing = false,
                accuracyStatus = AccuracyStatus.WAITING,
                error = "No location samples received"
            )
        }
    }

    fun retryGpsCapture(context: Context) {
        _uiState.value = _uiState.value.copy(
            accuracyStatus = AccuracyStatus.WAITING,
            bestLocation = null,
            accuracy = 0f,
            samples = emptyList()
        )
        startGpsCapture(context)
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun setPhotoUri(uri: Uri) {
        _uiState.value = _uiState.value.copy(photoUri = uri)
    }

    fun extractTextFromImage(context: Context, uri: Uri) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val extractedText = visionText.textBlocks.joinToString("\n") { it.text }
                    _uiState.value = _uiState.value.copy(ocrText = extractedText)
                }
                .addOnFailureListener { e ->
                    _uiState.value = _uiState.value.copy(error = "OCR failed: ${e.message}")
                }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Failed to process image: ${e.message}")
        }
    }

    fun useOcrText() {
        _uiState.value.ocrText?.let { text ->
            _uiState.value = _uiState.value.copy(name = text)
        }
    }

    fun saveCapture(featureType: String) {
        val state = _uiState.value
        val location = state.bestLocation ?: return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)

            try {
                val capture = Capture(
                    id = UUID.randomUUID().toString(),
                    name = state.name,
                    featureType = FeatureType.fromString(featureType),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    photoUrl = state.photoUri?.toString(),
                    ocrText = state.ocrText,
                    syncStatus = SyncStatus.PENDING
                )

                captureRepository.insertCapture(capture)
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save: ${e.message}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationCallback?.let { callback ->
            fusedLocationClient?.removeLocationUpdates(callback)
        }
    }
}
