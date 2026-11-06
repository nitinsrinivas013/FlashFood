package com.foodconnect.data.remote

import com.foodconnect.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface FoodConnectApi {

    // === Auth ===
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    // === Restaurant Profile ===
    @GET("restaurants/me")
    suspend fun getMyRestaurantProfile(): Response<RestaurantProfileResponse>

    @PUT("restaurants/me")
    suspend fun updateMyRestaurantProfile(@Body request: UpdateRestaurantProfileRequest): Response<RestaurantProfileResponse>

    @GET("restaurants/{id}")
    suspend fun getRestaurantProfile(@Path("id") id: Long): Response<RestaurantProfileResponse>

    // === NGO Profile ===
    @GET("ngos/me")
    suspend fun getMyNgoProfile(): Response<NgoProfileResponse>

    @PUT("ngos/me")
    suspend fun updateMyNgoProfile(@Body request: UpdateNgoProfileRequest): Response<NgoProfileResponse>

    @GET("ngos/{id}")
    suspend fun getNgoProfile(@Path("id") id: Long): Response<NgoProfileResponse>

    // === Donations ===
    @POST("donations")
    suspend fun createDonation(@Body request: CreateDonationRequest): Response<DonationResponse>

    @GET("donations/{id}")
    suspend fun getDonation(@Path("id") id: Long): Response<DonationResponse>

    @GET("donations/mine")
    suspend fun getMyDonations(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<DonationResponse>>

    @GET("donations/nearby")
    suspend fun getNearbyDonations(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusKm") radiusKm: Double? = null
    ): Response<List<DonationResponse>>

    @PUT("donations/{id}")
    suspend fun updateDonation(@Path("id") id: Long, @Body request: CreateDonationRequest): Response<DonationResponse>

    @POST("donations/{id}/accept")
    suspend fun acceptDonation(@Path("id") id: Long): Response<DonationClaimResponse>

    @POST("donations/{id}/cancel")
    suspend fun cancelDonation(@Path("id") id: Long): Response<DonationResponse>

    @POST("donations/{id}/collected")
    suspend fun markCollected(@Path("id") id: Long): Response<DonationResponse>

    @POST("donations/{id}/complete")
    suspend fun markCompleted(@Path("id") id: Long): Response<DonationResponse>

    @GET("donations/pickups")
    suspend fun getNgoPickups(@Query("status") status: String? = null): Response<List<DonationClaimResponse>>

    // === Notifications ===
    @GET("notifications")
    suspend fun getNotifications(): Response<List<NotificationResponse>>

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long): Response<Unit>

    // === User ===
    @PUT("users/me/location")
    suspend fun updateLocation(@Body request: UpdateLocationRequest): Response<Unit>

    @POST("users/me/device-tokens")
    suspend fun registerDeviceToken(@Body request: DeviceTokenRequest): Response<Unit>

    @DELETE("users/me/device-tokens/{id}")
    suspend fun removeDeviceToken(@Path("id") id: Long): Response<Unit>
}
