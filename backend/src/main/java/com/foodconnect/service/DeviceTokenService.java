package com.foodconnect.service;

import com.foodconnect.dto.DeviceTokenRequest;
import com.foodconnect.entity.DeviceToken;
import com.foodconnect.entity.User;
import com.foodconnect.exception.ResourceNotFoundException;
import com.foodconnect.exception.UnauthorizedActionException;
import com.foodconnect.repository.DeviceTokenRepository;
import com.foodconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing FCM device tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    /**
     * Register or update an FCM device token for the authenticated user.
     */
    @Transactional
    public void registerToken(Long userId, DeviceTokenRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if token already exists for this user
        Optional<DeviceToken> existing = deviceTokenRepository.findByUserIdAndFcmToken(userId, request.getFcmToken());
        if (existing.isPresent()) {
            // Reactivate if inactive
            DeviceToken token = existing.get();
            token.setActive(true);
            deviceTokenRepository.save(token);
            return;
        }

        // Also check if token exists for another user (device reassignment)
        Optional<DeviceToken> otherUser = deviceTokenRepository.findByFcmToken(request.getFcmToken());
        if (otherUser.isPresent()) {
            DeviceToken old = otherUser.get();
            old.setActive(false);
            deviceTokenRepository.save(old);
        }

        DeviceToken token = DeviceToken.builder()
                .user(user)
                .fcmToken(request.getFcmToken())
                .platform(request.getPlatform() != null ? request.getPlatform() : "ANDROID")
                .active(true)
                .build();
        deviceTokenRepository.save(token);

        log.info("Registered device token for user {}", userId);
    }

    /**
     * Remove a device token.
     */
    @Transactional
    public void removeToken(Long userId, Long tokenId) {
        DeviceToken token = deviceTokenRepository.findById(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Device token not found"));

        if (!token.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("Cannot remove another user's device token");
        }

        deviceTokenRepository.delete(token);
        log.info("Removed device token {} for user {}", tokenId, userId);
    }
}
