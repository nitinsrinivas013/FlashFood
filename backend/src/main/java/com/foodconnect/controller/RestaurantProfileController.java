package com.foodconnect.controller;

import com.foodconnect.dto.*;
import com.foodconnect.security.CustomUserDetails;
import com.foodconnect.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Restaurant profile management endpoints.
 */
@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
@Tag(name = "Restaurant Profiles", description = "Restaurant profile management")
public class RestaurantProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    @Operation(summary = "Get my restaurant profile")
    public ResponseEntity<RestaurantProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(profileService.getMyRestaurantProfile(userDetails.getId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update my restaurant profile")
    public ResponseEntity<RestaurantProfileResponse> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateRestaurantProfileRequest request) {
        return ResponseEntity.ok(profileService.updateMyRestaurantProfile(userDetails.getId(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get restaurant profile by ID")
    public ResponseEntity<RestaurantProfileResponse> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(profileService.getRestaurantProfile(id));
    }
}
