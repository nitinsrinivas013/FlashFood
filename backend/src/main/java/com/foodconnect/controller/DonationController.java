package com.foodconnect.controller;

import com.foodconnect.dto.*;
import com.foodconnect.entity.enums.DonationStatus;
import com.foodconnect.entity.enums.PickupStatus;
import com.foodconnect.security.CustomUserDetails;
import com.foodconnect.service.DonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Donation lifecycle endpoints: create, discover, accept, status transitions.
 */
@RestController
@RequestMapping("/api/v1/donations")
@RequiredArgsConstructor
@Tag(name = "Donations", description = "Donation CRUD and lifecycle management")
public class DonationController {

    private final DonationService donationService;

    @PostMapping
    @Operation(summary = "Create donation", description = "Create a new food donation listing (Restaurant only)")
    public ResponseEntity<DonationResponse> createDonation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateDonationRequest request) {
        DonationResponse response = donationService.createDonation(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get donation", description = "Get donation details by ID")
    public ResponseEntity<DonationResponse> getDonation(@PathVariable Long id) {
        return ResponseEntity.ok(donationService.getDonation(id));
    }

    @GetMapping("/mine")
    @Operation(summary = "Get my donations", description = "Get donations created by the authenticated restaurant")
    public ResponseEntity<PageResponse<DonationResponse>> getMyDonations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) DonationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<DonationResponse> response = donationService.getMyDonations(
                userDetails.getId(), status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/nearby")
    @Operation(summary = "Find nearby donations", description = "Discover available donations near a location (NGO use)")
    public ResponseEntity<List<DonationResponse>> getNearbyDonations(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Double radiusKm) {
        return ResponseEntity.ok(donationService.getNearbyDonations(lat, lng, radiusKm));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update donation", description = "Update an AVAILABLE donation (Restaurant owner only)")
    public ResponseEntity<DonationResponse> updateDonation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateDonationRequest request) {
        return ResponseEntity.ok(donationService.updateDonation(userDetails.getId(), id, request));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept donation", description = "Accept a donation for pickup (NGO only, race-safe)")
    public ResponseEntity<DonationClaimResponse> acceptDonation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(donationService.acceptDonation(userDetails.getId(), id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel donation", description = "Cancel a donation (Restaurant owner only)")
    public ResponseEntity<DonationResponse> cancelDonation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(donationService.cancelDonation(userDetails.getId(), id));
    }

    @PostMapping("/{id}/collected")
    @Operation(summary = "Mark collected", description = "Mark donation as collected (assigned NGO only)")
    public ResponseEntity<DonationResponse> markCollected(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(donationService.markCollected(userDetails.getId(), id));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Mark completed", description = "Mark donation as completed (assigned NGO or restaurant)")
    public ResponseEntity<DonationResponse> markCompleted(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(donationService.markCompleted(userDetails.getId(), id));
    }

    @GetMapping("/pickups")
    @Operation(summary = "Get NGO pickups", description = "Get pickups assigned to the authenticated NGO")
    public ResponseEntity<List<DonationClaimResponse>> getNgoPickups(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) PickupStatus status) {
        return ResponseEntity.ok(donationService.getNgoPickups(userDetails.getId(), status));
    }
}
