package com.foodconnect.repository;

import com.foodconnect.entity.RestaurantProfile;
import com.foodconnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for RestaurantProfile entity operations.
 */
@Repository
public interface RestaurantProfileRepository extends JpaRepository<RestaurantProfile, Long> {

    /**
     * Find a restaurant profile by the associated user's ID.
     */
    Optional<RestaurantProfile> findByUserId(Long userId);

    /**
     * Find a restaurant profile by the associated user entity.
     */
    Optional<RestaurantProfile> findByUser(User user);
}
