package com.foodconnect.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.foodconnect.entity.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for NGO profile information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NgoProfileResponse {

    private Long id;
    private Long userId;
    private String ngoName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String registrationNumber;
    private String profileImageUrl;
    private VerificationStatus verificationStatus;
}
