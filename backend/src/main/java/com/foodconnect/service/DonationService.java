package com.foodconnect.service;

import com.foodconnect.config.AppConfig;
import com.foodconnect.dto.*;
import com.foodconnect.entity.*;
import com.foodconnect.entity.enums.*;
import com.foodconnect.exception.*;
import com.foodconnect.mapper.DonationClaimMapper;
import com.foodconnect.mapper.DonationMapper;
import com.foodconnect.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Core donation lifecycle service.
 * Manages creation, discovery, acceptance (race-safe), status transitions,
 * expiration, and cancellation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DonationService {

    private final DonationRepository donationRepository;
    private final DonationClaimRepository donationClaimRepository;
    private final RestaurantProfileRepository restaurantProfileRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final UserRepository userRepository;
    private final DonationMapper donationMapper;
    private final DonationClaimMapper donationClaimMapper;
    private final NotificationService notificationService;
    private final AppConfig appConfig;

    /**
     * Create a new donation listing (RESTAURANT only).
     */
    @Transactional
    public DonationResponse createDonation(Long userId, CreateDonationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.RESTAURANT) {
            throw new UnauthorizedActionException("Only restaurants can create donations");
        }

        RestaurantProfile restaurant = restaurantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant profile not found"));

        // Validate deadlines
        if (request.getPickupDeadline().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Pickup deadline must be in the future");
        }
        if (request.getAvailableUntil().isAfter(request.getPickupDeadline())) {
            throw new BadRequestException("Available until must be before or at pickup deadline");
        }

        Donation donation = Donation.builder()
                .restaurant(restaurant)
                .foodName(request.getFoodName())
                .description(request.getDescription())
                .foodCategory(request.getFoodCategory() != null ? request.getFoodCategory() : FoodCategory.OTHER)
                .quantity(request.getQuantity())
                .quantityUnit(request.getQuantityUnit())
                .imageUrl(request.getImageUrl())
                .preparedAt(request.getPreparedAt())
                .availableUntil(request.getAvailableUntil())
                .pickupDeadline(request.getPickupDeadline())
                .storageInstructions(request.getStorageInstructions())
                .latitude(BigDecimal.valueOf(request.getLatitude()))
                .longitude(BigDecimal.valueOf(request.getLongitude()))
                .address(request.getAddress())
                .status(DonationStatus.AVAILABLE)
                .build();

        donation = donationRepository.save(donation);

        log.info("Donation created: id={}, restaurant={}, food={}",
                donation.getId(), restaurant.getRestaurantName(), donation.getFoodName());

        // Dispatch notifications to nearby NGOs (async, post-commit safe)
        final Long donationId = donation.getId();
        final double lat = request.getLatitude();
        final double lng = request.getLongitude();
        try {
            notificationService.notifyNearbyNgos(donationId, lat, lng);
        } catch (Exception e) {
            log.error("Failed to dispatch notifications for donation {}: {}", donationId, e.getMessage());
            // Do not rollback the donation creation due to notification failure
        }

        return donationMapper.toResponse(donation);
    }

    /**
     * Get a donation by ID.
     */
    @Transactional(readOnly = true)
    public DonationResponse getDonation(Long donationId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found: " + donationId));
        return donationMapper.toResponse(donation);
    }

    /**
     * Get donations created by the authenticated restaurant.
     */
    @Transactional(readOnly = true)
    public PageResponse<DonationResponse> getMyDonations(Long userId, DonationStatus status, Pageable pageable) {
        RestaurantProfile restaurant = restaurantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant profile not found"));

        Page<Donation> page;
        if (status != null) {
            page = donationRepository.findByRestaurantIdAndStatus(restaurant.getId(), status, pageable);
        } else {
            page = donationRepository.findByRestaurantId(restaurant.getId(), pageable);
        }
        return donationMapper.toPageResponse(page);
    }

    /**
     * Find nearby available donations for an NGO.
     * Uses the Haversine formula for accurate distance calculation.
     */
    @Transactional(readOnly = true)
    public List<DonationResponse> getNearbyDonations(double lat, double lng, Double radiusKm) {
        double radius = radiusKm != null ? radiusKm : appConfig.getNotificationRadiusKm();

        List<Donation> donations = donationRepository.findNearbyAvailableDonations(lat, lng, radius);

        return donations.stream()
                .map(d -> {
                    double distance = calculateDistance(lat, lng,
                            d.getLatitude().doubleValue(), d.getLongitude().doubleValue());
                    return donationMapper.toResponse(d, Math.round(distance * 100.0) / 100.0);
                })
                .toList();
    }

    /**
     * Accept a donation (NGO only). Race-safe using database unique constraint.
     * Only one NGO can accept a given donation.
     */
    @Transactional
    public DonationClaimResponse acceptDonation(Long userId, Long donationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.NGO) {
            throw new UnauthorizedActionException("Only NGOs can accept donations");
        }

        NgoProfile ngo = ngoProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found"));

        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found: " + donationId));

        // Validate donation can be accepted
        if (donation.getStatus() != DonationStatus.AVAILABLE) {
            throw new DonationNotAvailableException(
                    "Donation is not available for acceptance. Current status: " + donation.getStatus());
        }

        if (donation.getPickupDeadline().isBefore(LocalDateTime.now())) {
            throw new DonationNotAvailableException("Donation pickup deadline has passed");
        }

        // Check that restaurant owner is not also the NGO
        if (donation.getRestaurant().getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("A restaurant cannot accept its own donation");
        }

        // Check if already claimed (defensive — unique constraint also enforces this)
        if (donationClaimRepository.existsByDonationId(donationId)) {
            throw new ConflictException("This donation has already been claimed by another NGO");
        }

        // Atomically update donation status using conditional update
        // This is race-safe: only one thread can change AVAILABLE → ACCEPTED
        int updated = donationRepository.updateStatusAtomically(donationId, DonationStatus.AVAILABLE, DonationStatus.ACCEPTED);
        if (updated == 0) {
            throw new ConflictException("This donation has already been claimed by another NGO");
        }

        // Create claim record (unique constraint on donation_id prevents duplicates)
        DonationClaim claim;
        try {
            claim = DonationClaim.builder()
                    .donation(donation)
                    .ngo(ngo)
                    .acceptedAt(LocalDateTime.now())
                    .pickupStatus(PickupStatus.ASSIGNED)
                    .build();
            claim = donationClaimRepository.save(claim);
        } catch (Exception e) {
            // If claim creation fails due to unique constraint violation, another NGO won the race
            throw new ConflictException("This donation has already been claimed by another NGO");
        }

        log.info("Donation {} accepted by NGO {} (user {})", donationId, ngo.getNgoName(), userId);

        // Notify restaurant that donation was accepted
        try {
            notificationService.notifyDonationAccepted(donation, ngo);
        } catch (Exception e) {
            log.error("Failed to notify restaurant about acceptance: {}", e.getMessage());
        }

        return donationClaimMapper.toResponse(claim);
    }

    /**
     * Cancel a donation (RESTAURANT owner only).
     */
    @Transactional
    public DonationResponse cancelDonation(Long userId, Long donationId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found: " + donationId));

        // Only the restaurant owner can cancel
        if (!donation.getRestaurant().getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You can only cancel your own donations");
        }

        if (donation.getStatus() == DonationStatus.COMPLETED || donation.getStatus() == DonationStatus.COLLECTED) {
            throw new BadRequestException("Cannot cancel a completed or collected donation");
        }

        donation.setStatus(DonationStatus.CANCELLED);
        donation = donationRepository.save(donation);

        log.info("Donation {} cancelled by restaurant user {}", donationId, userId);

        // Notify assigned NGO if any
        if (donation.getClaim() != null) {
            try {
                notificationService.notifyDonationCancelled(donation);
            } catch (Exception e) {
                log.error("Failed to notify NGO about cancellation: {}", e.getMessage());
            }
        }

        return donationMapper.toResponse(donation);
    }

    /**
     * Mark donation as collected (NGO who accepted it only).
     */
    @Transactional
    public DonationResponse markCollected(Long userId, Long donationId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found: " + donationId));

        DonationClaim claim = donationClaimRepository.findByDonationId(donationId)
                .orElseThrow(() -> new BadRequestException("No claim found for this donation"));

        // Only the assigned NGO can mark as collected
        if (!claim.getNgo().getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("Only the assigned NGO can mark this donation as collected");
        }

        if (donation.getStatus() != DonationStatus.ACCEPTED) {
            throw new BadRequestException("Donation must be in ACCEPTED status to mark as collected. Current: " + donation.getStatus());
        }

        donation.setStatus(DonationStatus.COLLECTED);
        donationRepository.save(donation);

        claim.setPickupStatus(PickupStatus.COLLECTED);
        claim.setCollectedAt(LocalDateTime.now());
        donationClaimRepository.save(claim);

        log.info("Donation {} marked as collected by NGO user {}", donationId, userId);

        try {
            notificationService.notifyDonationCollected(donation, claim);
        } catch (Exception e) {
            log.error("Failed to send collected notification: {}", e.getMessage());
        }

        return donationMapper.toResponse(donation);
    }

    /**
     * Mark donation as completed (NGO or restaurant owner).
     */
    @Transactional
    public DonationResponse markCompleted(Long userId, Long donationId) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found: " + donationId));

        DonationClaim claim = donationClaimRepository.findByDonationId(donationId)
                .orElseThrow(() -> new BadRequestException("No claim found for this donation"));

        // Either the assigned NGO or the restaurant owner can complete
        boolean isAssignedNgo = claim.getNgo().getUser().getId().equals(userId);
        boolean isRestaurantOwner = donation.getRestaurant().getUser().getId().equals(userId);

        if (!isAssignedNgo && !isRestaurantOwner) {
            throw new UnauthorizedActionException("Only the assigned NGO or restaurant owner can complete this donation");
        }

        if (donation.getStatus() != DonationStatus.COLLECTED && donation.getStatus() != DonationStatus.ACCEPTED) {
            throw new BadRequestException("Donation must be ACCEPTED or COLLECTED to complete. Current: " + donation.getStatus());
        }

        donation.setStatus(DonationStatus.COMPLETED);
        donationRepository.save(donation);

        claim.setPickupStatus(PickupStatus.COMPLETED);
        claim.setCompletedAt(LocalDateTime.now());
        donationClaimRepository.save(claim);

        log.info("Donation {} marked as completed by user {}", donationId, userId);

        try {
            notificationService.notifyDonationCompleted(donation, claim);
        } catch (Exception e) {
            log.error("Failed to send completed notification: {}", e.getMessage());
        }

        return donationMapper.toResponse(donation);
    }

    /**
     * Update a donation (RESTAURANT owner only, only if still AVAILABLE).
     */
    @Transactional
    public DonationResponse updateDonation(Long userId, Long donationId, UpdateDonationRequest request) {
        Donation donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation not found: " + donationId));

        if (!donation.getRestaurant().getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You can only update your own donations");
        }

        if (donation.getStatus() != DonationStatus.AVAILABLE) {
            throw new BadRequestException("Can only update AVAILABLE donations. Current: " + donation.getStatus());
        }

        // Update non-null fields
        if (request.getFoodName() != null) donation.setFoodName(request.getFoodName());
        if (request.getDescription() != null) donation.setDescription(request.getDescription());
        if (request.getFoodCategory() != null) donation.setFoodCategory(request.getFoodCategory());
        if (request.getQuantity() != null) donation.setQuantity(request.getQuantity());
        if (request.getQuantityUnit() != null) donation.setQuantityUnit(request.getQuantityUnit());
        if (request.getImageUrl() != null) donation.setImageUrl(request.getImageUrl());
        if (request.getPreparedAt() != null) donation.setPreparedAt(request.getPreparedAt());
        if (request.getAvailableUntil() != null) donation.setAvailableUntil(request.getAvailableUntil());
        if (request.getPickupDeadline() != null) donation.setPickupDeadline(request.getPickupDeadline());
        if (request.getStorageInstructions() != null) donation.setStorageInstructions(request.getStorageInstructions());
        if (request.getLatitude() != null) donation.setLatitude(BigDecimal.valueOf(request.getLatitude()));
        if (request.getLongitude() != null) donation.setLongitude(BigDecimal.valueOf(request.getLongitude()));
        if (request.getAddress() != null) donation.setAddress(request.getAddress());

        donation = donationRepository.save(donation);
        return donationMapper.toResponse(donation);
    }

    /**
     * Get pickups assigned to an NGO.
     */
    @Transactional(readOnly = true)
    public List<DonationClaimResponse> getNgoPickups(Long userId, PickupStatus status) {
        NgoProfile ngo = ngoProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found"));

        List<DonationClaim> claims;
        if (status != null) {
            claims = donationClaimRepository.findByNgoIdAndPickupStatus(ngo.getId(), status);
        } else {
            claims = donationClaimRepository.findByNgoId(ngo.getId());
        }

        return claims.stream()
                .map(donationClaimMapper::toResponse)
                .toList();
    }

    /**
     * Expire overdue donations. Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void expireOverdueDonations() {
        List<Donation> expired = donationRepository.findExpiredDonations(LocalDateTime.now());
        for (Donation d : expired) {
            d.setStatus(DonationStatus.EXPIRED);
            donationRepository.save(d);
            log.info("Donation {} expired (deadline was {})", d.getId(), d.getPickupDeadline());
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} overdue donations", expired.size());
        }
    }

    /**
     * Calculate distance between two points using the Haversine formula.
     *
     * @return distance in kilometers
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
