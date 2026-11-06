package com.foodconnect.repository;

import com.foodconnect.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DeviceToken entity operations.
 */
@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    /**
     * Find all device tokens for a user.
     */
    List<DeviceToken> findByUserId(Long userId);

    /**
     * Find a device token by its FCM token string.
     */
    Optional<DeviceToken> findByFcmToken(String fcmToken);

    /**
     * Find all active device tokens for a user.
     * Used when sending push notifications.
     */
    List<DeviceToken> findByUserIdAndActiveTrue(Long userId);

    /**
     * Find a specific token for a user-device combination.
     */
    Optional<DeviceToken> findByUserIdAndFcmToken(Long userId, String fcmToken);
}
