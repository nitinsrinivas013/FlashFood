package com.foodconnect.service;

import com.foodconnect.dto.*;
import com.foodconnect.entity.NgoProfile;
import com.foodconnect.entity.RestaurantProfile;
import com.foodconnect.entity.User;
import com.foodconnect.entity.enums.Role;
import com.foodconnect.exception.ResourceNotFoundException;
import com.foodconnect.exception.UnauthorizedActionException;
import com.foodconnect.mapper.NgoProfileMapper;
import com.foodconnect.mapper.RestaurantProfileMapper;
import com.foodconnect.repository.NgoProfileRepository;
import com.foodconnect.repository.RestaurantProfileRepository;
import com.foodconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for managing restaurant and NGO profile operations.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final RestaurantProfileRepository restaurantProfileRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final RestaurantProfileMapper restaurantProfileMapper;
    private final NgoProfileMapper ngoProfileMapper;

    /**
     * Get the current user's restaurant profile.
     */
    @Transactional(readOnly = true)
    public RestaurantProfileResponse getMyRestaurantProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.RESTAURANT) {
            throw new UnauthorizedActionException("Only restaurant users can access restaurant profiles");
        }
        RestaurantProfile profile = restaurantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant profile not found"));
        return restaurantProfileMapper.toResponse(profile);
    }

    /**
     * Update the current user's restaurant profile.
     */
    @Transactional
    public RestaurantProfileResponse updateMyRestaurantProfile(Long userId, UpdateRestaurantProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.RESTAURANT) {
            throw new UnauthorizedActionException("Only restaurant users can update restaurant profiles");
        }
        RestaurantProfile profile = restaurantProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant profile not found"));

        if (request.getRestaurantName() != null) profile.setRestaurantName(request.getRestaurantName());
        if (request.getAddress() != null) profile.setAddress(request.getAddress());
        if (request.getLatitude() != null) profile.setLatitude(BigDecimal.valueOf(request.getLatitude()));
        if (request.getLongitude() != null) profile.setLongitude(BigDecimal.valueOf(request.getLongitude()));

        profile = restaurantProfileRepository.save(profile);
        return restaurantProfileMapper.toResponse(profile);
    }

    /**
     * Get a restaurant profile by ID (public).
     */
    @Transactional(readOnly = true)
    public RestaurantProfileResponse getRestaurantProfile(Long profileId) {
        RestaurantProfile profile = restaurantProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant profile not found: " + profileId));
        return restaurantProfileMapper.toResponse(profile);
    }

    /**
     * Get the current user's NGO profile.
     */
    @Transactional(readOnly = true)
    public NgoProfileResponse getMyNgoProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.NGO) {
            throw new UnauthorizedActionException("Only NGO users can access NGO profiles");
        }
        NgoProfile profile = ngoProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found"));
        return ngoProfileMapper.toResponse(profile);
    }

    /**
     * Update the current user's NGO profile.
     */
    @Transactional
    public NgoProfileResponse updateMyNgoProfile(Long userId, UpdateNgoProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.NGO) {
            throw new UnauthorizedActionException("Only NGO users can update NGO profiles");
        }
        NgoProfile profile = ngoProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found"));

        if (request.getNgoName() != null) profile.setNgoName(request.getNgoName());
        if (request.getAddress() != null) profile.setAddress(request.getAddress());
        if (request.getLatitude() != null) profile.setLatitude(BigDecimal.valueOf(request.getLatitude()));
        if (request.getLongitude() != null) profile.setLongitude(BigDecimal.valueOf(request.getLongitude()));
        if (request.getRegistrationNumber() != null) profile.setRegistrationNumber(request.getRegistrationNumber());

        profile = ngoProfileRepository.save(profile);
        return ngoProfileMapper.toResponse(profile);
    }

    /**
     * Get an NGO profile by ID (public).
     */
    @Transactional(readOnly = true)
    public NgoProfileResponse getNgoProfile(Long profileId) {
        NgoProfile profile = ngoProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("NGO profile not found: " + profileId));
        return ngoProfileMapper.toResponse(profile);
    }
}
