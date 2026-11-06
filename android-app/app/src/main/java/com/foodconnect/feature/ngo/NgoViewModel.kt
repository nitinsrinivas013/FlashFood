package com.foodconnect.feature.ngo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodconnect.core.security.SessionManager
import com.foodconnect.data.model.*
import com.foodconnect.data.repository.ApiResult
import com.foodconnect.data.repository.DonationRepository
import com.foodconnect.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NgoUiState(
    val isLoading: Boolean = false,
    val nearbyDonations: List<DonationResponse> = emptyList(),
    val myPickups: List<DonationClaimResponse> = emptyList(),
    val profile: NgoProfileResponse? = null,
    val error: String? = null,
    val currentLat: Double? = null,
    val currentLng: Double? = null
)

@HiltViewModel
class NgoViewModel @Inject constructor(
    private val donationRepository: DonationRepository,
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NgoUiState())
    val uiState: StateFlow<NgoUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        loadMyPickups()
    }

    fun loadProfile() {
        viewModelScope.launch {
            when (val result = profileRepository.getMyNgoProfile()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(profile = result.data)
                    // Use profile location as fallback
                    if (_uiState.value.currentLat == null) {
                        result.data.latitude?.let { lat ->
                            result.data.longitude?.let { lng ->
                                updateLocation(lat, lng)
                            }
                        }
                    }
                }
                is ApiResult.Error -> {}
            }
        }
    }

    fun updateLocation(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(currentLat = lat, currentLng = lng)
        loadNearbyDonations(lat, lng)
    }

    fun loadNearbyDonations(lat: Double, lng: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = donationRepository.getNearbyDonations(lat, lng)) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(nearbyDonations = result.data, isLoading = false)
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message, isLoading = false)
            }
        }
    }

    fun loadMyPickups() {
        viewModelScope.launch {
            when (val result = donationRepository.getNgoPickups()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(myPickups = result.data)
                is ApiResult.Error -> {}
            }
        }
    }

    fun acceptDonation(id: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = donationRepository.acceptDonation(id)) {
                is ApiResult.Success -> {
                    loadMyPickups()
                    _uiState.value.currentLat?.let { lat ->
                        _uiState.value.currentLng?.let { lng ->
                            loadNearbyDonations(lat, lng)
                        }
                    }
                    onSuccess()
                }
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    fun markCollected(id: Long) {
        viewModelScope.launch {
            when (donationRepository.markCollected(id)) {
                is ApiResult.Success -> loadMyPickups()
                is ApiResult.Error -> {}
            }
        }
    }

    fun markCompleted(id: Long) {
        viewModelScope.launch {
            when (donationRepository.markCompleted(id)) {
                is ApiResult.Success -> loadMyPickups()
                is ApiResult.Error -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch { sessionManager.clearSession() }
    }
}
