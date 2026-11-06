package com.foodconnect.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a restaurant profile.
 * All fields are optional; only provided fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRestaurantProfileRequest {

    @Size(min = 1, max = 255, message = "Restaurant name must be between 1 and 255 characters")
    private String restaurantName;

    private String address;

    private Double latitude;

    private Double longitude;
}
