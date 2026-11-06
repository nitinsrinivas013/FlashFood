package com.foodconnect.feature.restaurant

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

data class RestaurantUiState(
    val isLoading: Boolean = false,
    val donations: List<DonationResponse> = emptyList(),
    val profile: RestaurantProfileResponse? = null,
    val error: String? = null
)

data class CreateDonationUiState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RestaurantViewModel @Inject constructor(
    private val donationRepository: DonationRepository,
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestaurantUiState())
    val uiState: StateFlow<RestaurantUiState> = _uiState.asStateFlow()

    private val _createState = MutableStateFlow(CreateDonationUiState())
    val createState: StateFlow<CreateDonationUiState> = _createState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // Load profile
            when (val profileResult = profileRepository.getMyRestaurantProfile()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(profile = profileResult.data)
                is ApiResult.Error -> {}
            }
            // Load donations
            when (val result = donationRepository.getMyDonations()) {
                is ApiResult.Success -> _uiState.value = _uiState.value.copy(donations = result.data.content, isLoading = false)
                is ApiResult.Error -> _uiState.value = _uiState.value.copy(error = result.message, isLoading = false)
            }
        }
    }

    fun createDonation(
        foodName: String, description: String, category: FoodCategory, quantity: Double,
        unit: QuantityUnit, pickupDeadline: String, availableUntil: String,
        address: String, lat: Double, lng: Double, storageInstructions: String, imageUrl: String?
    ) {
        if (foodName.isBlank() || address.isBlank() || quantity <= 0) {
            _createState.value = CreateDonationUiState(error = "Please fill all required fields")
            return
        }
        viewModelScope.launch {
            _createState.value = CreateDonationUiState(isLoading = true)
            val request = CreateDonationRequest(
                foodName = foodName, description = description.takeIf { it.isNotBlank() },
                foodCategory = category, quantity = quantity, quantityUnit = unit,
                pickupDeadline = pickupDeadline, availableUntil = availableUntil,
                address = address, latitude = lat, longitude = lng,
                storageInstructions = storageInstructions.takeIf { it.isNotBlank() },
                imageUrl = imageUrl
            )
            when (val result = donationRepository.createDonation(request)) {
                is ApiResult.Success -> {
                    _createState.value = CreateDonationUiState(success = true)
                    loadDashboard()
                }
                is ApiResult.Error -> _createState.value = CreateDonationUiState(error = result.message)
            }
        }
    }

    fun cancelDonation(id: Long) {
        viewModelScope.launch {
            when (donationRepository.cancelDonation(id)) {
                is ApiResult.Success -> loadDashboard()
                is ApiResult.Error -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch { sessionManager.clearSession() }
    }
}
