package com.foodconnect.dto;

import com.foodconnect.entity.enums.FoodCategory;
import com.foodconnect.entity.enums.QuantityUnit;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for creating a new food donation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDonationRequest {

    @NotBlank(message = "Food name is required")
    private String foodName;

    private String description;

    private FoodCategory foodCategory;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull(message = "Quantity unit is required")
    private QuantityUnit quantityUnit;

    private LocalDateTime preparedAt;

    @NotNull(message = "Available until time is required")
    @Future(message = "Available until must be in the future")
    private LocalDateTime availableUntil;

    @NotNull(message = "Pickup deadline is required")
    @Future(message = "Pickup deadline must be in the future")
    private LocalDateTime pickupDeadline;

    private String storageInstructions;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    @NotBlank(message = "Address is required")
    private String address;

    private String imageUrl;
}
