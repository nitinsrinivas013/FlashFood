package com.foodconnect.mapper;

import com.foodconnect.dto.DonationResponse;
import com.foodconnect.dto.PageResponse;
import com.foodconnect.entity.Donation;
import com.foodconnect.entity.DonationClaim;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps Donation entities to DonationResponse DTOs.
 * Supports enrichment with calculated distance and claim information.
 */
@Component
public class DonationMapper {

    /**
     * Convert a Donation entity to a DonationResponse with a calculated distance.
     *
     * @param donation the donation entity
     * @param distance the calculated distance in km (nullable)
     * @return the donation response DTO
     */
    public DonationResponse toResponse(Donation donation, Double distance) {
        if (donation == null) {
            return null;
        }

        DonationResponse.DonationResponseBuilder builder = DonationResponse.builder()
                .id(donation.getId())
                .restaurantId(donation.getRestaurant() != null ? donation.getRestaurant().getId() : null)
                .restaurantName(donation.getRestaurant() != null ? donation.getRestaurant().getRestaurantName() : null)
                .foodName(donation.getFoodName())
                .description(donation.getDescription())
                .foodCategory(donation.getFoodCategory())
                .quantity(donation.getQuantity())
                .quantityUnit(donation.getQuantityUnit())
                .imageUrl(donation.getImageUrl())
                .preparedAt(donation.getPreparedAt())
                .availableUntil(donation.getAvailableUntil())
                .pickupDeadline(donation.getPickupDeadline())
                .storageInstructions(donation.getStorageInstructions())
                .latitude(donation.getLatitude())
                .longitude(donation.getLongitude())
                .address(donation.getAddress())
                .status(donation.getStatus())
                .distance(distance)
                .createdAt(donation.getCreatedAt())
                .updatedAt(donation.getUpdatedAt());

        // Populate claim information if present
        DonationClaim claim = donation.getClaim();
        if (claim != null) {
            builder.claimedByNgoId(claim.getNgo() != null ? claim.getNgo().getId() : null);
            builder.claimedByNgoName(claim.getNgo() != null ? claim.getNgo().getNgoName() : null);
        }

        return builder.build();
    }

    /**
     * Convert a Donation entity to a DonationResponse without distance.
     *
     * @param donation the donation entity
     * @return the donation response DTO
     */
    public DonationResponse toResponse(Donation donation) {
        return toResponse(donation, null);
    }

    /**
     * Convert a Spring Data Page of Donations to a PageResponse.
     *
     * @param donationPage the page of donations
     * @return the paginated response DTO
     */
    public PageResponse<DonationResponse> toPageResponse(Page<Donation> donationPage) {
        List<DonationResponse> content = donationPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<DonationResponse>builder()
                .content(content)
                .page(donationPage.getNumber())
                .size(donationPage.getSize())
                .totalElements(donationPage.getTotalElements())
                .totalPages(donationPage.getTotalPages())
                .last(donationPage.isLast())
                .build();
    }
}
