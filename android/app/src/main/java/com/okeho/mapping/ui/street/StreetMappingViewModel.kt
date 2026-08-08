package com.okeho.mapping.ui.street

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.okeho.mapping.domain.model.Street
import com.okeho.mapping.domain.model.SurfaceType
import com.okeho.mapping.domain.model.TrafficDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class StreetMappingUiState(
    val isMapping: Boolean = false,
    val points: List<Pair<Double, Double>> = emptyList(),
    val currentAccuracy: Float = 0f,
    val streetName: String = "",
    val surfaceType: SurfaceType = SurfaceType.PAVED,
    val trafficDirection: TrafficDirection = TrafficDirection.TWO_WAY,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StreetMappingViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(StreetMappingUiState())
    val uiState: StateFlow<StreetMappingUiState> = _uiState.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    fun startMapping(context: Context) {
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)

        _uiState.value = _uiState.value.copy(isMapping = true)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    _uiState.value = _uiState.value.copy(
                        currentAccuracy = location.accuracy
                    )
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
                isMapping = false
            )
        }
    }

    fun addControlPoint(latitude: Double, longitude: Double) {
        val currentPoints = _uiState.value.points.toMutableList()
        currentPoints.add(Pair(latitude, longitude))
        _uiState.value = _uiState.value.copy(points = currentPoints)
    }

    fun addControlPointFromCurrentLocation(context: Context) {
        try {
            fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                location?.let {
                    addControlPoint(it.latitude, it.longitude)
                }
            }
        } catch (e: SecurityException) {
            _uiState.value = _uiState.value.copy(error = "Location permission not granted")
        }
    }

    fun removeLastPoint() {
        val currentPoints = _uiState.value.points.toMutableList()
        if (currentPoints.isNotEmpty()) {
            currentPoints.removeLast()
            _uiState.value = _uiState.value.copy(points = currentPoints)
        }
    }

    fun stopMapping() {
        locationCallback?.let { callback ->
            fusedLocationClient?.removeLocationUpdates(callback)
        }
        _uiState.value = _uiState.value.copy(isMapping = false)
    }

    fun updateStreetName(name: String) {
        _uiState.value = _uiState.value.copy(streetName = name)
    }

    fun updateSurfaceType(type: SurfaceType) {
        _uiState.value = _uiState.value.copy(surfaceType = type)
    }

    fun updateTrafficDirection(direction: TrafficDirection) {
        _uiState.value = _uiState.value.copy(trafficDirection = direction)
    }

    fun getStreet(): Street {
        val state = _uiState.value
        return Street(
            name = state.streetName,
            points = state.points,
            surfaceType = state.surfaceType,
            trafficDirection = state.trafficDirection
        )
    }

    fun markSaved() {
        _uiState.value = _uiState.value.copy(isSaved = true)
    }

    override fun onCleared() {
        super.onCleared()
        locationCallback?.let { callback ->
            fusedLocationClient?.removeLocationUpdates(callback)
        }
    }
}
