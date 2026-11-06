package com.foodconnect.dto;

import com.foodconnect.entity.enums.FoodCategory;
import com.foodconnect.entity.enums.QuantityUnit;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for updating an existing donation.
 * All fields are optional; only provided fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDonationRequest {

    private String foodName;

    private String description;

    private FoodCategory foodCategory;

    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    private QuantityUnit quantityUnit;

    private LocalDateTime preparedAt;

    @Future(message = "Available until must be in the future")
    private LocalDateTime availableUntil;

    @Future(message = "Pickup deadline must be in the future")
    private LocalDateTime pickupDeadline;

    private String storageInstructions;

    private Double latitude;

    private Double longitude;

    private String address;

    private String imageUrl;
}
