package com.foodconnect.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an NGO profile.
 * All fields are optional; only provided fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNgoProfileRequest {

    @Size(min = 1, max = 255, message = "NGO name must be between 1 and 255 characters")
    private String ngoName;

    private String address;

    private Double latitude;

    private Double longitude;

    private String registrationNumber;
}
