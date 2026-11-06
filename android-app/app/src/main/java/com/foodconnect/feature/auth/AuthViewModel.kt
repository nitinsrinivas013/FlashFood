package com.foodconnect.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.foodconnect.core.security.SessionManager
import com.foodconnect.data.model.*
import com.foodconnect.data.repository.ApiResult
import com.foodconnect.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val role: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val isLoggedIn = sessionManager.isLoggedIn
    val userRole = sessionManager.userRole

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(error = "Email and password are required")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.login(LoginRequest(email.trim(), password))) {
                is ApiResult.Success -> {
                    val data = result.data
                    sessionManager.saveSession(
                        token = data.token,
                        userId = data.user?.id ?: 0,
                        name = data.user?.name ?: "",
                        email = data.user?.email ?: email,
                        role = data.role.name
                    )
                    _uiState.value = AuthUiState(success = true, role = data.role.name)
                }
                is ApiResult.Error -> {
                    _uiState.value = AuthUiState(error = result.message)
                }
            }
        }
    }

    fun register(
        name: String, email: String, password: String, role: String,
        phone: String = "", restaurantName: String = "", ngoName: String = "",
        address: String = "", latitude: Double? = null, longitude: Double? = null,
        registrationNumber: String = ""
    ) {
        if (name.isBlank() || email.isBlank() || password.length < 8) {
            _uiState.value = AuthUiState(error = "Please fill all required fields. Password must be at least 8 characters.")
            return
        }
        if (role == "RESTAURANT" && restaurantName.isBlank()) {
            _uiState.value = AuthUiState(error = "Restaurant name is required")
            return
        }
        if (role == "NGO" && ngoName.isBlank()) {
            _uiState.value = AuthUiState(error = "NGO name is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val request = RegisterRequest(
                name = name.trim(), email = email.trim(), password = password,
                role = Role.valueOf(role),
                phone = phone.takeIf { it.isNotBlank() },
                restaurantName = restaurantName.takeIf { it.isNotBlank() },
                ngoName = ngoName.takeIf { it.isNotBlank() },
                address = address.takeIf { it.isNotBlank() },
                latitude = latitude, longitude = longitude,
                registrationNumber = registrationNumber.takeIf { it.isNotBlank() }
            )
            when (val result = authRepository.register(request)) {
                is ApiResult.Success -> {
                    val data = result.data
                    sessionManager.saveSession(
                        token = data.token,
                        userId = data.user?.id ?: 0,
                        name = data.user?.name ?: name,
                        email = data.user?.email ?: email,
                        role = data.role.name
                    )
                    _uiState.value = AuthUiState(success = true, role = data.role.name)
                }
                is ApiResult.Error -> {
                    _uiState.value = AuthUiState(error = result.message)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
