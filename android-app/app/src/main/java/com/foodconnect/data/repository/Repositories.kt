package com.foodconnect.data.repository

import com.foodconnect.data.model.*
import com.foodconnect.data.remote.FoodConnectApi
import javax.inject.Inject
import javax.inject.Singleton

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

@Singleton
class AuthRepository @Inject constructor(private val api: FoodConnectApi) {

    suspend fun register(request: RegisterRequest): ApiResult<AuthResponse> = safeApiCall { api.register(request) }
    suspend fun login(request: LoginRequest): ApiResult<AuthResponse> = safeApiCall { api.login(request) }
    suspend fun getCurrentUser(): ApiResult<UserResponse> = safeApiCall { api.getCurrentUser() }
}

@Singleton
class DonationRepository @Inject constructor(private val api: FoodConnectApi) {

    suspend fun createDonation(request: CreateDonationRequest): ApiResult<DonationResponse> =
        safeApiCall { api.createDonation(request) }

    suspend fun getDonation(id: Long): ApiResult<DonationResponse> =
        safeApiCall { api.getDonation(id) }

    suspend fun getMyDonations(status: String? = null, page: Int = 0): ApiResult<PageResponse<DonationResponse>> =
        safeApiCall { api.getMyDonations(status, page) }

    suspend fun getNearbyDonations(lat: Double, lng: Double, radiusKm: Double? = null): ApiResult<List<DonationResponse>> =
        safeApiCall { api.getNearbyDonations(lat, lng, radiusKm) }

    suspend fun acceptDonation(id: Long): ApiResult<DonationClaimResponse> =
        safeApiCall { api.acceptDonation(id) }

    suspend fun cancelDonation(id: Long): ApiResult<DonationResponse> =
        safeApiCall { api.cancelDonation(id) }

    suspend fun markCollected(id: Long): ApiResult<DonationResponse> =
        safeApiCall { api.markCollected(id) }

    suspend fun markCompleted(id: Long): ApiResult<DonationResponse> =
        safeApiCall { api.markCompleted(id) }

    suspend fun getNgoPickups(status: String? = null): ApiResult<List<DonationClaimResponse>> =
        safeApiCall { api.getNgoPickups(status) }
}

@Singleton
class ProfileRepository @Inject constructor(private val api: FoodConnectApi) {

    suspend fun getMyRestaurantProfile(): ApiResult<RestaurantProfileResponse> =
        safeApiCall { api.getMyRestaurantProfile() }

    suspend fun updateMyRestaurantProfile(request: UpdateRestaurantProfileRequest): ApiResult<RestaurantProfileResponse> =
        safeApiCall { api.updateMyRestaurantProfile(request) }

    suspend fun getMyNgoProfile(): ApiResult<NgoProfileResponse> =
        safeApiCall { api.getMyNgoProfile() }

    suspend fun updateMyNgoProfile(request: UpdateNgoProfileRequest): ApiResult<NgoProfileResponse> =
        safeApiCall { api.updateMyNgoProfile(request) }

    suspend fun getRestaurantProfile(id: Long): ApiResult<RestaurantProfileResponse> =
        safeApiCall { api.getRestaurantProfile(id) }
}

@Singleton
class NotificationRepository @Inject constructor(private val api: FoodConnectApi) {

    suspend fun getNotifications(): ApiResult<List<NotificationResponse>> =
        safeApiCall { api.getNotifications() }

    suspend fun markAsRead(id: Long): ApiResult<Unit> =
        safeApiCall { api.markNotificationRead(id) }

    suspend fun registerDeviceToken(token: String): ApiResult<Unit> =
        safeApiCall { api.registerDeviceToken(DeviceTokenRequest(token)) }

    suspend fun updateLocation(lat: Double, lng: Double): ApiResult<Unit> =
        safeApiCall { api.updateLocation(UpdateLocationRequest(lat, lng)) }
}

private suspend fun <T> safeApiCall(call: suspend () -> retrofit2.Response<T>): ApiResult<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                ApiResult.Success(body)
            } else {
                // For 204 No Content or empty body success
                @Suppress("UNCHECKED_CAST")
                ApiResult.Success(Unit as T)
            }
        } else {
            val errorBody = response.errorBody()?.string()
            val message = try {
                com.google.gson.Gson().fromJson(errorBody, ApiErrorResponse::class.java)?.message
            } catch (_: Exception) { null } ?: "Request failed"
            ApiResult.Error(message, response.code())
        }
    } catch (e: Exception) {
        ApiResult.Error(e.localizedMessage ?: "Network error. Please check your connection.")
    }
}
