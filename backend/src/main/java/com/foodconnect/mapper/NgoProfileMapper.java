package com.foodconnect.mapper;

import com.foodconnect.dto.NgoProfileResponse;
import com.foodconnect.entity.NgoProfile;
import org.springframework.stereotype.Component;

/**
 * Maps NgoProfile entities to NgoProfileResponse DTOs.
 */
@Component
public class NgoProfileMapper {

    public NgoProfileResponse toResponse(NgoProfile profile) {
        if (profile == null) {
            return null;
        }
        return NgoProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser() != null ? profile.getUser().getId() : null)
                .ngoName(profile.getNgoName())
                .address(profile.getAddress())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .registrationNumber(profile.getRegistrationNumber())
                .profileImageUrl(profile.getProfileImageUrl())
                .verificationStatus(profile.getVerificationStatus())
                .build();
    }
}
