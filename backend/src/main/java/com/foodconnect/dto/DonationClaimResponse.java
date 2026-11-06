package com.foodconnect.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.foodconnect.entity.enums.PickupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for donation claim information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DonationClaimResponse {

    private Long id;
    private Long donationId;
    private Long ngoId;
    private String ngoName;
    private LocalDateTime acceptedAt;
    private PickupStatus pickupStatus;
    private LocalDateTime collectedAt;
    private LocalDateTime completedAt;
    private String completionNotes;
}
