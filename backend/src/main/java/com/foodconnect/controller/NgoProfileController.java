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
 * NGO profile management endpoints.
 */
@RestController
@RequestMapping("/api/v1/ngos")
@RequiredArgsConstructor
@Tag(name = "NGO Profiles", description = "NGO profile management")
public class NgoProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    @Operation(summary = "Get my NGO profile")
    public ResponseEntity<NgoProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(profileService.getMyNgoProfile(userDetails.getId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update my NGO profile")
    public ResponseEntity<NgoProfileResponse> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateNgoProfileRequest request) {
        return ResponseEntity.ok(profileService.updateMyNgoProfile(userDetails.getId(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get NGO profile by ID")
    public ResponseEntity<NgoProfileResponse> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(profileService.getNgoProfile(id));
    }
}
