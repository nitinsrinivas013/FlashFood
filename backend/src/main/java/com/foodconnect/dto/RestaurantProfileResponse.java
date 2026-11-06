package com.foodconnect.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.foodconnect.entity.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for restaurant profile information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestaurantProfileResponse {

    private Long id;
    private Long userId;
    private String restaurantName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String profileImageUrl;
    private VerificationStatus verificationStatus;
}
