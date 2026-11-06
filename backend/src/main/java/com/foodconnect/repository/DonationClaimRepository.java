package com.foodconnect.repository;

import com.foodconnect.entity.DonationClaim;
import com.foodconnect.entity.enums.PickupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DonationClaim entity operations.
 */
@Repository
public interface DonationClaimRepository extends JpaRepository<DonationClaim, Long> {

    /**
     * Find a claim by its donation ID.
     * Since donation_id has a UNIQUE constraint, this returns at most one result.
     */
    Optional<DonationClaim> findByDonationId(Long donationId);

    /**
     * Find all claims by NGO profile ID (paginated).
     */
    Page<DonationClaim> findByNgoId(Long ngoId, Pageable pageable);

    /**
     * Find all claims by NGO profile ID (list).
     */
    List<DonationClaim> findByNgoId(Long ngoId);

    /**
     * Find claims by NGO ID and pickup status.
     */
    List<DonationClaim> findByNgoIdAndPickupStatus(Long ngoId, PickupStatus pickupStatus);

    /**
     * Check if a claim already exists for the given donation.
     */
    boolean existsByDonationId(Long donationId);
}
