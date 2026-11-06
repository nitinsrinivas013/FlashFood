package com.foodconnect.mapper;

import com.foodconnect.dto.RestaurantProfileResponse;
import com.foodconnect.entity.RestaurantProfile;
import org.springframework.stereotype.Component;

/**
 * Maps RestaurantProfile entities to RestaurantProfileResponse DTOs.
 */
@Component
public class RestaurantProfileMapper {

    public RestaurantProfileResponse toResponse(RestaurantProfile profile) {
        if (profile == null) {
            return null;
        }
        return RestaurantProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser() != null ? profile.getUser().getId() : null)
                .restaurantName(profile.getRestaurantName())
                .address(profile.getAddress())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .profileImageUrl(profile.getProfileImageUrl())
                .verificationStatus(profile.getVerificationStatus())
                .build();
    }
}
