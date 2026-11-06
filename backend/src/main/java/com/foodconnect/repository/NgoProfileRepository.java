package com.foodconnect.repository;

import com.foodconnect.entity.NgoProfile;
import com.foodconnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for NgoProfile entity operations.
 */
@Repository
public interface NgoProfileRepository extends JpaRepository<NgoProfile, Long> {

    /**
     * Find an NGO profile by the associated user's ID.
     */
    Optional<NgoProfile> findByUserId(Long userId);

    /**
     * Find an NGO profile by the associated user entity.
     */
    Optional<NgoProfile> findByUser(User user);

    /**
     * Find all NGO profiles within a given radius (in km) of a point using the Haversine formula.
     * Only returns NGOs with VERIFIED status and non-null coordinates.
     *
     * @param lat      latitude of the center point
     * @param lng      longitude of the center point
     * @param radiusKm radius in kilometers
     * @return list of NGO profiles within the specified radius
     */
    @Query(value = """
            SELECT n.* FROM ngo_profiles n
            WHERE n.verification_status = 'VERIFIED'
              AND n.latitude IS NOT NULL
              AND n.longitude IS NOT NULL
              AND (
                6371 * acos(
                  cos(radians(:lat)) * cos(radians(n.latitude)) *
                  cos(radians(n.longitude) - radians(:lng)) +
                  sin(radians(:lat)) * sin(radians(n.latitude))
                )
              ) <= :radiusKm
            ORDER BY (
              6371 * acos(
                cos(radians(:lat)) * cos(radians(n.latitude)) *
                cos(radians(n.longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(n.latitude))
              )
            ) ASC
            """, nativeQuery = true)
    List<NgoProfile> findNgosWithinRadius(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm
    );
}
