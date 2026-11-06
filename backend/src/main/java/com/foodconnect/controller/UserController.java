package com.foodconnect.controller;

import com.foodconnect.dto.DeviceTokenRequest;
import com.foodconnect.dto.UpdateLocationRequest;
import com.foodconnect.entity.NgoProfile;
import com.foodconnect.entity.RestaurantProfile;
import com.foodconnect.entity.User;
import com.foodconnect.entity.enums.Role;
import com.foodconnect.exception.ResourceNotFoundException;
import com.foodconnect.repository.NgoProfileRepository;
import com.foodconnect.repository.RestaurantProfileRepository;
import com.foodconnect.repository.UserRepository;
import com.foodconnect.security.CustomUserDetails;
import com.foodconnect.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * User-level endpoints for location updates and device token management.
 */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "User", description = "User-level operations")
public class UserController {

    private final DeviceTokenService deviceTokenService;
    private final UserRepository userRepository;
    private final RestaurantProfileRepository restaurantProfileRepository;
    private final NgoProfileRepository ngoProfileRepository;

    @PutMapping("/location")
    @Operation(summary = "Update my location", description = "Update the user's current latitude/longitude on their profile")
    public ResponseEntity<Void> updateLocation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateLocationRequest request) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() == Role.RESTAURANT) {
            RestaurantProfile profile = restaurantProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Restaurant profile not found"));
            profile.setLatitude(BigDecimal.valueOf(request.getLatitude()));
            profile.setLongitude(BigDecimal.valueOf(request.getLongitude()));
            restaurantProfileRepository.save(profile);
        } else if (user.getRole() == Role.NGO) {
            NgoProfile profile = ngoProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found"));
            profile.setLatitude(BigDecimal.valueOf(request.getLatitude()));
            profile.setLongitude(BigDecimal.valueOf(request.getLongitude()));
            ngoProfileRepository.save(profile);
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/device-tokens")
    @Operation(summary = "Register device token", description = "Register an FCM device token for push notifications")
    public ResponseEntity<Void> registerDeviceToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DeviceTokenRequest request) {
        deviceTokenService.registerToken(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/device-tokens/{id}")
    @Operation(summary = "Remove device token")
    public ResponseEntity<Void> removeDeviceToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        deviceTokenService.removeToken(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
