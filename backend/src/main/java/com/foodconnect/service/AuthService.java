package com.foodconnect.service;

import com.foodconnect.dto.*;
import com.foodconnect.entity.NgoProfile;
import com.foodconnect.entity.RestaurantProfile;
import com.foodconnect.entity.User;
import com.foodconnect.entity.enums.Role;
import com.foodconnect.exception.BadRequestException;
import com.foodconnect.exception.DuplicateResourceException;
import com.foodconnect.exception.ResourceNotFoundException;
import com.foodconnect.mapper.UserMapper;
import com.foodconnect.repository.NgoProfileRepository;
import com.foodconnect.repository.RestaurantProfileRepository;
import com.foodconnect.repository.UserRepository;
import com.foodconnect.security.CustomUserDetails;
import com.foodconnect.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles user registration, login, and authentication.
 * Creates role-specific profiles during registration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RestaurantProfileRepository restaurantProfileRepository;
    private final NgoProfileRepository ngoProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;

    /**
     * Register a new user with role-specific profile.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        // Validate role-specific fields
        validateRoleFields(request);

        // Create user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .phone(request.getPhone())
                .build();
        user = userRepository.save(user);

        // Create role-specific profile
        if (request.getRole() == Role.RESTAURANT) {
            RestaurantProfile profile = RestaurantProfile.builder()
                    .user(user)
                    .restaurantName(request.getRestaurantName())
                    .address(request.getAddress())
                    .latitude(request.getLatitude() != null ? java.math.BigDecimal.valueOf(request.getLatitude()) : null)
                    .longitude(request.getLongitude() != null ? java.math.BigDecimal.valueOf(request.getLongitude()) : null)
                    .build();
            restaurantProfileRepository.save(profile);
        } else if (request.getRole() == Role.NGO) {
            NgoProfile profile = NgoProfile.builder()
                    .user(user)
                    .ngoName(request.getNgoName())
                    .address(request.getAddress())
                    .latitude(request.getLatitude() != null ? java.math.BigDecimal.valueOf(request.getLatitude()) : null)
                    .longitude(request.getLongitude() != null ? java.math.BigDecimal.valueOf(request.getLongitude()) : null)
                    .registrationNumber(request.getRegistrationNumber())
                    .build();
            ngoProfileRepository.save(profile);
        }

        log.info("User registered successfully: {} with role {}", user.getEmail(), user.getRole());

        // Authenticate and generate token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        String token = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userMapper.toUserResponse(user))
                .role(user.getRole())
                .build();
    }

    /**
     * Authenticate and log in a user.
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = tokenProvider.generateTokenForUser(userDetails);

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(userMapper.toUserResponse(user))
                .role(user.getRole())
                .build();
    }

    /**
     * Get current authenticated user info.
     */
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toUserResponse(user);
    }

    private void validateRoleFields(RegisterRequest request) {
        if (request.getRole() == Role.RESTAURANT) {
            if (request.getRestaurantName() == null || request.getRestaurantName().isBlank()) {
                throw new BadRequestException("Restaurant name is required for restaurant registration");
            }
        } else if (request.getRole() == Role.NGO) {
            if (request.getNgoName() == null || request.getNgoName().isBlank()) {
                throw new BadRequestException("NGO name is required for NGO registration");
            }
        }
    }
}
