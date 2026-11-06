package com.foodconnect.mapper;

import com.foodconnect.dto.DonationClaimResponse;
import com.foodconnect.entity.DonationClaim;
import org.springframework.stereotype.Component;

/**
 * Maps DonationClaim entities to DonationClaimResponse DTOs.
 */
@Component
public class DonationClaimMapper {

    public DonationClaimResponse toResponse(DonationClaim claim) {
        if (claim == null) {
            return null;
        }
        return DonationClaimResponse.builder()
                .id(claim.getId())
                .donationId(claim.getDonation() != null ? claim.getDonation().getId() : null)
                .ngoId(claim.getNgo() != null ? claim.getNgo().getId() : null)
                .ngoName(claim.getNgo() != null ? claim.getNgo().getNgoName() : null)
                .acceptedAt(claim.getAcceptedAt())
                .pickupStatus(claim.getPickupStatus())
                .collectedAt(claim.getCollectedAt())
                .completedAt(claim.getCompletedAt())
                .completionNotes(claim.getCompletionNotes())
                .build();
    }
}
