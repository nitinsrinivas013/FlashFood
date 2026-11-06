package com.foodconnect.repository;

import com.foodconnect.entity.Donation;
import com.foodconnect.entity.enums.DonationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Donation entity operations.
 */
@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    /**
     * Find all donations by restaurant profile ID.
     */
    Page<Donation> findByRestaurantId(Long restaurantId, Pageable pageable);

    /**
     * Find all donations with a specific status.
     */
    Page<Donation> findByStatus(DonationStatus status, Pageable pageable);

    /**
     * Find all donations by restaurant ID and status.
     */
    Page<Donation> findByRestaurantIdAndStatus(Long restaurantId, DonationStatus status, Pageable pageable);

    /**
     * Find available donations near a location using the Haversine formula.
     * Only returns AVAILABLE donations whose pickup deadline hasn't passed.
     *
     * @param lat      latitude of the center point
     * @param lng      longitude of the center point
     * @param radiusKm radius in kilometers
     * @return list of nearby available donations sorted by distance
     */
    @Query(value = """
            SELECT d.* FROM donations d
            WHERE d.status = 'AVAILABLE'
              AND d.pickup_deadline > NOW()
              AND (
                6371 * acos(
                  cos(radians(:lat)) * cos(radians(d.latitude)) *
                  cos(radians(d.longitude) - radians(:lng)) +
                  sin(radians(:lat)) * sin(radians(d.latitude))
                )
              ) <= :radiusKm
            ORDER BY (
              6371 * acos(
                cos(radians(:lat)) * cos(radians(d.latitude)) *
                cos(radians(d.longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(d.latitude))
              )
            ) ASC
            """, nativeQuery = true)
    List<Donation> findNearbyAvailableDonations(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm
    );

    /**
     * Find donations that have passed their pickup deadline but are still marked as AVAILABLE.
     * Used by the expiration scheduler.
     *
     * @param now the current timestamp
     * @return list of expired donations
     */
    @Query("SELECT d FROM Donation d WHERE d.status = 'AVAILABLE' AND d.pickupDeadline < :now")
    List<Donation> findExpiredDonations(@Param("now") LocalDateTime now);

    /**
     * Count donations by restaurant and status.
     */
    long countByRestaurantIdAndStatus(Long restaurantId, DonationStatus status);

    /**
     * Atomically update donation status from expectedStatus to newStatus.
     * Returns 1 if updated, 0 if the current status didn't match (race lost).
     * This is the core mechanism preventing two NGOs from accepting the same donation.
     */
    @Modifying
    @Query("UPDATE Donation d SET d.status = :newStatus WHERE d.id = :id AND d.status = :expectedStatus")
    int updateStatusAtomically(@Param("id") Long id,
                               @Param("expectedStatus") DonationStatus expectedStatus,
                               @Param("newStatus") DonationStatus newStatus);
}
