package com.okeho.mapping.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.okeho.mapping.domain.model.Capture
import com.okeho.mapping.domain.model.Street
import com.okeho.mapping.domain.repository.CaptureRepository
import com.okeho.mapping.domain.repository.StreetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordsUiState(
    val captures: List<Capture> = emptyList(),
    val streets: List<Street> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTab: Int = 0
)

@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val captureRepository: CaptureRepository,
    private val streetRepository: StreetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordsUiState())
    val uiState: StateFlow<RecordsUiState> = _uiState.asStateFlow()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            captureRepository.getAllCaptures().collect { captures ->
                _uiState.value = _uiState.value.copy(
                    captures = captures,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
            streetRepository.getAllStreets().collect { streets ->
                _uiState.value = _uiState.value.copy(
                    streets = streets,
                    isLoading = false
                )
            }
        }
    }

    fun selectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun deleteCapture(capture: Capture) {
        viewModelScope.launch {
            captureRepository.deleteCapture(capture)
        }
    }

    fun deleteStreet(street: Street) {
        viewModelScope.launch {
            streetRepository.deleteStreet(street)
        }
    }
}
