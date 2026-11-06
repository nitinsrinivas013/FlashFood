package com.foodconnect.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.foodconnect.entity.enums.DonationStatus;
import com.foodconnect.entity.enums.FoodCategory;
import com.foodconnect.entity.enums.QuantityUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for donation data.
 * Includes calculated fields like distance and claim information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DonationResponse {

    private Long id;
    private Long restaurantId;
    private String restaurantName;
    private String foodName;
    private String description;
    private FoodCategory foodCategory;
    private BigDecimal quantity;
    private QuantityUnit quantityUnit;
    private String imageUrl;
    private LocalDateTime preparedAt;
    private LocalDateTime availableUntil;
    private LocalDateTime pickupDeadline;
    private String storageInstructions;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private DonationStatus status;

    /** Calculated distance from the requesting user's location, in kilometers. */
    private Double distance;

    /** ID of the NGO that claimed this donation, if any. */
    private Long claimedByNgoId;

    /** Name of the NGO that claimed this donation, if any. */
    private String claimedByNgoName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
