package com.foodconnect.data.model

import com.google.gson.annotations.SerializedName

// === Enums ===
enum class Role { RESTAURANT, NGO }
enum class DonationStatus { AVAILABLE, ACCEPTED, COLLECTED, COMPLETED, EXPIRED, CANCELLED }
enum class FoodCategory { COOKED_MEALS, RAW_INGREDIENTS, BAKERY, DAIRY, FRUITS_VEGETABLES, BEVERAGES, PACKAGED, OTHER }
enum class QuantityUnit { KG, PORTIONS, PACKETS, LITERS, PIECES }
enum class PickupStatus { ASSIGNED, EN_ROUTE, ARRIVED, COLLECTED, COMPLETED }
enum class NotificationType { NEW_DONATION_NEARBY, DONATION_ACCEPTED, DONATION_COLLECTED, DONATION_COMPLETED, DONATION_CANCELLED, DONATION_EXPIRED }

// === Auth ===
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: Role,
    val phone: String? = null,
    val restaurantName: String? = null,
    val ngoName: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val registrationNumber: String? = null
)

data class LoginRequest(val email: String, val password: String)

data class AuthResponse(
    val token: String,
    val tokenType: String,
    val user: UserResponse?,
    val role: Role
)

data class UserResponse(
    val id: Long,
    val name: String,
    val email: String,
    val role: Role,
    val phone: String?,
    val accountStatus: String?,
    val createdAt: String?
)

// === Profiles ===
data class RestaurantProfileResponse(
    val id: Long,
    val userId: Long?,
    val restaurantName: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val profileImageUrl: String?,
    val verificationStatus: String?
)

data class NgoProfileResponse(
    val id: Long,
    val userId: Long?,
    val ngoName: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val registrationNumber: String?,
    val profileImageUrl: String?,
    val verificationStatus: String?
)

data class UpdateRestaurantProfileRequest(
    val restaurantName: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class UpdateNgoProfileRequest(
    val ngoName: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val registrationNumber: String? = null
)

// === Donations ===
data class CreateDonationRequest(
    val foodName: String,
    val description: String? = null,
    val foodCategory: FoodCategory? = FoodCategory.OTHER,
    val quantity: Double,
    val quantityUnit: QuantityUnit,
    val preparedAt: String? = null,
    val availableUntil: String,
    val pickupDeadline: String,
    val storageInstructions: String? = null,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val imageUrl: String? = null
)

data class DonationResponse(
    val id: Long,
    val restaurantId: Long?,
    val restaurantName: String?,
    val foodName: String,
    val description: String?,
    val foodCategory: FoodCategory?,
    val quantity: Double?,
    val quantityUnit: QuantityUnit?,
    val imageUrl: String?,
    val preparedAt: String?,
    val availableUntil: String?,
    val pickupDeadline: String?,
    val storageInstructions: String?,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val status: DonationStatus?,
    val distance: Double?,
    val claimedByNgoId: Long?,
    val claimedByNgoName: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class DonationClaimResponse(
    val id: Long,
    val donationId: Long?,
    val ngoId: Long?,
    val ngoName: String?,
    val acceptedAt: String?,
    val pickupStatus: PickupStatus?,
    val collectedAt: String?,
    val completedAt: String?,
    val completionNotes: String?
)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean
)

// === Notifications ===
data class NotificationResponse(
    val id: Long,
    val donationId: Long?,
    val notificationType: NotificationType?,
    val title: String?,
    val message: String?,
    @SerializedName("read") val isRead: Boolean?,
    val createdAt: String?
)

// === Device Tokens ===
data class DeviceTokenRequest(val fcmToken: String, val platform: String = "ANDROID")

// === Location ===
data class UpdateLocationRequest(val latitude: Double, val longitude: Double)

// === Error ===
data class ApiErrorResponse(
    val status: Int?,
    val error: String?,
    val message: String?,
    val timestamp: String?,
    val path: String?,
    val fieldErrors: Map<String, String>?
)
